package edu.illinois.library.cantaloupe.source.stream;

import java.io.IOException;

/**
 * The HTTP server does not support the HTTP {@literal Range} header, as
 * indicated by the absence of a {@literal Accept-Ranges: bytes} header in a
 * {@literal HEAD} response.
 */
public class RangesNotSupportedException extends IOException {
}
