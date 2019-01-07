package edu.illinois.library.cantaloupe.source;

import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import edu.illinois.library.cantaloupe.http.Range;
import edu.illinois.library.cantaloupe.http.Response;
import edu.illinois.library.cantaloupe.source.stream.HTTPImageInputStreamClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Implementation backed by an Azure Storage client.
 */
public class AzureStorageHTTPImageInputStreamClient
        implements HTTPImageInputStreamClient {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(AzureStorageHTTPImageInputStreamClient.class);

    private CloudBlockBlob blob;

    AzureStorageHTTPImageInputStreamClient(CloudBlockBlob blob) {
        this.blob = blob;
    }

    @Override
    public Response sendHEADRequest() throws IOException {
        try {
            blob.exists(); // will send the HEAD request if it hasn't been done already
            final Response response = new Response();
            response.setStatus(200);
            response.getHeaders().set("Content-Length",
                    Long.toString(blob.getProperties().getLength()));
            response.getHeaders().set("Accept-Ranges", "bytes");
            return response;
        } catch (StorageException e) {
            LOGGER.error(e.getMessage(), e);
            throw new IOException(e);
        }
    }

    @Override
    public Response sendGETRequest(Range range) throws IOException {
        try {
            final long length = range.end - range.start + 1;
            final byte[] bytes = new byte[(int) length];
            blob.downloadRangeToByteArray(range.start, length, bytes, 0);

            final Response response = new Response();
            response.setStatus(206);
            response.setBody(bytes);
            return response;
        } catch (StorageException e) {
            LOGGER.error(e.getMessage(), e);
            throw new IOException(e);
        }
    }

}
