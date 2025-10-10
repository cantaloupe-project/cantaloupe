package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.http.Status;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Permanently redirects (via HTTP 301) {@literal /some/path/} to {@literal
 * /some/path}, respecting the Servlet context root
 */
public class TrailingSlashRemovingResource extends Controller {
    TrailingSlashRemovingResource(HttpServletRequest request, HttpServletResponse response) {
        super(request, response);
    }

    @Override
    public void doGet(Request request) {
        final String uri = request.getPublicReference().getPath();
        int index = uri.lastIndexOf('/');
        response.setHeader("Location", uri.substring(0, index));
        response.setStatus(Status.MOVED_PERMANENTLY.getCode());
    }

}
