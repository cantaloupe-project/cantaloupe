package edu.illinois.library.cantaloupe.source;

/**
 * Contains information needed to access an object in S3.
 */
final class S3ObjectInfo {

    private String region, endpoint, accessKeyID, secretAccessKey, bucketName,
            key;
    private long length = -1;

    /**
     * @return Access key ID. May be {@code null}.
     */
    String getAccessKeyID() {
        return accessKeyID;
    }

    String getBucketName() {
        return bucketName;
    }

    /**
     * @return Service endpoint URI. May be {@code null}.
     */
    String getEndpoint() {
        return endpoint;
    }

    String getKey() {
        return key;
    }

    long getLength() {
        return length;
    }

    /**
     * @return Endpoint AWS region. Only used by AWS endpoints. May be {@code
     *         null}.
     */
    String getRegion() {
        return region;
    }

    /**
     * @return Secret access key. May be {@code null}.
     */
    String getSecretAccessKey() {
        return secretAccessKey;
    }

    void setAccessKeyID(String accessKeyID) {
        this.accessKeyID = accessKeyID;
    }

    void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    void setKey(String key) {
        this.key = key;
    }

    void setLength(long length) {
        this.length = length;
    }

    void setRegion(String region) {
        this.region = region;
    }

    void setSecretAccessKey(String secretAccessKey) {
        this.secretAccessKey = secretAccessKey;
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("[endpoint: ").append(getEndpoint()).append("] ");
        b.append("[region: ").append(getRegion()).append("] ");
        String tmp = getAccessKeyID() != null ? "******" : "null";
        b.append("[accessKeyID: ").append(tmp).append("] ");
        tmp = getSecretAccessKey() != null ? "******" : "null";
        b.append("[secretAccessKey: ").append(tmp).append("] ");
        b.append("[bucket: ").append(getBucketName()).append("] ");
        b.append("[key: ").append(getKey()).append("]");
        return b.toString();
    }

}
