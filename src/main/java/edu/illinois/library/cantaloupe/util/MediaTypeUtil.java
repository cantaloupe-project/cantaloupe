package edu.illinois.library.cantaloupe.util;

import org.apache.tika.detect.Detector;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.restlet.data.MediaType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MediaTypeUtil {

    public List<MediaType> detectMediaTypes(File file) throws IOException {
        final List<MediaType> types = new ArrayList<>();
        try (TikaInputStream is = TikaInputStream.get(file)) {
            AutoDetectParser parser = new AutoDetectParser();
            Detector detector = parser.getDetector();
            Metadata md = new Metadata();
            md.add(Metadata.RESOURCE_NAME_KEY, file.getAbsolutePath());
            org.apache.tika.mime.MediaType mediaType = detector.detect(is, md);
            types.add(new MediaType(mediaType.toString()));
        }
        return types;
    }

    public List<MediaType> detectMediaTypes(String pathname) throws IOException {
        return detectMediaTypes(new File(pathname));
    }

}
