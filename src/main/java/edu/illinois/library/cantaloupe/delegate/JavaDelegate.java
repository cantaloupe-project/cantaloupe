package edu.illinois.library.cantaloupe.delegate;

import java.util.List;
import java.util.Map;

/**
 * <p>Interface to be implemented by JVM language-based delegate classes.</p>
 *
 * <p>The application will create an instance of an implementation early in the
 * request cycle and dispose of it at the end of the request cycle. Instances
 * don't need to be thread-safe, but sharing information across instances
 * (requests) <strong>does</strong> need to be done thread-safely.</p>
 *
 * <p>The methods in this interface are considered public API. No other part of
 * the application is considered public API, unless so documented, and breaking
 * changes to it may happen at any time.</p>
 *
 * @since 5.0
 */
public interface JavaDelegate {

    /**
     * @return Object containing information about the current request.
     */
    JavaContext getContext();

    /**
     * Invoked automatically early in the request cycle and not for public use.
     */
    void setContext(JavaContext context);

    /**
     * <p>Returns authorization status for the current request. This method is
     * called upon all requests to all public endpoints early in the request
     * cycle, before any image has been accessed. This means that some context
     * properties (like {@code full_size}) will not be available yet.</p>
     *
     * <p>This method should implement all possible authorization logic except
     * that which requires any of the context keys that aren't yet available.
     * This will ensure efficient authorization failures.</p>
     *
     * <p>Implementations should assume that the underlying resource is
     * available, and not try to check for it.</p>
     *
     * <p>Possible return values:</p>
     *
     * <ol>
     *     <li>A boolean indicating whether the request is fully authorized or
     *     not. If not, the client will receive a 403 Forbidden response.</li>
     *     <li>Map with a {@code status_code} key.
     *         <ol>
     *             <li>If it corresponds to an integer from 200-299, the request
     *             is authorized.</li>
     *             <li>If it corresponds to an integer from 300-399:
     *                 <ol>
     *                     <li>If the map also contains a {@code location} key
     *                     corresponding to a URI string, the request will be
     *                     redirected to that URI using that code.</li>
     *                     <li>If the map also contains {@code scale_numerator}
     *                     and {@code scale_denominator} keys, the request will
     *                     be redirected using thhat code to a virtual reduced-
     *                     scale version of the source image.</li>
     *                 </ol>
     *             </li>
     *             <li>If it corresponds to 401, the map must include a {@code
     *             challenge} key corresponding to a {@code WWW-Authenticate}
     *             header value.</li>
     *         </ol>
     *     </li>
     * </ol>
     */
    Object preAuthorize();

    /**
     * <p>Returns authorization status for the current request. Will be called
     * upon all requests to all public image (not information) endpoints.</p>
     *
     * <p>This is a counterpart of {@link #preAuthorize()} that is invoked later
     * in the request cycle, once more information about the underlying image
     * has become available. It should only contain logic that depends on
     * context properties that contain information about the source image (like
     * {@link JavaContext#getFullSize()}, {@link JavaContext#getMetadata()},
     * etc.</p>
     *
     * <p>Implementations should assume that the underlying resource is
     * available, and not try to check for it.</p>
     *
     * <p>The available return values are the same as for {@link
     * #preAuthorize()}.</p>
     */
    Object authorize();

    /**
     * Adds additional keys to an Image API 2.x information response. See the
     * <a href="http://iiif.io/api/image/2.1/#image-information">IIIF Image API
     * 2.1</a> specification and "endpoints" section of the user manual.
     *
     * @return Map to merge into an Image API 2.x information response. Return
     *         an empty map to add nothing.
     */
    Map<String,Object> getExtraIIIF2InformationResponseKeys();

    /**
     * Adds additional keys to an Image API 2.x information response. See the
     * <a href="http://iiif.io/api/image/3.0/#image-information">IIIF Image API
     * 3.0</a> specification and "endpoints" section of the user manual.
     *
     * @return Map to merge into an Image API 3.x information response. Return
     *         an empty map to add nothing.
     */
    Map<String,Object> getExtraIIIF3InformationResponseKeys();

    /**
     * Tells the server which source for the {@link JavaContext#getIdentifier()
     * identifier in the context}.
     *
     * @return Source name.
     */
    String getSource();

    /**
     * N.B.: this method should not try to perform authorization. {@link
     * #preAuthorize()} and {@link #authorize()} should be used instead.
     *
     * @return Blob key of the image corresponding to the {@link
     *         JavaContext#getIdentifier() identifier in the context}, or
     *         {@code null} if not found.
     */
    String getAzureStorageSourceBlobKey();

    /**
     * N.B.: this method should not try to perform authorization. {@link
     * #preAuthorize()} and {@link #authorize()} should be used instead.
     *
     * @return Absolute pathname of the image corresponding to the {@link
     *         JavaContext#getIdentifier() identifier in the context}, or
     *         {@code null} if not found.
     */
    String getFilesystemSourcePathname();

