package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.image.OutputFormat;
import it.geosolutions.imageio.plugins.tiff.TIFFImageWriteParam;
import org.apache.commons.configuration.Configuration;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.media.jai.JAI;
import javax.media.jai.RenderedOp;
import java.awt.image.BufferedImage;
import java.awt.image.renderable.ParameterBlock;
import java.io.IOException;
import java.nio.channels.WritableByteChannel;
import java.util.Iterator;

/**
 * Image writer using ImageIO.
 */
class ImageIoImageWriter {

    /**
     * Writes an image to the given channel.
     *
     * @param image Image to write
     * @param outputFormat Format of the output image
     * @param writableChannel Channel to write the image to
     * @throws IOException
     */
    public void write(BufferedImage image,
                      OutputFormat outputFormat,
                      WritableByteChannel writableChannel) throws IOException {
        switch (outputFormat) {
            case JPG:
                // JPEG doesn't support alpha, so convert to RGB or else the
                // client will interpret as CMYK
                image = Java2dUtil.removeAlpha(image);
                Iterator iter = ImageIO.getImageWritersByFormatName("jpeg");
                ImageWriter writer = (ImageWriter) iter.next();
                try {
                    ImageWriteParam param = writer.getDefaultWriteParam();
                    param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                    param.setCompressionQuality(Application.getConfiguration().
                            getFloat(Java2dProcessor.JPG_QUALITY_CONFIG_KEY, 0.7f));
                    param.setCompressionType("JPEG");
                    ImageOutputStream os = ImageIO.createImageOutputStream(writableChannel);
                    writer.setOutput(os);
                    IIOImage iioImage = new IIOImage(image, null, null);
                    writer.write(null, iioImage, param);
                } finally {
                    writer.dispose();
                }
                break;
            /*case PNG: // an alternative in case ImageIO.write() ever causes problems
                writer = ImageIO.getImageWritersByFormatName("png").next();
                ImageOutputStream os = ImageIO.createImageOutputStream(writableChannel);
                writer.setOutput(os);
                writer.write(image);
                break;*/
            case TIF:
                Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("TIFF");
                while (writers.hasNext()) {
                    writer = writers.next();
                    if (writer instanceof it.geosolutions.imageioimpl.plugins.tiff.TIFFImageWriter) {
                        final String compressionType = Application.
                                getConfiguration().
                                getString(Java2dProcessor.TIF_COMPRESSION_CONFIG_KEY);
                        final TIFFImageWriteParam param =
                                (TIFFImageWriteParam) writer.getDefaultWriteParam();
                        if (compressionType != null) {
                            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                            param.setCompressionType(compressionType);
                        }

                        final IIOImage iioImage = new IIOImage(image, null, null);
                        ImageOutputStream ios =
                                ImageIO.createImageOutputStream(writableChannel);
                        writer.setOutput(ios);
                        try {
                            writer.write(null, iioImage, param);
                            ios.flush(); // http://stackoverflow.com/a/14489406
                        } finally {
                            writer.dispose();
                        }
                    }
                }
                break;
            default:
                // TODO: jp2 doesn't seem to work
                ImageIO.write(image, outputFormat.getExtension(),
                        ImageIO.createImageOutputStream(writableChannel));
                break;
        }
    }

    /**
     * Writes an image to the given channel.
     *
     * @param image Image to write
     * @param outputFormat Format of the output image
     * @param writableChannel Channel to write the image to
     * @throws IOException
     */
    public void write(RenderedOp image,
                      OutputFormat outputFormat,
                      WritableByteChannel writableChannel) throws IOException {
        final Configuration config = Application.getConfiguration();
        switch (outputFormat) {
            case GIF:
                Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("GIF");
                if (writers.hasNext()) {
                    // GIFWriter can't deal with a non-0,0 origin ("coordinate
                    // out of bounds!")
                    ParameterBlock pb = new ParameterBlock();
                    pb.addSource(image);
                    pb.add((float) -image.getMinX());
                    pb.add((float) -image.getMinY());
                    image = JAI.create("translate", pb);

                    ImageWriter writer = writers.next();
                    ImageOutputStream os = ImageIO.
                            createImageOutputStream(writableChannel);
                    writer.setOutput(os);
                    try {
                        writer.write(image);
                        os.flush(); // http://stackoverflow.com/a/14489406
                    } finally {
                        writer.dispose();
                    }
                }
                break;
            case JP2:
                /*
                TODO: this doesn't write anything
                ImageIO.write(image, outputFormat.getExtension(),
                        ImageIO.createImageOutputStream(writableChannel));
                // and this causes an error
                writers = ImageIO.getImageWritersByFormatName("JPEG2000");
                if (writers.hasNext()) {
                    ImageWriter writer = writers.next();
                    J2KImageWriteParam j2Param = new J2KImageWriteParam();
                    j2Param.setLossless(false);
                    j2Param.setEncodingRate(Double.MAX_VALUE);
                    j2Param.setCodeBlockSize(new int[]{128, 8});
                    j2Param.setTilingMode(ImageWriteParam.MODE_DISABLED);
                    j2Param.setProgressionType("res");
                    ImageOutputStream os = ImageIO.
                            createImageOutputStream(writableChannel);
                    writer.setOutput(os);
                    IIOImage iioImage = new IIOImage(image, null, null);
                    try {
                        writer.write(null, iioImage, j2Param);
                    } finally {
                        writer.dispose();
                    }
                } */
                break;
            case JPG:
                Iterator iter = ImageIO.getImageWritersByFormatName("jpeg");
                ImageWriter writer = (ImageWriter) iter.next();
                try {
                    ImageWriteParam param = writer.getDefaultWriteParam();
                    param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                    param.setCompressionQuality(config.getFloat(
                            JaiProcessor.JPG_QUALITY_CONFIG_KEY, 0.7f));
                    param.setCompressionType("JPEG");
                    ImageOutputStream os = ImageIO.createImageOutputStream(writableChannel);
                    writer.setOutput(os);
                    // JPEGImageWriter doesn't like RenderedOps, so give it a
                    // BufferedImage
                    IIOImage iioImage = new IIOImage(image.getAsBufferedImage(), null, null);
                    writer.write(null, iioImage, param);
                } finally {
                    writer.dispose();
                }
                break;
            case PNG:
                ImageIO.write(image, outputFormat.getExtension(),
                        ImageIO.createImageOutputStream(writableChannel));
                break;
            case TIF:
                writers = ImageIO.getImageWritersByFormatName("TIFF");
                while (writers.hasNext()) {
                    writer = writers.next();
                    if (writer instanceof it.geosolutions.imageioimpl.plugins.tiff.TIFFImageWriter) {
                        final String compressionType = config.getString(
                                JaiProcessor.TIF_COMPRESSION_CONFIG_KEY);
                        final TIFFImageWriteParam param =
                                (TIFFImageWriteParam) writer.getDefaultWriteParam();
                        param.setTilingMode(ImageWriteParam.MODE_EXPLICIT);
                        param.setTiling(128, 128, 0, 0);
                        if (compressionType != null) {
                            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                            param.setCompressionType(compressionType);
                        }

                        final IIOImage iioImage = new IIOImage(image, null, null);
                        ImageOutputStream ios =
                                ImageIO.createImageOutputStream(writableChannel);
                        writer.setOutput(ios);
                        try {
                            writer.write(null, iioImage, param);
                            ios.flush(); // http://stackoverflow.com/a/14489406
                        } finally {
                            writer.dispose();
                        }
                    }
                }
                break;
        }

    }

}
