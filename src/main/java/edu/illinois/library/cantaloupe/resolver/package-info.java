/**
 * <p>Resolvers locate and provide uniform access to source images.</p>
 *
 * <p>A resolver translates an identifier to an image locator, such as a
 * pathname, in the particular type of underlying storage it is written to
 * interface with. It can then check whether the underlying image object exists
 * and is accessible, and if so, provide access to it to other application
 * components. The rest of of the application does not need to know where an
 * image resides, or how to access it natively&mdash;it can simply ask the
 * resolver it has access to.</p>
 *
 * <h3>Writing Custom Resolvers</h3>
 *
 * <p>Resolvers must implement either or both of the
 * {@link edu.illinois.library.cantaloupe.resolver.StreamResolver} and
 * {@link edu.illinois.library.cantaloupe.resolver.FileResolver} interfaces.
 * (Note that
 * {@link edu.illinois.library.cantaloupe.resolver.FilesystemResolver}
 * is already available for files, and is customizable enough [via the delegate
 * mechanism] to meet virtually any need.) These are documented in the source
 * code and are pretty simple. Inheriting from AbstractResolver will get you
 * a couple of them for free.</p>
 *
 * <p>To add a custom resolver, the
 * {@link edu.illinois.library.cantaloupe.resolver.ResolverFactory#getAllResolvers()}
 * method must be modified to return it. Then, it will be available for use
 * like any other resolver.</p>
 */
package edu.illinois.library.cantaloupe.resolver;
