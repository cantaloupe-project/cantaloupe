package edu.illinois.library.cantaloupe.processor.codec.tiff;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.image.Compression;
import edu.illinois.library.cantaloupe.image.Dimension;
import edu.illinois.library.cantaloupe.image.Metadata;
import edu.illinois.library.cantaloupe.image.ScaleConstraint;
import edu.illinois.library.cantaloupe.operation.Crop;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.operation.CropByPercent;
import edu.illinois.library.cantaloupe.operation.Scale;
import edu.illinois.library.cantaloupe.operation.ReductionFactor;
import edu.illinois.library.cantaloupe.operation.ScaleByPercent;
import edu.illinois.library.cantaloupe.processor.SourceFormatException;
import edu.illinois.library.cantaloupe.processor.codec.AbstractIIOImageReader;
import edu.illinois.library.cantaloupe.processor.codec.ImageReader;
import edu.illinois.library.cantaloupe.processor.codec.ReaderHint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.NodeList;

import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class TIFFImageReader extends AbstractIIOImageReader
        implements ImageReader {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(TIFFImageReader.class);

    static final String IMAGEIO_PLUGIN_CONFIG_KEY =
            "processor.imageio.tif.reader";

    static {
        // See: https://github.com/geosolutions-it/imageio-ext/wiki/TIFF-plugin
        System.setProperty("it.geosolutions.codec.tiff.lazy", "true");
    }

    @Override
    public boolean canSeek() {
        return true;
    }

    @Override
    protected String[] getApplicationPreferredIIOImplementations() {
        // The GeoSolutions TIFF reader supports BigTIFF among other
        // enhancements. The Sun reader will do as a fallback, although there
        // shouldn't be any need to fall back.
        String[] impls = new String[2];
        impls[0] = it.geosolutions.imageioimpl.plugins.tiff.TIFFImageReader.class.getName();
        impls[1] = "com.sun.imageio.plugins.tiff.TIFFImageReader";

        return impls;
    }

    @Override
    public Compression getCompression(int imageIndex) throws IOException {
        // Throw any contract-required exceptions.
        getSize(0);
        final IIOMetadata metadata = iioReader.getImageMetadata(imageIndex);
        final IIOMetadataNode node =
                (IIOMetadataNode) metadata.getAsTree(metadata.getNativeMetadataFormatName());
        final NodeList fields = node.getElementsByTagName("TIFFField");

        String compStr = "";
        for (int i = 0; i < fields.getLength(); i++) {
            if ("259".equals(fields.item(i).getAttributes().getNamedItem("number").getNodeValue())) {
                compStr = fields.item(i).getChildNodes().item(0).
                        getChildNodes().item(0).getAttributes().
                        getNamedItem("description").getNodeValue();
                break;
            }
        }

        switch (compStr) {
            case "JPEG":
                return Compression.JPEG;
            case "LZW":
                return Compression.LZW;
            case "PackBits":
                return Compression.RLE;
            case "Uncompressed":
                return Compression.UNCOMPRESSED;
            case "ZLib":
                return Compression.DEFLATE;
            default:
                return Compression.UNDEFINED;
        }
    }

    @Override
    protected Format getFormat() {
        return Format.get("tif");
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

    @Override
    public Metadata getMetadata(int imageIndex) throws IOException {
        // Throw any contract-required exceptions.
        getSize(0);
        final IIOMetadata metadata  = iioReader.getImageMetadata(imageIndex);
        final String metadataFormat = metadata.getNativeMetadataFormatName();
        return new TIFFMetadata(metadata, metadataFormat);
    }

    /**
     * Only a pyramidal TIFF will have more than one resolution; but there is
     * no simple flag in a pyramidal TIFF that indicates that it's pyramidal.
     * For files that contain multiple images, this method checks the
     * dimensions of each, and if they look to make up a pyramid, returns their
     * count. If not, it returns {@code 1}.
     */
    @Override
    public int getNumResolutions() throws IOException {
        return isPyramidal() ? getNumImages() : 1;
    }

    @Override
    protected String getUserPreferredIIOImplementation() {
        Configuration config = Configuration.getInstance();
        return config.getString(IMAGEIO_PLUGIN_CONFIG_KEY);
    }

    private boolean isPyramidal() throws IOException {
        final int numImages = getNumImages();
        final List<Dimension> sizes = new ArrayList<>(numImages);
        for (int i = 0; i < numImages; i++) {
            sizes.add(getSize(i));
        }
        return Dimension.isPyramid(sizes);
    }

    /**
     * <p>Override that is multi-resolution- and tile-aware.</p>
     *
     * <p>After reading, clients should check the reader hints to see whether
     * the returned image will require additional cropping.</p>
     */
    @Override
    public BufferedImage read(int imageIndex,
                              Crop crop,
                              Scale scale,
                              final ScaleConstraint scaleConstraint,
                              final ReductionFactor reductionFactor,
                              final Set<ReaderHint> hints) throws IOException {
        if (crop == null) {
            crop = new CropByPercent();
        }
        if (scale == null) {
            scale = new ScaleByPercent();
        }

        BufferedImage image;
        if (hints != null && hints.contains(ReaderHint.IGNORE_CROP)) {
            image = read(imageIndex);
        } else if (getNumResolutions() > 1) {
            try {
                image = readSmallestUsableSubimage(
                        crop, scale, scaleConstraint, reductionFactor,
                        hints);
            } catch (IndexOutOfBoundsException e) {
                throw new SourceFormatException();
            }
        } else {
            image = readMonoResolution(
                    imageIndex, crop, scaleConstraint, hints);
        }
        if (image == null) {
            throw new SourceFormatException(iioReader.getFormatName());
        }
        return image;
    }

}
