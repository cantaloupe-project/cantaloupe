package edu.illinois.library.cantaloupe.source;

import edu.illinois.library.cantaloupe.image.Format;

import java.io.IOException;

/**
 * <p>Infers a {@link Format} from some kind of information.</p>
 *
 * <p>This is used by some of the {@link Source} implementations but isn't
 * part of their public contract.</p>
 */
interface FormatChecker {

    Format check() throws IOException;

}
