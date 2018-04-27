/**
 * <p>Package in which processors reside.</p>
 *
 * <p>A processor is an encapsulation of an image codec and processing library.
 * Given a reference to a source image, it applies the
 * {@link edu.illinois.library.cantaloupe.operation.Operation operations} from
 * an {@link edu.illinois.library.cantaloupe.operation.OperationList} in
 * order, and writes the result to an {@link java.io.OutputStream}. In this way
 * it is source- and output-agnostic (it doesn't care where the source image is
 * coming from or going to).</p>
 *
 * <h3>Reading from files vs. streams</h3>
 *
 * <p>Processors must implement either or both of the
 * {@link edu.illinois.library.cantaloupe.processor.FileProcessor} and
 * {@link edu.illinois.library.cantaloupe.processor.StreamProcessor} interfaces,
 * which enable them to read from {@link java.io.File files} and/or
 * streams obtained from a
 * {@link edu.illinois.library.cantaloupe.source.StreamFactory}. A
 * {@link edu.illinois.library.cantaloupe.processor.StreamProcessor} will work
 * with any source, and if it can use streams as efficiently as direct file
 * access, then there is no need to implement
 * {@link edu.illinois.library.cantaloupe.processor.FileProcessor}.
 * {@link edu.illinois.library.cantaloupe.processor.FileProcessor} should be
 * implemented only by processors that can't read (as efficiently or at all)
 * from streams.</p>
 *
 * <h3>Writing custom processors</h3>
 *
 * <p>As mentioned above, a processor must implement either or both of the
 * {@link edu.illinois.library.cantaloupe.processor.FileProcessor} and
 * {@link edu.illinois.library.cantaloupe.processor.StreamProcessor} interfaces.
 * Once it has been written, the
 * {@link edu.illinois.library.cantaloupe.processor.ProcessorFactory#getAllProcessors()}
 * method must be modified to return it. Then, it can be used like any other
 * processor.</p>
 *
 * <p>Format availability (or "awareness") is governed by the
 * {@link edu.illinois.library.cantaloupe.image.Format} enum. If a processor
 * wishes to support a format not contained therein, it must be added.</p>
 *
 * <h3>Other means of adding format support</h3>
 *
 * <p>Adding new processors is one way of adding image codec support to the
 * application. However, note that the {@link javax.imageio.ImageIO} framework
 * used by {@link edu.illinois.library.cantaloupe.processor.Java2dProcessor}
 * and {@link edu.illinois.library.cantaloupe.processor.JaiProcessor} supports
 * format plugins, which are trivial to make available in these processors.
 * ImageIO also supports image access via an
 * {@link javax.imageio.stream.ImageInputStream ImageInputStream}, which can
 * offer major efficiency advantages.</p>
 */
package edu.illinois.library.cantaloupe.processor;