package edu.illinois.library.cantaloupe.test;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.blob.CloudBlob;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.microsoft.azure.storage.blob.ListBlobItem;

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
        final CloudBlobClient client = client();
        final CloudBlobContainer container =
                client.getContainerReference(getContainer());
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
            final CloudBlockBlob blob = container.getBlockBlobReference(key);

            if (!OBJECT_KEY_WITH_NO_CONTENT_TYPE_AND_RECOGNIZED_EXTENSION.equals(key) &&
                    !OBJECT_KEY_WITH_NO_CONTENT_TYPE_AND_UNRECOGNIZED_EXTENSION.equals(key) &&
                    !OBJECT_KEY_WITH_NO_CONTENT_TYPE_AND_INCORRECT_EXTENSION.equals(key) &&
                    !OBJECT_KEY_WITH_NO_CONTENT_TYPE_OR_EXTENSION.equals(key)) {
                blob.getProperties().setContentType("image/jpeg");
            }

            try (OutputStream os = blob.openOutputStream()) {
                Files.copy(fixture, os);
            }
        }

        // Add a non-image
        fixture = TestUtil.getImage("text.txt");
        final CloudBlockBlob blob = container.getBlockBlobReference(NON_IMAGE_KEY);
        try (OutputStream os = blob.openOutputStream()) {
            Files.copy(fixture, os);
        }
    }

    public static void deleteFixtures() throws Exception {
        final CloudBlobClient client = client();
        final CloudBlobContainer container =
                client.getContainerReference(getContainer());

        for (ListBlobItem item : container.listBlobs()) {
            if (item instanceof CloudBlob) {
                ((CloudBlob) item).deleteIfExists();
            }
        }
    }

    public static CloudBlobClient client() throws Exception {
        final String accountName = getAccountName();
        final String accountKey  = getAccountKey();

        final String connectionString = String.format(
                "DefaultEndpointsProtocol=https;" +
                        "AccountName=%s;" +
                        "AccountKey=%s", accountName, accountKey);
        final CloudStorageAccount account =
                CloudStorageAccount.parse(connectionString);
        CloudBlobClient client = account.createCloudBlobClient();
        client.getContainerReference(getContainer()).createIfNotExists();
        return client;
    }

    public static String getAccountName() {
        org.apache.commons.configuration.Configuration testConfig =
                TestUtil.getTestConfig();
        return testConfig.getString(ConfigurationConstants.AZURE_ACCOUNT_NAME.getKey());
    }

    public static String getAccountKey() {
        org.apache.commons.configuration.Configuration testConfig =
                TestUtil.getTestConfig();
        return testConfig.getString(ConfigurationConstants.AZURE_ACCOUNT_KEY.getKey());
    }

    public static String getContainer() {
        org.apache.commons.configuration.Configuration testConfig =
                TestUtil.getTestConfig();
        return testConfig.getString(ConfigurationConstants.AZURE_CONTAINER.getKey());
    }

    private AzureStorageTestUtil() {}

}
