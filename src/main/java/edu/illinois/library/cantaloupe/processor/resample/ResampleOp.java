/*
 * Copyright 2013, Morten Nobel-Joergensen
 *
 * License: The BSD 3-Clause License
 * http://opensource.org/licenses/BSD-3-Clause
 */
package edu.illinois.library.cantaloupe.processor.resample;

import edu.illinois.library.cantaloupe.async.ThreadPool;

import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.WritableRaster;
import java.util.concurrent.CountDownLatch;

/**
 * @author Morten Nobel-Joergensen
 * @author Heinz Doerr
 * @author Alex Dolski UIUC
 */
public class ResampleOp extends AdvancedResizeOp {

    private static class SubSamplingData {

        /**
         * Individual - per row or per column - number of contributions.
         */
        private final int[] arrN;

        /**
         * 2D: [width or height][contrib]
         */
        private final int[] arrPixel;

        /**
         * 2D: [width or height][contrib]
         */
        private final float[] arrWeight;

        /**
         * Primary index length for the arrPixel and arrWeight arrays.
         */
        private final int numContributors;

        private SubSamplingData(int[] arrN, int[] arrPixel, float[] arrWeight,
                                int numContributors) {
            this.arrN = arrN;
            this.arrPixel = arrPixel;
            this.arrWeight = arrWeight;
            this.numContributors = numContributors;
        }

    }

    private static final int MAX_CHANNEL_VALUE = 255;

    private static final int THREAD_COUNT =
            Runtime.getRuntime().availableProcessors();

    private int numChannels;
    private int srcWidth, srcHeight;
    private int destWidth, destHeight;
    private boolean isLinear;

    private SubSamplingData horizontalSubsamplingData;
    private SubSamplingData verticalSubsamplingData;

    /**
     * Set by {@link #setFilter(ResampleFilter)}.
     */
    private ResampleFilter filter = ResampleFilters.getLanczos3Filter();

    private static SubSamplingData createSubSampling(ResampleFilter filter,
                                                     int srcSize,
                                                     int dstSize) {
        float scale = (float) dstSize / (float) srcSize;
        int[] arrN = new int[dstSize];
        int numContributors;
        float[] arrWeight;
        int[] arrPixel;

        final float fwidth = filter.getSamplingRadius();

        float centerOffset = 0.5f / scale;

        if (scale < 1.0f) {
            final float width = fwidth / scale;
            // Add 1 to be safe with the ceiling.
            numContributors = (int) (width * 2.0f + 2);
            arrWeight = new float[dstSize * numContributors];
            arrPixel = new int[dstSize * numContributors];

            final float fNormFac = (float) (1f / (Math.ceil(width) / fwidth));

            for (int i = 0; i < dstSize; i++) {
                final int subindex = i * numContributors;
                float center = i / scale + centerOffset;
                int left = (int) Math.floor(center - width);
                int right = (int) Math.ceil(center + width);
                for (int j = left; j <= right; j++) {
                    float weight;
                    weight = filter.apply((center - j) * fNormFac);

                    if (weight == 0.0f) {
                        continue;
                    }
                    int n;
                    if (j < 0) {
                        n = -j;
                    } else if (j >= srcSize) {
                        n = srcSize - j + srcSize - 1;
                    } else {
                        n = j;
                    }
                    int k = arrN[i];
                    //assert k == j-left:String.format("%s = %s %s", k,j,left);
                    arrN[i]++;
                    if (n < 0 || n >= srcSize) {
                        weight = 0.0f;// Flag that cell should not be used
                    }
                    arrPixel[subindex + k] = n;
                    arrWeight[subindex + k] = weight;
                }
                // Normalize the filter's weights so the sum equals 1.0; very
                // important for avoiding box type of artifacts.
                final int max = arrN[i];
                float tot = 0;
                for (int k = 0; k < max; k++)
                    tot += arrWeight[subindex + k];
                if (tot != 0f) { // 0 should never happen except bug in filter
                    for (int k = 0; k < max; k++)
                        arrWeight[subindex + k] /= tot;
                }
            }
        } else {
            // super-sampling
            // Scales from smaller to bigger height
            numContributors = (int) (fwidth * 2.0f + 1);
            arrWeight = new float[dstSize * numContributors];
            arrPixel = new int[dstSize * numContributors];
            //
            for (int i = 0; i < dstSize; i++) {
                final int subindex = i * numContributors;
                final float center = i / scale + centerOffset;
                final int left = (int) Math.floor(center - fwidth);
                final int right = (int) Math.ceil(center + fwidth);

                for (int j = left; j <= right; j++) {
                    float weight = filter.apply(center - j);
                    if (weight == 0.0f) {
                        continue;
                    }
                    int n;
                    if (j < 0) {
                        n = -j;
                    } else if (j >= srcSize) {
                        n = srcSize - j + srcSize - 1;
                    } else {
                        n = j;
                    }
                    int k = arrN[i];
                    arrN[i]++;
                    if (n < 0 || n >= srcSize) {
                        weight = 0.0f;// Flag that cell should not be used
                    }
                    arrPixel[subindex + k] = n;
                    arrWeight[subindex + k] = weight;
                }
                // Normalize the filter's weights so the sum equals 1.0;
                // very important for avoiding box type of artifacts.
                final int max = arrN[i];
                float tot = 0;
                for (int k = 0; k < max; k++)
                    tot += arrWeight[subindex + k];
                assert tot != 0 : "probable bug in filter";

                for (int k = 0; k < max; k++) {
                    arrWeight[subindex + k] /= tot;
                }
            }
        }
        return new SubSamplingData(arrN, arrPixel, arrWeight, numContributors);
    }

