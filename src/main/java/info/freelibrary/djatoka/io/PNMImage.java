/*
 * This file is part of Mapyrus, software for plotting maps.
 * Copyright (C) 2003 - 2011 Simon Chenery.
 *
 * Mapyrus is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Mapyrus is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Mapyrus; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 * ===========================================================================
 *
 * Reusing this in freelibrary-djatoka to get rid of current JAI dependency; I
 * also made a few minor changes (changed exceptions, parameters passed, etc.)
 *
 * Kevin S. Clarke <ksclarke@gmail.com>
 */

package info.freelibrary.djatoka.io;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/*
 * Holds an image read from a Netpbm PPM and PGM format image files.
 */
public class PNMImage {

    private BufferedImage m_image;

    /**
     * Read Netpbm PNM image from file.
     *
     * @param filename name of file.
     */
    public PNMImage(String filename) throws IOException {
        this(new FileInputStream(filename));
    }

    /**
     * Read Netpbm PNM image from open stream.
     *
     * @param aInputStream A Netpbm PNM image's input stream
     */
    public PNMImage(InputStream aInputStream) throws IOException {
        DataInputStream stream = new DataInputStream(new BufferedInputStream(aInputStream));

        try {
            /*
             * Check for 'P5' or 'P6' magic number in file.
             */
            int magic1 = stream.read();
            int magic2 = stream.read();
            boolean isGreyscale;
            boolean isBitmap;

            if (magic1 == 'P' && magic2 == '6') {
                isGreyscale = isBitmap = false;
            } else if (magic1 == 'P' && magic2 == '5') {
                isGreyscale = true;
                isBitmap = false;
            } else if (magic1 == 'P' && magic2 == '4') {
                isBitmap = true;
                isGreyscale = false;
            } else {
                throw new IOException("Bad PPM magic number: " + magic1 + magic2);
            }

            /*
             * Read image header.
             */
            int width = readNumber(stream);
            int height = readNumber(stream);
            int maxValue = 1;
            if (!isBitmap) {
                maxValue = readNumber(stream);
            }

            int nBytes = (maxValue < 256) ? 1 : 2;

            /*
             * Read pixel values into image.
             */
            m_image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

            int nextByte = 0;
            for (int y = 0; y < height; y++) {
                int bitMask = 0;
                for (int x = 0; x < width; x++) {
                    int r, g, b, pixel;
                    if (nBytes == 1) {
                        if (isBitmap) {
                            /*
                             * Extract pixel from next bit.
                             */
                            if (bitMask == 0) {
                                nextByte = stream.read();
                                bitMask = 128;
                            }
                            r = g = b = (((nextByte & bitMask) != 0) ? 0 : 255);
                            bitMask >>= 1;
                        } else if (isGreyscale) {
                            r = g = b = stream.read();
                        } else {
                            r = stream.read();
                            g = stream.read();
                            b = stream.read();
                        }
                    } else {
                        if (isGreyscale) {
                            r = g = b = stream.readShort();
                        } else {
                            r = stream.readShort();
                            g = stream.readShort();
                            b = stream.readShort();
                        }
                    }
                    pixel = (r << 16) | (g << 8) | b;
                    m_image.setRGB(x, y, pixel);
                }
            }
        } finally {
            try {
                stream.close();
            } catch (IOException e) {
            }
        }
    }

    /**
     * Read decimal number from stream.
     *
     * @param stream stream to read from.
     * @return number read from stream.
     */
    private int readNumber(InputStream stream) throws IOException {
        int retval = 0;
        int c = stream.read();
        boolean inComment = (c == '#');
        while (c != -1 && (inComment || Character.isWhitespace((char) c))) {
            c = stream.read();
            if (c == '#') {
                inComment = true;
            } else if (inComment && (c == '\r' || c == '\n')) {
                inComment = false;
            }
        }

        while (c >= '0' && c <= '9') {
            retval = retval * 10 + (c - '0');
            c = stream.read();
        }
        return (retval);
    }

    /**
     * Get Netpbm PNM image as buffered image.
     *
     * @return image.
     */
    public BufferedImage getBufferedImage() {
        return (m_image);
    }

    /**
     * Write an image to a Netpbm PPM format file.
     *
     * @param image image to write
     * @param stream output stream to write image to.
     */
    public static void write(BufferedImage image, OutputStream stream) throws IOException {
        /*
         * Write file header.
         */
        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();
        stream.write('P');
        stream.write('6');
        stream.write('\n');
        stream.write(Integer.toString(imageWidth).getBytes());
        stream.write(' ');
        stream.write(Integer.toString(imageHeight).getBytes());
        stream.write('\n');
        stream.write(Integer.toString(255).getBytes());
        stream.write('\n');

        /*
         * Write each row of pixels.
         */
        for (int y = 0; y < imageHeight; y++) {
            for (int x = 0; x < imageWidth; x++) {
                int pixel = image.getRGB(x, y);
                int b = (pixel & 0xff);
                int g = ((pixel >> 8) & 0xff);
                int r = ((pixel >> 16) & 0xff);
                stream.write(r);
                stream.write(g);
                stream.write(b);
            }
        }
        stream.flush();
    }
}