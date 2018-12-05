package edu.illinois.library.cantaloupe.source;

final class S3ObjectInfo {

    private String bucketName, key;
    private long length = -1;

    S3ObjectInfo(String key, String bucketName) {
        this.key = key;
        this.bucketName = bucketName;
    }

    String getBucketName() {
        return bucketName;
    }

    String getKey() {
        return key;
    }

    long getLength() {
        return length;
    }

    void setLength(long length) {
        this.length = length;
    }

    @Override
    public String toString() {
        return getBucketName() + "/" + getKey();
    }

}
