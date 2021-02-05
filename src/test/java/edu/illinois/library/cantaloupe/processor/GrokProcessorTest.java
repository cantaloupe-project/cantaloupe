package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class GrokProcessorTest extends AbstractProcessorTest {

    private GrokProcessor instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        Configuration.getInstance().clearProperty(
                Key.GROKPROCESSOR_PATH_TO_BINARIES);
        GrokProcessor.resetInitialization();

        instance = newInstance();
    }

    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
        instance.close();
    }

    @Override
    protected GrokProcessor newInstance() {
        GrokProcessor proc = new GrokProcessor();
        try {
            proc.setSourceFormat(Format.get("jp2"));
        } catch (SourceFormatException e) {
            fail("Huge bug");
        }
        return proc;
    }

    @Test
    void testGetInitializationErrorWithNoException() {
        assertNull(instance.getInitializationError());
    }

    @Test
    void testGetInitializationErrorWithMissingBinaries() {
        Configuration.getInstance().setProperty(
                Key.GROKPROCESSOR_PATH_TO_BINARIES,
                "/bogus/bogus/bogus");
        GrokProcessor.resetInitialization();
        assertNotNull(instance.getInitializationError());
    }

    @Test
    void testReadInfoIPTCAwareness() throws Exception {
        instance.setSourceFile(TestUtil.getImage("jp2-iptc.jp2"));
        Info info = instance.readInfo();
        assertTrue(info.getMetadata().getIPTC().isPresent());
    }

    @Test
    void testReadInfoXMPAwareness() throws Exception {
        instance.setSourceFile(TestUtil.getImage("jp2-xmp.jp2"));
        Info info = instance.readInfo();
        assertTrue(info.getMetadata().getXMP().isPresent());
    }

    @Test
    void testReadInfoTileAwareness() throws Exception {
        // untiled image
        instance.setSourceFile(TestUtil.getImage("jp2-5res-rgb-64x56x8-monotiled-lossy.jp2"));
        Info expectedInfo = Info.builder()
                .withSize(64, 56)
                .withTileSize(64, 56)
                .withFormat(Format.get("jp2"))
                .withNumResolutions(5)
                .build();
        assertEquals(expectedInfo, instance.readInfo());

        // tiled image
        instance.setSourceFile(TestUtil.getImage("jp2-6res-rgb-64x56x8-multitiled-lossy.jp2"));
        expectedInfo = Info.builder()
                .withSize(64, 56)
                .withTileSize(32, 28)
                .withNumResolutions(6)
                .withFormat(Format.get("jp2"))
                .build();
        assertEquals(expectedInfo, instance.readInfo());
    }

    @Test
    void testSupportsSourceFormatWithSupportedFormat() {
        try (Processor instance = newInstance()) {
            assertTrue(instance.supportsSourceFormat(Format.get("jp2")));
        }
    }

    @Test
    void testSupportsSourceFormatWithUnsupportedFormat() {
        try (Processor instance = newInstance()) {
            assertFalse(instance.supportsSourceFormat(Format.get("gif")));
        }
    }

}
