package edu.illinois.library.cantaloupe.auth;

import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.Test;

import static org.junit.Assert.*;

public class AuthInfoBooleanBuilderTest extends BaseTest {

    @Test
    public void testBuild() {
        AuthInfo info = new AuthInfo.BooleanBuilder(true).build();
        assertEquals(200, info.getResponseStatus());
        assertNull(info.getRedirectURI());
        assertNull(info.getChallengeValue());

        info = new AuthInfo.BooleanBuilder(false).build();
        assertEquals(403, info.getResponseStatus());
        assertNull(info.getRedirectURI());
        assertNull(info.getChallengeValue());
    }

}
