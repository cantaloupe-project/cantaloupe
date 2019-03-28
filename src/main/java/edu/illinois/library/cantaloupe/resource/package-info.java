/**
 * <p>Servlet and HTTP resource classes.</p>
 *
 * <p>There is only one Servlet class, {@link
 * edu.illinois.library.cantaloupe.resource.HandlerServlet}, which serves as a
 * "front controller," dispatching request-handling to the various {@link
 * edu.illinois.library.cantaloupe.resource.AbstractResource} subclasses in
 * this package and underneath it.</p>
 *
 * <h1>Adding request handlers</h1>
 *
 * <ol>
 *     <li>Add an {@link
 *     edu.illinois.library.cantaloupe.resource.AbstractResource} subclass
 *     overriding {@link
 *     edu.illinois.library.cantaloupe.resource.AbstractResource#doGET()}
 *     and/or the other similar methods that map to HTTP methods</li>
 *     <li>In the handler method, read information from the {@link
 *     edu.illinois.library.cantaloupe.resource.AbstractResource#getRequest()
 *     request} and/or write information to the {@link
 *     edu.illinois.library.cantaloupe.resource.AbstractResource#getResponse()
 *     response}</li>
 *     <li>Connect it to a URI path pattern in the static initializer of {@link
 *     edu.illinois.library.cantaloupe.resource.Route}</li>
 * </ol>
 */
package edu.illinois.library.cantaloupe.resource;
