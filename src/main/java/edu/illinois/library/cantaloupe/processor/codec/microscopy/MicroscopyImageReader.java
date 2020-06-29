package edu.illinois.library.cantaloupe.processor.codec.microscopy;

import edu.illinois.library.cantaloupe.image.*;
import edu.illinois.library.cantaloupe.image.Dimension;
import edu.illinois.library.cantaloupe.image.Rectangle;
import edu.illinois.library.cantaloupe.operation.*;
import edu.illinois.library.cantaloupe.processor.codec.BufferedImageSequence;
import edu.illinois.library.cantaloupe.processor.codec.ReaderHint;
import edu.illinois.library.cantaloupe.source.StreamFactory;
import loci.common.DataTools;
import loci.formats.FormatException;
import loci.formats.FormatTools;
import loci.formats.ImageTools;
import loci.formats.MetadataTools;
import loci.formats.gui.BufferedImageReader;
import loci.formats.meta.IMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.stream.ImageInputStream;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.*;
import java.io.IOException;
import java.nio.*;
import java.nio.file.Path;
import java.util.Set;

public class MicroscopyImageReader implements edu.illinois.library.cantaloupe.processor.codec.ImageReader {

    private final BufferedImageReader reader;

    private static final Logger LOGGER =
            LoggerFactory.getLogger(MicroscopyImageReader.class);
    private final Color[] defaultChannelsColors = {
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

                try {
                    if (isSingleChannelImage()) {
                        // image is grayscale, RGB or RGBA
                        bestImage = reader.openImage(0, x, y, width, height);
                    } else {
                        // image has multiple channels (e.g. fluorescence microscopy image)
                        // assign a different color to each channel and merge them using alpha compositing
                        bestImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                        Graphics2D g = bestImage.createGraphics();
                        g.setBackground(Color.BLACK);
                        g.clearRect(0, 0, width, height);
                        g.setComposite(AlphaComposite.SrcOver);

                        ByteOrder byteOrder = reader.isLittleEndian() ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN;
                        int pixelType = reader.getPixelType();
                        byte[][] bytes_channels = new byte[reader.getSizeC()][];
                        Color[] channelsColors = null;

                        if (reader.getEffectiveSizeC() > 1) {
                            // channels are stored in separate planes

                            // check if channels colors are defined in the metadata
                            ome.xml.model.primitives.Color testOmeColor = ((IMetadata) reader.getMetadataStore()).getChannelColor(1, 0);
                            if (testOmeColor != null) {
                                channelsColors = new Color[reader.getSizeC()];
                            } else {
                                LOGGER.warn("No channels colors defined, using defaults");
                            }

                            for (int c = 0; c < reader.getEffectiveSizeC(); c++) {
                                int plane = reader.getIndex(0, c, 0);
                                bytes_channels[c] = reader.openBytes(plane, x, y, width, height);
                                if (channelsColors != null) {
                                    ome.xml.model.primitives.Color omeColor = ((IMetadata) reader.getMetadataStore()).getChannelColor(plane, 0);
                                    LOGGER.warn("{} {}", c, omeColor);
                                    channelsColors[c] = new Color(omeColor.getRed(), omeColor.getGreen(), omeColor.getBlue());
                                }
                            }
                        } else {
                            // channels are stored in the same plane

                            // check if channels colors are defined in the metadata
                            ome.xml.model.primitives.Color testOmeColor = ((IMetadata) reader.getMetadataStore()).getChannelColor(0, 1);
                            if (testOmeColor != null) {
                                channelsColors = new Color[reader.getSizeC()];
                            } else {
                                LOGGER.warn("No channels colors defined, using defaults");
                            }

                            int plane = reader.getIndex(0, 0, 0);
                            byte[] bytes = reader.openBytes(plane, x, y, width, height);
                            for (int c = 0; c < reader.getRGBChannelCount(); c++) {
                                bytes_channels[c] = ImageTools.splitChannels(bytes, c, reader.getRGBChannelCount(), FormatTools.getBytesPerPixel(pixelType), false, reader.isInterleaved());
                                if (channelsColors != null) {
                                    ome.xml.model.primitives.Color omeColor = ((IMetadata) reader.getMetadataStore()).getChannelColor(plane, c);
                                    channelsColors[c] = new Color(omeColor.getRed(), omeColor.getGreen(), omeColor.getBlue());
                                }
                            }
                        }
                        for (int c = 0; c < reader.getSizeC(); c++) {
                            DataBuffer dataBuffer = getDataBuffer(bytes_channels[c], pixelType, byteOrder);
                            Color color;
                            if (channelsColors != null) {
                                color = channelsColors[c];
                            } else if (c < defaultChannelsColors.length) {
                                color = defaultChannelsColors[c];
                            } else {
                                color = Color.WHITE;
                                LOGGER.warn("Maximum number of channels exceeded ({}): " +
                                                "cannot assign color to channel {}, defaulting to grayscale",
                                        defaultChannelsColors.length, c);
                            }
                            BufferedImage image = makeRGBAImage(dataBuffer, width, height, color);
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

    private boolean isSingleChannelImage() {
        int pixelType = reader.getPixelType();
        int c = reader.getSizeC();
        return (reader.getEffectiveSizeC() == 1 &&
                (c == 1 || ((c == 3 || c == 4) && FormatTools.UINT8 == pixelType)));
    }

    private DataBuffer getDataBuffer(byte[] bytes, int pixelType, ByteOrder byteOrder) throws UnsupportedOperationException {
        DataBuffer dataBuffer;
        int size = bytes.length;
        switch (pixelType) {
            case (FormatTools.UINT8):
                dataBuffer = new DataBufferByte(bytes, size);
                break;
            case (FormatTools.UINT16):
            case (FormatTools.INT16):
                size /= 2;
                ShortBuffer buffer = ByteBuffer.wrap(bytes).order(byteOrder).asShortBuffer();
                short[] shortArray = new short[size];
                buffer.get(shortArray);
                dataBuffer = new DataBufferUShort(shortArray, size);
                break;
            case (FormatTools.INT32):
                size /= 4;
                IntBuffer intBuffer = ByteBuffer.wrap(bytes).order(byteOrder).asIntBuffer();
                int[] intArray = new int[size];
                intBuffer.get(intArray);
                dataBuffer = new DataBufferInt(intArray, size);
                break;
            case (FormatTools.FLOAT):
                size /= 4;
                FloatBuffer byteBuffer = ByteBuffer.wrap(bytes).order(byteOrder).asFloatBuffer();
                float[] floatArray = new float[size];
                byteBuffer.get(floatArray);
                if (!reader.isNormalized())
                    floatArray = DataTools.normalizeFloats(floatArray);
                dataBuffer = new DataBufferFloat(floatArray, size);
                break;
            case (FormatTools.DOUBLE):
                size /= 8;
                DoubleBuffer doubleBuffer = ByteBuffer.wrap(bytes).order(byteOrder).asDoubleBuffer();
                double[] doubleArray = new double[size];
                doubleBuffer.get(doubleArray);
                if (!reader.isNormalized())
                    doubleArray = DataTools.normalizeDoubles(doubleArray);
                dataBuffer = new DataBufferDouble(doubleArray, size);
                break;
            default:
                throw new UnsupportedOperationException("Unsupported pixel type " + pixelType);
        }
        return dataBuffer;
    }

    private BufferedImage makeRGBAImage(DataBuffer dataBuffer, int w, int h, Color color) throws UnsupportedOperationException {
        BufferedImage destImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        WritableRaster destRaster = destImage.getRaster();
        int[][] destSamples = new int[4][w * h];

        for (int i = 0; i < w * h; i++) {
            int luminance;
            switch (dataBuffer.getDataType()) {
                case (DataBuffer.TYPE_BYTE):
                case (DataBuffer.TYPE_SHORT):
                case (DataBuffer.TYPE_INT):
                    luminance = dataBuffer.getElem(i);
                    break;
                case (DataBuffer.TYPE_FLOAT):
                    luminance = (int) (dataBuffer.getElemFloat(i) * 255);
                    break;
                case (DataBuffer.TYPE_DOUBLE):
                    luminance = (int) (dataBuffer.getElemDouble(i) * 255);
                    break;
                default:
                    throw new UnsupportedOperationException("Unsupported data type " + dataBuffer.getDataType());
            }
            destSamples[0][i] = color.getRed();
            destSamples[1][i] = color.getGreen();
            destSamples[2][i] = color.getBlue();
            destSamples[3][i] = luminance;
        }
        for (int c = 0; c < 4; c++) {
            destRaster.setSamples(0, 0, w, h, c, destSamples[c]);
        }
        return destImage;
    }
}