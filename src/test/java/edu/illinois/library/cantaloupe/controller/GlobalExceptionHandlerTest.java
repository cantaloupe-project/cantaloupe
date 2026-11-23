package edu.illinois.library.cantaloupe.controller;

import edu.illinois.library.cantaloupe.config.Configuration;
import org.eclipse.jetty.io.EofException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler instance;
    private WebRequest request;

    @BeforeEach
    void setUp() {
        instance = new GlobalExceptionHandler(mock(Configuration.class));
        MockHttpServletRequest servletRequest =
                new MockHttpServletRequest("GET", "/iiif/3/cats/full/max/0/default.jpg");
        request = new ServletWebRequest(servletRequest);
    }

    @Test
    void testHandleGenericExceptionWithEofException() {
        assertNull(instance.handleGenericException(
                new EofException("Closed"), request));
    }

    @Test
    void testHandleGenericExceptionWithWrappedEofException() {
        Exception e = new RuntimeException(new IOException(new EofException()));
        assertNull(instance.handleGenericException(e, request));
    }

    @Test
    void testHandleGenericExceptionWithBrokenPipe() {
        assertNull(instance.handleGenericException(
                new IOException("Broken pipe"), request));
    }

    @Test
    void testHandleGenericExceptionWithConnectionReset() {
        assertNull(instance.handleGenericException(
                new IOException("Connection reset by peer"), request));
    }

    @Test
    void testHandleGenericExceptionWithOtherErrors() {
        ResponseEntity<Map<String, Object>> response =
                instance.handleGenericException(new IOException("Disk full"), request);
        assertNotNull(response);
        assertEquals(500, response.getStatusCode().value());
    }

}
