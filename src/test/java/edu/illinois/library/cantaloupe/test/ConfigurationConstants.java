package edu.illinois.library.cantaloupe.test;

public enum ConfigurationConstants {

    AZURE_ACCOUNT_KEY("azurestorage.account_key"),
    AZURE_ACCOUNT_NAME("azurestorage.account_name"),
    AZURE_CONTAINER("azurestorage.container"),
    DYNAMODB_ENDPOINT("dynamodb.endpoint"),
    DYNAMODB_TABLE("dynamodb.table"),
    DYNAMODB_ACCESS_KEY_ID("dynamodb.access_key_id"),
    DYNAMODB_SECRET_KEY("dynamodb.secret_key"),
    GECKO_WEBDRIVER("webdriver.gecko"),
    REDIS_DATABASE("redis.database"),
    REDIS_HOST("redis.host"),
    REDIS_PASSWORD("redis.password"),
    REDIS_PORT("redis.port"),
    REDIS_SSL("redis.ssl"),
    S3_ACCESS_KEY_ID("s3.access_key_id"),
    S3_BUCKET("s3.bucket"),
    S3_ENDPOINT("s3.endpoint"),
    S3_REGION("s3.region"),
    S3_SECRET_KEY("s3.secret_key"),
    S3_SERVICE("s3.service");

    private String key;

    ConfigurationConstants(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

}
