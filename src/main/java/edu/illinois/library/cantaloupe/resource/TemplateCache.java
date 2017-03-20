package edu.illinois.library.cantaloupe.resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * <p>Caches pre-processed view templates.</p>
 *
 * <p>Developer note from alexd@n-ary.net: originally the application's Restlet
 * resources returned org.restlet.ext.velocity.TemplateRepresentations that
 * read the template files in the resource bundle. In OpenJDK 1.8.0_121, I have
 * been experiencing an issue where the templates "go missing"
 * (Class.getResource() and getResourceAsStream() start returning null) after a
 * certain period of use. It smells like a resource leak, but I haven't been
 * able to track it down.</p>
 *
 * <p>This class is therefore an attempt at a workaround for this issue. It
 * loads templates just once, and caches their contents in a map.</p>
 *
 * <p>This class is thread-safe.</p>
 */
class TemplateCache {

    private static Logger logger = LoggerFactory.getLogger(TemplateCache.class);

    private final ConcurrentMap<String,String> templates =
            new ConcurrentHashMap<>();

    /**
     * @param name Template name.
     * @return Template contents.
     */
    String get(String name) {
        if (!templates.containsKey(name)) {
            logger.debug("get(): miss for {}", name);
            try {
                load(name);
            } catch (IOException e) {
                logger.error(e.getMessage());
            }
        } else {
            logger.debug("get(): hit for {}", name);
        }
        return templates.get(name);
    }

    synchronized void load(String name) throws IOException {
        logger.debug("load(): loading {}", name);
        try (InputStream is = TemplateCache.class.getResourceAsStream(name)) {
            if (is != null) {
                try (BufferedReader reader =
                             new BufferedReader(new InputStreamReader(is))) {
                    final StringBuilder builder = new StringBuilder();
                    String str = "";
                    while ((str = reader.readLine()) != null) {
                        builder.append(str);
                    }
                    templates.putIfAbsent(name, builder.toString());
                }
            }
        }
    }

    int size() {
        return templates.size();
    }

}
