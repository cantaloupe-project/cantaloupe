package edu.illinois.library.cantaloupe.resolver;

import static org.junit.Assert.*;

import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import org.junit.Test;

public class ResolverUtilTest {

    @Test
    public void testInferSourceFormat() {
        assertEquals(Format.BMP,
                ResolverUtil.inferSourceFormat(new Identifier("bla.bmp")));
        assertEquals(Format.GIF,
                ResolverUtil.inferSourceFormat(new Identifier("bla.gif")));
        assertEquals(Format.JP2,
                ResolverUtil.inferSourceFormat(new Identifier("bla.JP2")));
        assertEquals(Format.PDF,
                ResolverUtil.inferSourceFormat(new Identifier("bla.pdf")));
        assertEquals(Format.PNG,
                ResolverUtil.inferSourceFormat(new Identifier("bla.png")));
        assertEquals(Format.TIF,
                ResolverUtil.inferSourceFormat(new Identifier("bla.tif")));
        assertEquals(Format.UNKNOWN,
                ResolverUtil.inferSourceFormat(new Identifier("bla.bogus")));
        assertEquals(Format.UNKNOWN,
                ResolverUtil.inferSourceFormat(new Identifier("bla")));
    }

}
