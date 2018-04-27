/**
 * <p>Package for sources, which locate and provide uniform access to source
 * images.</p>
 *
 * <p>A source translates an identifier to an image locator, such as a
 * pathname, in the particular type of underlying storage it is written to
 * interface with. It can then check whether the underlying image object exists
 * and is accessible, and if so, provide access to it to other application
 * components. The rest of of the application does not need to know where an
 * image resides, or how to access it natively&mdash;it can simply ask the
 * source it has access to.</p>
 *
 * <h1>Writing Custom Sources</h1>
 *
 * <p>Sources must implement either or both of the
 * {@link edu.illinois.library.cantaloupe.source.StreamSource} and
 * {@link edu.illinois.library.cantaloupe.source.FileSource} interfaces.
 * (Note that
 * {@link edu.illinois.library.cantaloupe.source.FilesystemSource}
 * is already available for files, and is customizable enough [via the delegate
 * mechanism] to meet virtually any need.) These are documented in the source
 * code and are pretty simple. Inheriting from {@link
 * edu.illinois.library.cantaloupe.source.AbstractSource} will get you a
 * couple of them for free.</p>
 *
 * <p>To add a custom source, the
 * {@link edu.illinois.library.cantaloupe.source.SourceFactory#getAllSources()}
 * method must be modified to return it. Then, it will be available for use
 * like any other source.</p>
 */
package edu.illinois.library.cantaloupe.source;
