package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.image.Format;
import org.apache.commons.configuration.Configuration;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.media.jai.JAI;
import javax.media.jai.OpImage;
import javax.media.jai.PlanarImage;
import java.awt.image.BufferedImage;
import java.awt.image.renderable.ParameterBlock;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Image writer using ImageIO.
 */
class ImageIoImageWriter {

    /**
     * @return Set of supported output formats.
     */
    public static Set<Format> supportedFormats() {
        return new HashSet<>(Arrays.asList(Format.GIF, Format.JPG,
                Format.PNG, Format.TIF));
    }

    /**
     * Writes an image to the given output stream.
     *
     * @param image Image to write
     * @param outputFormat Format of the output image
     * @param outputStream Stream to write the image to
     * @throws IOException
     */
    public void write(BufferedImage image,
                      final Format outputFormat,
                      final OutputStream outputStream) throws IOException {
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
                    ImageOutputStream os = ImageIO.createImageOutputStream(outputStream);
                    writer.setOutput(os);
                    IIOImage iioImage = new IIOImage(image, null, null);
                    writer.write(null, iioImage, param);
                } finally {
                    writer.dispose();
                }
                break;
            /*case PNG: // an alternative in case ImageIO.write() ever causes problems
                writer = ImageIO.getImageWritersByFormatName("png").next();
                ImageOutputStream os = ImageIO.createImageOutputStream(outputStream);
                writer.setOutput(os);
                writer.write(image);
                break;*/
            case TIF:
                Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("TIFF");
                if (writers.hasNext()) {
                    writer = writers.next();
                    final String compressionType = Application.
                            getConfiguration().
                            getString(Java2dProcessor.TIF_COMPRESSION_CONFIG_KEY);
                    final ImageWriteParam param = writer.getDefaultWriteParam();
                    if (compressionType != null) {
                        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                        param.setCompressionType(compressionType);
                    }

                    final IIOImage iioImage = new IIOImage(image, null, null);
                    ImageOutputStream ios =
                            ImageIO.createImageOutputStream(outputStream);
                    writer.setOutput(ios);
                    try {
                        writer.write(null, iioImage, param);
                        ios.flush(); // http://stackoverflow.com/a/14489406
                    } finally {
                        writer.dispose();
                    }
                }
                break;
            default:
                // TODO: jp2 doesn't seem to work
                ImageIO.write(image, outputFormat.getPreferredExtension(),
                        ImageIO.createImageOutputStream(outputStream));
                break;
        }
    }

    /**
     * Writes an image to the given output stream.
     *
     * @param image Image to write
     * @param outputFormat Format of the output image
     * @param outputStream Stream to write the image to
     * @throws IOException
     */
    @SuppressWarnings({ "deprecation" })
    public void write(PlanarImage image,
                      Format outputFormat,
                      OutputStream outputStream) throws IOException {
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
                            createImageOutputStream(outputStream);
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
                        ImageIO.createImageOutputStream(outputStream));
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
                            createImageOutputStream(outputStream);
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
                Iterator iter = ImageIO.getImageWritersByFormatName("JPEG");
                ImageWriter writer = (ImageWriter) iter.next();
                try {
                    // JPEGImageWriter will interpret a >3-band image as CMYK.
                    // So, select only the first 3 bands.
                    if (OpImage.getExpandedNumBands(image.getSampleModel(),
                            image.getColorModel()) == 4) {
                        final ParameterBlock pb = new ParameterBlock();
                        pb.addSource(image);
                        final int[] bands = { 0, 1, 2 };
                        pb.add(bands);
                        image = JAI.create("bandselect", pb, null);
                    }
                    ImageWriteParam param = writer.getDefaultWriteParam();
                    param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                    param.setCompressionQuality(config.getFloat(
                            JaiProcessor.JPG_QUALITY_CONFIG_KEY, 0.7f));
                    param.setCompressionType("JPEG");
                    ImageOutputStream os = ImageIO.createImageOutputStream(outputStream);
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
                ImageIO.write(image, outputFormat.getPreferredExtension(),
                        ImageIO.createImageOutputStream(outputStream));
                break;
            case TIF:
                writers = ImageIO.getImageWritersByFormatName("TIFF");
                if (writers.hasNext()) {
                    writer = writers.next();
                    final String compressionType = config.getString(
                            JaiProcessor.TIF_COMPRESSION_CONFIG_KEY);
                    final ImageWriteParam param = writer.getDefaultWriteParam();
                    if (compressionType != null) {
                        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                        param.setCompressionType(compressionType);
                    }

                    final IIOImage iioImage = new IIOImage(image, null, null);
                    ImageOutputStream ios =
                            ImageIO.createImageOutputStream(outputStream);
                    writer.setOutput(ios);
                    try {
                        writer.write(null, iioImage, param);
                        ios.flush(); // http://stackoverflow.com/a/14489406
                    } finally {
                        writer.dispose();
                    }
                }
                break;
        }

    }

}
