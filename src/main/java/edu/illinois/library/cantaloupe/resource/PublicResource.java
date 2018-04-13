package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.operation.OperationList;

import java.awt.Dimension;
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
        if (opList.hasEffect(sourceFormat)) {
            final long maxAllowedSize =
                    Configuration.getInstance().getLong(Key.MAX_PIXELS, 0);
            if (maxAllowedSize > 0 &&
                    resultingSize.width * resultingSize.height > maxAllowedSize) {
                throw new PayloadTooLargeException();
            }
        }
    }

}
