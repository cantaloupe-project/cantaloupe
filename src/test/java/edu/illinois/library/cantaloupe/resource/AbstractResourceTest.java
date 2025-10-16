package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class AbstractResourceTest extends BaseTest {

    private AbstractResource instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        instance = new AbstractResource() {
            @Override
            protected Logger getLogger() {
                return LoggerFactory.getLogger(AbstractResourceTest.class);
            }
        };

        Request mockRequest = new Request(new MockHttpServletRequest(), Collections.emptyList());
        instance.setRequest(mockRequest);
        instance.setResponse(new MockHttpServletResponse());
    }

    @Test
    void testDoGET() throws Exception {
        instance.doGET();
        assertEquals(405, instance.getResponse().getStatus());
    }

    @Test
    void testDoHEAD() throws Exception {
        instance.doHEAD();
        assertEquals(405, instance.getResponse().getStatus());
    }

    @Test
    void testDoOPTIONS() {
        instance.doOPTIONS();
        assertEquals(204, instance.getResponse().getStatus());
    }

    @Test
    void testDoPOST() throws Exception {
        instance.doPOST();
        assertEquals(405, instance.getResponse().getStatus());
    }

    @Test
    void testDoPUT() throws Exception {
        instance.doPUT();
        assertEquals(405, instance.getResponse().getStatus());
    }

    @Test
    void testGetCommonTemplateVars() {
        Map<String,Object> vars = instance.getCommonTemplateVars();
        assertFalse(((String) vars.get("baseUri")).endsWith("/"));
        assertNotNull(vars.get("version"));
    }

    @Test
    void testGetPreferredMediaTypesWithAcceptHeaderSet() {
        instance.getRequest().getHeaders().set("Accept",
                "text/html;q=0.9, application/xhtml+xml, */*;q=0.2, text/plain;q=0.5");

        List<String> types = instance.getPreferredMediaTypes();
        assertEquals(3, types.size());
        assertEquals("application/xhtml+xml", types.get(0));
        assertEquals("text/html", types.get(1));
        assertEquals("text/plain", types.get(2));
    }

    @Test
    void testGetPreferredMediaTypesWithAcceptHeaderNotSet() {
        instance.getRequest().getHeaders().removeAll("Accept");

        List<String> types = instance.getPreferredMediaTypes();
        assertTrue(types.isEmpty());
    }

    /* getRepresentationDisposition() */

    @Test
    void testGetRepresentationDispositionWithNoQueryArgument() {
        instance.getRequest().getReference().getQuery().remove(
                AbstractResource.RESPONSE_CONTENT_DISPOSITION_QUERY_ARG);
        String disposition = instance.getRepresentationDisposition(
                "cats?/\\dogs", Format.get("jpg"));
        assertNull(disposition);
    }

    @Test
    void testGetRepresentationDispositionWithInlineQueryArgument() {
        instance.getRequest().getReference().getQuery().set(
                AbstractResource.RESPONSE_CONTENT_DISPOSITION_QUERY_ARG,
                "inline");
        String disposition = instance.getRepresentationDisposition(
                "cats?/\\dogs", Format.get("jpg"));
        assertEquals("inline; filename=\"cats___dogs.jpg\"", disposition);
    }

    @Test
    void testGetRepresentationDispositionWithAttachmentQueryArgument() {
        instance.getRequest().getReference().getQuery().set(
                AbstractResource.RESPONSE_CONTENT_DISPOSITION_QUERY_ARG,
                "attachment");
        String disposition = instance.getRepresentationDisposition(
                "cats?/\\dogs", Format.get("jpg"));
        assertEquals("attachment; filename=\"cats___dogs.jpg\"", disposition);
    }

    @Test
    void testGetRepresentationDispositionWithAttachmentQueryArgumentWithASCIIFilename() {
        instance.getRequest().getReference().getQuery().set(
                AbstractResource.RESPONSE_CONTENT_DISPOSITION_QUERY_ARG,
                "attachment; filename=\"dogs.jpg\"");
        String disposition = instance.getRepresentationDisposition(
                "cats?/\\dogs", Format.get("jpg"));
        assertEquals("attachment; filename=\"dogs.jpg\"", disposition);
    }

    @Test
    void testGetRepresentationDispositionWithAttachmentQueryArgumentWithUnsafeASCIIFilename() {
        instance.getRequest().getReference().getQuery().set(
                AbstractResource.RESPONSE_CONTENT_DISPOSITION_QUERY_ARG,
                "attachment; filename=\"unsafe_path../\\.jpg\"");
        String disposition = instance.getRepresentationDisposition(
                "cats?/\\dogs", Format.get("jpg"));
        assertEquals("attachment; filename=\"unsafe_path.jpg\"",
                disposition);

        // attachment; filename="unsafe_injection_.....//./.jpg"
        instance.getRequest().getReference().getQuery().set(
                AbstractResource.RESPONSE_CONTENT_DISPOSITION_QUERY_ARG,
                "attachment; filename=\"unsafe_injection_.....//./.jpg\"");
        disposition = instance.getRepresentationDisposition(
                "cats?/\\dogs", Format.get("jpg"));
        assertEquals("attachment; filename=\"unsafe_injection_.jpg\"",
                disposition);
    }

    @Test
    void testGetRepresentationDispositionWithAttachmentQueryArgumentWithUnicodeFilename() {
        instance.getRequest().getReference().getQuery().set(
                AbstractResource.RESPONSE_CONTENT_DISPOSITION_QUERY_ARG,
                "attachment; filename*= UTF-8''dogs.jpg");
        String disposition = instance.getRepresentationDisposition(
                "cats?/\\dogs", Format.get("jpg"));
        assertEquals("attachment; filename=\"cats___dogs.jpg\"; filename*= UTF-8''dogs.jpg",
                disposition);
    }

    @Test
    void testGetRepresentationDispositionWithAttachmentQueryArgumentWithUnsafeUnicodeFilename() {
        instance.getRequest().getReference().getQuery().set(
                AbstractResource.RESPONSE_CONTENT_DISPOSITION_QUERY_ARG,
                "attachment; filename*=UTF-8''unsafe_path../\\.jpg");
        String disposition = instance.getRepresentationDisposition(
                "cats?/\\dogs", Format.get("jpg"));
        assertEquals("attachment; filename=\"cats___dogs.jpg\"; filename*= UTF-8''unsafe_path.jpg",
                disposition);

        // attachment; filename*= utf-8''"unsafe_injection_.....//./.jpg"
        instance.getRequest().getReference().getQuery().set(
                AbstractResource.RESPONSE_CONTENT_DISPOSITION_QUERY_ARG,
                "attachment; filename*= utf-8''unsafe_injection_.....//./.jpg");
        disposition = instance.getRepresentationDisposition(
                "cats?/\\dogs", Format.get("jpg"));
        assertEquals("attachment; filename=\"cats___dogs.jpg\"; filename*= UTF-8''unsafe_injection_.jpg",
                disposition);
    }

    @Test
    void testGetRepresentationDispositionWithAttachmentQueryArgumentWithASCIIAndUnicodeFilenames() {
        instance.getRequest().getReference().getQuery().set(
                AbstractResource.RESPONSE_CONTENT_DISPOSITION_QUERY_ARG,
                "attachment; filename=\"dogs.jpg\"; filename*= UTF-8''dogs.jpg");
        String disposition = instance.getRepresentationDisposition(
                "cats?/\\dogs", Format.get("jpg"));
        assertEquals("attachment; filename=\"dogs.jpg\"; filename*= UTF-8''dogs.jpg",
                disposition);
    }

    @Test
    void testGetRepresentationDispositionFallsBackToNone() {
        instance.getRequest().getReference().getQuery().remove(
                AbstractResource.RESPONSE_CONTENT_DISPOSITION_QUERY_ARG);
        String disposition = instance.getRepresentationDisposition(
                "cats?/\\dogs", Format.get("jpg"));
        assertNull(disposition);
    }

}
