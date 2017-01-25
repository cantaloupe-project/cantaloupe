/**
 * <p>Package in which processors reside.</p>
 *
 * <h3>Writing Custom Processors</h3>
 *
 * <p>Processors must implement either or both of the
 * {@link edu.illinois.library.cantaloupe.processor.StreamProcessor} and
 * {@link edu.illinois.library.cantaloupe.processor.FileProcessor} interfaces.
 * Implementing StreamProcessor will enable the processor to work with any
 * resolver, and if it can provide performance equal to FileProcessor with all
 * resolvers (this will depend on its implementation), then there is no need to
 * implement FileProcessor. FileProcessor should be implemented only by
 * processors that can't read (as efficiently or at all) from streams.</p>
 *
 * <p>Format availability (or "awareness") is governed by the
 * {@link edu.illinois.library.cantaloupe.image.Format} enum. If this does not
 * already contain the formats you wish to support, you will need to add
 * them.</p>
 *
 * <p>Once the custom processor is written, the
 * {@link edu.illinois.library.cantaloupe.processor.ProcessorFactory#getAllProcessors()}
 * method must be modified to return it. Then, it can be used like any other
 * processor.</p>
 */
package edu.illinois.library.cantaloupe.processor;