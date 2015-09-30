/*
 * Copyright (c) 2008  Los Alamos National Security, LLC.
 *
 * Los Alamos National Laboratory
 * Research Library
 * Digital Library Research & Prototyping Team
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 * 
 */

package gov.lanl.adore.djatoka.io.reader;

import info.freelibrary.djatoka.io.PNMImage;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

/**
 * Returns BufferedImage give a PNM image InputStream or file path.
 *
 * @author Ryan Chute
 * @author Kevin S. Clarke &lt;<a href="mailto:ksclarke@gmail.com">ksclarke@gmail.com</a>&gt;
 */
public class PNMReader {

    /**
     * Returns a BufferedImage instance for provided image file path.
     *
     * @param aFileName absolute file path for image file
     * @return a <code>BufferedImage</code> instance for source image file
     * @throws IOException
     */
    public BufferedImage open(final String aFileName) throws IOException {
        try {
            return new PNMImage(aFileName).getBufferedImage();
        } catch (final IOException details) {
            throw new IOException(details);
        }
    }

    /**
     * Returns a BufferedImage instance for provided InputStream
     *
     * @param aInputStream an InputStream consisting of an image bitstream
     * @return a <code>BufferedImage</code> instance for source image InputStream
     * @throws IOException
     */
    public BufferedImage open(final InputStream aInputStream) throws IOException {
        try {
            return new PNMImage(aInputStream).getBufferedImage();
        } catch (final IOException details) {
            throw new IOException(details);
        }
    }

}