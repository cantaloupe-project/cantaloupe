package edu.illinois.library.cantaloupe.test;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.options.BlockBlobOutputStreamOptions;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public final class AzureStorageTestUtil {

    public static final String OBJECT_KEY_WITH_CONTENT_TYPE_AND_RECOGNIZED_EXTENSION      = "jpeg.jpg";
    public static final String OBJECT_KEY_WITH_CONTENT_TYPE_AND_UNRECOGNIZED_EXTENSION    = "jpeg.unknown";
    public static final String OBJECT_KEY_WITH_CONTENT_TYPE_BUT_NO_EXTENSION              = "jpg";
    public static final String OBJECT_KEY_WITH_NO_CONTENT_TYPE_AND_RECOGNIZED_EXTENSION   = "jpeg.jpg";
    public static final String OBJECT_KEY_WITH_NO_CONTENT_TYPE_AND_UNRECOGNIZED_EXTENSION = "jpeg.unknown";
    public static final String OBJECT_KEY_WITH_NO_CONTENT_TYPE_AND_INCORRECT_EXTENSION    = "jpeg.png";
    public static final String OBJECT_KEY_WITH_NO_CONTENT_TYPE_OR_EXTENSION               = "jpg";
    public static final String NON_IMAGE_KEY                                              = "NotAnImage";

    public static void uploadFixtures() throws Exception {
        final BlobServiceClient client = client();
        final BlobContainerClient container =
                client.getBlobContainerClient(getContainer());
        container.createIfNotExists();

        Path fixture = TestUtil.getImage("jpg");

        for (final String key : new String[] {
                OBJECT_KEY_WITH_CONTENT_TYPE_AND_RECOGNIZED_EXTENSION,
                OBJECT_KEY_WITH_CONTENT_TYPE_AND_UNRECOGNIZED_EXTENSION,
                OBJECT_KEY_WITH_CONTENT_TYPE_BUT_NO_EXTENSION,
                OBJECT_KEY_WITH_NO_CONTENT_TYPE_AND_RECOGNIZED_EXTENSION,
                OBJECT_KEY_WITH_NO_CONTENT_TYPE_AND_UNRECOGNIZED_EXTENSION,
                OBJECT_KEY_WITH_NO_CONTENT_TYPE_AND_INCORRECT_EXTENSION,
                OBJECT_KEY_WITH_NO_CONTENT_TYPE_OR_EXTENSION}) {
            final BlobClient blob = container.getBlobClient(key);
            BlobHttpHeaders headers = null;

            if (!OBJECT_KEY_WITH_NO_CONTENT_TYPE_AND_RECOGNIZED_EXTENSION.equals(key) &&
                    !OBJECT_KEY_WITH_NO_CONTENT_TYPE_AND_UNRECOGNIZED_EXTENSION.equals(key) &&
                    !OBJECT_KEY_WITH_NO_CONTENT_TYPE_AND_INCORRECT_EXTENSION.equals(key) &&
                    !OBJECT_KEY_WITH_NO_CONTENT_TYPE_OR_EXTENSION.equals(key)) {
                headers = new BlobHttpHeaders().setContentType("image/jpeg");
            }

            try (OutputStream os = blob.getBlockBlobClient().getBlobOutputStream(
                    new BlockBlobOutputStreamOptions().setHeaders(headers))) {
                Files.copy(fixture, os);
            }
        }

        // Add a non-image
        fixture = TestUtil.getImage("text.txt");
        final BlobClient blob = container.getBlobClient(NON_IMAGE_KEY);
        try (OutputStream os = blob.getBlockBlobClient().getBlobOutputStream()) {
            Files.copy(fixture, os);
        }
    }

    public static void deleteFixtures() throws Exception {
        final BlobServiceClient client = client();
        final BlobContainerClient container =
                client.getBlobContainerClient(getContainer());

        for (BlobItem item : container.listBlobs()) {
            container.getBlobClient(item.getName()).deleteIfExists();
        }
    }

    public static BlobServiceClient client() {
        final String accountName = getAccountName();
        final String accountKey  = getAccountKey();

        final String connectionString = String.format(
                "DefaultEndpointsProtocol=https;" +
                        "AccountName=%s;" +
                        "AccountKey=%s", accountName, accountKey);
        BlobServiceClient client = new BlobServiceClientBuilder().
                connectionString(connectionString).buildClient();
        client.getBlobContainerClient(getContainer()).createIfNotExists();
        return client;
    }

    public static String getAccountName() {
        org.apache.commons.configuration2.Configuration testConfig =
                TestUtil.getTestConfig();
        return testConfig.getString(ConfigurationConstants.AZURE_ACCOUNT_NAME.getKey());
    }

    public static String getAccountKey() {
        org.apache.commons.configuration2.Configuration testConfig =
                TestUtil.getTestConfig();
        return testConfig.getString(ConfigurationConstants.AZURE_ACCOUNT_KEY.getKey());
    }

    public static String getContainer() {
        org.apache.commons.configuration2.Configuration testConfig =
                TestUtil.getTestConfig();
        return testConfig.getString(ConfigurationConstants.AZURE_CONTAINER.getKey());
    }

    private AzureStorageTestUtil() {}

}
