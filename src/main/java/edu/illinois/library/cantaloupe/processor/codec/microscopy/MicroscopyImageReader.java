package edu.illinois.library.cantaloupe.processor.codec.microscopy;

import edu.illinois.library.cantaloupe.image.*;
import edu.illinois.library.cantaloupe.image.Dimension;
import edu.illinois.library.cantaloupe.image.Rectangle;
import edu.illinois.library.cantaloupe.operation.*;
import edu.illinois.library.cantaloupe.processor.codec.BufferedImageSequence;
import edu.illinois.library.cantaloupe.processor.codec.ReaderHint;
import edu.illinois.library.cantaloupe.source.StreamFactory;
import loci.formats.FormatException;
import loci.formats.MetadataTools;
import loci.formats.gui.BufferedImageReader;
import loci.formats.meta.IMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.stream.ImageInputStream;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Set;

public class MicroscopyImageReader implements edu.illinois.library.cantaloupe.processor.codec.ImageReader {

    private final BufferedImageReader reader;

    private static final Logger LOGGER =
            LoggerFactory.getLogger(MicroscopyImageReader.class);
    private final Color[] channels_colors = {
            new Color(0xFF0000),
            new Color(0x00FF00),
            new Color(0x0000FF),
            new Color(0xFFFF00),
            new Color(0x00FFFF),
            new Color(0xFF00FF),
            new Color(0xFFAA00),
            new Color(0x7F00FF)
    };

    public MicroscopyImageReader() {
        reader = BufferedImageReader.makeBufferedImageReader(new loci.formats.ImageReader());
        IMetadata omeMeta = MetadataTools.createOMEXMLMetadata();
        reader.setMetadataStore(omeMeta);
        reader.setFlattenedResolutions(false);
    }

    @Override
    public boolean canSeek() {
        return true;
    }

