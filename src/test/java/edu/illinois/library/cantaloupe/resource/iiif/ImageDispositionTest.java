package edu.illinois.library.cantaloupe.resource.iiif;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.resource.Request;
import edu.illinois.library.cantaloupe.resource.MockHttpServletRequest;
import java.util.Collections;
public class ImageDispositionTest {
    @BeforeAll
    public static void beforeClass() throws Exception {
        ConfigurationFactory.clearInstance();
        System.setProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT, "memory");
    }

    @Test
    void testGetRepresentationDispositionWithNoQueryArgument() {

        Request request = new Request(new MockHttpServletRequest(), Collections.emptyList());
        request.getReference().getQuery().remove(
                ImageDisposition.RESPONSE_CONTENT_DISPOSITION_QUERY_ARG);
        String disposition = ImageDisposition.getRepresentationDisposition(
            request,
                "cats?/\\dogs", Format.get("jpg"));
        assertNull(disposition);
    }

    @Test
    void testGetRepresentationDispositionWithInlineQueryArgument() {
        Request request = new Request(new MockHttpServletRequest(), Collections.emptyList());
        request.getReference().getQuery().set(
                ImageDisposition.RESPONSE_CONTENT_DISPOSITION_QUERY_ARG,
                "inline");

        String disposition = ImageDisposition.getRepresentationDisposition(
            request, "cats?/\\dogs", Format.get("jpg"));
        assertEquals("inline; filename=\"cats___dogs.jpg\"", disposition);
    }

    @Test
    void testGetRepresentationDispositionWithAttachmentQueryArgument() {
        Request request = new Request(new MockHttpServletRequest(), Collections.emptyList());

        request.getReference().getQuery().set(
                ImageDisposition.RESPONSE_CONTENT_DISPOSITION_QUERY_ARG,
                "attachment");

        String disposition = ImageDisposition.getRepresentationDisposition(
            request, "cats?/\\dogs", Format.get("jpg"));
        assertEquals("attachment; filename=\"cats___dogs.jpg\"", disposition);
    }

    @Test
    void testGetRepresentationDispositionWithAttachmentQueryArgumentWithASCIIFilename() {
        Request request = new Request(new MockHttpServletRequest(), Collections.emptyList());
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
        Request request = new Request(new MockHttpServletRequest(), Collections.emptyList());
        request.getReference().getQuery().set(
                ImageDisposition.RESPONSE_CONTENT_DISPOSITION_QUERY_ARG,
                "attachment; filename=\"unsafe_path../\\.jpg\"");


        String disposition = ImageDisposition.getRepresentationDisposition(
            request, "cats?/\\dogs", Format.get("jpg"));
        assertEquals("attachment; filename=\"unsafe_path.jpg\"",
                disposition);

        // attachment; filename="unsafe_injection_.....//./.jpg"
        request = new Request(new MockHttpServletRequest(), Collections.emptyList());
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
        Request request = new Request(new MockHttpServletRequest(), Collections.emptyList());

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
        Request request = new Request(new MockHttpServletRequest(), Collections.emptyList());
        request.getReference().getQuery().set(
                ImageDisposition.RESPONSE_CONTENT_DISPOSITION_QUERY_ARG,
                "attachment; filename*=UTF-8''unsafe_path../\\.jpg");

        String disposition = ImageDisposition.getRepresentationDisposition(
            request, "cats?/\\dogs", Format.get("jpg"));
        assertEquals("attachment; filename=\"cats___dogs.jpg\"; filename*= UTF-8''unsafe_path.jpg",
                disposition);

        // attachment; filename*= utf-8''"unsafe_injection_.....//./.jpg"
        request = new Request(new MockHttpServletRequest(), Collections.emptyList());
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
        Request request = new Request(new MockHttpServletRequest(), Collections.emptyList());
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
        Request request = new Request(new MockHttpServletRequest(), Collections.emptyList());

        request.getReference().getQuery().remove(
                ImageDisposition.RESPONSE_CONTENT_DISPOSITION_QUERY_ARG);

        String disposition = ImageDisposition.getRepresentationDisposition(
            request, "cats?/\\dogs", Format.get("jpg"));
        assertNull(disposition);
    }

}
