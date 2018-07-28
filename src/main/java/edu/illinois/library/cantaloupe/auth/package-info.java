/**
 * <p>Contains authentication- and authorization-related classes.</p>
 *
 * <h1>Authentication</h1>
 *
 * <p>{@link edu.illinois.library.cantaloupe.auth.CredentialStore} can be used
 * for username/password authentication.</p>
 *
 * <h1>Authorization</h1>
 *
 * <p>The general usage pattern is to use an {@link
 * edu.illinois.library.cantaloupe.auth.AuthorizerFactory} to instantiate an
 * {@link edu.illinois.library.cantaloupe.auth.Authorizer}, whose {@link
 * edu.illinois.library.cantaloupe.auth.Authorizer#authorize()} method returns
 * an authorization result.</p>
 */
package edu.illinois.library.cantaloupe.auth;