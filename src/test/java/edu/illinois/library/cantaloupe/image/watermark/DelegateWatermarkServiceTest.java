package edu.illinois.library.cantaloupe.image.watermark;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.OperationList;
import edu.illinois.library.cantaloupe.script.ScriptEngineFactory;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.Before;
import org.junit.Test;

import java.awt.Dimension;
import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class DelegateWatermarkServiceTest {

    private DelegateWatermarkService instance;

    @Before
    public void setUp() throws Exception {
        System.setProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT, "memory");
        Configuration config = ConfigurationFactory.getInstance();
        config.setProperty(ScriptEngineFactory.DELEGATE_SCRIPT_ENABLED_CONFIG_KEY, true);
        config.setProperty(ScriptEngineFactory.DELEGATE_SCRIPT_PATHNAME_CONFIG_KEY,
                TestUtil.getFixture("delegates.rb").getAbsolutePath());

        instance = new DelegateWatermarkService();
    }

    @Test
    public void testGetWatermarkDefsFromScriptReturningWatermark() throws Exception {
        final OperationList opList = new OperationList();
        opList.setIdentifier(new Identifier("cats"));
        opList.setOutputFormat(Format.JPG);
        final Dimension fullSize = new Dimension(100, 100);
        final URL requestUrl = new URL("http://example.org/");
        final Map<String,String> requestHeaders = new HashMap<>();
        final String clientIp = "";
        final Map<String,String> cookies = new HashMap<>();

        Map<String,Object> result = instance.getWatermarkDefsFromScript(
                opList, fullSize, requestUrl, requestHeaders, clientIp, cookies);
        assertEquals(new File("/dev/cats"), result.get("file"));
        assertEquals((long) 5, result.get("inset"));
        assertEquals(Position.BOTTOM_LEFT, result.get("position"));
    }

    @Test
    public void testNewWatermarkWithScriptStrategyReturningFalse() throws Exception {
        final OperationList opList = new OperationList();
        opList.setIdentifier(new Identifier("dogs"));
        opList.setOutputFormat(Format.JPG);
        final Dimension fullSize = new Dimension(100, 100);
        final URL requestUrl = new URL("http://example.org/");
        final Map<String,String> requestHeaders = new HashMap<>();
        final String clientIp = "";
        final Map<String,String> cookies = new HashMap<>();

        Map<String,Object> result = instance.getWatermarkDefsFromScript(
                opList, fullSize, requestUrl, requestHeaders, clientIp, cookies);
        assertNull(result);
    }

}
