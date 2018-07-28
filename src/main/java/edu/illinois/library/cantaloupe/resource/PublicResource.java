package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.cache.CacheFacade;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.processor.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Dimension;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public abstract class PublicResource extends AbstractResource {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(PublicResource.class);

    protected Future<Path> tempFileFuture;

    @Override
    public void doInit() throws Exception {
        super.doInit();

        addHeaders();
    }

    private void addHeaders() {
        getResponse().setHeader("Access-Control-Allow-Origin", "*");
        getResponse().setHeader("Vary",
                "Accept, Accept-Charset, Accept-Encoding, Accept-Language, Origin");

        if (!isBypassingCache()) {
            final Configuration config = Configuration.getInstance();
            if (config.getBoolean(Key.CLIENT_CACHE_ENABLED, false)) {
                final List<String> directives = new ArrayList<>();
                final String maxAge = config.getString(Key.CLIENT_CACHE_MAX_AGE, "");
                if (!maxAge.isEmpty()) {
                    directives.add("max-age=" + maxAge);
                }
                String sMaxAge = config.getString(Key.CLIENT_CACHE_SHARED_MAX_AGE, "");
                if (!sMaxAge.isEmpty()) {
                    directives.add("s-maxage=" + sMaxAge);
                }
                if (config.getBoolean(Key.CLIENT_CACHE_PUBLIC, true)) {
                    directives.add("public");
                } else if (config.getBoolean(Key.CLIENT_CACHE_PRIVATE, false)) {
                    directives.add("private");
                }
                if (config.getBoolean(Key.CLIENT_CACHE_NO_CACHE, false)) {
                    directives.add("no-cache");
                }
                if (config.getBoolean(Key.CLIENT_CACHE_NO_STORE, false)) {
                    directives.add("no-store");
                }
                if (config.getBoolean(Key.CLIENT_CACHE_MUST_REVALIDATE, false)) {
                    directives.add("must-revalidate");
                }
                if (config.getBoolean(Key.CLIENT_CACHE_PROXY_REVALIDATE, false)) {
                    directives.add("proxy-revalidate");
                }
                if (config.getBoolean(Key.CLIENT_CACHE_NO_TRANSFORM, false)) {
                    directives.add("no-transform");
                }
                getResponse().setHeader("Cache-Control",
                        directives.stream().collect(Collectors.joining(", ")));
            }
        }
    }

    @Override
    public void destroy() {
        if (tempFileFuture != null) {
            try {
                Path tempFile = tempFileFuture.get();
                if (tempFile != null) {
                    Files.deleteIfExists(tempFile);
                }
            } catch (Exception e) {
                LOGGER.error("destroy(): {}", e.getMessage(), e);
            }
        }
    }

    /**
     * <p>Returns the info for the source image corresponding to the
     * given identifier as efficiently as possible.</p>
     *
     * @param identifier Image identifier.
     * @param proc       Processor from which to read the info if it can't be
     *                   retrieved from a cache.
     * @return           Info for the image with the given identifier.
     */
    protected final Info getOrReadInfo(final Identifier identifier,
                                       final Processor proc) throws IOException {
        Info info;
        if (!isBypassingCache()) {
            info = new CacheFacade().getOrReadInfo(identifier, proc);
            info.setIdentifier(identifier);
        } else {
            LOGGER.debug("getOrReadInfo(): bypassing the cache, as requested");
            info = proc.readImageInfo();
            info.setIdentifier(identifier);
        }
        return info;
    }

    /**
     * @return Page index (a.k.a. page number - 1) from the {@literal page}
     *         query argument, or {@literal 0} if not supplied.
     */
    protected int getPageIndex() {
        String arg = getRequest().getReference().getQuery()
                .getFirstValue("page", "1");
        try {
            int index = Integer.parseInt(arg) - 1;
            if (index >= 0) {
                return index;
            }
        } catch (NumberFormatException ignore) {
        }
        return 0;
    }

    /**
     * @return Whether there is a {@literal cache} argument set to {@literal
     *         false} in the URI query string.
     */
    protected final boolean isBypassingCache() {
        return "false".equals(getRequest().getReference().getQuery()
                .getFirstValue("cache"));
    }

    /**
     * <p>Checks that the requested area is greater than zero and less than or
     * equal to {@link Key#MAX_PIXELS}.</p>
     *
     * <p>This does not check that any requested crop lies entirely within the
     * bounds of the source image.</p>
     */
    protected final void validateRequestedArea(final OperationList opList,
                                               final Format sourceFormat,
                                               final Dimension fullSize)
            throws EmptyPayloadException, PayloadTooLargeException {
        final Dimension resultingSize = opList.getResultingSize(fullSize);

        if (resultingSize.width < 1 || resultingSize.height < 1) {
            throw new EmptyPayloadException();
        }

        // Max allowed size is ignored when the processing is a no-op.
        if (opList.hasEffect(fullSize, sourceFormat)) {
            final long maxAllowedSize =
                    Configuration.getInstance().getLong(Key.MAX_PIXELS, 0);
            if (maxAllowedSize > 0 &&
                    resultingSize.width * resultingSize.height > maxAllowedSize) {
                throw new PayloadTooLargeException();
            }
        }
    }

}
