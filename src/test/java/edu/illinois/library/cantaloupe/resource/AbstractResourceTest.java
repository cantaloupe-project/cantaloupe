package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.Test;
import org.restlet.data.Disposition;
import org.restlet.data.Header;
import org.restlet.data.Reference;
import org.restlet.util.Series;

import static org.junit.Assert.*;

public class AbstractResourceTest extends BaseTest {

    private static class TestResource extends AbstractResource {
    }

    private TestResource resource = new TestResource();

    /* getPublicReference(Reference, Reference, Series<Header>) */

    @Test
    public void testGetPublicReferenceUsingConfiguration() {
        final String baseURI = "http://example.net/cats";
        final String rootURI = "http://example.org/dogs";
        final String resourceURI = "http://example.org/dogs/llamas";
        Configuration.getInstance().setProperty(Key.BASE_URI, baseURI);

        Series<Header> headers = new Series<>(Header.class);

        Reference ref = AbstractResource.getPublicReference(
                new Reference(rootURI), new Reference(resourceURI), headers);
        assertEquals(baseURI + "/llamas", ref.toString());
    }

    @Test
    public void testGetPublicReferenceUsingXForwardedHeaders() {
        Reference rootRef = new Reference("http://bogus");
        Reference resourceRef = new Reference("http://bogus/cats");

        // HTTP & port 80
        Series<Header> headers = new Series<>(Header.class);
        headers.set("X-Forwarded-Proto", "HTTP");
        headers.set("X-Forwarded-Host", "example.org");
        headers.set("X-Forwarded-Port", "80");
        headers.set("X-Forwarded-Path", "/");
        Reference ref = AbstractResource.getPublicReference(rootRef, resourceRef, headers);
        assertEquals("http://example.org/cats", ref.toString());

        // HTTP & port != 80
        headers.set("X-Forwarded-Proto", "HTTP");
        headers.set("X-Forwarded-Host", "example.org");
        headers.set("X-Forwarded-Port", "85");
        headers.set("X-Forwarded-Path", "/");
        ref = AbstractResource.getPublicReference(rootRef, resourceRef, headers);
        assertEquals("http://example.org:85/cats", ref.toString());

        // HTTPS & port 443
        headers.set("X-Forwarded-Proto", "HTTPS");
        headers.set("X-Forwarded-Host", "example.org");
        headers.set("X-Forwarded-Port", "443");
        headers.set("X-Forwarded-Path", "/");
        ref = AbstractResource.getPublicReference(rootRef, resourceRef, headers);
        assertEquals("https://example.org/cats", ref.toString());

        // HTTPS & port != 443
        headers.set("X-Forwarded-Proto", "HTTPS");
        headers.set("X-Forwarded-Host", "example.org");
        headers.set("X-Forwarded-Port", "450");
        headers.set("X-Forwarded-Path", "/");
        ref = AbstractResource.getPublicReference(rootRef, resourceRef, headers);
        assertEquals("https://example.org:450/cats", ref.toString());

        // Multiple values
        headers.set("X-Forwarded-Proto", "http,http");
        headers.set("X-Forwarded-Host", "example.org,example.mil");
        headers.set("X-Forwarded-Port", "80,8080");
        headers.set("X-Forwarded-Path", "/foxes,/dogs");
        ref = AbstractResource.getPublicReference(rootRef, resourceRef, headers);
        assertEquals("http://example.org/foxes/cats", ref.toString());
    }

    @Test
    public void testGetPublicReferenceFallsBackToRequest() {
        final String rootURI = "http://example.net/cats";
        final String resourceURI = "http://example.net/cats/dogs";

        Series<Header> headers = new Series<>(Header.class);

        Reference ref = AbstractResource.getPublicReference(
                new Reference(rootURI), new Reference(resourceURI), headers);
        assertEquals(resourceURI, ref.toString());
    }

    /* getRepresentationDisposition() */

    @Test
    public void testGetRepresentationDispositionWithQueryArg()
            throws Exception {
        final Identifier identifier = new Identifier("cats?/\\dogs");
        final Format outputFormat = Format.JPG;

        // none
        Disposition disposition = resource.getRepresentationDisposition(
                null, identifier, outputFormat);
        assertEquals(Disposition.TYPE_NONE, disposition.getType());

        // inline
        disposition = resource.getRepresentationDisposition(
                "inline", identifier, outputFormat);
        assertEquals(Disposition.TYPE_INLINE, disposition.getType());

        // attachment
        disposition = resource.getRepresentationDisposition(
                "attachment", identifier, outputFormat);
        assertEquals(Disposition.TYPE_ATTACHMENT, disposition.getType());
        assertEquals("cats___dogs.jpg", disposition.getFilename());

        // attachment; filename="dogs.jpg"
        disposition = resource.getRepresentationDisposition(
                "attachment; filename=\"dogs.jpg\"", identifier, outputFormat);
        assertEquals(Disposition.TYPE_ATTACHMENT, disposition.getType());
        assertEquals("dogs.jpg", disposition.getFilename());

        // attachment; filename="unsafe../\.jpg"
        disposition = resource.getRepresentationDisposition(
                "attachment; filename=\"unsafe../\\.jpg\"", identifier, outputFormat);
        assertEquals(Disposition.TYPE_ATTACHMENT, disposition.getType());
        assertEquals("unsafe___.jpg", disposition.getFilename());
    }

    @Test
    public void testGetRepresentationDispositionUsingConfiguration()
            throws Exception {
        Configuration config = Configuration.getInstance();

        final Identifier identifier = new Identifier("cats?/\\dogs");
        final Format outputFormat = Format.JPG;

        // test with config key set to "inline"
        config.setProperty(Key.IIIF_CONTENT_DISPOSITION, "inline");
        Disposition disposition = resource.getRepresentationDisposition(
                null, identifier, outputFormat);
        assertEquals(Disposition.TYPE_INLINE, disposition.getType());

        // test with config key set to "attachment"
        config.setProperty(Key.IIIF_CONTENT_DISPOSITION, "attachment");
        disposition = resource.getRepresentationDisposition(
                null, identifier, outputFormat);
        assertEquals(Disposition.TYPE_ATTACHMENT, disposition.getType());
        assertEquals("cats___dogs.jpg", disposition.getFilename());
    }

    @Test
    public void testGetRepresentationDispositionFallsBackToNone()
            throws Exception {
        Configuration config = Configuration.getInstance();

        final Identifier identifier = new Identifier("cats?/\\dogs");
        final Format outputFormat = Format.JPG;

        // undefined config key
        Disposition disposition = resource.getRepresentationDisposition(
                null, identifier, outputFormat);
        assertEquals(Disposition.TYPE_NONE, disposition.getType());

        // empty config key
        config.setProperty(Key.IIIF_CONTENT_DISPOSITION, "");
        disposition = resource.getRepresentationDisposition(
                null, identifier, outputFormat);
        assertEquals(Disposition.TYPE_NONE, disposition.getType());
    }

    /* template() */

    @Test
    public void testTemplate() {
        assertNotNull(resource.template("/error.vm"));
    }

}
