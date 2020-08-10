package edu.illinois.library.cantaloupe.script;

import edu.illinois.library.cantaloupe.resource.RequestContext;

import javax.script.ScriptException;
import java.util.List;
import java.util.Map;

/**
 * <p>Proxy for a delegate object, which is supplied by the user in a script
 * file.</p>
 *
 * <p>Instances should be acquired via {@link
 * DelegateProxyService#newDelegateProxy(RequestContext)}.</p>
 */
public interface DelegateProxy {

    /**
     * Used to provide context information to the delegate object. Should
     * typically be called before any other method.
     *
     * @param context Context to set.
     * @throws ScriptException if the delegate object does not contain a
     *                         setter method for the context.
     */
    void setRequestContext(RequestContext context) throws ScriptException;

    /**
     * @return Return value of {@link DelegateMethod#AUTHORIZE}.
     */
    Object authorize() throws ScriptException;

    /**
     * @return Return value of {@link
     *         DelegateMethod#EXTRA_IIIF2_INFORMATION_RESPONSE_KEYS}, or an
     *         empty map if it returned {@code null}.
     */
    Map<String, Object> getExtraIIIF2InformationResponseKeys()
            throws ScriptException;

    /**
     * @return Return value of {@link
     *         DelegateMethod#EXTRA_IIIF3_INFORMATION_RESPONSE_KEYS}, or an
     *         empty map if it returned {@code null}.
     */
    Map<String, Object> getExtraIIIF3InformationResponseKeys()
            throws ScriptException;

    /**
     * {@return Return value of {@link
     *          DelegateMethod#AZURESTORAGESOURCE_BLOB_KEY}. May be {@code
     *          null}.
     */
    String getAzureStorageSourceBlobKey() throws ScriptException;

    /**
     * @return Return value of {@link
     *         DelegateMethod#FILESYSTEMSOURCE_PATHMAME}. May be {@code null}.
     */
    String getFilesystemSourcePathname() throws ScriptException;

    /**
     * @return Map based on the return value of {@link
     *         DelegateMethod#HTTPSOURCE_RESOURCE_INFO}, or an empty map if
     *         it returned {@code null}.
     */
    Map<String,?> getHttpSourceResourceInfo() throws ScriptException;

    /**
     * @return Return value of {@link
     *         DelegateMethod#JDBCSOURCE_DATABASE_IDENTIFIER}. May be
     *         {@code null}.
     */
    String getJdbcSourceDatabaseIdentifier() throws ScriptException;

    /**
     * @return Return value of {@link DelegateMethod#JDBCSOURCE_MEDIA_TYPE}.
     */
    String getJdbcSourceMediaType() throws ScriptException;

    /**
     * @return Return value of {@link DelegateMethod#JDBCSOURCE_LOOKUP_SQL}.
     */
    String getJdbcSourceLookupSQL() throws ScriptException;

    /**
     * @return Return value of {@link DelegateMethod#METADATA}.
     */
    String getMetadata() throws ScriptException;

    /**
     * @return Return value of {@link DelegateMethod#OVERLAY}, or an empty map
     *         if it returned {@code null}.
     */
    Map<String,Object> getOverlayProperties() throws ScriptException;

    /**
     * @return Return value of {@link DelegateMethod#REDACTIONS}, or an empty
     *         list if it returned {@code null}.
     */
    List<Map<String,Long>> getRedactions() throws ScriptException;

    /**
     * @return Return value of {@link DelegateMethod#SOURCE}. May be
     *         {@code null}.
     */
    String getSource() throws ScriptException;

    /**
     * @return Return value of {@link DelegateMethod#S3SOURCE_OBJECT_INFO},
     *         or an empty map if it returned {@code null}.
     */
    Map<String,String> getS3SourceObjectInfo() throws ScriptException;

}
