package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.delegate.DelegateProxyService;
import edu.illinois.library.cantaloupe.http.Cookies;
import edu.illinois.library.cantaloupe.image.MetaIdentifier;
import edu.illinois.library.cantaloupe.image.ScaleConstraint;

import java.util.Enumeration;

public class RequestContextDecorator {
    public static void decorateRequestContext(IIIFRequest request) {
        DelegateProxyService proxyService = DelegateProxyService.getInstance();
        if (!proxyService.isDelegateAvailable()) {
            return;
        }
        RequestContext context = request.getRequestContext();
        context.setLocalURI(request.getReference());
        context.setRequestURI(request.getPublicReference());
        context.setRequestHeaders(request.getHeaders().toMap());
        context.setClientIP(getCanonicalClientIPAddress(request));
        context.setCookies(getCookies(request).toMap());
        MetaIdentifier metaID = request.getMetaIdentifier();
        if (metaID != null) {
            context.setIdentifier(metaID.getIdentifier());
            context.setPageNumber(metaID.getPageNumber());
            ScaleConstraint scaleConstraint = metaID.getScaleConstraint();
            if (scaleConstraint == null) {
                // Delegate users will appreciate not having to check for
                // null.
                scaleConstraint = new ScaleConstraint(1, 1);
            }
            context.setScaleConstraint(scaleConstraint);
        }
    }

        /**
     * @return User agent's IP address, respecting the {@code X-Forwarded-For}
     *         request header, if present.
     */
    private static String getCanonicalClientIPAddress(IIIFRequest request) {
        // The value is expected to be in the format: "client, proxy1, proxy2"
        final String forwardedFor =
                request.getHeaders().getFirstValue("X-Forwarded-For", "");
        if (!forwardedFor.isEmpty()) {
            return forwardedFor.split(",")[0].trim();
        } else {
            // Fall back to the client IP address.
            return request.getRemoteAddr();
        }
    }

    protected static Cookies getCookies(IIIFRequest request) {
        Cookies cookies = new Cookies();
            final Enumeration<String> headers = request.getServletRequest().getHeaders("Cookie");
            while (headers.hasMoreElements()) {
                String value = headers.nextElement();
                Cookies batch = Cookies.fromHeaderValue(value);
                cookies.addAll(batch);
            }
        return cookies;
    }
}