    /**
     * <p>Returns one of the following:</p>
     *
     * <ol>
     *     <li>String URI</li>
     *     <li>Map with the following keys:
     *         <dl>
     *             <dt>{@code uri}</dt>
     *             <dd>String (required)</dd>
     *             <dt>{@code username}</dt>
     *             <dd>String (required for HTTP Basic authentication)</dd>
     *             <dt>{@code secret}</dt>
     *             <dd>String (required for HTTP Basic authentication)</dd>
     *             <dt>{@code headers}</dt>
     *             <dd>Map of request header name-value pairs (optional)</dd>
     *         </dl>
     *     </li>
     *     <li>{@code null} if not found</li>
     * </ol>
     *
     * <p>N.B.: this method should not try to perform authorization. {@link
     * #preAuthorize()} and {@link #authorize()} should be used instead.</p>
     */
    Map<String,Object> getHTTPSourceResourceInfo();

    /**
     * <p>N.B.: this method should not try to perform authorization. {@link
     * #preAuthorize()} and {@link #authorize()} should be used instead.</p>
     *
     * @return Database identifier of the image corresponding to the {@link
     *         JavaContext#getIdentifier() identifier in the context}, or
     *         {@code null} if not found.
     */
    String getJDBCSourceDatabaseIdentifier();

    /**
     * Returns either the media (MIME) type of an image, or an SQL statement
     * that can be used to retrieve it, if it is stored in the database. In the
     * latter case, the {@code SELECT} and {@code FROM} clauses should be in
     * uppercase in order to be autodetected. If {@code null} is returned, the
     * media type will be inferred some other way, such as by identifier
     * extension or magic bytes.
     */
    String getJDBCSourceMediaType();

    /**
     * @return SQL statement that selects the BLOB corresponding to the value
     *         returned by {@link #getJDBCSourceDatabaseIdentifier()}.
     */
    String getJDBCSourceLookupSQL();

    /**
     * N.B.: this method should not try to perform authorization. {@link
     * #preAuthorize()} and {@link #authorize()} should be used instead.
     *
     * @return Map containing {@code bucket} and {@code key} keys, or {@code
     *         null} if not found.
     */
    Map<String,String> getS3SourceObjectInfo();

    /**
     * <p>Tells the server what overlay, if any, to apply to an image. Called
     * upon all image requests to any endpoint if overlays are enabled and the
     * overlay strategy is set to {@code ScriptStrategy} in the application
     * configuration.</p>
     *
     * <p>Possible return values:</p>
     *
     * <ol>
     *     <li>For string overlays, a map with the following keys:
     *         <dl>
     *             <dt>{@code background_color}</dt>
     *             <dd>CSS-compliant RGB(A) color.</dd>
     *             <dt>{@code color}</dt>
     *             <dd>CSS-compliant RGB(A) color.</dd>
     *             <dt>{@code font}</dt>
     *             <dd>Font name.</dd>
     *             <dt>{@code font_min_size}</dt>
     *             <dd>Minimum font size in points (ignored when {@code
     *             word_wrap} is {@code true}.</dd>
     *             <dt>{@code font_size}</dt>
     *             <dd>Font size in points.</dd>
     *             <dt>{@code font_weight}</dt>
     *             <dd>Font weight based on 1.</dd>
     *             <dt>{@code glyph_spacing}</dt>
     *             <dd>Glyph spacing based on 0.</dd>
     *             <dt>{@code inset}</dt>
     *             <dd>Pixels of inset.</dd>
     *             <dt>{@code position}</dt>
     *             <dd>Position like {@code top left}, {@code center right},
     *             etc.</dd>
     *             <dt>{@code string}</dt>
     *             <dd>String to render.</dd>
     *             <dt>{@code stroke_color}</dt>
     *             <dd>CSS-compliant RGB(A) text outline color.</dd>
     *             <dt>{@code stroke_width}</dt>
     *             <dd>Text outline width in pixels.</dd>
     *             <dt>{@code word_wrap}</dt>
     *             <dd>Whether to wrap long lines within {@code string}.</dd>
     *         </dl>
     *     </li>
     *     <li>For image overlays, a map with the following keys:
     *         <dl>
     *             <dt>{@code image}</dt>
     *             <dd>Image pathname or URL.</dd>
     *             <dt>{@code position}</dt>
     *             <dd>See above.</dd>
     *             <dt>{@code inset}</dt>
     *             <dd>See above.</dd>
     *         </dl>
     *     </li>
     *     <li>{@code null} for no overlay.</li>
     * </ol>
     */
    Map<String,Object> getOverlay();

    /**
     * Tells the server what regions of an image to redact in response to a
     * request. Will be called upon all image requests to any endpoint.
     *
     * @return List of maps, each with {@code x}, {@code y}, {@code width}, and
     *         {@code height} keys; or an empty list if no redactions are to
     *         be applied.
     */
    List<Map<String,Long>> getRedactions();

    /**
     * <p>Returns XMP metadata to embed in the derivative image.</p>
     *
     * <p>Source image metadata is available in the {@link
     * JavaContext#getMetadata() metadata context property}.
     *
     * <p>Only XMP can be embedded in derivative images. See the user manual
     * for examples of working with the XMP model programmatically.
     *
     * @return String or containing XMP data to embed in the derivative image,
     *         or {@code null} to not embed anything.
     */
    String getMetadata();

}

