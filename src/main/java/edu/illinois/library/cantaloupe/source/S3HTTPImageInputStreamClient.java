package edu.illinois.library.cantaloupe.source;

import edu.illinois.library.cantaloupe.http.Range;
import edu.illinois.library.cantaloupe.http.Response;
import edu.illinois.library.cantaloupe.source.stream.HTTPImageInputStreamClient;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AccessDeniedException;
import java.nio.file.NoSuchFileException;

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
        final S3Client client = S3Source.getClientInstance();
        final String bucket   = objectInfo.getBucketName();
        final String key      = objectInfo.getKey();
        try {
            final HeadObjectResponse headResponse =
                    client.headObject(HeadObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .build());
            final Response response = new Response();
            response.setStatus(200);
            response.getHeaders().set("Content-Length",
                    Long.toString(headResponse.contentLength()));
            response.getHeaders().set("Accept-Ranges", "bytes");
            return response;
        } catch (NoSuchBucketException | NoSuchKeyException e) {
            throw new NoSuchFileException(objectInfo.toString());
        } catch (S3Exception e) {
            final int code = e.statusCode();
            if (code == 403) {
                throw new AccessDeniedException(objectInfo.toString());
            } else {
                LOGGER.error(e.getMessage(), e);
                throw new IOException(e);
            }
        } catch (SdkClientException e) {
            LOGGER.error(e.getMessage(), e);
            throw new IOException(objectInfo.toString(), e);
        }
    }

    @Override
    public Response sendGETRequest(Range range) throws IOException {
        try (InputStream is = S3Source.newObjectInputStream(objectInfo, range)) {
            final Response response = new Response();
            response.setStatus(206);
            response.setBody(IOUtils.toByteArray(is));
            return response;
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            throw new IOException(objectInfo.toString(), e);
        }
    }

}
