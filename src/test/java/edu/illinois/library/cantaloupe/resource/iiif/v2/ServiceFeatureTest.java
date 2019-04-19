package edu.illinois.library.cantaloupe.resource.iiif.v2;

import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ServiceFeatureTest extends BaseTest {

    @Test
    void testGetName() {
        assertEquals("baseUriRedirect", ServiceFeature.BASE_URI_REDIRECT.getName());
        assertEquals("canonicalLinkHeader", ServiceFeature.CANONICAL_LINK_HEADER.getName());
        assertEquals("cors", ServiceFeature.CORS.getName());
        assertEquals("jsonldMediaType", ServiceFeature.JSON_LD_MEDIA_TYPE.getName());
        assertEquals("profileLinkHeader", ServiceFeature.PROFILE_LINK_HEADER.getName());
        assertEquals("sizeByWhListed", ServiceFeature.SIZE_BY_WHITELISTED.getName());
    }

    @Test
    void testToString() {
        assertEquals("baseUriRedirect", ServiceFeature.BASE_URI_REDIRECT.toString());
        assertEquals("canonicalLinkHeader", ServiceFeature.CANONICAL_LINK_HEADER.toString());
        assertEquals("cors", ServiceFeature.CORS.toString());
        assertEquals("jsonldMediaType", ServiceFeature.JSON_LD_MEDIA_TYPE.toString());
        assertEquals("profileLinkHeader", ServiceFeature.PROFILE_LINK_HEADER.toString());
        assertEquals("sizeByWhListed", ServiceFeature.SIZE_BY_WHITELISTED.toString());
    }

}
