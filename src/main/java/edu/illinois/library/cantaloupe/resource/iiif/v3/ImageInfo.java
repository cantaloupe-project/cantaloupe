package edu.illinois.library.cantaloupe.resource.iiif.v3;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * <p>Class whose instances are intended to be serialized to JSON for use in
 * IIIF Image Information responses.</p>
 *
 * <p>Extends {@link LinkedHashMap} in order to support arbitrary keys and
 * preserve key order.</p>
 *
 * @see <a href="https://iiif.io/api/image/3.0/#52-technical-properties">IIIF
 * Image API 3.0: Technical Properties</a>
 */
class ImageInfo<K,V> extends LinkedHashMap<K,V> {

    @JsonPropertyOrder({ "width", "height" })
    public static final class Size {
        public Integer height;
        public Integer width;

        /** No-op constructor needed by Jackson */
        public Size() {}

        public Size(Integer width, Integer height) {
            this.width  = width;
            this.height = height;
        }
    }

    @JsonPropertyOrder({ "width", "height", "scaleFactors" })
    public static final class Tile {
        public Integer height;
        public List<Integer> scaleFactors = new ArrayList<>();
        public Integer width;
    }

}
