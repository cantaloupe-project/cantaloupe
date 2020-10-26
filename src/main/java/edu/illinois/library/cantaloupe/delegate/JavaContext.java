package edu.illinois.library.cantaloupe.delegate;

import java.util.List;
import java.util.Map;

/**
 * <p>Contains information about the current request for consumption within the
 * {@link JavaDelegate}.</p>
 *
 * <p>In order to minimize coupling against user code, no internal application
 * API is exposed.</p>
 *
 * @since 5.0
 */
public interface JavaContext {

    /**
     * @return Client IP address.
     */
    String getClientIPAddress();

    /**
     * @return Map of cookie name-value pairs, or an empty map if none exist.
     */
    Map<String, String> getCookies();

    /**
     * N.B.: The return value is only available after the image has been
     * accessed; before then, it is an empty map.
     *
     * @return Map with {@code width} and {@code height} keys corresponding to
     *         the pixel dimensions of the source image.
     */
    Map<String, Integer> getFullSize();

    /**
     * @return Image identifier.
     */
    String getIdentifier();

    /**
     * @return URI seen by the application, which may be different from {@link
     *         #getRequestURI()} when operating behind a reverse proxy server.
     */
    String getLocalURI();

    /**
     * <p>Map with the following structure:</p>
     *
     * <pre>{@code {
     *     "exif": {
     *         "tagSet": "Baseline TIFF",
     *         "fields": {
     *             "Field1Name": value,
     *             "Field2Name": value,
     *             "EXIFIFD": {
     *                 "tagSet": "EXIF",
     *                 "fields": {
     *                     "Field1Name": value,
     *                     "Field2Name": value
     *                 }
     *             }
     *         }
     *     },
     *     "iptc": [
     *         "Field1Name": value,
     *         "Field2Name": value
     *     ],
     *     "xmp_string": "<rdf:RDF>...</rdf:RDF>",
     *     "xmp_model": https://jena.apache.org/documentation/javadoc/jena/org/apache/jena/rdf/model/Model.html
     *     "native": {
     *         # structure varies
     *     }
     * }}</pre>
     *
     * <ul>
     *     <li>The {@code exif} key refers to embedded EXIF data. This also
     *     includes IFD0 metadata from source TIFFs, whether or not an EXIF
     *     IFD is present.</li>
     *     <li>The {@code iptc} key refers to embedded IPTC IIM data.</li>
     *     <li>The {@code xmp_string} key refers to raw embedded XMP data,
     *     which may or may not contain EXIF and/or IPTC information.</li>
     *     <li>The {@code xmp_model} key contains a {@link
     *     org.apache.jena.rdf.model.Model} object pre-loaded with the
     *     contents of {@code xmp_string}.</li>
     *     <li>The {@code native} key refers to format-specific metadata.</li>
     * </ul>
     *
     * <p>Any combination of the above keys may be present or missing depending
     * on what is available in a particular source image.</p>
     *
     * <p>N.B.: The return value is only available after the image has been
     * accessed; before then, it is an empty map.</p>
     *
     * @return See above.
     */
    Map<String, Object> getMetadata();

    /**
     * N.B.: The return value is only available after the image has been
     * accessed; before then, it is an empty map.
     *
     * @return List of operations in order of application. Only operations that
     *         are not no-ops are included. Every map contains a {@code class}
     *         key corresponding to the operation class name, which will be one
     *         of the {@link
     *         edu.illinois.library.cantaloupe.operation.Operation}
     *         implementations.
     */
    List<Map<String, Object>> getOperations();

    /**
     * N.B.: The return value is only available after the image has been
     * accessed; before then, it is an empty map.
     *
     * @return Output format media (MIME) type.
     */
    String getOutputFormat();

    /**
     * @return Page number requested by the client.
     */
    Integer getPageNumber();

    /**
     * @return Map of header name-value pairs.
     */
    Map<String, String> getRequestHeaders();

    /**
     * @return URI requested by the client.
     */
    String getRequestURI();

    /**
     * N.B.: The return value is only available after the image has been
     * accessed; before then, it is an empty map.
     *
     * @return Map with {@code width} and {@code height} keys corresponding to
     *         the pixel dimensions of the resulting image after all operations
     *         have been applied.
     */
    Map<String, Integer> getResultingSize();

    /**
     * @return Two-element array with numerator at position 0 and denominator
     *         at position 1.
     */
    int[] getScaleConstraint();
}
