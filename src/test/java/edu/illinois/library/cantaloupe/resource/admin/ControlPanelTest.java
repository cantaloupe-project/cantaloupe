package edu.illinois.library.cantaloupe.resource.admin;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.WebApplication;
import edu.illinois.library.cantaloupe.WebServer;
import edu.illinois.library.cantaloupe.processor.ProcessorFactory;
import edu.illinois.library.cantaloupe.resolver.ResolverFactory;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.Select;

import static org.junit.Assert.*;

public class ControlPanelTest {

    private static final String username = "admin";
    private static final String secret = "secret";

    private WebDriver webDriver = new FirefoxDriver();
    private WebServer webServer = Application.getWebServer();

    private WebElement css(String selector) {
        return webDriver.findElement(By.cssSelector(selector));
    }

    private String getAdminUri() {
        return String.format("http://%s:%s@localhost:%d%s",
                username, secret,
                webServer.getHttpPort(),
                WebApplication.ADMIN_PATH);
    }

    @Before
    public void setUp() throws Exception {
        final Configuration config = new BaseConfiguration();
        config.setProperty(WebApplication.ADMIN_SECRET_CONFIG_KEY, secret);
        config.setProperty(ResolverFactory.STATIC_RESOLVER_CONFIG_KEY,
                "FilesystemResolver");
        config.setProperty(ProcessorFactory.FALLBACK_PROCESSOR_CONFIG_KEY,
                "Java2dProcessor");
        Application.setConfiguration(config);

        webServer.setHttpEnabled(true);
        webServer.setHttpPort(TestUtil.getOpenPort());
        webServer.start();
    }

    @After
    public void tearDown() throws Exception {
        webServer.stop();
        webDriver.close();
    }

    @Test
    public void testServerSectionUi() throws Exception {
        final Configuration config = Application.getConfiguration();

        // Navigate to the server section
        webDriver.get(getAdminUri());
        webDriver.findElement(By.cssSelector("#cl-server-button")).click();

        // Fill in the form
        css("[name=\"http.enabled\"]").click();
        css("[name=\"http.port\"]").sendKeys("8989");
        css("[name=\"https.enabled\"]").click();
        css("[name=\"https.port\"]").sendKeys("8990");
        css("[name=\"https.key_store_type\"]").sendKeys("PKCS12");
        css("[name=\"https.key_store_path\"]").sendKeys("/something");
        css("[name=\"https.key_store_password\"]").sendKeys("cats");
        css("[name=\"auth.basic.enabled\"]").click();
        css("[name=\"auth.basic.username\"]").sendKeys("dogs");
        css("[name=\"auth.basic.secret\"]").sendKeys("foxes");
        css("[name=\"base_uri\"]").sendKeys("http://bla/bla/");
        css("[name=\"slash_substitute\"]").sendKeys("^");
        css("[name=\"print_stack_trace_on_error_pages\"]").click();

        // Submit the form
        css("#cl-server input[type=\"submit\"]").click();

        Thread.sleep(500);

        // Assert that the application configuration has been updated correctly
        assertTrue(config.getBoolean("http.enabled"));
        assertEquals(8989, config.getInt("http.port"));
        assertTrue(config.getBoolean("https.enabled"));
        assertEquals(8990, config.getInt("https.port"));
        assertEquals("PKCS12", config.getString("https.key_store_type"));
        assertEquals("/something", config.getString("https.key_store_path"));
        assertEquals("cats", config.getString("https.key_store_password"));
        assertTrue(config.getBoolean("auth.basic.enabled"));
        assertEquals("dogs", config.getString("auth.basic.username"));
        assertEquals("foxes", config.getString("auth.basic.secret"));
        assertEquals("http://bla/bla/", config.getString("base_uri"));
        assertEquals("^", config.getString("slash_substitute"));
        assertTrue(config.getBoolean("print_stack_trace_on_error_pages"));
    }

    @Test
    public void testEndpointsSectionUi() throws Exception {
        final Configuration config = Application.getConfiguration();

        // Navigate to the endpoints section
        webDriver.get(getAdminUri());
        webDriver.findElement(By.cssSelector("#cl-endpoints-button")).click();

        // Fill in the form
        css("[name=\"max_pixels\"]").sendKeys("5000");
        new Select(css("[name=\"endpoint.iiif.content_disposition\"]")).
                selectByValue("attachment");
        css("[name=\"endpoint.iiif.min_tile_size\"]").sendKeys("250");
        css("[name=\"endpoint.iiif.1.enabled\"]").click();
        css("[name=\"endpoint.iiif.2.enabled\"]").click();
        css("[name=\"endpoint.iiif.2.restrict_to_sizes\"]").click();

        // Submit the form
        css("#cl-endpoints input[type=\"submit\"]").click();

        Thread.sleep(500);

        // Assert that the application configuration has been updated correctly
        assertEquals(5000, config.getInt("max_pixels"));
        assertEquals("attachment",
                config.getString("endpoint.iiif.content_disposition"));
        assertEquals(250, config.getInt("endpoint.iiif.min_tile_size"));
        assertTrue(config.getBoolean("endpoint.iiif.1.enabled"));
        assertTrue(config.getBoolean("endpoint.iiif.2.enabled"));
        assertTrue(config.getBoolean("endpoint.iiif.2.restrict_to_sizes"));

    }
/*
    @Test
    public void testResolverSectionUi() {
        // TODO: write this
    }

    @Test
    public void testProcessorsSectionUi() {
        // TODO: write this
    }

    @Test
    public void testCachesSectionUi() {
        // TODO: write this
    }

    @Test
    public void testOverlaysSectionUi() {
        // TODO: write this
    }

    @Test
    public void testDelegateScriptSectionUi() {
        // TODO: write this
    }

    @Test
    public void testLoggingSectionUi() {
        // TODO: write this
    }
*/
}
