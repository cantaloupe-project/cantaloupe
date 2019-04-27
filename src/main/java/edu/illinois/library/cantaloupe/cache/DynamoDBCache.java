package edu.illinois.library.cantaloupe.cache;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkBaseException;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.document.spec.ScanSpec;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.BatchWriteItemRequest;
import com.amazonaws.services.dynamodbv2.model.Get;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.ItemResponse;
import com.amazonaws.services.dynamodbv2.model.PutRequest;
import com.amazonaws.services.dynamodbv2.model.TransactGetItem;
import com.amazonaws.services.dynamodbv2.model.TransactGetItemsRequest;
import com.amazonaws.services.dynamodbv2.model.TransactGetItemsResult;
import com.amazonaws.services.dynamodbv2.model.WriteRequest;
import edu.illinois.library.cantaloupe.async.ThreadPool;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.util.AWSClientBuilder;
import edu.illinois.library.cantaloupe.util.ArrayUtils;
import edu.illinois.library.cantaloupe.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * <p>DynamoDB is a scalable table-based NoSQL database in the Amazon Web
 * Services (AWS) environment.</p>
 *
 * <h1>Item Sizes</h1>
 *
 * <p>DynamoDB has some limitations that hamstring it for this use case. This
 * class tries to work around them with partial success.</p>
 *
 * <p>The main limitation is that DynamoDB items are limited to 400 KB. When
 * cached data does not exceed this, it is uploaded as one info/image per
 * DynamoDB item. But images/infos larger than 400 KB must be split into
 * multiple DynamoDB items. It's not an option to simply chunk them and upload
 * one item per chunk, though, because concurrent reads might not see all of
 * the chunks. One would think of transactions as an obvious solution, but a
 * second limitation is that DynamoDB only supports up to 10 items per
 * transaction, and a third limitation is that DynamoDB can't handle multiple
 * items with the same primary key in the same transaction.</p>
 *
 * <p>The solution is to assign all chunk items different opaque primary keys,
 * and upload a "directory" item, containing references to all of the chunk
 * items, last. When reading from the cache, the DynamoDB item is checked to
 * see whether it is a full item, or if it contains a directory of other items.
 * If the latter, then all of the items it points to are read transactionally
 * (in case any no longer exist).</p>
 *
 * <p>The implication is that the total image/info size cannot exceed 400 KB
 * &times; 10, so anything larger than this is not cached.</p>
 *
 * <h1>Invalidation</h1>
 *
 * <p>Cached items are not validated; only their expiration times are updated.
 * If LRU behavior is needed, a time-to-live must be set on {@link
 * #EXPIRES_COLUMN} on the AWS side. This saves money (because DynamoDB will
 * auto- purge invalid content) and makes the {@link CacheWorker}
 * redundant.</p>
 *
 * @author Alex Dolski UIUC
 * @since 5.0
 */
class DynamoDBCache implements DerivativeCache {

    /**
     * Buffers written data in memory and writes it to DynamoDB upon closure.
     */
    private static class DynamoDBImageOutputStream extends OutputStream {

        private static final Logger LOGGER =
                LoggerFactory.getLogger(DynamoDBImageOutputStream.class);

        private OperationList opList;
        private final ByteArrayOutputStream wrappedStream =
                new ByteArrayOutputStream();

        DynamoDBImageOutputStream(OperationList opList) {
            this.opList = opList;
        }

        @Override
        public void close() throws IOException {
            try {
                wrappedStream.close();

                // At this point, the client has received all image data, but
                // it is still waiting for the connection to close. Uploading
                // in a separate thread will allow this to happen immediately.
                uploadAsync();
            } finally {
                super.close();
            }
        }

        private void uploadAsync() {
            var data = wrappedStream.toByteArray();
            if (data.length < 1) {
                return;
            } else if (data.length > MAX_OBJECT_SIZE) {
                LOGGER.debug("close(): image for {} is too large " +
                                "({} bytes; max {} bytes); ignoring",
                        opList, data.length, MAX_OBJECT_SIZE);
                return;
            }

            ThreadPool.getInstance().submit(() -> {
                try {
                    chunkifyAndPutItem(
                            getPrimaryKey(opList),
                            opList.getIdentifier(),
                            wrappedStream.toByteArray());
                } catch (IOException e) {
                    LOGGER.warn("close(): {}", e.getMessage());
                }
            });
        }

        @Override
        public void flush() throws IOException {
            wrappedStream.flush();
        }

        @Override
        public void write(int b) throws IOException {
            wrappedStream.write(b);
        }

        @Override
        public void write(byte[] b) throws IOException {
            wrappedStream.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            wrappedStream.write(b, off, len);
        }
    }

