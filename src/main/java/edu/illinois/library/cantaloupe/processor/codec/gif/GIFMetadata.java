package edu.illinois.library.cantaloupe.processor.codec.gif;

import com.fasterxml.jackson.annotation.JsonProperty;
import edu.illinois.library.cantaloupe.image.Metadata;
import edu.illinois.library.cantaloupe.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class GIFMetadata extends Metadata {

    public class NativeMetadata {
        private int delayTime, loopCount;

        @JsonProperty
        public int getDelayTime() {
            return delayTime;
        }

        @JsonProperty
        public int getLoopCount() {
            return loopCount;
        }

        void setDelayTime(int delayTime) {
            this.delayTime = delayTime;
        }

        void setLoopCount(int loopCount) {
            this.loopCount = loopCount;
        }

        Map<String,Integer> toMap() {
            return Map.of(
                    "delayTime", getDelayTime(),
                    "loopCount", getLoopCount());
        }
    }

    private static final Logger LOGGER =
            LoggerFactory.getLogger(GIFMetadata.class);

    private GIFMetadataReader reader;
    private boolean checkedForXMP;

    GIFMetadata(GIFMetadataReader reader) {
        this.reader = reader;
    }

    @Override
    public Optional<NativeMetadata> getNativeMetadata() {
        final NativeMetadata metadata = new NativeMetadata();
        try {
            metadata.setDelayTime(reader.getDelayTime());
            metadata.setLoopCount(reader.getLoopCount());
        } catch (IOException e) {
            LOGGER.warn("getNativeMetadata(): {}", e.getMessage());
        }
        return Optional.of(metadata);
    }

    @Override
    public Optional<String> getXMP() {
        if (!checkedForXMP) {
            checkedForXMP = true;
            try {
                xmp = reader.getXMP();
                if (xmp != null) {
                    xmp = StringUtils.trimXMP(xmp);
                }
            } catch (IOException e) {
                LOGGER.warn("getXMP(): {}", e.getMessage());
            }
        }
        return Optional.ofNullable(xmp);
    }

    @Override
    public Map<String, Object> toMap() {
        final Map<String,Object> map = new HashMap<>(super.toMap());
        getNativeMetadata().ifPresent(m -> map.put("native", m.toMap()));
        return Collections.unmodifiableMap(map);
    }

}
