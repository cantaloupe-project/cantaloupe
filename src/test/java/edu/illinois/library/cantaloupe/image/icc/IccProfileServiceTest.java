package edu.illinois.library.cantaloupe.image.icc;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.script.ScriptEngineFactory;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import static org.junit.Assert.*;

public class IccProfileServiceTest {

    IccProfileService instance;

    @Before
    public void setUp() throws IOException {
        final Configuration config = Configuration.getInstance();
        config.setProperty(ScriptEngineFactory.DELEGATE_SCRIPT_ENABLED_CONFIG_KEY,
                true);
        config.setProperty(ScriptEngineFactory.DELEGATE_SCRIPT_PATHNAME_CONFIG_KEY,
                TestUtil.getFixture("delegates.rb").getAbsolutePath());

        instance = new IccProfileService();
    }

    /* getProfile(Identifier, Map, String) */

    @Test
    public void testGetExistingProfileWithBasicStrategyAndExistingAbsolutePath()
            throws Exception {
        final Configuration config = Configuration.getInstance();
        config.setProperty(IccProfileService.ICC_STRATEGY_CONFIG_KEY,
                "BasicStrategy");
        config.setProperty(IccProfileService.ICC_BASIC_STRATEGY_PROFILE_CONFIG_KEY,
                TestUtil.getFixture("AdobeRGB1998.icc").getAbsolutePath());
        config.setProperty(IccProfileService.ICC_BASIC_STRATEGY_PROFILE_NAME_CONFIG_KEY,
                "cats");

        IccProfile profile = instance.getProfile(new Identifier("dogs"),
                new HashMap<String, String>(),
                "127.0.0.1");

        assertEquals("cats", profile.getName());
        assertNotNull(profile.getProfile());
    }

    @Test
    public void testGetExistingProfileWithBasicStrategyAndNonExistingAbsolutePath()
            throws Exception {
        final Configuration config = Configuration.getInstance();
        config.setProperty(IccProfileService.ICC_STRATEGY_CONFIG_KEY,
                "BasicStrategy");
        config.setProperty(IccProfileService.ICC_BASIC_STRATEGY_PROFILE_CONFIG_KEY,
                "/bogus/bogus/bogus.icc");
        config.setProperty(IccProfileService.ICC_BASIC_STRATEGY_PROFILE_NAME_CONFIG_KEY,
                "cats");

        IccProfile profile = instance.getProfile(new Identifier("dogs"),
                new HashMap<String, String>(),
                "127.0.0.1");
        assertEquals("cats", profile.getName());
        assertEquals(new File("/bogus/bogus/bogus.icc"), profile.getFile());
    }

    @Test
    public void testGetExistingProfileWithBasicStrategyAndExistingFilename()
            throws Exception {
        File profileFile = TestUtil.getFixture("AdobeRGB1998.icc");
        File dest = new File("./test.icc");
        try {
            FileUtils.copyFile(profileFile, dest);

            final Configuration config = Configuration.getInstance();
            config.setProperty(IccProfileService.ICC_STRATEGY_CONFIG_KEY,
                    "BasicStrategy");
            config.setProperty(IccProfileService.ICC_BASIC_STRATEGY_PROFILE_CONFIG_KEY,
                    "test.icc");
            config.setProperty(IccProfileService.ICC_BASIC_STRATEGY_PROFILE_NAME_CONFIG_KEY,
                    "cats");

            IccProfile profile = instance.getProfile(new Identifier("dogs"),
                    new HashMap<String, String>(),
                    "127.0.0.1");

            assertEquals("cats", profile.getName());
            assertNotNull(profile.getProfile());
        } finally {
            FileUtils.forceDelete(dest);
        }
    }

    @Test
    public void testGetExistingProfileWithBasicStrategyAndNonExistingFilename()
            throws Exception {
        final Configuration config = Configuration.getInstance();
        config.setProperty(IccProfileService.ICC_STRATEGY_CONFIG_KEY,
                "BasicStrategy");
        config.setProperty(IccProfileService.ICC_BASIC_STRATEGY_PROFILE_CONFIG_KEY,
                "bogus.icc");
        config.setProperty(IccProfileService.ICC_BASIC_STRATEGY_PROFILE_NAME_CONFIG_KEY,
                "cats");

        IccProfile profile = instance.getProfile(new Identifier("dogs"),
                new HashMap<String, String>(),
                "127.0.0.1");
        assertEquals("cats", profile.getName());
        assertEquals("bogus.icc", profile.getFile().getName());
    }

    @Test
    public void testGetExistingProfileWithScriptStrategyAndExistingAbsolutePath()
            throws Exception {
        final Configuration config = Configuration.getInstance();
        config.setProperty(IccProfileService.ICC_STRATEGY_CONFIG_KEY,
                "ScriptStrategy");

        IccProfile profile = instance.getProfile(new Identifier("cats"),
                new HashMap<String, String>(),
                "127.0.0.1");
        assertEquals("AdobeRGB1998", profile.getName());
        assertEquals(new File("/bogus/AdobeRGB1998.icc"), profile.getFile());
    }

    /* isEnabled() */

    @Test
    public void testIsEnabled() {
        Configuration config = Configuration.getInstance();
        config.setProperty(IccProfileService.ICC_ENABLED_CONFIG_KEY, true);
        assertTrue(instance.isEnabled());

        config.setProperty(IccProfileService.ICC_ENABLED_CONFIG_KEY, false);
        assertFalse(instance.isEnabled());
    }

}
