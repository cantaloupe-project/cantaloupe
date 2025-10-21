package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.http.ContentTypeNegotiator;
import edu.illinois.library.cantaloupe.http.Method;
import edu.illinois.library.cantaloupe.http.Status;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.List;
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

    private Request request;
    private HttpServletResponse response;


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
