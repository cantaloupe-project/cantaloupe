package edu.illinois.library.cantaloupe.delegate;

import edu.illinois.library.cantaloupe.resource.RequestContext;

import java.util.List;
import java.util.Map;

/**
 * Shims a {@link JavaDelegate} to serve as a {@link DelegateProxy}.
 *
 * @since 5.0
 */
final class JavaDelegateProxy implements DelegateProxy {

    private final JavaDelegate delegate;
    private RequestContext requestContext;

    /**
     * @param delegate Instance to wrap.
     */
    JavaDelegateProxy(JavaDelegate delegate) {
        this.delegate = delegate;
    }

    @Override
    public Object authorize() {
        return delegate.authorize();
    }

    @Override
    public Map<String, Object> deserializeMetaIdentifier(String metaIdentifier) {
        return delegate.deserializeMetaIdentifier(metaIdentifier);
    }

    @Override
    public String getAzureStorageSourceBlobKey() {
        return delegate.getAzureStorageSourceBlobKey();
    }

    @Override
    public Map<String, Object> getExtraIIIF2InformationResponseKeys() {
        return delegate.getExtraIIIF2InformationResponseKeys();
    }

    @Override
    public Map<String, Object> getExtraIIIF3InformationResponseKeys() {
        return delegate.getExtraIIIF3InformationResponseKeys();
    }

    @Override
    public String getFilesystemSourcePathname() {
        return delegate.getFilesystemSourcePathname();
    }

    @Override
    public Map<String, Object> getHttpSourceResourceInfo() {
        return delegate.getHTTPSourceResourceInfo();
    }


    @Override
    public String getJdbcSourceDatabaseIdentifier() {
        return delegate.getJDBCSourceDatabaseIdentifier();
    }

    @Override
    public String getJdbcSourceLookupSQL() {
        return delegate.getJDBCSourceLookupSQL();
    }

    @Override
    public String getJdbcSourceMediaType() {
        return delegate.getJDBCSourceMediaType();
    }

    @Override
    public String getMetadata() {
        return delegate.getMetadata();
    }

    @Override
    public Map<String, Object> getOverlayProperties() {
        return delegate.getOverlay();
    }

    @Override
    public List<Map<String, Long>> getRedactions() {
        return delegate.getRedactions();
    }

    @Override
    public Map<String, String> getS3SourceObjectInfo() {
        return delegate.getS3SourceObjectInfo();
    }

    @Override
    public String getSource() {
        return delegate.getSource();
    }

    @Override
    public Object preAuthorize() {
        return delegate.preAuthorize();
    }

    @Override
    public String serializeMetaIdentifier(Map<String, Object> metaIdentifier) {
        return delegate.serializeMetaIdentifier(metaIdentifier);
    }

    @Override
    public RequestContext getRequestContext() {
        return requestContext;
    }

    @Override
    public void setRequestContext(RequestContext context) {
        delegate.setContext(new JavaRequestContext(context));
        this.requestContext = context;
    }

}
