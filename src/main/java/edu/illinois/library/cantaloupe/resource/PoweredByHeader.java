package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import jakarta.servlet.http.HttpServletResponse;

public class PoweredByHeader {
    public static void addToResponse(HttpServletResponse response) {
        final Configuration config = Configuration.getInstance();
        // Only show the x-powered-by header if configured to do so.
        if (config.getBoolean(Key.HEADERS_POWERED_BY_DISPLAY, true)) {
          response.setHeader("X-Powered-By",
                  Application.getName() + "/" + Application.getVersion());
        }
    }
}
