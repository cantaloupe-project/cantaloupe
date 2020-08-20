package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.image.Metadata;
import edu.illinois.library.cantaloupe.processor.codec.ImageReaderFactory;
import edu.illinois.library.cantaloupe.processor.codec.ImageWriterFactory;
import edu.illinois.library.cantaloupe.source.PathStreamFactory;
import edu.illinois.library.cantaloupe.source.StreamFactory;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

abstract class AbstractImageIOProcessorTest extends AbstractProcessorTest {

    @Test
    void testGetAvailableOutputFormats() throws Exception {
        final HashMap<Format,Set<Format>> formats = new HashMap<>();
        for (Format format : ImageReaderFactory.supportedFormats()) {
            formats.put(format, ImageWriterFactory.supportedFormats());
        }

        try (Processor proc = newInstance()) {
            proc.setSourceFormat(Format.get("jpg"));
            Set<Format> expectedFormats = formats.get(Format.get("jpg"));
            assertEquals(expectedFormats, proc.getAvailableOutputFormats());
        }
    }

    @Test
    void testReadInfoEXIFAwareness() throws Exception {
        final Path fixture = TestUtil.getImage("jpg-exif.jpg");

        try (FileProcessor fproc = (FileProcessor) newInstance()) {
            fproc.setSourceFile(fixture);
            fproc.setSourceFormat(Format.get("jpg"));

            final Info info = fproc.readInfo();
            assertTrue(info.getMetadata().getEXIF().isPresent());
        }
    }

    @Test
    void testReadInfoIPTCAwareness() throws Exception {
        final Path fixture = TestUtil.getImage("jpg-iptc.jpg");

        try (FileProcessor fproc = (FileProcessor) newInstance()) {
            fproc.setSourceFile(fixture);
            fproc.setSourceFormat(Format.get("jpg"));

            final Info info = fproc.readInfo();
            assertTrue(info.getMetadata().getIPTC().isPresent());
        }
    }

    @Test
    void testReadInfoXMPAwareness() throws Exception {
        final Path fixture = TestUtil.getImage("jpg-xmp.jpg");

        try (FileProcessor fproc = (FileProcessor) newInstance()) {
            fproc.setSourceFile(fixture);
            fproc.setSourceFormat(Format.get("jpg"));

            final Info info = fproc.readInfo();
            assertTrue(info.getMetadata().getXMP().isPresent());
        }
    }

    @Test
    void testReadInfoTileAwareness() throws Exception {
        Info expectedInfo = Info.builder()
                .withSize(64, 56)
                .withTileSize(16, 16)
                .withNumResolutions(1)
                .withFormat(Format.get("tif"))
                .build();
        final Path fixture = TestUtil.
                getImage("tif-rgb-1res-64x56x8-tiled-uncompressed.tif");

        // test as a StreamProcessor
        try (StreamProcessor sproc = (StreamProcessor) newInstance()) {
            StreamFactory streamFactory = new PathStreamFactory(fixture);
            sproc.setStreamFactory(streamFactory);
            sproc.setSourceFormat(Format.get("tif"));
            Info actualInfo = sproc.readInfo();
            actualInfo.setMetadata(new Metadata()); // we don't care about this
            assertEquals(expectedInfo, actualInfo);
        }

        // test as a FileProcessor
        try (FileProcessor fproc = (FileProcessor) newInstance()) {
            fproc.setSourceFile(fixture);
            fproc.setSourceFormat(Format.get("tif"));
            Info actualInfo = fproc.readInfo();
            actualInfo.setMetadata(new Metadata()); // we don't care about this
            assertEquals(expectedInfo, actualInfo);
        }
    }

}
