/**
 * <p>Contains an EXIF directory reader and supporting classes.</p>
 *
 * <p>N.B.: There is some functionality overlap in this package with the {@link
 * javax.imageio.plugins.tiff}. Reasons for not using that include supporting
 * arbitrary tags and tag sets, having more control over serialization, and
 * decoupling from Image I/O.</p>
 *
 * @see <a href="http://www.cipa.jp/std/documents/e/DC-008-Translation-2016-E.pdf">
 *     Exchangeable image file format for digital still cameras version 2.31</a>
 * @since 5.0
 */
package edu.illinois.library.cantaloupe.image.exif;
