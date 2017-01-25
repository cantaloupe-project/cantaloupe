package edu.illinois.library.cantaloupe.image;

import org.apache.commons.lang3.StringUtils;
import org.apache.tika.detect.Detector;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class MediaType {

    private String subtype;
    private String type;

    /**
     * @param file File to probe.
     * @return Media types associated with the given file.
     * @throws IOException
     */
    public static List<MediaType> detectMediaTypes(File file)
            throws IOException {
        final List<MediaType> types = new ArrayList<>();
        try (TikaInputStream is = TikaInputStream.get(file.toPath())) {
            AutoDetectParser parser = new AutoDetectParser();
            Detector detector = parser.getDetector();
            Metadata md = new Metadata();
            md.add(Metadata.RESOURCE_NAME_KEY, file.getAbsolutePath());
            org.apache.tika.mime.MediaType mediaType = detector.detect(is, md);
            types.add(new MediaType(mediaType.toString()));
        }
        return types;
    }

    /**
     * No-op constructor.
     */
    public MediaType() {
    }

    /**
     * @param mediaType
     * @throws IllegalArgumentException
     */
    public MediaType(String mediaType) {
        String[] parts = StringUtils.split(mediaType, "/");
        if (parts.length == 2) {
            type = parts[0];
            subtype = parts[1];
        } else {
            throw new IllegalArgumentException("Invalid media type: " + mediaType);
        }
    }

    /**
     * @param obj Object to compare against.
     * @return True if the string representation of the given object matches
     *         that of the instance.
     */
    @Override
    public boolean equals(Object obj) {
        return (obj.toString() != null && obj.toString().equals(toString()));
    }

    /**
     * @return Format corresponding with the instance.
     */
    public Format toFormat() {
        for (Format enumValue : Format.values()) {
            for (MediaType type : enumValue.getMediaTypes()) {
                if (type.equals(this)) {
                    return enumValue;
                }
            }
        }
        return Format.UNKNOWN;
    }

    @Override
    public String toString() {
        return String.format("%s/%s", type, subtype);
    }

}
