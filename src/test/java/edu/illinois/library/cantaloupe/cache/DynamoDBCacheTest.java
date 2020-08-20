package edu.illinois.library.cantaloupe.cache;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.ScanSpec;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.DeleteTableRequest;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ResourceInUseException;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.image.Metadata;
import edu.illinois.library.cantaloupe.operation.Encode;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.ConfigurationConstants;
import edu.illinois.library.cantaloupe.test.TestUtil;
import edu.illinois.library.cantaloupe.util.AWSClientBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.util.Optional;

import static edu.illinois.library.cantaloupe.cache.DynamoDBCache.EXPIRES_COLUMN;
import static edu.illinois.library.cantaloupe.cache.DynamoDBCache.PRIMARY_KEY_COLUMN;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test of {@link DynamoDBCache}. Can run against AWS DynamoDB, or
 * <a href="https://hub.docker.com/r/amazon/dynamodb-local/">DynamoDB Local</a>:
 *
 * {@code docker run -p 8000:8000 amazon/dynamodb-local}
 */
public class DynamoDBCacheTest extends AbstractCacheTest {

    private static final long READ_THROUGHPUT  = 100L;
    private static final long WRITE_THROUGHPUT = 100L;

    private static AmazonDynamoDB client;

    private static String getAccessKeyID() {
        org.apache.commons.configuration.Configuration testConfig =
                TestUtil.getTestConfig();
        return testConfig.getString(ConfigurationConstants.DYNAMODB_ACCESS_KEY_ID.getKey());
    }

    private static String getEndpointURI() {
        org.apache.commons.configuration.Configuration testConfig =
                TestUtil.getTestConfig();
        return testConfig.getString(ConfigurationConstants.DYNAMODB_ENDPOINT.getKey());
    }

    private static String getSecretAccessKey() {
        org.apache.commons.configuration.Configuration testConfig =
                TestUtil.getTestConfig();
        return testConfig.getString(ConfigurationConstants.DYNAMODB_SECRET_KEY.getKey());
    }

    private static String getTableName() {
        org.apache.commons.configuration.Configuration testConfig =
                TestUtil.getTestConfig();
        return testConfig.getString(ConfigurationConstants.DYNAMODB_TABLE.getKey());
    }

    private static synchronized AmazonDynamoDB getClient() {
        if (client == null) {
            var credentialsProvider = AWSClientBuilder.newCredentialsProvider(
                    getAccessKeyID(), getSecretAccessKey());
            var endpointConfig = new AwsClientBuilder.EndpointConfiguration(
                    getEndpointURI(), null);
            client = AmazonDynamoDBClientBuilder
                    .standard()
                    .withCredentials(credentialsProvider)
                    .withEndpointConfiguration(endpointConfig)
                    .build();
        }
        return client;
    }

    private static void createTable() {
        CreateTableRequest request = new CreateTableRequest()
                .withAttributeDefinitions(
                        new AttributeDefinition(PRIMARY_KEY_COLUMN, ScalarAttributeType.S))
                .withKeySchema(
                        new KeySchemaElement(PRIMARY_KEY_COLUMN, KeyType.HASH))
                .withProvisionedThroughput(new ProvisionedThroughput(READ_THROUGHPUT, WRITE_THROUGHPUT))
                .withTableName(getTableName());
        try {
            getClient().createTable(request);
        } catch (ResourceInUseException ignore) {
            // table already exists, that's fine
        }
    }

    private static void deleteTable() {
        var client = DynamoDBCache.getClient();
        var request = new DeleteTableRequest().withTableName(getTableName());
        client.deleteTable(request);
    }

    private static void deleteAllItems() {
        final AmazonDynamoDB client = getClient();
        final DynamoDB dynamoDB     = new DynamoDB(client);
        final Table table           = dynamoDB.getTable(getTableName());

        ScanSpec spec = new ScanSpec()
                .withFilterExpression(EXPIRES_COLUMN + " < :v_exp")
                .withValueMap(new ValueMap().withNumber(":v_exp",
                        Instant.MAX.getEpochSecond()));
        for (Item item : table.scan(spec)) {
            table.deleteItem(PRIMARY_KEY_COLUMN,
                    item.getString(PRIMARY_KEY_COLUMN));
        }
    }

    private static void initConfiguration() {
        final var config = Configuration.getInstance();
        config.setProperty(Key.DYNAMODBCACHE_ENDPOINT, getEndpointURI());
        config.setProperty(Key.DYNAMODBCACHE_TABLE_NAME, getTableName());
        config.setProperty(Key.DYNAMODBCACHE_ACCESS_KEY_ID, getAccessKeyID());
        config.setProperty(Key.DYNAMODBCACHE_SECRET_KEY, getSecretAccessKey());
    }

    @BeforeAll
    public static void beforeClass() throws Exception {
        BaseTest.beforeClass();
        initConfiguration();
        createTable();
        Thread.sleep(12000); // table creation is asynchronous
    }

    @AfterAll
    public static void afterClass() throws Exception {
        BaseTest.afterClass();
        deleteTable();
    }

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        initConfiguration();
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
        deleteAllItems();
    }

    @Override
    DynamoDBCache newInstance() {
        return new DynamoDBCache();
    }

    @Test
    void testNewDerivativeImageOutputStreamWithMultiChunkImage() throws Exception {
        final DerivativeCache instance = newInstance();
        final OperationList ops = new OperationList(
                new Identifier("cats"), new Encode(Format.get("jpg")));

        // Assert that it's not already cached
        assertNull(instance.newDerivativeImageInputStream(ops));

        // Add it to the cache
        final int length = DynamoDBCache.MAX_CHUNK_BYTES * 3;
        try (OutputStream outputStream =
                     instance.newDerivativeImageOutputStream(ops)) {
            for (int i = 0; i < length; i++) {
                outputStream.write(0);
            }
        }

        // Wait for it to upload
        Thread.sleep(ASYNC_WAIT);

        // Read it back in
        try (InputStream is = instance.newDerivativeImageInputStream(ops)) {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            is.transferTo(os);
            os.close();
            assertEquals(length, os.toByteArray().length);
        }
    }

    @Test
    void testPutWithMultiChunkInfo() throws Exception {
        final DerivativeCache instance = newInstance();
        final Identifier identifier    = new Identifier("cats");
        // Create an implausibly large Info.
        final Info info                = new Info();
        final Metadata metadata        = new Metadata();
        metadata.setXMP("A".repeat(DynamoDBCache.MAX_CHUNK_BYTES * 3));
        info.setMetadata(metadata);

        instance.put(identifier, info);

        Optional<Info> actualInfo = instance.getInfo(identifier);
        assertEquals(info, actualInfo.orElseThrow());
    }

}
