package edu.illinois.library.cantaloupe.util;

import eu.medsea.mimeutil.MimeUtil;
import org.restlet.data.MediaType;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

public class MediaTypeUtil {

    static {
        MimeUtil.registerMimeDetector(
                "eu.medsea.mimeutil.detector.MagicMimeMimeDetector");
    }

    public List<MediaType> detectMediaTypes(File file) {
        return detectMediaTypes(file.getAbsolutePath());
    }

    @SuppressWarnings("unchecked")
    public List<MediaType> detectMediaTypes(String pathname) {
        return (List<MediaType>) MimeUtil.getMimeTypes(pathname).stream().
                map(t -> new MediaType(t.toString())).
                collect(Collectors.toList());
    }

}
