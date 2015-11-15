package edu.illinois.library.cantaloupe.resolver;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.CantaloupeTestCase;
import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.request.Identifier;
import org.apache.commons.configuration.BaseConfiguration;
import org.junit.Ignore;

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * TODO:
 * https://github.com/treelogic-swe/aws-mock/wiki/User's-Guide
 */
public class AmazonS3ResolverTest extends CantaloupeTestCase {

    private static final Identifier IMAGE = new Identifier("14405804_o1.jpg");
    AmazonS3Resolver instance;

    public void setUp() throws IOException {
        BaseConfiguration config = new BaseConfiguration();
        config.setProperty(AmazonS3Resolver.BUCKET_NAME_CONFIG_KEY,
                "KFC");
        config.setProperty(AmazonS3Resolver.ENDPOINT_CONFIG_KEY,
                "http://localhost/");
        Application.setConfiguration(config);

        instance = new AmazonS3Resolver();
    }

    // TODO: need to mock S3 somehow
    public void testGetInputStream() {
        /*
        // present, readable image
        try {
            assertNotNull(instance.getInputStream(IMAGE));
        } catch (IOException e) {
            fail();
        }
        // missing image
        try {
            instance.getInputStream(new Identifier("bogus"));
            fail("Expected exception");
        } catch (FileNotFoundException e) {
            // pass
        } catch (IOException e) {
            fail("Expected FileNotFoundException");
        }
        // present, unreadable image
        // TODO: write this
        */
    }

    // TODO: need to mock S3 somehow
    public void testGetSourceFormat() throws IOException {
        /*
        assertEquals(SourceFormat.JPG, instance.getSourceFormat(IMAGE));
        try {
            instance.getSourceFormat(new Identifier("image.bogus"));
            fail("Expected exception");
        } catch (IOException e) {
            // pass
        }
        try {
            instance.getSourceFormat(new Identifier("image"));
            fail("Expected exception");
        } catch (IOException e) {
            // pass
        }*/
    }

}