    @Override
    public void dispose() {
        try {
            reader.close(false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Compression getCompression(int imageIndex) {
        return Compression.UNDEFINED;
    }

    @Override
    public Metadata getMetadata(int imageIndex) {
        return new Metadata();
    }

    @Override
    public int getNumImages() {
        return 1;
    }

    @Override
    public int getNumResolutions() {
        return reader.getResolutionCount();
    }

    @Override
    public Dimension getSize(int imageIndex) {
        final int width = reader.getSizeX();
        final int height = reader.getSizeY();
        return new Dimension(width, height);
    }

    @Override
    public Dimension getTileSize(int imageIndex) {
        final int width = reader.getOptimalTileWidth();
        final int height = reader.getOptimalTileHeight();
        return new Dimension(width, height);
    }

    @Override
    public BufferedImage read() throws IOException {
        return null;
    }

    /**
     * <p>Override that is multi-resolution and tile-aware.</p>
     *
     * <p>After reading, clients should check the reader hints to see whether
     * the returned image will require additional cropping.</p>
     */
    @Override
    public BufferedImage read(Crop crop, Scale scale, ScaleConstraint scaleConstraint, ReductionFactor reductionFactor, Set<ReaderHint> hints) throws IOException {
        if (crop == null) {
            crop = new CropByPercent();
        }
        if (scale == null) {
            scale = new ScaleByPercent();
        }
        BufferedImage image = readSmallestUsableSubimage(crop, scale, scaleConstraint, reductionFactor);
        hints.add(ReaderHint.ALREADY_CROPPED);
        return image;
    }

    @Override
    public RenderedImage readRendered(Crop crop, Scale scale, ScaleConstraint scaleConstraint, ReductionFactor reductionFactor, Set<ReaderHint> hints) throws IOException {
        return null;
    }

    @Override
    public BufferedImageSequence readSequence() throws IOException {
        return null;
    }

    @Override
    public void setSource(Path imageFile) throws IOException {
        try {
            reader.close(true);
            reader.setId(imageFile.toString());
        } catch (FormatException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void setSource(ImageInputStream inputStream) throws IOException {
        throw new IOException("Stream source not supported");
    }

    @Override
    public void setSource(StreamFactory streamFactory) throws IOException {
        throw new IOException("Stream source not supported");
    }

    /**
     * Reads the smallest image that can fulfill the given crop and scale from
     * a multi-resolution image.
     *
     * @param crop            Requested crop.
     * @param scale           Requested scale.
     * @param scaleConstraint Virtual scale constraint applied to the image.
     * @param reductionFactor Will be set to the reduction factor of the
     *                        returned image.
     * @return                Smallest image fitting the requested operations.
     */
    private BufferedImage readSmallestUsableSubimage (
            final Crop crop,
            final Scale scale,
            final ScaleConstraint scaleConstraint,
            final ReductionFactor reductionFactor) throws IOException {
        reader.setResolution(0);
        final Dimension fullSize = getSize(0);
        final Rectangle regionRect = crop.getRectangle(
                fullSize, new ReductionFactor(), scaleConstraint);
        BufferedImage bestImage = null;

        int numResolutions = reader.getResolutionCount();

        for (int i = numResolutions - 1; i >= 0; i--) {
            reader.setResolution(i);
            final int subimageWidth = reader.getSizeX();
            final int subimageHeight = reader.getSizeY();

            final double reducedScale =  (double) subimageWidth / fullSize.width();
            if (fits(regionRect.size(), scale, scaleConstraint, reducedScale)) {
                reductionFactor.factor = ReductionFactor.forScale(reducedScale).factor;
                LOGGER.debug("Subimage {}: {}x{} - fits! " + "({}x reduction factor)",
                        i, subimageWidth, subimageHeight,
                        reductionFactor.factor);
                int x = (int)(regionRect.x() * reducedScale);
                int y = (int)(regionRect.y() * reducedScale);
                int width = (int)(regionRect.width() * reducedScale);
                int height = (int)(regionRect.height() * reducedScale);

                // crop requested region if it exceeds image dimensions
                // otherwise the Bio-formats reader will return an invalid tile size error
                width = (x + width > subimageWidth) ? subimageWidth - x : width;
                height = (y + height > subimageHeight) ? subimageWidth - y : height;

                final Rectangle reducedRect = new Rectangle(x, y, width, height);

                try {
                    if (reader.getEffectiveSizeC() == 1) {
                        // image is grayscale or RGB
                        bestImage = reader.openImage(0, reducedRect.intX(), reducedRect.intY(), reducedRect.intWidth(), reducedRect.intHeight());
                    } else {
                        // image has multiple channels (e.g. fluorescence microscopy image)
                        // assign a different color to each channel and merge them using alpha compositing
                        if (reader.getEffectiveSizeC() > channels_colors.length) {
                            throw new IOException(MessageFormat.format("A maximum of {0} channels is supported (image has {1} channels)", channels_colors.length, reader.getEffectiveSizeC()));
                        }
                        bestImage = new BufferedImage(reducedRect.intWidth(), reducedRect.intHeight(), BufferedImage.TYPE_INT_ARGB);
                        Graphics2D g = bestImage.createGraphics();
                        g.setBackground(Color.BLACK);
                        g.clearRect(0,0,reducedRect.intWidth(), reducedRect.intHeight());
                        g.setComposite(AlphaComposite.SrcAtop);

                        for (int c = 0; c < reader.getEffectiveSizeC(); c++) {
                            int plane = reader.getIndex(0, c, 0);
                            BufferedImage image = reader.openImage(plane, reducedRect.intX(), reducedRect.intY(), reducedRect.intWidth(), reducedRect.intHeight());
                            image = gray_to_RGBA(image, channels_colors[c]);
                            g.drawImage(image, 0, 0, null);
                        }
                        g.dispose();
                    }
                } catch (FormatException e) {
                    throw new IOException(e);
                }
                break;
            } else {
                LOGGER.trace("Subimage {}: {}x{} - too small",
                        i, subimageWidth, subimageHeight);
            }
        }
        reader.setResolution(0);
        return bestImage;
    }

    /**
     * @param regionSize      Size of a cropped source image region.
     * @param scale           Requested scale.
     * @param scaleConstraint Scale constraint to be applied to the requested
     *                        scale.
     * @param reducedScale    Reduced scale of a pyramid level.
     * @return                Whether the given source image region can be
     *                        satisfied by the given reduced scale at the
     *                        requested scale.
     */
    private boolean fits(final Dimension regionSize,
                         final Scale scale,
                         final ScaleConstraint scaleConstraint,
                         final double reducedScale) {
        if (scale instanceof ScaleByPercent) {
            return fits((ScaleByPercent) scale, scaleConstraint, reducedScale);
        }
        return fits(regionSize, (ScaleByPixels) scale, reducedScale);
    }

    private boolean fits(final ScaleByPercent scale,
                         final ScaleConstraint scaleConstraint,
                         final double reducedScale) {
        final double scScale = scaleConstraint.getRational().doubleValue();
        double cappedScale = (scale.getPercent() > 1) ? 1 : scale.getPercent();
        return (cappedScale * scScale <= reducedScale);
    }

    private boolean fits(final Dimension regionSize,
                         final ScaleByPixels scale,
                         final double reducedScale) {
        switch (scale.getMode()) {
            case ASPECT_FIT_WIDTH:
                double cappedWidth = (scale.getWidth() > regionSize.width()) ?
                        regionSize.width() : scale.getWidth();
                return (cappedWidth / regionSize.width() <= reducedScale);
            case ASPECT_FIT_HEIGHT:
                double cappedHeight = (scale.getHeight() > regionSize.height()) ?
                        regionSize.height() : scale.getHeight();
                return (cappedHeight / regionSize.height() <= reducedScale);
            default:
                cappedWidth = (scale.getWidth() > regionSize.width()) ?
                        regionSize.width() : scale.getWidth();
                cappedHeight = (scale.getHeight() > regionSize.height()) ?
                        regionSize.height() : scale.getHeight();
                return (cappedWidth / regionSize.width() <= reducedScale &&
                        cappedHeight / regionSize.height() <= reducedScale);
        }
    }

    private BufferedImage gray_to_RGBA(BufferedImage srcImage, Color color) {
        int w = srcImage.getWidth();
        int h = srcImage.getHeight();
        float[] RGBA = new float[4];
        color.getRGBComponents(RGBA);

        BufferedImage destImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        WritableRaster srcRaster = srcImage.getRaster();
        WritableRaster destRaster = destImage.getRaster();
        int[] srcSamples = new int[w*h];
        int[][] destSamples = new int[4][w*h];
        srcRaster.getSamples(0,0,w, h, 0, srcSamples);

        for (int i=0; i<srcSamples.length; i++) {
            int gray = srcSamples[i];
            for (int c=0; c<4; c++) {
                destSamples[c][i] = (int)(gray*RGBA[c]);
            }
        }
        for (int c=0; c<4; c++) {
            destRaster.setSamples(0,0,w,h, c, destSamples[c]);
        }
        return destImage;
    }
}