package edu.illinois.library.cantaloupe.test;

public enum ConfigurationConstants {

    AZURE_ACCOUNT_KEY("azurestorage.account_key"),
    AZURE_ACCOUNT_NAME("azurestorage.account_name"),
    AZURE_CONTAINER("azurestorage.container"),
    GECKO_WEBDRIVER("webdriver.gecko"),
    REDIS_DATABASE("redis.database"),
    REDIS_HOST("redis.host"),
    REDIS_PASSWORD("redis.password"),
    REDIS_PORT("redis.port"),
    REDIS_SSL("redis.ssl"),
    S3_ACCESS_KEY_ID("amazons3.access_key_id"),
    S3_BUCKET("amazons3.bucket"),
    S3_REGION("amazons3.region"),
    S3_SECRET_KEY("amazons3.secret_key");

    private String key;

    ConfigurationConstants(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

}