    private ResampleOp(DimensionConstraint dimensionConstraint) {
        super(dimensionConstraint);
    }

    /**
     * @param destWidth
     * @param destHeight
     * @param isLinear Whether to use a linear RGB output image.
     *                 TurboJpegProcessor requires this to be false.
     */
    public ResampleOp(int destWidth, int destHeight, boolean isLinear) {
        this(DimensionConstraint.createAbsolutionDimension(destWidth, destHeight));
        this.isLinear = isLinear;
    }

    public ResampleFilter getFilter() {
        return filter;
    }

    public void setFilter(ResampleFilter filter) {
        this.filter = filter;
    }

    @Override
    public BufferedImage doFilter(BufferedImage srcImage,
                                  BufferedImage destImage,
                                  int dstWidth, int dstHeight) {
        this.destWidth = dstWidth;
        this.destHeight = dstHeight;

        if (dstWidth < 3 || dstHeight < 3) {
            throw new RuntimeException("Error doing rescale. Target size was " +
                    dstWidth + "x" + dstHeight + " but must be at least 3x3.");
        }

        if (srcImage.getType() == BufferedImage.TYPE_BYTE_BINARY ||
                srcImage.getType() == BufferedImage.TYPE_BYTE_INDEXED) {
            srcImage = ImageUtils.convert(srcImage, srcImage.getColorModel().hasAlpha() ?
                    BufferedImage.TYPE_4BYTE_ABGR : BufferedImage.TYPE_3BYTE_BGR);
        }
        this.numChannels = srcImage.getSampleModel().getNumBands();
        assert numChannels > 0;
        this.srcWidth = srcImage.getWidth();
        this.srcHeight = srcImage.getHeight();

        byte[][] workPixels = new byte[srcHeight][dstWidth * numChannels];

        // Pre-calculate sub-sampling
        horizontalSubsamplingData = createSubSampling(filter, srcWidth, dstWidth);
        verticalSubsamplingData = createSubSampling(filter, srcHeight, dstHeight);

        final ThreadPool threadPool = ThreadPool.getInstance();
        final CountDownLatch inputLatch = new CountDownLatch(THREAD_COUNT - 1);
        final BufferedImage scrImgCopy = srcImage;
        final byte[][] workPixelsCopy = workPixels;

        for (int i = 1; i < THREAD_COUNT; i++) {
            final int finalI = i;
            threadPool.submit(() -> {
                horizontalFromSrcToWork(scrImgCopy, workPixelsCopy, finalI,
                        THREAD_COUNT);
                inputLatch.countDown();
            });
        }
        horizontalFromSrcToWork(scrImgCopy, workPixelsCopy, 0, THREAD_COUNT);
        waitOnLatch(inputLatch);

        final CountDownLatch outputLatch = new CountDownLatch(THREAD_COUNT - 1);
        byte[] outPixels = new byte[dstWidth * dstHeight * numChannels];

        // Apply filter to sample vertically from Work to Dst.
        final byte[] outPixelsCopy = outPixels;
        for (int i = 1; i < THREAD_COUNT; i++) {
            final int finalI = i;
            threadPool.submit(() -> {
                verticalFromWorkToDst(workPixelsCopy, outPixelsCopy, finalI,
                        THREAD_COUNT);
                outputLatch.countDown();
            });
        }
        verticalFromWorkToDst(workPixelsCopy, outPixelsCopy, 0, THREAD_COUNT);
        waitOnLatch(outputLatch);

        //noinspection UnusedAssignment
        workPixels = null; // free memory
        BufferedImage out;
        if (destImage != null && dstWidth == destImage.getWidth() &&
                dstHeight == destImage.getHeight()) {
            out = destImage;
            int nrDestChannels = destImage.getSampleModel().getNumBands();
            if (nrDestChannels != numChannels) {
                String errorMsg = String.format("Source image has %d " +
                                "channels; destination image has %d channels",
                        numChannels, nrDestChannels);
                throw new RuntimeException(errorMsg);
            }
        } else if (isLinear) {
            ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_LINEAR_RGB);
            ComponentColorModel cm = new ComponentColorModel(
                    cs, false, false,
                    Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
            WritableRaster raster = cm.createCompatibleWritableRaster(
                    dstWidth, dstHeight);
            out = new BufferedImage(cm, raster, cm.isAlphaPremultiplied(), null);
        } else {
            out = new BufferedImage(dstWidth, dstHeight,
                    getResultBufferedImageType(srcImage));
        }

        ImageUtils.setBGRPixels(outPixels, out, 0, 0, dstWidth, dstHeight);

        return out;
    }

