/**
 * <p>Servlet and HTTP resource classes.</p>
 *
 * <p>There is only a single Servlet class, {@link
 * edu.illinois.library.cantaloupe.resource.HandlerServlet}, which serves as a
 * router and "front controller," dispatching request-handling to the various
 * {@link edu.illinois.library.cantaloupe.resource.AbstractResource} subclasses
 * &mdash;of which there is generally one per URI path&mdash;in this package
 * and underneath it.</p>
 */
package edu.illinois.library.cantaloupe.resource;
