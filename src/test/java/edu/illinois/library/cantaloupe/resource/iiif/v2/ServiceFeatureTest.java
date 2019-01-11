package edu.illinois.library.cantaloupe.resource.iiif.v2;

import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.Test;

import static org.junit.Assert.*;

public class ServiceFeatureTest extends BaseTest {

    @Test
    public void testValues() {
        ServiceFeature f = ServiceFeature.BASE_URI_REDIRECT;
        f = ServiceFeature.CANONICAL_LINK_HEADER;
        f = ServiceFeature.CORS;
        f = ServiceFeature.JSON_LD_MEDIA_TYPE;
        f = ServiceFeature.PROFILE_LINK_HEADER;
        f = ServiceFeature.SIZE_BY_WHITELISTED;
    }

    @Test
    public void testGetName() {
        assertEquals("baseUriRedirect", ServiceFeature.BASE_URI_REDIRECT.getName());
        assertEquals("canonicalLinkHeader", ServiceFeature.CANONICAL_LINK_HEADER.getName());
        assertEquals("cors", ServiceFeature.CORS.getName());
        assertEquals("jsonldMediaType", ServiceFeature.JSON_LD_MEDIA_TYPE.getName());
        assertEquals("profileLinkHeader", ServiceFeature.PROFILE_LINK_HEADER.getName());
        assertEquals("sizeByWhListed", ServiceFeature.SIZE_BY_WHITELISTED.getName());
    }

    @Test
    public void testToString() {
        assertEquals("baseUriRedirect", ServiceFeature.BASE_URI_REDIRECT.toString());
        assertEquals("canonicalLinkHeader", ServiceFeature.CANONICAL_LINK_HEADER.toString());
        assertEquals("cors", ServiceFeature.CORS.toString());
        assertEquals("jsonldMediaType", ServiceFeature.JSON_LD_MEDIA_TYPE.toString());
        assertEquals("profileLinkHeader", ServiceFeature.PROFILE_LINK_HEADER.toString());
        assertEquals("sizeByWhListed", ServiceFeature.SIZE_BY_WHITELISTED.toString());
    }

}