    private static final Logger LOGGER =
            LoggerFactory.getLogger(DynamoDBCache.class);

    /**
     * String type. MD5-hashed {@link Identifier} or {@link OperationList}
     * string representation.
     */
    static final String PRIMARY_KEY_COLUMN = "PrimaryKey";

    /**
     * String type. Contains a {@link #DIRECTORY_CHUNK_KEY_DELIMITER}-separated
     * list of chunk item primary keys. May be {@code null}. See class doc.
     */
    static final String DIRECTORY_COLUMN = "Directory";

    /**
     * String type. {@link Identifier} with which the item is associated.
     */
    static final String IDENTIFIER_COLUMN = "Identifier";

    /**
     * Number type. Expiration epoch second.
     */
    static final String EXPIRES_COLUMN = "Expires";

    /**
     * Binary type. Image bytes or {@link Info#writeAsJSON(OutputStream) JSON
     * UTF-8 bytes}.
     */
    static final String DATA_COLUMN = "Data";

    /**
     * Primary keys in the {@link #DIRECTORY_COLUMN} are joined using this
     * delimiter.
     */
    private static final String DIRECTORY_CHUNK_KEY_DELIMITER = ";";

    /**
     * DynamoDB imposes a max item size of 400 KB. We set this to a value
     * somewhat less than that in order to leave room for the other key values.
     */
    static final int MAX_CHUNK_BYTES = 396 * 1024;

    /**
     * This is {@link #MAX_CHUNK_BYTES} multiplied by the maximum number of
     * items allowed in a transaction.
     */
    private static final int MAX_OBJECT_SIZE = 10 * MAX_CHUNK_BYTES;

    private static AmazonDynamoDB client;

