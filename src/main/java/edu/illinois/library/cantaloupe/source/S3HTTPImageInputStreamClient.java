package edu.illinois.library.cantaloupe.source;

import edu.illinois.library.cantaloupe.http.Range;
import edu.illinois.library.cantaloupe.http.Response;
import edu.illinois.library.cantaloupe.source.stream.HTTPImageInputStreamClient;
import io.minio.MinioClient;
import io.minio.ObjectStat;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InvalidBucketNameException;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AccessDeniedException;
import java.nio.file.NoSuchFileException;
import java.security.InvalidKeyException;

/**
 * Implementation backed by an AWS S3 client.
 */
class S3HTTPImageInputStreamClient implements HTTPImageInputStreamClient {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(S3HTTPImageInputStreamClient.class);

    private S3ObjectInfo objectInfo;

    S3HTTPImageInputStreamClient(S3ObjectInfo objectInfo) {
        this.objectInfo = objectInfo;
    }

    @Override
    public Response sendHEADRequest() throws IOException {
        final MinioClient mc = S3Source.getClientInstance();
        final String bucket  = objectInfo.getBucketName();
        final String key     = objectInfo.getKey();
        try {
            final ObjectStat stat = mc.statObject(bucket, key);
            final Response response = new Response();
            response.setStatus(200);
            response.getHeaders().set("Content-Length",
                    Long.toString(stat.length()));
            response.getHeaders().set("Accept-Ranges", "bytes");
            return response;
        } catch (InvalidBucketNameException | InvalidKeyException e) {
            throw new NoSuchFileException(objectInfo.toString());
        } catch (ErrorResponseException e) {
            final String code = e.errorResponse().code();
            if ("NoSuchBucket".equals(code) || "NoSuchKey".equals(code)) {
                throw new NoSuchFileException(objectInfo.toString());
            } else if ("AccessDenied".equals(code) ||
                    "AllAccessDisabled".equals(code)) {
                throw new AccessDeniedException(objectInfo.toString());
            } else {
                LOGGER.error(e.getMessage(), e);
                throw new IOException(e);
            }
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
            throw e;
        } catch (Exception e) {
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
