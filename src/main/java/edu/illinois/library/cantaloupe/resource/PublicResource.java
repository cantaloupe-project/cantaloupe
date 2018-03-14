package edu.illinois.library.cantaloupe.resource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Future;

public abstract class PublicResource extends AbstractResource {

    protected Future<Path> tempFileFuture;

    @Override
    protected void doCatch(Throwable throwable) {
        super.doCatch(throwable);

        if (tempFileFuture != null) {
            try {
                Files.deleteIfExists(tempFileFuture.get());
            } catch (Exception e) {
                getLogger().severe(e.getMessage());
            }
        }
    }

}
