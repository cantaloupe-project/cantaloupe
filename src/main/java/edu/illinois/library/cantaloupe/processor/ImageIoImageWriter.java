package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.image.OutputFormat;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.channels.WritableByteChannel;
import java.util.Iterator;

/**
 * Image writer using ImageIO.
 */
class ImageIoImageWriter {

    /**
     * Writes an image to the given byte channel.
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
            default:
                // TODO: jp2 doesn't seem to work
                ImageIO.write(image, outputFormat.getExtension(),
                        ImageIO.createImageOutputStream(writableChannel));
                break;
        }
    }

}
