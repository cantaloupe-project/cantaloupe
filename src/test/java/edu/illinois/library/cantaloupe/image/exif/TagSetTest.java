package edu.illinois.library.cantaloupe.image.exif;

import it.geosolutions.imageio.plugins.tiff.BaselineTIFFTagSet;
import it.geosolutions.imageio.plugins.tiff.EXIFGPSTagSet;
import it.geosolutions.imageio.plugins.tiff.EXIFInteroperabilityTagSet;
import it.geosolutions.imageio.plugins.tiff.EXIFTIFFTagSet;
import org.junit.Test;

import static org.junit.Assert.*;

public class TagSetTest {

    @Test
    public void testForTIFFTagSet() {
        assertEquals(TagSet.BASELINE_TIFF, TagSet.forTIFFTagSet(BaselineTIFFTagSet.class));
        assertEquals(TagSet.EXIF, TagSet.forTIFFTagSet(EXIFTIFFTagSet.class));
        assertEquals(TagSet.GPS, TagSet.forTIFFTagSet(EXIFGPSTagSet.class));
        assertEquals(TagSet.INTEROPERABILITY, TagSet.forTIFFTagSet(EXIFInteroperabilityTagSet.class));
    }

    @Test
    public void testForIFDPointerTag() {
        assertEquals(TagSet.BASELINE_TIFF,
                TagSet.forIFDPointerTag(TagSet.BASELINE_TIFF.getIFDPointerTag()));
        assertEquals(TagSet.EXIF,
                TagSet.forIFDPointerTag(TagSet.EXIF.getIFDPointerTag()));
        assertEquals(TagSet.GPS,
                TagSet.forIFDPointerTag(TagSet.GPS.getIFDPointerTag()));
        assertEquals(TagSet.INTEROPERABILITY,
                TagSet.forIFDPointerTag(TagSet.INTEROPERABILITY.getIFDPointerTag()));
    }

    @Test
    public void testContainsTag() {
        assertTrue(TagSet.EXIF.containsTag(Tag.F_NUMBER.getID()));
        assertFalse(TagSet.EXIF.containsTag(Tag.MAKE.getID()));
    }

    @Test
    public void testGetTag() {
        assertEquals(Tag.F_NUMBER, TagSet.EXIF.getTag(Tag.F_NUMBER.getID()));
        assertNull(TagSet.EXIF.getTag(9999999));
    }

    @Test
    public void testGetTags() {
        assertEquals(77, TagSet.EXIF.getTags().size());
    }

}