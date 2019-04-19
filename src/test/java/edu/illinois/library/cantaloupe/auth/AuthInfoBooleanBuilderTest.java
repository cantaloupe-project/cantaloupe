package edu.illinois.library.cantaloupe.auth;

import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AuthInfoBooleanBuilderTest extends BaseTest {

    @Test
    void testBuild() {
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
