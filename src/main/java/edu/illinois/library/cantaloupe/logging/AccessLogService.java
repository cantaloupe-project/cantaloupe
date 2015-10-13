package edu.illinois.library.cantaloupe.logging;

import edu.illinois.library.cantaloupe.ImageServerApplication;
import org.restlet.Request;
import org.restlet.service.LogService;

/**
 * Handles HTTP access log messages. An instance is used as the Restlet
 * Component's log service.
 */
public class AccessLogService extends LogService {

    @Override
    public String getLoggerName() {
        return getClass().getName();
    }

    @Override
    public boolean isLoggable(Request request) {
        // exclude requests to /static from the log
        final String path = ImageServerApplication.STATIC_ROOT_PATH;
        return !request.getResourceRef().getPath().startsWith(path);
    }

}
