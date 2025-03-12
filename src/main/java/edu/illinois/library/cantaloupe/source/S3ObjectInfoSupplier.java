package edu.illinois.library.cantaloupe.source;

import java.io.IOException;

/**
 * Deferred accessor to {@link S3ObjectInfo}.
 */
@FunctionalInterface
public interface S3ObjectInfoSupplier {
    S3ObjectInfo get() throws IOException;
}