    private void waitOnLatch(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    private void verticalFromWorkToDst(byte[][] workPixels, byte[] outPixels,
                                       int start, int delta) {
        if (numChannels == 1) {
            verticalFromWorkToDstGray(workPixels, outPixels, start,
                    THREAD_COUNT);
            return;
        }
        boolean useChannel3 = numChannels > 3;
        for (int x = start; x < destWidth; x += delta) {
            final int xLocation = x * numChannels;
            for (int y = destHeight - 1; y >= 0; y--) {
                final int yTimesNumContributors =
                        y * verticalSubsamplingData.numContributors;
                final int max = verticalSubsamplingData.arrN[y];
                final int sampleLocation = (y * destWidth + x) * numChannels;

                float sample0 = 0.0f;
                float sample1 = 0.0f;
                float sample2 = 0.0f;
                float sample3 = 0.0f;
                int index = yTimesNumContributors;
                for (int j = max - 1; j >= 0; j--) {
                    int valueLocation = verticalSubsamplingData.arrPixel[index];
                    float arrWeight = verticalSubsamplingData.arrWeight[index];
                    sample0 += (workPixels[valueLocation][xLocation] & 0xff) * arrWeight;
                    sample1 += (workPixels[valueLocation][xLocation + 1] & 0xff) * arrWeight;
                    sample2 += (workPixels[valueLocation][xLocation + 2] & 0xff) * arrWeight;
                    if (useChannel3) {
                        sample3 += (workPixels[valueLocation][xLocation + 3] & 0xff) * arrWeight;
                    }
                    index++;
                }

                outPixels[sampleLocation]     = toByte(sample0);
                outPixels[sampleLocation + 1] = toByte(sample1);
                outPixels[sampleLocation + 2] = toByte(sample2);
                if (useChannel3) {
                    outPixels[sampleLocation + 3] = toByte(sample3);
                }
            }
        }
    }

    private void verticalFromWorkToDstGray(byte[][] workPixels, byte[] outPixels,
                                           int start, int delta) {
        for (int x = start; x < destWidth; x += delta) {
            for (int y = destHeight - 1; y >= 0; y--) {
                final int yTimesNumContributors =
                        y * verticalSubsamplingData.numContributors;
                final int max = verticalSubsamplingData.arrN[y];
                final int sampleLocation = (y * destWidth + x);

                float sample0 = 0.0f;
                int index = yTimesNumContributors;
                for (int j = max - 1; j >= 0; j--) {
                    int valueLocation = verticalSubsamplingData.arrPixel[index];
                    float arrWeight = verticalSubsamplingData.arrWeight[index];
                    sample0 += (workPixels[valueLocation][x] & 0xff) * arrWeight;
                    index++;
                }

                outPixels[sampleLocation] = toByte(sample0);
            }
        }
    }

    /**
     * Apply filter to sample horizontally from Src to Work
     */
    private void horizontalFromSrcToWork(BufferedImage srcImg,
                                         byte[][] workPixels,
                                         int start, int delta) {
        if (numChannels == 1) {
            horizontalFromSrcToWorkGray(srcImg, workPixels, start, delta);
            return;
        }

        // create reusable row to minimize memory overhead
        final byte[] srcPixels    = new byte[srcWidth * numChannels];
        // Used if we work on int based bitmaps
        final int[] tempPixels    = new int[srcWidth];
        final boolean useChannel3 = numChannels > 3;

        for (int k = start; k < srcHeight; k = k + delta) {
            ImageUtils.readPixelsBGR(srcImg, k, srcWidth, srcPixels, tempPixels);

            for (int i = destWidth - 1; i >= 0; i--) {
                int sampleLocation = i * numChannels;
                final int max = horizontalSubsamplingData.arrN[i];

                float sample0 = 0.0f;
                float sample1 = 0.0f;
                float sample2 = 0.0f;
                float sample3 = 0.0f;
                int index = i * horizontalSubsamplingData.numContributors;
                for (int j = max - 1; j >= 0; j--) {
                    float arrWeight = horizontalSubsamplingData.arrWeight[index];
                    int pixelIndex = horizontalSubsamplingData.arrPixel[index] * numChannels;

                    sample0 += (srcPixels[pixelIndex] & 0xff) * arrWeight;
                    sample1 += (srcPixels[pixelIndex + 1] & 0xff) * arrWeight;
                    sample2 += (srcPixels[pixelIndex + 2] & 0xff) * arrWeight;
                    if (useChannel3) {
                        sample3 += (srcPixels[pixelIndex + 3] & 0xff) * arrWeight;
                    }
                    index++;
                }

                workPixels[k][sampleLocation] = toByte(sample0);
                workPixels[k][sampleLocation + 1] = toByte(sample1);
                workPixels[k][sampleLocation + 2] = toByte(sample2);
                if (useChannel3) {
                    workPixels[k][sampleLocation + 3] = toByte(sample3);
                }
            }
        }
    }

    /**
     * Apply filter to sample horizontally from Src to Work
     */
    private void horizontalFromSrcToWorkGray(BufferedImage srcImage,
                                             byte[][] workPixels,
                                             int start, int delta) {
        // Used if we work on int-based bitmaps. Later used to keep channel
        // values
        final int[] tempPixels = new int[srcWidth];
        // Create reusable row to minimize memory overhead.
        final byte[] srcPixels = new byte[srcWidth];

        for (int k = start; k < srcHeight; k = k + delta) {
            ImageUtils.readPixelsBGR(srcImage, k, srcWidth, srcPixels, tempPixels);

            for (int i = destWidth - 1; i >= 0; i--) {
                final int max = horizontalSubsamplingData.arrN[i];
                float sample0 = 0.0f;
                int index = i * horizontalSubsamplingData.numContributors;

                for (int j = max - 1; j >= 0; j--) {
                    float arrWeight = horizontalSubsamplingData.arrWeight[index];
                    int pixelIndex = horizontalSubsamplingData.arrPixel[index];

                    sample0 += (srcPixels[pixelIndex] & 0xff) * arrWeight;
                    index++;
                }

                workPixels[k][i] = toByte(sample0);
            }
        }
    }

    private byte toByte(float f) {
        if (f < 0) {
            return 0;
        }
        if (f > MAX_CHANNEL_VALUE) {
            return (byte) MAX_CHANNEL_VALUE;
        }
        return (byte) (f + 0.5f); // add 0.5 same as Math.round
    }

    private int getResultBufferedImageType(BufferedImage srcImg) {
        return numChannels == 3 ? BufferedImage.TYPE_3BYTE_BGR :
                (numChannels == 4 ? BufferedImage.TYPE_4BYTE_ABGR :
                        (srcImg.getSampleModel().getDataType() == DataBuffer.TYPE_USHORT ?
                                BufferedImage.TYPE_USHORT_GRAY : BufferedImage.TYPE_BYTE_GRAY));
    }

}
