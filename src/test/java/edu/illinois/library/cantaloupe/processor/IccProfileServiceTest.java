package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.script.ScriptEngineFactory;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import java.awt.color.ICC_Profile;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import static org.junit.Assert.*;

public class IccProfileServiceTest {

    IccProfileService instance;

    @Before
    public void setUp() throws IOException {
        Configuration config = Configuration.getInstance();
        config.setProperty(ScriptEngineFactory.DELEGATE_SCRIPT_ENABLED_CONFIG_KEY,
                true);
        config.setProperty(ScriptEngineFactory.DELEGATE_SCRIPT_PATHNAME_CONFIG_KEY,
                TestUtil.getFixture("delegates.rb").getAbsolutePath());

        instance = new IccProfileService();
    }

    /* getProfile() */

    @Test
    public void testGetExistingProfileWithFilename() throws Exception {
        File dest = new File("./test.icc");
        try {
            FileUtils.copyFile(TestUtil.getFixture("AdobeRGB1998.icc"), dest);
            assertNotNull(instance.getProfile(dest.getName()));
        } finally {
            FileUtils.forceDelete(dest);
        }
    }

    @Test
    public void testGetNonExistingProfileWithFilename() throws Exception {
        try {
            File dest = new File("./cats.icc");
            instance.getProfile(dest.getName());
            fail("Expected exception");
        } catch (FileNotFoundException e) {
            // pass
        }
    }

    @Test
    public void testGetExistingProfileWithAbsolutePath() throws Exception {
        File profile = TestUtil.getFixture("AdobeRGB1998.icc");
        assertNotNull(instance.getProfile(profile.getAbsolutePath()));
    }

    @Test
    public void testGetNonExistingProfileWithAbsolutePath() throws Exception {
        File profile = new File("/tmp/bogus.cats");
        try {
            instance.getProfile(profile.getAbsolutePath());
            fail("Expected exception");
        } catch (FileNotFoundException e) {
            // pass
        }
    }

    /* getProfileFromDelegateMethod() */

    @Test
    public void testGetProfileFromDelegateMethodReturningProfile() throws Exception {
        // The icc_profile() delegate method can't return a valid absolute
        // pathname (because it is preloaded into jruby as a string and thus
        // can't orient itself on the filesystem), so it has been rigged to
        // return a bogus one. Checking for that is the best we can do while
        // keeping this test portable.
        try {
            instance.getProfileFromDelegateMethod(
                    new Identifier("cats"), null, "127.0.0.1");
            fail("Expected exception");
        } catch (FileNotFoundException e) {
            // pass
        }
    }

    @Test
    public void testGetProfileFromDelegateMethodReturningNil() throws Exception {
        ICC_Profile result = instance.getProfileFromDelegateMethod(
                    new Identifier("bogus"), null, "127.0.0.1");
        assertNull(result);
    }

}
