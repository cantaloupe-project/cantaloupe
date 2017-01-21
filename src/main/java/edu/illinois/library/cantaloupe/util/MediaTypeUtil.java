package edu.illinois.library.cantaloupe.util;

import org.apache.tika.detect.Detector;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.restlet.data.MediaType;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class MediaTypeUtil {

    public List<MediaType> detectMediaTypes(File file) throws IOException {
        return detectMediaTypes(file.getAbsolutePath());
    }

    public List<MediaType> detectMediaTypes(String pathname) throws IOException {
        final List<MediaType> types = new ArrayList<>();
        try (InputStream is = new BufferedInputStream(new FileInputStream(pathname))) {
            AutoDetectParser parser = new AutoDetectParser();
            Detector detector = parser.getDetector();
            Metadata md = new Metadata();
            md.add(Metadata.RESOURCE_NAME_KEY, pathname);
            org.apache.tika.mime.MediaType mediaType = detector.detect(is, md);
            types.add(new MediaType(mediaType.toString()));
        }
        return types;
    }

}
