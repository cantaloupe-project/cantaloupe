package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.http.ContentTypeNegotiator;
import edu.illinois.library.cantaloupe.http.Method;
import edu.illinois.library.cantaloupe.http.Reference;
import edu.illinois.library.cantaloupe.http.Status;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.util.StringUtils;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * <p>Abstract HTTP resource. Instances should subclass and override one or
 * more of the HTTP-method-specific methods {@link #doGET()} etc., and may
 * optionally use {@link #doInit()} and {@link #destroy()}.</p>
 *
 * <p>Unlike {@link jakarta.servlet.http.HttpServlet}s, instances are only used
 * once and not shared across threads.</p>
 */
public abstract class AbstractResource {

    static final String RESPONSE_CONTENT_DISPOSITION_QUERY_ARG =
            "response-content-disposition";


    private Request request;
    private HttpServletResponse response;


    /**
     * <p>Returns a sanitized value for a {@code Content-Disposition} header
     * based on the value of the {@link #RESPONSE_CONTENT_DISPOSITION_QUERY_ARG}
     * query argument.</p>
     *
     * <p>If the disposition is {@code attachment} and the filename is not
     * set, it is set to a reasonable value based on the given identifier and
     * output format.</p>
     *
     * @param queryArg      Value of the unsanitized {@link
     *                      #RESPONSE_CONTENT_DISPOSITION_QUERY_ARG} query
     *                      argument.
     * @param identifierStr Identifier or meta-identifier.
     * @param outputFormat  Output format.
     * @return              Value for a {@code Content-Disposition} header,
     *                      which may be {@code null}.
     */
    private static String getSafeContentDisposition(String queryArg,
                                                    String identifierStr,
                                                    Format outputFormat) {
        String disposition = null;
        if (queryArg != null) {
            queryArg = URLDecoder.decode(queryArg, StandardCharsets.UTF_8);
            if (queryArg.startsWith("inline")) {
                disposition = "inline; filename=\"" +
                        safeContentDispositionFilename(identifierStr, outputFormat) + "\"";
            } else if (queryArg.startsWith("attachment")) {
                final List<String> dispositionParts = new ArrayList<>(3);
                dispositionParts.add("attachment");

                // Check for ISO-8859-1 filename pattern
                Pattern pattern = Pattern.compile(".*filename=\"?([^\"]*)\"?.*");
                Matcher matcher = pattern.matcher(queryArg);
                String filename;
                if (matcher.matches()) {
                    // Filter out filename-unsafe characters as well as "..".
                    filename = StringUtils.sanitize(
                            matcher.group(1),
                            Pattern.compile("\\.\\."),
                            Pattern.compile(StringUtils.ASCII_FILENAME_UNSAFE_REGEX));
                } else {
                    filename = safeContentDispositionFilename(identifierStr,
                            outputFormat);
                }
                dispositionParts.add("filename=\"" + filename + "\"");

                // Check for Unicode filename pattern
                pattern = Pattern.compile(".*filename\\*= ?(utf-8|UTF-8)''([^\"]*).*");
                matcher = pattern.matcher(queryArg);
                if (matcher.matches()) {
                    // Filter out filename-unsafe characters as well as "..".
                    filename = StringUtils.sanitize(
                            matcher.group(2),
                            Pattern.compile("\\.\\."),
                            Pattern.compile(StringUtils.UNICODE_FILENAME_UNSAFE_REGEX,
                                    Pattern.UNICODE_CHARACTER_CLASS));
                    filename = Reference.encode(filename);
                    dispositionParts.add("filename*= UTF-8''" + filename);
                }
                disposition = String.join("; ", dispositionParts);
            }
        }
        return disposition;
    }

    private static String safeContentDispositionFilename(String identifierStr,
                                                         Format outputFormat) {
        return identifierStr.replaceAll(StringUtils.ASCII_FILENAME_UNSAFE_REGEX, "_") +
                "." + outputFormat.getPreferredExtension();
    }

    /**
     * <p>Initialization method, called after all necessary setters have been
     * called but before any request-handler method (like {@link #doGET()}
     * etc.)</p>
     *
     * <p>Overrides must call {@code super}.</p>
     */
    public void doInit() throws Exception {
        logRequestStart();
    }

    protected void logRequestStart() {
        getLogger().info("Handling {} {}",
                request.getServletRequest().getMethod(), request.getReference().getPath());
        getLogger().debug("Request headers: {}",
                request.getHeaders().stream()
                        .map(h -> h.getName() + ": " +
                                ("Authorization".equals(h.getName()) ? "******" : h.getValue()))
                        .collect(Collectors.joining("; ")));
    }

    /**
     * <p>Must be overridden by implementations that support {@literal GET}.</p>
     *
     * <p>Overrides must not call {@code super}.</p>
     */
    public void doGET() throws Exception {
        response.setStatus(Status.METHOD_NOT_ALLOWED.getCode());
    }

    /**
     * This implementation simply calls {@link #doGET}. When that is
     * overridden, this may also be overridden in order to set headers only and
     * not compute a response body.
     */
    public void doHEAD() throws Exception {
        doGET();
    }

    /**
     * May be overridden by implementations that support {@literal OPTIONS}.
     */
    protected void doOPTIONS() {
        Method[] methods = getSupportedMethods();
        if (methods.length > 0) {
            response.setStatus(Status.NO_CONTENT.getCode());
            response.setHeader("Allow", Arrays.stream(methods)
                    .map(Method::toString)
                    .collect(Collectors.joining(",")));
        } else {
            response.setStatus(Status.METHOD_NOT_ALLOWED.getCode());
        }
    }

    /**
     * <p>Must be overridden by implementations that support {@literal
     * POST}.</p>
     *
     * <p>Overrides must not call {@code super}.</p>
     */
    public void doPOST() throws Exception {
        response.setStatus(Status.METHOD_NOT_ALLOWED.getCode());
    }

    /**
     * <p>Must be overridden by implementations that support {@literal PUT}.</p>
     *
     * <p>Overrides must not call {@code super}.</p>
     */
    public void doPUT() throws Exception {
        response.setStatus(Status.METHOD_NOT_ALLOWED.getCode());
    }

    abstract protected Logger getLogger();


    /**
     * @return List of client-preferred media types as expressed in the
     *         {@code Accept} request header.
     * @see    <a href="https://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html">
     *         RFC 2616</a>
     */
    protected final List<String> getPreferredMediaTypes() {
        ContentTypeNegotiator negotiator = new ContentTypeNegotiator(request.getHeaders());
        return negotiator.getPreferredMediaTypes();
    }

    /**
     * <p>Returns a sanitized value for a {@code Content-Disposition} header
     * based on the value of the {@link #RESPONSE_CONTENT_DISPOSITION_QUERY_ARG}
     * query argument.</p>
     *
     * <p>If the disposition is {@code attachment} and the filename is not
     * set, it will be set to a reasonable value based on the given identifier
     * and output format.</p>
     *
     * @return Value for a {@code Content-Disposition} header, which may be
     *         {@code null}.
     */
    protected String getRepresentationDisposition(String identifierStr,
                                                  Format outputFormat) {
        String queryArg = getRequest().getReference().getQuery()
                .getFirstValue(RESPONSE_CONTENT_DISPOSITION_QUERY_ARG);
        if (queryArg != null) {
            getLogger().warn("Passing the {} query argument is deprecated and support for it will be " +
                             "removed in the next major release. Consider the `download' attribute on the anchor tag " +
                             "instead. See https://developer.mozilla.org/en-US/docs/Web/API/HTMLAnchorElement/download", RESPONSE_CONTENT_DISPOSITION_QUERY_ARG);
        }

        return getSafeContentDisposition(queryArg, identifierStr, outputFormat);
    }

    /**
     * @return Request being handled.
     */
    protected Request getRequest() {
        return request;
    }

    /**
     * @return Response to be sent.
     */
    protected final HttpServletResponse getResponse() {
        return response;
    }

    /**
     * <p>This implementation returns a one-element array containing {@link
     * Method#OPTIONS}. It can be overridden to declare or not declare support
     * for:</p>
     *
     * <ul>
     *     <li>{@link #doGET() GET} (note that {@link #doHEAD() HEAD} is
     *     implicitly supported when this is supported)</li>
     *     <li>{@link #doPOST() POST}</li>
     *     <li>{@link #doPUT() PUT}</li>
     * </ul>
     *
     * <p>Overrides should include {@link Method#OPTIONS}.</p>
     */
    public Method[] getSupportedMethods() {
        return new Method[] { Method.OPTIONS };
    }


    /**
     * @param request Request being handled.
     */
    public final void setRequest(Request request) {
        this.request = request;
    }

    /**
     * @param response Response that will be sent.
     */
    public final void setResponse(HttpServletResponse response) {
        this.response = response;
    }

}
