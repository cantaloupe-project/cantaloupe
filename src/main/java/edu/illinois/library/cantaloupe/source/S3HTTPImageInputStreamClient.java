package edu.illinois.library.cantaloupe.source;

import edu.illinois.library.cantaloupe.http.Range;
import edu.illinois.library.cantaloupe.http.Response;
import edu.illinois.library.cantaloupe.source.stream.HTTPImageInputStreamClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AccessDeniedException;
import java.nio.file.NoSuchFileException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * Implementation backed by an AWS S3 client.
 */
class S3HTTPImageInputStreamClient implements HTTPImageInputStreamClient {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(S3HTTPImageInputStreamClient.class);

    private final S3ObjectInfo objectInfo;

    S3HTTPImageInputStreamClient(S3ObjectInfo objectInfo) {
        this.objectInfo = objectInfo;
    }

    @Override
    public Response sendHEADRequest() throws IOException {
        final S3AsyncClient client = S3Source.getClientInstance(objectInfo);
        final String bucket        = objectInfo.getBucketName();
        final String key           = objectInfo.getKey();
        try {
            final HeadObjectResponse headResponse =
                    client.headObject(HeadObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .build()).join();
            final Response response = new Response();
            response.setStatus(200);
            response.getHeaders().set("Content-Length",
                    Long.toString(headResponse.contentLength()));
            response.getHeaders().set("Accept-Ranges", "bytes");
            return response;
        } catch (CompletionException e) {
            final Throwable cause = e.getCause();
            if (cause instanceof NoSuchBucketException || cause instanceof NoSuchKeyException) {
                throw new NoSuchFileException(objectInfo.toString());
            } else if (cause instanceof S3Exception s3e) {
                final int code = s3e.statusCode();
                if (code == 403) {
                    throw new AccessDeniedException(objectInfo.toString());
                } else {
                    LOGGER.error(s3e.getMessage(), s3e);
                    throw new IOException(s3e);
                }
            } else if (cause instanceof SdkClientException sdkE) {
                LOGGER.error(sdkE.getMessage(), sdkE);
                throw new IOException(objectInfo.toString(), sdkE);
            }
            throw new IOException(objectInfo.toString(), e);
        }
    }

    @Override
    public Response sendGETRequest(Range range) throws IOException {
        try (InputStream is = new BufferedInputStream(
                S3Source.newObjectInputStream(objectInfo, range));
             ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            final Response response = new Response();
            response.setStatus(206);
            is.transferTo(os);
            response.setBody(os.toByteArray());
            return response;
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            throw new IOException(objectInfo.toString(), e);
        }
    }

    @Override
    public CompletableFuture<Response> sendGETRequestAsync(Range range) {
        final S3AsyncClient client = S3Source.getClientInstance(objectInfo);
        final GetObjectRequest request = GetObjectRequest.builder()
                .bucket(objectInfo.getBucketName())
                .key(objectInfo.getKey())
                .range("bytes=" + range.start + "-" + range.end)
                .build();
        return client.getObject(request, AsyncResponseTransformer.toBytes())
                .thenApply(bytes -> {
                    final Response response = new Response();
                    response.setStatus(206);
                    response.setBody(bytes.asByteArray());
                    return response;
                });
    }

}
