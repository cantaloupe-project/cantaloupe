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

    private static final int SLEEP_AFTER_SUBMIT = 700;
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
    public void testUi() throws Exception {
        webDriver.get(getAdminUri());
        testServerSection();
        testEndpointsSection();
        testResolverSection();
        testProcessorsSection();
        testCachesSection();
        testOverlaysSection();
        testDelegateScriptSection();
        testLoggingSection();
    }

    private void testServerSection() throws Exception {
        css("#cl-server-button").click();

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

        Thread.sleep(SLEEP_AFTER_SUBMIT);

        // Assert that the application configuration has been updated correctly
        final Configuration config = Application.getConfiguration();
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

    private void testEndpointsSection() throws Exception {
        css("#cl-endpoints-button").click();

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

        Thread.sleep(SLEEP_AFTER_SUBMIT);

        // Assert that the application configuration has been updated correctly
        final Configuration config = Application.getConfiguration();
        assertEquals(5000, config.getInt("max_pixels"));
        assertEquals("attachment",
                config.getString("endpoint.iiif.content_disposition"));
        assertEquals(250, config.getInt("endpoint.iiif.min_tile_size"));
        assertTrue(config.getBoolean("endpoint.iiif.1.enabled"));
        assertTrue(config.getBoolean("endpoint.iiif.2.enabled"));
        assertTrue(config.getBoolean("endpoint.iiif.2.restrict_to_sizes"));
    }

    private void testResolverSection() throws Exception {
        css("#cl-resolver-button").click();

        // Fill in the form
        new Select(css("[name=\"resolver.delegate\"]")).selectByValue("false");
        new Select(css("[name=\"resolver.static\"]")).
                selectByVisibleText("FilesystemResolver");
        // AmazonS3Resolver section
        css("#cl-resolver li > a[href=\"#AmazonS3Resolver\"]").click();
        css("[name=\"AmazonS3Resolver.access_key_id\"]").sendKeys("123");
        css("[name=\"AmazonS3Resolver.secret_key\"]").sendKeys("456");
        css("[name=\"AmazonS3Resolver.bucket.name\"]").sendKeys("cats");
        css("[name=\"AmazonS3Resolver.bucket.region\"]").sendKeys("antarctica");
        new Select(css("[name=\"AmazonS3Resolver.lookup_strategy\"]")).
                selectByValue("BasicLookupStrategy");
        // AzureStorageResolver
        css("#cl-resolver li > a[href=\"#AzureStorageResolver\"]").click();
        css("[name=\"AzureStorageResolver.account_name\"]").sendKeys("bla");
        css("[name=\"AzureStorageResolver.account_key\"]").sendKeys("cats");
        css("[name=\"AzureStorageResolver.container_name\"]").sendKeys("bucket");
        new Select(css("[name=\"AzureStorageResolver.lookup_strategy\"]")).
                selectByValue("BasicLookupStrategy");
        // FilesystemResolver
        css("#cl-resolver li > a[href=\"#FilesystemResolver\"]").click();
        new Select(css("[name=\"FilesystemResolver.lookup_strategy\"]")).
                selectByValue("BasicLookupStrategy");
        css("[name=\"FilesystemResolver.BasicLookupStrategy.path_prefix\"]").
                sendKeys("/prefix");
        css("[name=\"FilesystemResolver.BasicLookupStrategy.path_suffix\"]").
                sendKeys("/suffix");
        // HttpResolver
        css("#cl-resolver li > a[href=\"#HttpResolver\"]").click();
        new Select(css("[name=\"HttpResolver.lookup_strategy\"]")).
                selectByValue("BasicLookupStrategy");
        css("[name=\"HttpResolver.BasicLookupStrategy.url_prefix\"]").
                sendKeys("http://prefix/");
        css("[name=\"HttpResolver.BasicLookupStrategy.url_suffix\"]").
                sendKeys("/suffix");
        css("[name=\"HttpResolver.auth.basic.username\"]").sendKeys("username");
        css("[name=\"HttpResolver.auth.basic.secret\"]").sendKeys("password");
        // JdbcResolver
        css("#cl-resolver li > a[href=\"#JdbcResolver\"]").click();
        css("[name=\"JdbcResolver.url\"]").sendKeys("cats://dogs");
        css("[name=\"JdbcResolver.user\"]").sendKeys("user");
        css("[name=\"JdbcResolver.password\"]").sendKeys("password");
        css("[name=\"JdbcResolver.max_pool_size\"]").sendKeys("4");
        css("[name=\"JdbcResolver.connection_timeout\"]").sendKeys("5");

        // Submit the form
        css("#cl-resolver input[type=\"submit\"]").click();

        Thread.sleep(SLEEP_AFTER_SUBMIT);

        // Assert that the application configuration has been updated correctly
        final Configuration config = Application.getConfiguration();
        assertFalse(config.getBoolean("resolver.delegate"));
        assertEquals("FilesystemResolver", config.getString("resolver.static"));
        // AmazonS3Resolver
        assertEquals("123", config.getString("AmazonS3Resolver.access_key_id"));
        assertEquals("456", config.getString("AmazonS3Resolver.secret_key"));
        assertEquals("cats", config.getString("AmazonS3Resolver.bucket.name"));
        assertEquals("antarctica",
                config.getString("AmazonS3Resolver.bucket.region"));
        assertEquals("BasicLookupStrategy",
                config.getString("AmazonS3Resolver.lookup_strategy"));
        // AzureStorageResolver
        assertEquals("bla", config.getString("AzureStorageResolver.account_name"));
        assertEquals("cats", config.getString("AzureStorageResolver.account_key"));
        assertEquals("bucket", config.getString("AzureStorageResolver.container_name"));
        assertEquals("BasicLookupStrategy",
                config.getString("AzureStorageResolver.lookup_strategy"));
        // FilesystemResolver
        assertEquals("BasicLookupStrategy",
                config.getString("FilesystemResolver.lookup_strategy"));
        assertEquals("/prefix",
                config.getString("FilesystemResolver.BasicLookupStrategy.path_prefix"));
        assertEquals("/suffix",
                config.getString("FilesystemResolver.BasicLookupStrategy.path_suffix"));
        // HttpResolver
        assertEquals("BasicLookupStrategy",
                config.getString("HttpResolver.lookup_strategy"));
        assertEquals("http://prefix/",
                config.getString("HttpResolver.BasicLookupStrategy.url_prefix"));
        assertEquals("/suffix",
                config.getString("HttpResolver.BasicLookupStrategy.url_suffix"));
        assertEquals("username",
                config.getString("HttpResolver.auth.basic.username"));
        assertEquals("password",
                config.getString("HttpResolver.auth.basic.secret"));
        // JdbcResolver
        assertEquals("cats://dogs", config.getString("JdbcResolver.url"));
        assertEquals("user", config.getString("JdbcResolver.user"));
        assertEquals("password", config.getString("JdbcResolver.password"));
        assertEquals("4", config.getString("JdbcResolver.max_pool_size"));
        assertEquals("5", config.getString("JdbcResolver.connection_timeout"));
    }

    private void testProcessorsSection() throws Exception {
        css("#cl-processors-button").click();

        // Fill in the form
        css("#cl-processors li > a[href=\"#cl-image-assignments\"]").click();
        new Select(css("[name=\"processor.gif\"]")).
                selectByVisibleText("Java2dProcessor");
        new Select(css("[name=\"processor.fallback\"]")).
                selectByVisibleText("JaiProcessor");
        new Select(css("[name=\"StreamProcessor.retrieval_strategy\"]")).
                selectByValue("StreamStrategy");
        // FfmpegProcessor
        css("#cl-processors li > a[href=\"#FfmpegProcessor\"]").click();
        css("[name=\"FfmpegProcessor.path_to_binaries\"]").sendKeys("/ffpath");
        // GraphicsMagickProcessor
        css("#cl-processors li > a[href=\"#GraphicsMagickProcessor\"]").click();
        css("[name=\"GraphicsMagickProcessor.path_to_binaries\"]").sendKeys("/gmpath");
        new Select(css("[name=\"GraphicsMagickProcessor.background_color\"]")).
                selectByValue("black");
        // ImageMagickProcessor
        css("#cl-processors li > a[href=\"#ImageMagickProcessor\"]").click();
        css("[name=\"ImageMagickProcessor.path_to_binaries\"]").sendKeys("/impath");
        new Select(css("[name=\"ImageMagickProcessor.background_color\"]")).
                selectByValue("white");
        // JaiProcessor
        css("#cl-processors li > a[href=\"#JaiProcessor\"]").click();
        css("[name=\"JaiProcessor.jpg.quality\"]").sendKeys("0.55");
        new Select(css("[name=\"JaiProcessor.tif.compression\"]")).
                selectByVisibleText("PackBits");
        // Java2dProcessor
        css("#cl-processors li > a[href=\"#Java2dProcessor\"]").click();
        new Select(css("[name=\"Java2dProcessor.scale_mode\"]")).
                selectByValue("quality");
        css("[name=\"Java2dProcessor.jpg.quality\"]").sendKeys("0.55");
        new Select(css("[name=\"Java2dProcessor.tif.compression\"]")).
                selectByVisibleText("PackBits");
        // KakaduProcessor
        css("#cl-processors li > a[href=\"#KakaduProcessor\"]").click();
        css("[name=\"KakaduProcessor.path_to_binaries\"]").sendKeys("/kpath");
        new Select(css("[name=\"KakaduProcessor.post_processor\"]")).
                selectByValue("jai");
        new Select(css("[name=\"KakaduProcessor.post_processor.java2d.scale_mode\"]")).
                selectByValue("quality");
        // OpenJpegProcessor
        css("#cl-processors li > a[href=\"#OpenJpegProcessor\"]").click();
        css("[name=\"OpenJpegProcessor.path_to_binaries\"]").sendKeys("/ojpath");
        new Select(css("[name=\"OpenJpegProcessor.post_processor\"]")).
                selectByValue("java2d");
        new Select(css("[name=\"OpenJpegProcessor.post_processor.java2d.scale_mode\"]")).
                selectByValue("speed");
        // PdfBoxProcessor
        css("#cl-processors li > a[href=\"#PdfBoxProcessor\"]").click();
        css("[name=\"PdfBoxProcessor.dpi\"]").sendKeys("300");
        new Select(css("[name=\"PdfBoxProcessor.post_processor.java2d.scale_mode\"]")).
                selectByValue("quality");

        // Submit the form
        css("#cl-processors input[type=\"submit\"]").click();

        Thread.sleep(SLEEP_AFTER_SUBMIT);

        // Assert that the application configuration has been updated correctly
        final Configuration config = Application.getConfiguration();
        assertEquals("Java2dProcessor", config.getString("processor.gif"));
        assertEquals("JaiProcessor", config.getString("processor.fallback"));
        assertEquals("StreamStrategy",
                config.getString("StreamProcessor.retrieval_strategy"));
        // FfmpegProcessor
        assertEquals("/ffpath",
                config.getString("FfmpegProcessor.path_to_binaries"));
        // GraphicsMagickProcessor
        assertEquals("/gmpath",
                config.getString("GraphicsMagickProcessor.path_to_binaries"));
        assertEquals("black",
                config.getString("GraphicsMagickProcessor.background_color"));
        // ImageMagickProcessor
        assertEquals("/impath",
                config.getString("ImageMagickProcessor.path_to_binaries"));
        assertEquals("white",
                config.getString("ImageMagickProcessor.background_color"));
        // JaiProcessor
        assertEquals("0.55", config.getString("JaiProcessor.jpg.quality"));
        assertEquals("PackBits",
                config.getString("JaiProcessor.tif.compression"));
        // Java2dProcessor
        assertEquals("quality", config.getString("Java2dProcessor.scale_mode"));
        assertEquals("0.55",
                config.getString("Java2dProcessor.jpg.quality"));
        assertEquals("PackBits",
                config.getString("Java2dProcessor.tif.compression"));
        // KakaduProcessor
        assertEquals("/kpath",
                config.getString("KakaduProcessor.path_to_binaries"));
        assertEquals("jai", config.getString("KakaduProcessor.post_processor"));
        assertEquals("quality",
                config.getString("KakaduProcessor.post_processor.java2d.scale_mode"));
        // OpenJpegProcessor
        assertEquals("/ojpath",
                config.getString("OpenJpegProcessor.path_to_binaries"));
        assertEquals("java2d",
                config.getString("OpenJpegProcessor.post_processor"));
        assertEquals("speed",
                config.getString("OpenJpegProcessor.post_processor.java2d.scale_mode"));
        // PdfBoxProcessor
        assertEquals(300, config.getInt("PdfBoxProcessor.dpi"));
        assertEquals("quality",
                config.getString("PdfBoxProcessor.post_processor.java2d.scale_mode"));
    }

    private void testCachesSection() throws Exception {
        css("#cl-caches-button").click();

        // Fill in the form
        css("[name=\"cache.client.enabled\"]").click();
        css("[name=\"cache.client.max_age\"]").sendKeys("250");
        css("[name=\"cache.client.shared_max_age\"]").sendKeys("220");
        css("[name=\"cache.client.public\"]").click();
        css("[name=\"cache.client.private\"]").click();
        css("[name=\"cache.client.no_cache\"]").click();
        css("[name=\"cache.client.no_store\"]").click();
        css("[name=\"cache.client.must_revalidate\"]").click();
        css("[name=\"cache.client.proxy_revalidate\"]").click();
        css("[name=\"cache.client.no_transform\"]").click();
        new Select(css("[name=\"cache.source\"]")).selectByValue("");
        new Select(css("[name=\"cache.derivative\"]")).
                selectByVisibleText("FilesystemCache");
        css("[name=\"cache.server.purge_missing\"]").click();
        css("[name=\"cache.server.resolve_first\"]").click();
        css("[name=\"cache.server.worker.enabled\"]").click();
        css("[name=\"cache.server.worker.interval\"]").sendKeys("25");
        // AmazonS3Cache
        css("#cl-caches li > a[href=\"#AmazonS3Cache\"]").click();
        css("[name=\"AmazonS3Cache.access_key_id\"]").sendKeys("cats");
        css("[name=\"AmazonS3Cache.secret_key\"]").sendKeys("dogs");
        css("[name=\"AmazonS3Cache.bucket.name\"]").sendKeys("bucket");
        css("[name=\"AmazonS3Cache.bucket.region\"]").sendKeys("greenland");
        css("[name=\"AmazonS3Cache.object_key_prefix\"]").sendKeys("obj");
        css("[name=\"AmazonS3Cache.ttl_seconds\"]").sendKeys("543");
        // AzureStorageCache
        css("#cl-caches li > a[href=\"#AzureStorageCache\"]").click();
        css("[name=\"AzureStorageCache.account_name\"]").sendKeys("bees");
        css("[name=\"AzureStorageCache.account_key\"]").sendKeys("birds");
        css("[name=\"AzureStorageCache.container_name\"]").sendKeys("badger");
        css("[name=\"AzureStorageCache.object_key_prefix\"]").sendKeys("obj");
        css("[name=\"AzureStorageCache.ttl_seconds\"]").sendKeys("345");
        // FilesystemCache
        css("#cl-caches li > a[href=\"#FilesystemCache\"]").click();
        css("[name=\"FilesystemCache.pathname\"]").sendKeys("/path");
        css("[name=\"FilesystemCache.dir.depth\"]").sendKeys("8");
        css("[name=\"FilesystemCache.dir.name_length\"]").sendKeys("4");
        css("[name=\"FilesystemCache.ttl_seconds\"]").sendKeys("59");
        // JdbcCache
        css("#cl-caches li > a[href=\"#JdbcCache\"]").click();
        css("[name=\"JdbcCache.url\"]").sendKeys("jdbc://dogs");
        css("[name=\"JdbcCache.user\"]").sendKeys("person");
        css("[name=\"JdbcCache.password\"]").sendKeys("cats");
        css("[name=\"JdbcCache.max_pool_size\"]").sendKeys("92");
        css("[name=\"JdbcCache.connection_timeout\"]").sendKeys("9");
        css("[name=\"JdbcCache.derivative_image_table\"]").sendKeys("hula");
        css("[name=\"JdbcCache.info_table\"]").sendKeys("box");
        css("[name=\"JdbcCache.ttl_seconds\"]").sendKeys("9");

        // Submit the form
        css("#cl-caches input[type=\"submit\"]").click();

        Thread.sleep(SLEEP_AFTER_SUBMIT);

        // Assert that the application configuration has been updated correctly
        final Configuration config = Application.getConfiguration();
        assertTrue(config.getBoolean("cache.client.enabled"));
        assertEquals("250", config.getString("cache.client.max_age"));
        assertEquals("220", config.getString("cache.client.shared_max_age"));
        assertTrue(config.getBoolean("cache.client.public"));
        assertTrue(config.getBoolean("cache.client.private"));
        assertTrue(config.getBoolean("cache.client.no_cache"));
        assertTrue(config.getBoolean("cache.client.no_store"));
        assertTrue(config.getBoolean("cache.client.must_revalidate"));
        assertTrue(config.getBoolean("cache.client.proxy_revalidate"));
        assertTrue(config.getBoolean("cache.client.no_transform"));
        assertEquals("", config.getString("cache.source"));
        assertEquals("FilesystemCache", config.getString("cache.derivative"));
        assertTrue(config.getBoolean("cache.server.purge_missing"));
        assertTrue(config.getBoolean("cache.server.resolve_first"));
        assertTrue(config.getBoolean("cache.server.worker.enabled"));
        assertEquals(25, config.getInt("cache.server.worker.interval"));
        // AmazonS3Cache
        assertEquals("cats", config.getString("AmazonS3Cache.access_key_id"));
        assertEquals("dogs", config.getString("AmazonS3Cache.secret_key"));
        assertEquals("bucket", config.getString("AmazonS3Cache.bucket.name"));
        assertEquals("greenland", config.getString("AmazonS3Cache.bucket.region"));
        assertEquals("obj", config.getString("AmazonS3Cache.object_key_prefix"));
        assertEquals("543", config.getString("AmazonS3Cache.ttl_seconds"));
        // AzureStorageCache
        assertEquals("bees", config.getString("AzureStorageCache.account_name"));
        assertEquals("birds", config.getString("AzureStorageCache.account_key"));
        assertEquals("badger", config.getString("AzureStorageCache.container_name"));
        assertEquals("obj", config.getString("AzureStorageCache.object_key_prefix"));
        assertEquals("345", config.getString("AzureStorageCache.ttl_seconds"));
        // FilesystemCache
        assertEquals("/path", config.getString("FilesystemCache.pathname"));
        assertEquals("8", config.getString("FilesystemCache.dir.depth"));
        assertEquals("4", config.getString("FilesystemCache.dir.name_length"));
        assertEquals("59", config.getString("FilesystemCache.ttl_seconds"));
        // JdbcCache
        assertEquals("jdbc://dogs", config.getString("JdbcCache.url"));
        assertEquals("person", config.getString("JdbcCache.user"));
        assertEquals("cats", config.getString("JdbcCache.password"));
        assertEquals("92", config.getString("JdbcCache.max_pool_size"));
        assertEquals("9", config.getString("JdbcCache.connection_timeout"));
        assertEquals("hula", config.getString("JdbcCache.derivative_image_table"));
        assertEquals("box", config.getString("JdbcCache.info_table"));
        assertEquals("9", config.getString("JdbcCache.ttl_seconds"));
    }

    private void testOverlaysSection() throws Exception {
        css("#cl-overlays-button").click();

        // Fill in the form
        css("[name=\"watermark.enabled\"]").click();
        new Select(css("[name=\"watermark.strategy\"]")).
                selectByValue("BasicStrategy");
        css("[name=\"watermark.BasicStrategy.image\"]").sendKeys("/image.png");
        new Select(css("[name=\"watermark.BasicStrategy.position\"]")).
                selectByValue("bottom left");
        css("[name=\"watermark.BasicStrategy.inset\"]").sendKeys("35");
        css("[name=\"watermark.BasicStrategy.output_width_threshold\"]").sendKeys("5");
        css("[name=\"watermark.BasicStrategy.output_height_threshold\"]").sendKeys("6");
        css("[name=\"redaction.enabled\"]").click();

        // Submit the form
        css("#cl-overlays input[type=\"submit\"]").click();

        Thread.sleep(SLEEP_AFTER_SUBMIT);

        // Assert that the application configuration has been updated correctly
        final Configuration config = Application.getConfiguration();
        assertTrue(config.getBoolean("watermark.enabled"));
        assertEquals("BasicStrategy", config.getString("watermark.strategy"));
        assertEquals("/image.png",
                config.getString("watermark.BasicStrategy.image"));
        assertEquals("bottom left",
                config.getString("watermark.BasicStrategy.position"));
        assertEquals("35",
                config.getString("watermark.BasicStrategy.inset"));
        assertEquals("5",
                config.getString("watermark.BasicStrategy.output_width_threshold"));
        assertEquals("6",
                config.getString("watermark.BasicStrategy.output_height_threshold"));
        assertTrue(config.getBoolean("redaction.enabled"));
    }

    private void testDelegateScriptSection() {
        // TODO: write this
    }

    private void testLoggingSection() {
        // TODO: write this
    }

}
