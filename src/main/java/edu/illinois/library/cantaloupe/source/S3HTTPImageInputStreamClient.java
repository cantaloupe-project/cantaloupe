package edu.illinois.library.cantaloupe.source;

import com.amazonaws.SdkBaseException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.ObjectMetadata;
import edu.illinois.library.cantaloupe.http.Range;
import edu.illinois.library.cantaloupe.http.Response;
import edu.illinois.library.cantaloupe.source.stream.HTTPImageInputStreamClient;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.NoSuchFileException;

/**
 * Implementation backed by an AWS S3 client.
 */
class S3HTTPImageInputStreamClient implements HTTPImageInputStreamClient {

    private S3ObjectInfo objectInfo;

    S3HTTPImageInputStreamClient(S3ObjectInfo objectInfo) {
        this.objectInfo = objectInfo;
    }

    @Override
    public Response sendHEADRequest() throws IOException {
        final AmazonS3 s3   = S3Source.getClientInstance();
        final String bucket = objectInfo.getBucketName();
        final String key    = objectInfo.getKey();
        try {
            final ObjectMetadata metadata = s3.getObjectMetadata(bucket, key);
            final Response response = new Response();
            response.setStatus(200);
            response.getHeaders().set("Content-Length",
                    Long.toString(metadata.getContentLength()));
            response.getHeaders().set("Accept-Ranges", "bytes");
            return response;
        } catch (AmazonS3Exception e) {
            if (e.getStatusCode() == 404) {
                throw new NoSuchFileException(bucket + "/" + key);
            } else {
                throw new IOException(e);
            }
        } catch (SdkBaseException e) {
            throw new IOException(e);
        }
    }

    @Override
    public Response sendGETRequest(Range range) throws IOException {
        final String bucket = objectInfo.getBucketName();
        final String key    = objectInfo.getKey();
        try (InputStream is = S3Source.fetchObjectContent(
                objectInfo, range.start, range.end)) {
            final byte[] body = IOUtils.toByteArray(is);

            final Response response = new Response();
            response.setStatus(206);
            response.setBody(body);
            return response;
        } catch (AmazonS3Exception e) {
            if (e.getStatusCode() == 404) {
                throw new NoSuchFileException(bucket + "/" + key);
            } else {
                throw new IOException(e);
            }
        } catch (SdkBaseException e) {
            throw new IOException(e);
        }
    }

}
