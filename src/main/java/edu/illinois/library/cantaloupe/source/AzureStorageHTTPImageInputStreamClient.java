package edu.illinois.library.cantaloupe.source;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.models.BlobProperties;
import com.azure.storage.blob.models.BlobRange;
import edu.illinois.library.cantaloupe.http.Range;
import edu.illinois.library.cantaloupe.http.Response;
import edu.illinois.library.cantaloupe.source.stream.HTTPImageInputStreamClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.ByteArrayOutputStream;

/**
 * Implementation backed by an Azure Storage client.
 */
public class AzureStorageHTTPImageInputStreamClient
        implements HTTPImageInputStreamClient {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(AzureStorageHTTPImageInputStreamClient.class);

    private final BlobClient blob;

    AzureStorageHTTPImageInputStreamClient(BlobClient blob) {
        this.blob = blob;
    }

    @Override
    public Response sendHEADRequest() throws IOException {
        try {
            BlobProperties properties = blob.getProperties();
            final Response response = new Response();
            response.setStatus(200);
            response.getHeaders().set("Content-Length",
                    Long.toString(properties.getBlobSize()));
            response.getHeaders().set("Accept-Ranges", "bytes");
            return response;
        } catch (RuntimeException e) {
            LOGGER.error(e.getMessage(), e);
            throw new IOException(e);
        }
    }

    @Override
    public Response sendGETRequest(Range range) throws IOException {
        try {
            final long length = range.end - range.start + 1;
            final ByteArrayOutputStream body =
                    new ByteArrayOutputStream((int) length);
            blob.downloadStreamWithResponse(body,
                    new BlobRange(range.start, length), null, null,
                    false, null, null);

            final Response response = new Response();
            response.setStatus(206);
            response.setBody(body.toByteArray());
            return response;
        } catch (RuntimeException e) {
            LOGGER.error(e.getMessage(), e);
            throw new IOException(e);
        }
    }

}
