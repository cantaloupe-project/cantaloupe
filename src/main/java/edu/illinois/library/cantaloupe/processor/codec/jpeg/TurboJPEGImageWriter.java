package edu.illinois.library.cantaloupe.processor.codec.jpeg;

import org.libjpegturbo.turbojpeg.TJ;
import org.libjpegturbo.turbojpeg.TJCompressor;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;

/**
 * @see org.libjpegturbo.turbojpeg for libjpeg-turbo setup.
 * @author Alex Dolski UIUC
 */
public final class TurboJPEGImageWriter {

    /**
     * SOI (2 bytes) + APP0 (18 bytes)
     */
    private static final int APP1_OFFSET         = 20;
    private static final int DEFAULT_QUALITY     = 80;
    private static final int DEFAULT_SUBSAMPLING = TJ.SAMP_444;

    private int quality     = DEFAULT_QUALITY;
    private int subsampling = DEFAULT_SUBSAMPLING;
    private boolean useFastDCT, useAccurateDCT;
    private boolean isProgressive;
    private String xmp;

    public void setProgressive(boolean isProgressive) {
        this.isProgressive = isProgressive;
    }

    /**
     * @param quality Compression quality. This is ignored when the image to
     *                to be written is already compressed.
     */
    public void setQuality(int quality) {
        this.quality = quality;
    }

    public void setSubsampling(int subsampling) {
        this.subsampling = subsampling;
    }

    /**
     * Use the most accurate DCT/IDCT algorithm available in the underlying
     * codec. The default if this flag is not specified is implementation-
     * specific. For example, the implementation of TurboJPEG for
     * libjpeg[-turbo] uses the fast algorithm by default when compressing,
     * because this has been shown to have only a very slight effect on
     * accuracy.
     */
    public void setUseAccurateDCT(boolean useAccurateDCT) {
        this.useAccurateDCT = useAccurateDCT;
        this.useFastDCT     = !useAccurateDCT;
    }

    /**
     * @see #setUseAccurateDCT(boolean)
     */
    public void setUseFastDCT(boolean useFastDCT) {
        this.useFastDCT     = useFastDCT;
        this.useAccurateDCT = !useFastDCT;
    }

    /**
     * @param xmp XMP string (with an outermost {@literal rdf:RDF} element) to
     *            embed in an {@literal APP1} segment.
     */
    public void setXMP(String xmp) {
        this.xmp = xmp;
    }

    private int getFlags() {
        int flags = 0;
        if (isProgressive) {
            flags |= TJ.FLAG_PROGRESSIVE;
        }
        if (useFastDCT) {
            flags |= TJ.FLAG_FASTDCT;
        } else if (useAccurateDCT) {
            flags |= TJ.FLAG_ACCURATEDCT;
        }
        return flags;
    }

    public void write(TurboJPEGImage image,
                      OutputStream os) throws IOException { // TODO: XMP
        if (image.isDecompressed()) {
            try (TJCompressor tjc = new TJCompressor()) {
                tjc.setSubsamp(subsampling);
                tjc.setJPEGQuality(quality);
                tjc.setSourceImage(image.getData(), 0, 0,
                        image.getScaledWidth(),
                        0,           // pitch
                        image.getScaledHeight(),
                        TJ.PF_BGRX); // pixel format
                byte[] jpegBuf = tjc.compress(getFlags());
                os.write(jpegBuf, 0, tjc.getCompressedSize());
            }
        } else {
            os.write(image.getData(), 0, image.getDataLength());
        }
    }

    /**
     * @param image Image to write.
     * @param os    Stream to write to.
     */
    public void write(BufferedImage image,
                      OutputStream os) throws IOException {
        try (TJCompressor tjc = new TJCompressor()) {
            tjc.setSubsamp(subsampling);
            tjc.setJPEGQuality(quality);
            tjc.setSourceImage(image, 0, 0, 0, 0);
            byte[] jpegBuf = tjc.compress(getFlags());
            // Write SOI
            os.write(jpegBuf, 0, APP1_OFFSET);
            // Write the APP1 segment, if necessary.
            if (xmp != null) {
                os.write(Util.assembleAPP1Segment(xmp));
            }
            // Write the rest of the image data.
            os.write(jpegBuf, APP1_OFFSET, tjc.getCompressedSize() - APP1_OFFSET);
        }
    }

}
