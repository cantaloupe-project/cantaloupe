package edu.illinois.library.cantaloupe.resource.iiif;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.resource.IIIFRequest;
import edu.illinois.library.cantaloupe.resource.MockHttpServletRequest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
public class ImageDispositionTest {
    IIIFRequest request = new IIIFRequest(new MockHttpServletRequest(), Collections.emptyList(), Configuration.getInstance());

    @BeforeAll
    public static void beforeClass() throws Exception {
        ConfigurationFactory.clearInstance();
        System.setProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT, "memory");
    }

    @Test
    void testGetRepresentationDispositionWithNoQueryArgument() {

        request.getReference().getQuery().remove(
                ImageDisposition.RESPONSE_CONTENT_DISPOSITION_QUERY_ARG);
        String disposition = ImageDisposition.getRepresentationDisposition(
            request,
                "cats?/\\dogs", Format.get("jpg"));
        assertNull(disposition);
    }

    @Test
    void testGetRepresentationDispositionWithInlineQueryArgument() {
        request.getReference().getQuery().set(
                ImageDisposition.RESPONSE_CONTENT_DISPOSITION_QUERY_ARG,
                "inline");

        String disposition = ImageDisposition.getRepresentationDisposition(
            request, "cats?/\\dogs", Format.get("jpg"));
        assertEquals("inline; filename=\"cats___dogs.jpg\"", disposition);
    }

    @Test
    void testGetRepresentationDispositionWithAttachmentQueryArgument() {
        request.getReference().getQuery().set(
                ImageDisposition.RESPONSE_CONTENT_DISPOSITION_QUERY_ARG,
                "attachment");

        String disposition = ImageDisposition.getRepresentationDisposition(
            request, "cats?/\\dogs", Format.get("jpg"));
        assertEquals("attachment; filename=\"cats___dogs.jpg\"", disposition);
    }

    @Test
    void testGetRepresentationDispositionWithAttachmentQueryArgumentWithASCIIFilename() {
        request.getReference().getQuery().set(
                ImageDisposition.RESPONSE_CONTENT_DISPOSITION_QUERY_ARG,
                "attachment; filename=\"dogs.jpg\"");


        String disposition = ImageDisposition.getRepresentationDisposition(
            request,
                "cats?/\\dogs", Format.get("jpg"));
        assertEquals("attachment; filename=\"dogs.jpg\"", disposition);
    }

    @Test
    void testGetRepresentationDispositionWithAttachmentQueryArgumentWithUnsafeASCIIFilename() {
        request.getReference().getQuery().set(
                ImageDisposition.RESPONSE_CONTENT_DISPOSITION_QUERY_ARG,
                "attachment; filename=\"unsafe_path../\\.jpg\"");


        String disposition = ImageDisposition.getRepresentationDisposition(
            request, "cats?/\\dogs", Format.get("jpg"));
        assertEquals("attachment; filename=\"unsafe_path.jpg\"",
                disposition);

        // attachment; filename="unsafe_injection_.....//./.jpg"
        request = new IIIFRequest(new MockHttpServletRequest(), Collections.emptyList(), Configuration.getInstance());
        request.getReference().getQuery().set(
                ImageDisposition.RESPONSE_CONTENT_DISPOSITION_QUERY_ARG,
                "attachment; filename=\"unsafe_injection_.....//./.jpg\"");

        disposition = ImageDisposition.getRepresentationDisposition(
            request, "cats?/\\dogs", Format.get("jpg"));
        assertEquals("attachment; filename=\"unsafe_injection_.jpg\"",
                disposition);
    }

    @Test
    void testGetRepresentationDispositionWithAttachmentQueryArgumentWithUnicodeFilename() {
        request.getReference().getQuery().set(
                ImageDisposition.RESPONSE_CONTENT_DISPOSITION_QUERY_ARG,
                "attachment; filename*= UTF-8''dogs.jpg");
        String disposition = ImageDisposition.getRepresentationDisposition(
            request,
                "cats?/\\dogs", Format.get("jpg"));
        assertEquals("attachment; filename=\"cats___dogs.jpg\"; filename*= UTF-8''dogs.jpg",
                disposition);
    }

    @Test
    void testGetRepresentationDispositionWithAttachmentQueryArgumentWithUnsafeUnicodeFilename() {
        request.getReference().getQuery().set(
                ImageDisposition.RESPONSE_CONTENT_DISPOSITION_QUERY_ARG,
                "attachment; filename*=UTF-8''unsafe_path../\\.jpg");

        String disposition = ImageDisposition.getRepresentationDisposition(
            request, "cats?/\\dogs", Format.get("jpg"));
        assertEquals("attachment; filename=\"cats___dogs.jpg\"; filename*= UTF-8''unsafe_path.jpg",
                disposition);

        // attachment; filename*= utf-8''"unsafe_injection_.....//./.jpg"
        request = new IIIFRequest(new MockHttpServletRequest(), Collections.emptyList(), Configuration.getInstance());
        request.getReference().getQuery().set(
                ImageDisposition.RESPONSE_CONTENT_DISPOSITION_QUERY_ARG,
                "attachment; filename*= utf-8''unsafe_injection_.....//./.jpg");
        disposition = ImageDisposition.getRepresentationDisposition(
            request, "cats?/\\dogs", Format.get("jpg"));
        assertEquals("attachment; filename=\"cats___dogs.jpg\"; filename*= UTF-8''unsafe_injection_.jpg",
                disposition);
    }

    @Test
    void testGetRepresentationDispositionWithAttachmentQueryArgumentWithASCIIAndUnicodeFilenames() {
        request.getReference().getQuery().set(
                ImageDisposition.RESPONSE_CONTENT_DISPOSITION_QUERY_ARG,
                "attachment; filename=\"dogs.jpg\"; filename*= UTF-8''dogs.jpg");

        String disposition = ImageDisposition.getRepresentationDisposition(
            request, "cats?/\\dogs", Format.get("jpg"));
        assertEquals("attachment; filename=\"dogs.jpg\"; filename*= UTF-8''dogs.jpg",
                disposition);
    }

    @Test
    void testGetRepresentationDispositionFallsBackToNone() {
        request.getReference().getQuery().remove(
                ImageDisposition.RESPONSE_CONTENT_DISPOSITION_QUERY_ARG);

        String disposition = ImageDisposition.getRepresentationDisposition(
            request, "cats?/\\dogs", Format.get("jpg"));
        assertNull(disposition);
    }

}
