package edu.illinois.library.cantaloupe.image.iptc;

import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.imageio.stream.MemoryCacheImageInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ReaderTest extends BaseTest {

    private Reader instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = new Reader();
    }

    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
        instance.close();
    }

    @Test
    void testReadWithValidBytes() throws Exception {
        byte[] iptc = Files.readAllBytes(TestUtil.getImage("iptc.bin"));
        instance.setSource(iptc);

        List<DataSet> expected = List.of(
                new DataSet(Tag.CATEGORY, "Supl. Category2".getBytes()),
                new DataSet(Tag.CATEGORY, "Supl. Category1".getBytes()),
                new DataSet(Tag.CATEGORY, "Cat".getBytes()),
                new DataSet(Tag.COPYRIGHT_NOTICE, "Copyright".getBytes()),
                new DataSet(Tag.SPECIAL_INSTRUCTIONS, "Special Instr.".getBytes()),
                new DataSet(Tag.HEADLINE, "Headline".getBytes()),
                new DataSet(Tag.WRITER_EDITOR, "CaptionWriter".getBytes()),
                new DataSet(Tag.CAPTION_ABSTRACT, "Caption".getBytes()),
                new DataSet(Tag.ORIGINAL_TRANSMISSION_REFERENCE, "Transmission".getBytes()),
                new DataSet(Tag.COUNTRY_PRIMARY_LOCATION_NAME, "Country".getBytes()),
                new DataSet(Tag.PROVINCE_STATE, "State".getBytes()),
                new DataSet(Tag.CITY, "City".getBytes()),
                new DataSet(Tag.DATE_CREATED, new byte[] {
                        0x32, 0x30, 0x30, 0x30, 0x30, 0x31, 0x30, 0x31 }),
                new DataSet(Tag.OBJECT_NAME, "ObjectName".getBytes()),
                new DataSet(Tag.SOURCE, "Source".getBytes()),
                new DataSet(Tag.CREDIT, "Credits".getBytes()),
                new DataSet(Tag.BYLINE_TITLE, "BylineTitle".getBytes()),
                new DataSet(Tag.BYLINE, "Byline".getBytes())
        );
        List<DataSet> actual = instance.read();

        assertEquals(expected, actual);
    }

    @Test
    void testReadWithInvalidBytes() throws Exception {
        byte[] iptc = new byte[] { 0x02, 0x05, 0x09 }; // random bytes
        instance.setSource(iptc);
        assertEquals(0, instance.read().size());
    }

    @Test
    void testSetSourceCalledConsecutively1() {
        instance.setSource(new byte[] {});
        assertThrows(IllegalStateException.class,
                () -> instance.setSource(new byte[] {}));
    }

    @Test
    void testSetSourceCalledConsecutively2() {
        instance.setSource(new MemoryCacheImageInputStream(InputStream.nullInputStream()));
        assertThrows(IllegalStateException.class,
                () -> instance.setSource(new MemoryCacheImageInputStream(InputStream.nullInputStream())));
    }

    @Test
    void testSetSourceCalledConsecutively3() {
        instance.setSource(new byte[] {});
        assertThrows(IllegalStateException.class,
                () -> instance.setSource(new MemoryCacheImageInputStream(InputStream.nullInputStream())));
    }

    @Test
    void testSetSourceCalledConsecutively4() {
        instance.setSource(new MemoryCacheImageInputStream(InputStream.nullInputStream()));
        assertThrows(IllegalStateException.class,
                () -> instance.setSource(new byte[] {}));
    }

}
