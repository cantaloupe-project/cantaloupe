/**
 * <p>Package in which resolvers reside.</p>
 *
 * <h3>Writing Custom Resolvers</h3>
 *
 * <p>Resolvers must implement either or both of the
 * {@link edu.illinois.library.cantaloupe.resolver.StreamResolver} and
 * {@link edu.illinois.library.cantaloupe.resolver.FileResolver} interfaces
 * (but probably only StreamResolver, as
 * {@link edu.illinois.library.cantaloupe.resolver.FilesystemResolver} is
 * already available for files, and is customizable enough meet virtually any
 * need). These are documented in the source code and are pretty simple.
 * Inheriting from AbstractResolver will implement a couple of them for
 * free.</p>
 *
 * <p>To add a custom resolver, the
 * {@link edu.illinois.library.cantaloupe.resolver.ResolverFactory#getAllResolvers()}
 * method must be modified to return it. Then, it will be available for use
 * like any other resolver.</p>
 */
package edu.illinois.library.cantaloupe.resolver;