package edu.illinois.library.cantaloupe.http;

import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

public class ResponseTest extends BaseTest {

    @Test
    void testGetBodyAsString() {
        final byte[] body = "cats".getBytes(StandardCharsets.UTF_8);

        Response response = new Response();
        response.setBody(body);
        assertEquals(new String(body, StandardCharsets.UTF_8),
                response.getBodyAsString());
    }

}
