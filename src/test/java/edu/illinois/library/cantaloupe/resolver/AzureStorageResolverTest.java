package edu.illinois.library.cantaloupe.resolver;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.script.ScriptEngineFactory;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import static org.junit.Assert.*;

/**
 * <p>Tests AzureStorageResolver against Azure Storage. Requires an AWS
 * account).</p>
 *
 * <p>This test requires a file to be present at
 * {user.home}/.azure/cantaloupe that is formatted as such:</p>
 *
 * <pre>account_name=xxxxxx
 * account_key=xxxxxx
 * container=xxxxxx</pre>
 *
 * <p>Also, the container must contain an image called
 * orion-hubble-4096.jpg.</p>
 */
public class AzureStorageResolverTest {

    private static final Identifier IDENTIFIER =
            new Identifier("orion-hubble-4096.jpg");

    AzureStorageResolver instance;

    @Before
    public void setUp() throws IOException {
        FileInputStream fis = new FileInputStream(new File(
                System.getProperty("user.home") + "/.azure/cantaloupe"));
        String authInfo = IOUtils.toString(fis);
        String[] lines = StringUtils.split(authInfo, "\n");
        final String accountName = lines[0].replace("account_name=", "").trim();
        final String accountKey = lines[1].replace("account_key=", "").trim();
        final String container = lines[2].replace("container=", "").trim();

        BaseConfiguration config = new BaseConfiguration();
        config.setProperty(AzureStorageResolver.CONTAINER_NAME_CONFIG_KEY, container);
        config.setProperty(AzureStorageResolver.ACCOUNT_NAME_CONFIG_KEY, accountName);
        config.setProperty(AzureStorageResolver.ACCOUNT_KEY_CONFIG_KEY, accountKey);
        config.setProperty(AzureStorageResolver.LOOKUP_STRATEGY_CONFIG_KEY,
                "BasicLookupStrategy");
        Application.setConfiguration(config);

        instance = new AzureStorageResolver();
        instance.setIdentifier(IDENTIFIER);
    }

    @Test
    public void testGetStreamSourceWithBasicLookupStrategy() {
        // present, readable image
        try {
            assertNotNull(instance.getStreamSource());
        } catch (IOException e) {
            fail();
        }
        // missing image
        try {
            instance.setIdentifier(new Identifier("bogus"));
            instance.getStreamSource();
            fail("Expected exception");
        } catch (FileNotFoundException e) {
            // pass
        } catch (IOException e) {
            fail("Expected FileNotFoundException");
        }
    }

    @Test
    public void testGetStreamSourceWithScriptLookupStrategy() throws Exception {
        Configuration config = Application.getConfiguration();
        config.setProperty(AmazonS3Resolver.LOOKUP_STRATEGY_CONFIG_KEY,
                "ScriptLookupStrategy");
        config.setProperty(ScriptEngineFactory.DELEGATE_SCRIPT_ENABLED_CONFIG_KEY,
                "true");
        config.setProperty(ScriptEngineFactory.DELEGATE_SCRIPT_PATHNAME_CONFIG_KEY,
                TestUtil.getFixture("delegates.rb").getAbsolutePath());
        // present image
        try {
            StreamSource source = instance.getStreamSource();
            assertNotNull(source.newInputStream());
        } catch (IOException e) {
            fail();
        }
        // missing image
        try {
            instance.setIdentifier(new Identifier("bogus"));
            instance.getStreamSource();
            fail("Expected exception");
        } catch (FileNotFoundException e) {
            // pass
        } catch (IOException e) {
            fail("Expected FileNotFoundException");
        }
    }

    @Test
    public void testGetSourceFormatWithBasicLookupStrategy() throws IOException {
        assertEquals(Format.JPG, instance.getSourceFormat());
        try {
            instance.setIdentifier(new Identifier("image.bogus"));
            instance.getSourceFormat();
            fail("Expected exception");
        } catch (IOException e) {
            // pass
        }
        try {
            instance.setIdentifier(new Identifier("image"));
            instance.getSourceFormat();
            fail("Expected exception");
        } catch (IOException e) {
            // pass
        }
    }

    @Test
    public void testGetSourceFormatWithScriptLookupStrategy() throws IOException {
        Configuration config = Application.getConfiguration();
        config.setProperty(AmazonS3Resolver.LOOKUP_STRATEGY_CONFIG_KEY,
                "ScriptLookupStrategy");
        config.setProperty(ScriptEngineFactory.DELEGATE_SCRIPT_ENABLED_CONFIG_KEY,
                "true");
        config.setProperty(ScriptEngineFactory.DELEGATE_SCRIPT_PATHNAME_CONFIG_KEY,
                TestUtil.getFixture("delegates.rb").getAbsolutePath());
        // present image
        assertEquals(Format.JPG, instance.getSourceFormat());
        // present image without extension TODO: write this

        // missing image with extension
        try {
            instance.setIdentifier(new Identifier("image.bogus"));
            instance.getSourceFormat();
            fail("Expected exception");
        } catch (IOException e) {
            // pass
        }
        // missing image without extension
        try {
            instance.setIdentifier(new Identifier("image"));
            instance.getSourceFormat();
            fail("Expected exception");
        } catch (IOException e) {
            // pass
        }
    }

}
