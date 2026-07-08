package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class PoweredByHeader implements Filter {
    private final Configuration config;

    @Autowired
    public PoweredByHeader(@Lazy Configuration config) {
        this.config = config;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            // Only show the x-powered-by header if configured to do so.
            if (config != null && config.getBoolean(Key.HEADERS_POWERED_BY_DISPLAY, true)) {
                ((HttpServletResponse) response).setHeader("X-Powered-By",
                        Application.getName() + "/" + Application.getVersion());
            } else if (config == null) {
                // Fallback: show header by default if configuration is not available
                ((HttpServletResponse) response).setHeader("X-Powered-By",
                        Application.getName() + "/" + Application.getVersion());
            }
        } catch (Exception e) {
            // If configuration access fails, show header by default and continue
            ((HttpServletResponse) response).setHeader("X-Powered-By",
                    Application.getName() + "/" + Application.getVersion());
        }
        chain.doFilter(request, response);
    }
}
