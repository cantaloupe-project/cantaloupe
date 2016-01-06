package edu.illinois.library.cantaloupe.resource.iiif;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.OutputFormat;
import org.apache.commons.configuration.BaseConfiguration;
import org.junit.Test;
import org.restlet.data.Disposition;

import static org.junit.Assert.*;

public class ResourceUtilsTest {

    @Test
    public void testGetRepresentationDisposition() {
        BaseConfiguration config = new BaseConfiguration();
        Application.setConfiguration(config);

        final Identifier identifier = new Identifier("cats?/\\dogs");
        final OutputFormat outputFormat = OutputFormat.JPG;

        // test with undefined config key
        Disposition disposition = ResourceUtils.
                getRepresentationDisposition(identifier, outputFormat);
        assertEquals(Disposition.TYPE_NONE, disposition.getType());

        // test with empty config key
        config.setProperty(ResourceUtils.CONTENT_DISPOSITION_CONFIG_KEY, "");
        disposition = ResourceUtils.
                getRepresentationDisposition(identifier, outputFormat);
        assertEquals(Disposition.TYPE_NONE, disposition.getType());

        // test with config key set to "inline"
        config.setProperty(ResourceUtils.CONTENT_DISPOSITION_CONFIG_KEY,
                "inline");
        disposition = ResourceUtils.
                getRepresentationDisposition(identifier, outputFormat);
        assertEquals(Disposition.TYPE_INLINE, disposition.getType());

        // test with config key set to "attachment"
        config.setProperty(ResourceUtils.CONTENT_DISPOSITION_CONFIG_KEY,
                "attachment");
        disposition = ResourceUtils.
                getRepresentationDisposition(identifier, outputFormat);
        assertEquals(Disposition.TYPE_ATTACHMENT, disposition.getType());
        assertEquals("cats___dogs.jpg", disposition.getFilename());
    }

}
