package edu.illinois.library.cantaloupe.processor.codec.jpeg;

import java.nio.charset.StandardCharsets;

final class Constants {

    /**
     * Header immediately following an {@literal APP1} segment marker
     * indicating that the segment contains EXIF data.
     */
    static final byte[] EXIF_SEGMENT_HEADER =
            "Exif\0\0".getBytes(StandardCharsets.US_ASCII);

    static final String EXTENDED_XMP_PREDICATE =
            "http://ns.adobe.com/xmp/note/HasExtendedXMP";

    /**
     * Header immediately following an {@literal APP1} segment marker
     * indicating that the segment contains "ExtendedXMP" XMP data.
     */
    static final byte[] EXTENDED_XMP_SEGMENT_HEADER =
            "http://ns.adobe.com/xmp/extension/\0".getBytes(StandardCharsets.US_ASCII);

    /**
     * Header immediately following an {@literal APP2} segment marker
     * indicating that the segment contains an ICC profile.
     */
    static final byte[] ICC_SEGMENT_HEADER =
            "ICC_PROFILE\0".getBytes(StandardCharsets.US_ASCII);

    /**
     * Header immediately following an {@literal APP13} segment marker
     * indicating that the segment contains Photoshop/IPTC data.
     */
    static final byte[] PHOTOSHOP_SEGMENT_HEADER =
            "Photoshop 3.0\0".getBytes(StandardCharsets.US_ASCII);

    /**
     * Header immediately following an {@literal APP1} segment marker
     * indicating that the segment contains "StandardXMP" XMP data.
     */
    static final byte[] STANDARD_XMP_SEGMENT_HEADER =
            "http://ns.adobe.com/xap/1.0/\0".getBytes(StandardCharsets.US_ASCII);

    private Constants() {}

}