    static synchronized AmazonDynamoDB getClient() {
        if (client == null) {
            var config = Configuration.getInstance();
            var credentialsProvider = AWSClientBuilder.newCredentialsProvider(
                    config.getString(Key.DYNAMODBCACHE_ACCESS_KEY_ID),
                    config.getString(Key.DYNAMODBCACHE_SECRET_KEY));
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

    /**
     * Writes the given data to an item with the given primary key, splitting
     * it into chunks if necessary (see class documentation).
     *
     * @param primaryKey Primary key of the whole or directory item.
     * @param identifier Identifier of the image corresponding to the item.
     * @param data       Data to write.
     * @throws           IllegalArgumentException if the given data is larger
     *                   than {@link #MAX_OBJECT_SIZE}.
     */
    private static void chunkifyAndPutItem(String primaryKey,
                                           Identifier identifier,
                                           byte[] data) throws IOException {
        if (data.length > MAX_OBJECT_SIZE) {
            throw new IllegalArgumentException("Data length > MAX_OBJECT_SIZE");
        }

        final AmazonDynamoDB client   = getClient();
        final DynamoDB db             = new DynamoDB(client);
        final String tableName        = getTableName();
        final Table table             = db.getTable(tableName);
        final List<byte[]> dataChunks = ArrayUtils.chunkify(data, MAX_CHUNK_BYTES);
        final List<String> itemPKeys  = new ArrayList<>(dataChunks.size());
        final long expires            = getExpiryTime();

        try {
            if (dataChunks.size() == 1) {
                table.putItem(new Item()
                        .withString(PRIMARY_KEY_COLUMN, primaryKey)
                        .withString(IDENTIFIER_COLUMN, identifier.toString())
                        .withBinary(DATA_COLUMN, data)
                        .withNumber(EXPIRES_COLUMN, expires));
            } else {
                final List<WriteRequest> requestItems =
                        new ArrayList<>(dataChunks.size());
                for (byte[] dataChunk : dataChunks) {
                    String chunkPKey = UUID.randomUUID().toString();
                    itemPKeys.add(chunkPKey);
                    PutRequest putRequest = new PutRequest();
                    putRequest.addItemEntry(PRIMARY_KEY_COLUMN,
                            new AttributeValue(chunkPKey));
                    putRequest.addItemEntry(DATA_COLUMN,
                            new AttributeValue().withB(ByteBuffer.wrap(dataChunk)));
                    putRequest.addItemEntry(EXPIRES_COLUMN,
                            new AttributeValue().withN("" + expires));
                    requestItems.add(new WriteRequest().withPutRequest(putRequest));
                }

                BatchWriteItemRequest request = new BatchWriteItemRequest()
                        .withRequestItems(Map.of(tableName, requestItems));
                client.batchWriteItem(request);

                // Upload a directory item referring to the chunks.
                table.putItem(new Item()
                        .withString(PRIMARY_KEY_COLUMN, primaryKey)
                        .withString(IDENTIFIER_COLUMN, identifier.toString())
                        .withString(DIRECTORY_COLUMN,
                                String.join(DIRECTORY_CHUNK_KEY_DELIMITER, itemPKeys))
                        .withNumber(EXPIRES_COLUMN, expires));
            }
        } catch (AmazonServiceException e) {
            LOGGER.warn("chunkifyAndPutItem(): {}", e.getMessage(), e);
            throw new IOException(e);
        } catch (SdkBaseException e) {
            LOGGER.error("chunkifyAndPutItem(): {}", e.getMessage(), e);
            throw new IOException(e);
        }
    }

    /**
     * Retrieves and reassembles an item (or items) written by {@link
     * #chunkifyAndPutItem(String, Identifier, byte[])}.
     *
     * @param primaryKey Primary key of the whole or directory item.
     * @return           Reassembled item data, or {@code null} if there is no
     *                   item corresponding to {@code primaryKey}.
     */
    private static byte[] fetchAndAssembleItemData(String primaryKey) throws IOException {
        final AmazonDynamoDB client = getClient();
        final String tableName      = getTableName();

        // Get the main item. This may be a whole item or a directory item that
        // refers to the primary keys of other items containing the chunks.
        final GetItemRequest request = new GetItemRequest()
                .withTableName(tableName)
                .withKey(Map.of(PRIMARY_KEY_COLUMN, new AttributeValue(primaryKey)));
        try {
            GetItemResult result             = client.getItem(request);
            Map<String, AttributeValue> item = result.getItem();
            if (item == null) {
                return null;
            }

            long expires = Long.parseLong(item.get(EXPIRES_COLUMN).getN());
            long now     = Instant.now().getEpochSecond();
            if (expires < now) {
                return null;
            }

            touchAsync(tableName, primaryKey);

            // Check if it's a directory item.
            AttributeValue dirValue = item.get(DIRECTORY_COLUMN);
            if (dirValue != null) {
                // It's a directory item. Assemble a transaction to retrieve
                // the chunk items.
                final String[] chunkPKeys           = dirValue.getS().split(
                        DIRECTORY_CHUNK_KEY_DELIMITER);
                final List<byte[]> chunks           = new ArrayList<>(chunkPKeys.length);
                final List<TransactGetItem> txItems = new ArrayList<>(chunkPKeys.length);
                for (String chunkPKey : chunkPKeys) {
                    Map<String,AttributeValue> chunkKey = Map.of(
                            PRIMARY_KEY_COLUMN, new AttributeValue(chunkPKey));
                    Get get = new Get()
                            .withTableName(tableName)
                            .withKey(chunkKey);
                    txItems.add(new TransactGetItem().withGet(get));
                }

                TransactGetItemsRequest txRequest = new TransactGetItemsRequest()
                        .withTransactItems(txItems);
                TransactGetItemsResult txResult =
                        client.transactGetItems(txRequest);

                for (ItemResponse itemResponse : txResult.getResponses()) {
                    Map<String,AttributeValue> txItem = itemResponse.getItem();
                    AttributeValue dataValue = txItem.get(DATA_COLUMN);
                    ByteBuffer data = dataValue.getB();
                    chunks.add(data.array());
                    touchAsync(tableName, txItem.get(PRIMARY_KEY_COLUMN).getS());
                }
                return ArrayUtils.merge(chunks);
            } else {
                AttributeValue value = item.get(DATA_COLUMN);
                ByteBuffer buffer = value.getB();
                return buffer.array();
            }
        } catch (AmazonServiceException e) {
            LOGGER.warn("fetchAndAssembleItemData(): {}", e.getMessage(), e);
            throw new IOException(e);
        } catch (SdkBaseException e) {
            LOGGER.error("fetchAndAssembleItemData(): {}", e.getMessage(), e);
            throw new IOException(e);
        }
    }

    private static String getEndpointURI() {
        Configuration config = Configuration.getInstance();
        return config.getString(Key.DYNAMODBCACHE_ENDPOINT);
    }

    private static long getExpiryTime() {
        final Configuration config = Configuration.getInstance();
        final long ttl             = config.getLong(Key.DERIVATIVE_CACHE_TTL, 0);
        final Instant instant      = (ttl > 0) ?
                Instant.now().plusSeconds(ttl) : Instant.MAX;
        return instant.getEpochSecond();
    }

    private static String getPrimaryKey(Identifier identifier) {
        return StringUtils.md5(identifier.toString());
    }

    private static String getPrimaryKey(OperationList opList) {
        return StringUtils.md5(opList.toFilename());
    }

    private static String getTableName() {
        Configuration config = Configuration.getInstance();
        return config.getString(Key.DYNAMODBCACHE_TABLE_NAME);
    }

    private static void touchAsync(String tableName, String primaryKey) {
        final AmazonDynamoDB client = getClient();
        final DynamoDB dynamoDB     = new DynamoDB(client);
        final Table table           = dynamoDB.getTable(tableName);

        // N.B.: The condition expression ensures that updateItem() doesn't
        // create a new item.
        UpdateItemSpec spec = new UpdateItemSpec()
                .withPrimaryKey(PRIMARY_KEY_COLUMN, primaryKey)
                .withUpdateExpression("set " + EXPIRES_COLUMN + " = :exp")
                .withConditionExpression(PRIMARY_KEY_COLUMN + " = :pkey")
                .withValueMap(new ValueMap()
                        .withNumber(":exp", getExpiryTime())
                        .withString(":pkey", primaryKey));
        table.updateItem(spec);
    }

    @Override
    public Optional<Info> getInfo(Identifier identifier) throws IOException {
        final String primaryKey = getPrimaryKey(identifier);
        byte[] data = fetchAndAssembleItemData(primaryKey);
        if (data != null) {
            String json = new String(data, StandardCharsets.UTF_8);
            return Optional.of(Info.fromJSON(json));
        }
        return Optional.empty();
    }

    @Override
    public InputStream newDerivativeImageInputStream(OperationList opList)
            throws IOException {
        final String primaryKey = getPrimaryKey(opList);
        byte[] data = fetchAndAssembleItemData(primaryKey);
        if (data != null) {
            return new ByteArrayInputStream(data);
        }
        return null;
    }

    @Override
    public OutputStream newDerivativeImageOutputStream(OperationList opList) {
        return new DynamoDBImageOutputStream(opList);
    }

    @Override
    public void purge() {
        final AmazonDynamoDB client = getClient();
        final DynamoDB dynamoDB     = new DynamoDB(client);
        final Table table           = dynamoDB.getTable(getTableName());

        ScanSpec spec = new ScanSpec();
        deleteAll(table, table.scan(spec));
    }

    @Override
    public void purge(Identifier identifier) {
        final AmazonDynamoDB client = getClient();
        final DynamoDB dynamoDB     = new DynamoDB(client);
        final Table table           = dynamoDB.getTable(getTableName());
        final String pkey           = identifier.toString();

        ScanSpec spec = new ScanSpec()
                .withFilterExpression(IDENTIFIER_COLUMN + " = :v_id")
                .withValueMap(new ValueMap().withString(":v_id", pkey));
        deleteAll(table, table.scan(spec));
    }

    @Override
    public void purge(OperationList opList) {
        final AmazonDynamoDB client = getClient();
        final DynamoDB dynamoDB     = new DynamoDB(client);
        final Table table           = dynamoDB.getTable(getTableName());
        final String pkey           = getPrimaryKey(opList);

        QuerySpec spec = new QuerySpec()
                .withKeyConditionExpression(PRIMARY_KEY_COLUMN + " = :v_id")
                .withValueMap(new ValueMap().withString(":v_id", pkey));
        deleteAll(table, table.query(spec));
    }

    @Override
    public void purgeInvalid() {
        final AmazonDynamoDB client = getClient();
        final DynamoDB dynamoDB     = new DynamoDB(client);
        final Table table           = dynamoDB.getTable(getTableName());
        final long expiryTime       = getExpiryTime();

        ScanSpec spec = new ScanSpec()
                .withFilterExpression(EXPIRES_COLUMN + " < :v_exp")
                .withValueMap(new ValueMap().withNumber(":v_exp", expiryTime));
        deleteAll(table, table.scan(spec));
    }

    private void deleteAll(Table table, ItemCollection<?> items) {
        for (Item item : items) {
            try {
                table.deleteItem(PRIMARY_KEY_COLUMN,
                        item.getString(PRIMARY_KEY_COLUMN));
            } catch (AmazonServiceException e) {
                LOGGER.warn("deleteAll(): {}", e.getMessage(), e);
            } catch (SdkBaseException e) {
                LOGGER.error("deleteAll(): {}", e.getMessage(), e);
            }
        }
    }

    @Override
    public void put(Identifier identifier, Info info) throws IOException {
        // Ignore incomplete Infos.
        if (!info.isComplete()) {
            LOGGER.debug("put(): info for {} is incomplete; ignoring",
                    identifier);
            return;
        }

        // Assemble the data to write,
        final byte[] data = info.toJSON().getBytes(StandardCharsets.UTF_8);

        // Discard too-large Infos (which are probably extremely rare).
        if (data.length > MAX_OBJECT_SIZE) {
            LOGGER.debug("put(): info for {} is too large " +
                            "({} bytes; max {} bytes); ignoring",
                    identifier, data.length, MAX_OBJECT_SIZE);
            return;
        }

        chunkifyAndPutItem(getPrimaryKey(identifier), identifier, data);
    }

}
