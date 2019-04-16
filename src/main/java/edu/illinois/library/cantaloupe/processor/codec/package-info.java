/**
 * <p>Contains classes related to reading/decoding and writing/coding of images
 * and image metadata.</p>
 *
 * <h1>Image I/O readers &amp; writers</h1>
 *
 * <p>{@link edu.illinois.library.cantaloupe.processor.codec.ImageReader} and
 * {@link edu.illinois.library.cantaloupe.processor.codec.ImageWriter}
 * implementations wrap {@link javax.imageio.ImageReader} and {@link
 * javax.imageio.ImageWriter} instances to augment them with improved
 * functionality, including simplified reading and writing methods, improved
 * metadata access, and more efficient handling of multi-resolution source
 * images.</p>
 *
 * <p>Instances can be obtained from {@link
 * edu.illinois.library.cantaloupe.processor.codec.ImageReaderFactory} and
 * {@link edu.illinois.library.cantaloupe.processor.codec.ImageWriterFactory},
 * respectively.</p>
 *
 * <h1>Custom readers &amp; writers</h1>
 *
 * <p>The Image I/O readers &amp; writers work well within the bounds of their
 * capabilities, but there are some things they can't do, or for which the API
 * is awkward. Where going through Image I/O would be too difficult, there are
 * also some custom classes in subpackages that access images directly.</p>
 *
 * <h1>Metadata</h1>
 *
 * <p>Metadata may be encoded in different ways in different image formats.
 * {@link edu.illinois.library.cantaloupe.image.Metadata} is an
 * interface for normalized metadata that can either be returned from a reader,
 * or assembled pretty easily, and then passed to a writer which codes it
 * differently depending on the format being written.</p>
 */
package edu.illinois.library.cantaloupe.processor.codec;
