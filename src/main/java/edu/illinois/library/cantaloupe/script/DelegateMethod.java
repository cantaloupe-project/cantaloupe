package edu.illinois.library.cantaloupe.script;

/**
 * Available delegate methods.
 */
public enum DelegateMethod {

    /**
     * Called by {@link DelegateProxy#isAuthorized()}.
     */
    AUTHORIZED("authorized?"),

    /**
     * Called by {@link DelegateProxy#getAzureStorageResolverBlobKey()}.
     */
    AZURESTORAGERESOLVER_BLOB_KEY("azurestorageresolver_blob_key"),

    /**
     * Called by {@link DelegateProxy#getExtraIIIFInformationResponseKeys()}.
     */
    EXTRA_IIIF2_INFORMATION_RESPONSE_KEYS("extra_iiif2_information_response_keys"),

    /**
     * Called by {@link DelegateProxy#getFilesystemResolverPathname()}.
     */
    FILESYSTEMRESOLVER_PATHMAME("filesystemresolver_pathname"),

    /**
     * Called by {@link DelegateProxy#getHttpResolverResourceInfo()}.
     */
    HTTPRESOLVER_RESOURCE_INFO("httpresolver_resource_info"),

    /**
     * Called by {@link DelegateProxy#getJdbcResolverDatabaseIdentifier()}.
     */
    JDBCRESOLVER_DATABASE_IDENTIFIER("jdbcresolver_database_identifier"),

    /**
     * Called by {@link DelegateProxy#getJdbcResolverMediaType()}.
     */
    JDBCRESOLVER_MEDIA_TYPE("jdbcresolver_media_type"),

    /**
     * Called by {@link DelegateProxy#getJdbcResolverLookupSQL()}.
     */
    JDBCRESOLVER_LOOKUP_SQL("jdbcresolver_lookup_sql"),

    /**
     * Called by {@link DelegateProxy#getOverlayProperties()}.
     */
    OVERLAY("overlay"),

    /**
     * Called by {@link DelegateProxy#getRedactions()}.
     */
    REDACTIONS("redactions"),

    /**
     * Called by {@link DelegateProxy#getRedirect()}.
     */
    REDIRECT("redirect"),

    /**
     * Called by {@link DelegateProxy#getResolver()}.
     */
    RESOLVER("resolver"),

    /**
     * Called by {@link DelegateProxy#getS3ResolverObjectInfo()}.
     */
    S3RESOLVER_OBJECT_INFO("s3resolver_object_info");

    private String methodName;

    DelegateMethod(String methodName) {
        this.methodName = methodName;
    }

    /**
     * @return Name of the delegate method.
     */
    String getMethodName() {
        return methodName;
    }

    /**
     * @return Name of the delegate method.
     */
    @Override
    public String toString() {
        return methodName;
    }

}
