package edu.illinois.library.cantaloupe.processor.codec;

import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.Before;
import org.junit.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.Assert.*;

public class ImageWriterFactoryTest extends BaseTest {

    private ImageWriterFactory instance;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        instance = new ImageWriterFactory();
    }

    @Test
    public void testSupportedFormats() {
        Set<Format> outputFormats = EnumSet.of(
                Format.GIF, Format.JPG, Format.PNG, Format.TIF);
        assertEquals(outputFormats, ImageWriterFactory.supportedFormats());
    }

    @Test
    public void testNewImageWriter1() {
        OperationList ops = new OperationList(new Identifier("cats"), Format.JPG);
        assertNotNull(instance.newImageWriter(ops));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewImageWriter1WithUnsupportedFormat() {
        OperationList ops = new OperationList(new Identifier("cats"), Format.UNKNOWN);
        instance.newImageWriter(ops);
    }

    @Test
    public void testNewImageWriter2() {
        OperationList ops = new OperationList(new Identifier("cats"), Format.JPG);
        assertNotNull(instance.newImageWriter(ops, new NullMetadata(null, "")));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewImageWriter2WithUnsupportedFormat() {
        OperationList ops = new OperationList(new Identifier("cats"), Format.UNKNOWN);
        instance.newImageWriter(ops, new NullMetadata(null, ""));
    }

}
