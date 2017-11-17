package edu.illinois.library.cantaloupe.resource.admin;

import edu.illinois.library.cantaloupe.RestletApplication;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.resource.ResourceTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.support.ui.Select;

import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.Assert.*;

/**
 * Functional test of the Control Panel using Selenium.
 */
public class ControlPanelTest extends ResourceTest {

    private static final int WAIT_AFTER_SUBMIT = 1200;
    private static final String SECRET = "secret";

    private static WebDriver webDriver;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.ADMIN_SECRET, SECRET);
        config.setProperty(Key.RESOLVER_STATIC, "FilesystemResolver");
        config.setProperty(Key.PROCESSOR_FALLBACK, "Java2dProcessor");

        config.clearProperty(Key.FILESYSTEMRESOLVER_PATH_PREFIX);
        config.clearProperty(Key.DELEGATE_SCRIPT_PATHNAME);

        DesiredCapabilities capabilities = DesiredCapabilities.htmlUnitWithJs();
        webDriver = new HtmlUnitDriver(capabilities);
        ((HtmlUnitDriver) webDriver).setJavascriptEnabled(true);
        webDriver.get(getHTTPURI("").toString());
    }

    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
        webDriver.close();
    }

    @Override
    protected String getEndpointPath() {
        return RestletApplication.ADMIN_PATH;
    }

    @Override
    protected URI getHTTPURI(String path) {
        try {
            return new URI("http://admin:" + SECRET + "@localhost:" +
                    appServer.getHTTPPort() + getEndpointPath() + path);
        } catch (URISyntaxException e) {
            fail(e.getMessage());
        }
        return null;
    }

    private WebElement css(String selector) {
        return webDriver.findElement(By.cssSelector(selector));
    }

    private WebElement inputNamed(Key key) {
        return inputNamed(key.key());
    }

    private WebElement inputNamed(String key) {
        return css("[name=\"" + key + "\"]");
    }

    private Select selectNamed(Key key) {
        return selectNamed(key.key());
    }

    private Select selectNamed(String key) {
        return new Select(inputNamed(key));
    }

    @Test
    public void testApplicationSection() throws Exception {
        css("#cl-application-button > a").click();

        // Fill in the form

        // Temporary Directory
        inputNamed(Key.TEMP_PATHNAME).sendKeys("/bla/bla");

        // Delegate Script
        inputNamed(Key.DELEGATE_SCRIPT_ENABLED).click();
        inputNamed(Key.DELEGATE_SCRIPT_PATHNAME).sendKeys("file");
        inputNamed(Key.DELEGATE_METHOD_INVOCATION_CACHE_ENABLED).click();

        // Application log
        selectNamed(Key.APPLICATION_LOG_LEVEL).selectByValue("warn");
        inputNamed(Key.APPLICATION_LOG_CONSOLEAPPENDER_ENABLED).click();
        inputNamed(Key.APPLICATION_LOG_FILEAPPENDER_ENABLED).click();
        inputNamed(Key.APPLICATION_LOG_FILEAPPENDER_PATHNAME).sendKeys("/path1");
        inputNamed(Key.APPLICATION_LOG_ROLLINGFILEAPPENDER_ENABLED).click();
        inputNamed(Key.APPLICATION_LOG_ROLLINGFILEAPPENDER_PATHNAME).
                sendKeys("/path2");
        inputNamed(Key.APPLICATION_LOG_ROLLINGFILEAPPENDER_FILENAME_PATTERN).
                sendKeys("pattern");
        inputNamed(Key.APPLICATION_LOG_ROLLINGFILEAPPENDER_MAX_HISTORY).
                sendKeys("15");
        inputNamed(Key.APPLICATION_LOG_SYSLOGAPPENDER_ENABLED).click();
        inputNamed(Key.APPLICATION_LOG_SYSLOGAPPENDER_HOST).sendKeys("host");
        inputNamed(Key.APPLICATION_LOG_SYSLOGAPPENDER_PORT).sendKeys("555");
        inputNamed(Key.APPLICATION_LOG_SYSLOGAPPENDER_FACILITY).
                sendKeys("cats");
        // Error log
        inputNamed(Key.ERROR_LOG_FILEAPPENDER_ENABLED).click();
        inputNamed(Key.ERROR_LOG_FILEAPPENDER_PATHNAME).sendKeys("/path50");
        inputNamed(Key.ERROR_LOG_ROLLINGFILEAPPENDER_ENABLED).click();
        inputNamed(Key.ERROR_LOG_ROLLINGFILEAPPENDER_PATHNAME).
                sendKeys("/path2");
        inputNamed(Key.ERROR_LOG_ROLLINGFILEAPPENDER_FILENAME_PATTERN).
                sendKeys("pattern2");
        inputNamed(Key.ERROR_LOG_ROLLINGFILEAPPENDER_MAX_HISTORY).
                sendKeys("20");
        // Access log
        inputNamed(Key.ACCESS_LOG_CONSOLEAPPENDER_ENABLED).click();
        inputNamed(Key.ACCESS_LOG_FILEAPPENDER_ENABLED).click();
        inputNamed(Key.ACCESS_LOG_FILEAPPENDER_PATHNAME).
                sendKeys("/path3");
        inputNamed(Key.ACCESS_LOG_ROLLINGFILEAPPENDER_ENABLED).click();
        inputNamed(Key.ACCESS_LOG_ROLLINGFILEAPPENDER_PATHNAME).
                sendKeys("/path4");
        inputNamed(Key.ACCESS_LOG_ROLLINGFILEAPPENDER_FILENAME_PATTERN).
                sendKeys("dogs");
        inputNamed(Key.ACCESS_LOG_ROLLINGFILEAPPENDER_MAX_HISTORY).
                sendKeys("531");
        inputNamed(Key.ACCESS_LOG_SYSLOGAPPENDER_ENABLED).click();
        inputNamed(Key.ACCESS_LOG_SYSLOGAPPENDER_HOST).sendKeys("host2");
        inputNamed(Key.ACCESS_LOG_SYSLOGAPPENDER_PORT).sendKeys("251");
        inputNamed(Key.ACCESS_LOG_SYSLOGAPPENDER_FACILITY).sendKeys("foxes");

        // Submit the form
        css("#cl-application input[type=\"submit\"]").click();

        Thread.sleep(WAIT_AFTER_SUBMIT);

        // Assert that the application configuration has been updated correctly
        final Configuration config = Configuration.getInstance();

        // Temporary Directory
        assertEquals("/bla/bla", config.getString(Key.TEMP_PATHNAME));

        // Delegate Script
        assertTrue(config.getBoolean(Key.DELEGATE_SCRIPT_ENABLED));
        assertEquals("file", config.getString(Key.DELEGATE_SCRIPT_PATHNAME));
        assertTrue(config.getBoolean(Key.DELEGATE_METHOD_INVOCATION_CACHE_ENABLED));

        // Application log
        assertEquals("warn", config.getString(Key.APPLICATION_LOG_LEVEL));
        assertTrue(config.getBoolean(Key.APPLICATION_LOG_CONSOLEAPPENDER_ENABLED));

        assertTrue(config.getBoolean(Key.APPLICATION_LOG_FILEAPPENDER_ENABLED));
        assertEquals("/path1",
                config.getString(Key.APPLICATION_LOG_FILEAPPENDER_PATHNAME));

        assertTrue(config.getBoolean(Key.APPLICATION_LOG_ROLLINGFILEAPPENDER_ENABLED));
        assertEquals("/path2",
                config.getString(Key.APPLICATION_LOG_ROLLINGFILEAPPENDER_PATHNAME));
        assertEquals("pattern",
                config.getString(Key.APPLICATION_LOG_ROLLINGFILEAPPENDER_FILENAME_PATTERN));
        assertEquals("15",
                config.getString(Key.APPLICATION_LOG_ROLLINGFILEAPPENDER_MAX_HISTORY));

        assertTrue(config.getBoolean(Key.APPLICATION_LOG_SYSLOGAPPENDER_ENABLED));
        assertEquals("host",
                config.getString(Key.APPLICATION_LOG_SYSLOGAPPENDER_HOST));
        assertEquals("555",
                config.getString(Key.APPLICATION_LOG_SYSLOGAPPENDER_PORT));
        assertEquals("cats",
                config.getString(Key.APPLICATION_LOG_SYSLOGAPPENDER_FACILITY));

        // Error log
        assertTrue(config.getBoolean(Key.ERROR_LOG_FILEAPPENDER_ENABLED));
        assertEquals("/path50",
                config.getString(Key.ERROR_LOG_FILEAPPENDER_PATHNAME));

        assertTrue(config.getBoolean(Key.ERROR_LOG_ROLLINGFILEAPPENDER_ENABLED));
        assertEquals("/path2",
                config.getString(Key.ERROR_LOG_ROLLINGFILEAPPENDER_PATHNAME));
        assertEquals("pattern2",
                config.getString(Key.ERROR_LOG_ROLLINGFILEAPPENDER_FILENAME_PATTERN));
        assertEquals("20",
                config.getString(Key.ERROR_LOG_ROLLINGFILEAPPENDER_MAX_HISTORY));

        // Access log
        assertTrue(config.getBoolean(Key.ACCESS_LOG_CONSOLEAPPENDER_ENABLED));

        assertTrue(config.getBoolean(Key.ACCESS_LOG_FILEAPPENDER_ENABLED));
        assertEquals("/path3",
                config.getString(Key.ACCESS_LOG_FILEAPPENDER_PATHNAME));

        assertTrue(config.getBoolean(Key.ACCESS_LOG_ROLLINGFILEAPPENDER_ENABLED));
        assertEquals("/path4",
                config.getString(Key.ACCESS_LOG_ROLLINGFILEAPPENDER_PATHNAME));
        assertEquals("dogs",
                config.getString(Key.ACCESS_LOG_ROLLINGFILEAPPENDER_FILENAME_PATTERN));
        assertEquals("531",
                config.getString(Key.ACCESS_LOG_ROLLINGFILEAPPENDER_MAX_HISTORY));

        assertTrue(config.getBoolean(Key.ACCESS_LOG_SYSLOGAPPENDER_ENABLED));
        assertEquals("host2",
                config.getString(Key.ACCESS_LOG_SYSLOGAPPENDER_HOST));
        assertEquals("251",
                config.getString(Key.ACCESS_LOG_SYSLOGAPPENDER_PORT));
        assertEquals("foxes",
                config.getString(Key.ACCESS_LOG_SYSLOGAPPENDER_FACILITY));
    }

    @Test
    public void testServerSection() throws Exception {
        css("#cl-http-button > a").click();

        // Fill in the form
        inputNamed(Key.HTTP_ENABLED).click();
        inputNamed(Key.HTTP_HOST).sendKeys("1.2.3.4");
        inputNamed(Key.HTTP_PORT).sendKeys("8989");
        inputNamed(Key.HTTP_HTTP2_ENABLED).click();
        inputNamed(Key.HTTPS_ENABLED).click();
        inputNamed(Key.HTTPS_HOST).sendKeys("2.3.4.5");
        inputNamed(Key.HTTPS_PORT).sendKeys("8990");
        selectNamed(Key.HTTPS_KEY_STORE_TYPE).selectByVisibleText("PKCS12");
        inputNamed(Key.HTTPS_KEY_STORE_PATH).sendKeys("/something");
        inputNamed(Key.HTTPS_KEY_STORE_PASSWORD).sendKeys("cats");
        inputNamed(Key.HTTPS_HTTP2_ENABLED).click();
        inputNamed(Key.HTTP_ACCEPT_QUEUE_LIMIT).sendKeys("50");
        inputNamed(Key.BASE_URI).sendKeys("http://bla/bla/");
        inputNamed(Key.SLASH_SUBSTITUTE).sendKeys("^");
        inputNamed(Key.PRINT_STACK_TRACE_ON_ERROR_PAGES).click();

        // Submit the form
        css("#cl-http input[type=\"submit\"]").click();

        Thread.sleep(WAIT_AFTER_SUBMIT);

        // Assert that the application configuration has been updated correctly
        final Configuration config = Configuration.getInstance();
        assertTrue(config.getBoolean(Key.HTTP_ENABLED));
        assertEquals("1.2.3.4", config.getString(Key.HTTP_HOST));
        assertEquals(8989, config.getInt(Key.HTTP_PORT));
        assertTrue(config.getBoolean(Key.HTTP_HTTP2_ENABLED));
        assertTrue(config.getBoolean(Key.HTTPS_ENABLED));
        assertEquals("2.3.4.5", config.getString(Key.HTTPS_HOST));
        assertEquals(8990, config.getInt(Key.HTTPS_PORT));
        assertEquals("PKCS12", config.getString(Key.HTTPS_KEY_STORE_TYPE));
        assertEquals("/something", config.getString(Key.HTTPS_KEY_STORE_PATH));
        assertEquals("cats", config.getString(Key.HTTPS_KEY_STORE_PASSWORD));
        assertTrue(config.getBoolean(Key.HTTPS_HTTP2_ENABLED));
        assertEquals("50", config.getString(Key.HTTP_ACCEPT_QUEUE_LIMIT));
        assertEquals("http://bla/bla/", config.getString(Key.BASE_URI));
        assertEquals("^", config.getString(Key.SLASH_SUBSTITUTE));
        assertTrue(config.getBoolean(Key.PRINT_STACK_TRACE_ON_ERROR_PAGES));
    }

    @Test
    public void testEndpointsSection() throws Exception {
        css("#cl-endpoints-button > a").click();

        // Fill in the form
        inputNamed(Key.MAX_PIXELS).sendKeys("5000");
        inputNamed(Key.BASIC_AUTH_ENABLED).click();
        inputNamed(Key.BASIC_AUTH_USERNAME).sendKeys("dogs");
        inputNamed(Key.BASIC_AUTH_SECRET).sendKeys("foxes");
        selectNamed(Key.IIIF_CONTENT_DISPOSITION).selectByValue("attachment");
        inputNamed(Key.IIIF_MIN_SIZE).sendKeys("75");
        inputNamed(Key.IIIF_MIN_TILE_SIZE).sendKeys("250");
        inputNamed(Key.IIIF_1_ENDPOINT_ENABLED).click();
        inputNamed(Key.IIIF_2_ENDPOINT_ENABLED).click();
        inputNamed(Key.IIIF_2_RESTRICT_TO_SIZES).click();
        inputNamed(Key.API_ENABLED).click();
        inputNamed(Key.API_USERNAME).sendKeys("cats");
        inputNamed(Key.API_SECRET).sendKeys("dogs");

        // Submit the form
        css("#cl-endpoints input[type=\"submit\"]").click();

        Thread.sleep(WAIT_AFTER_SUBMIT);

        // Assert that the application configuration has been updated correctly
        final Configuration config = Configuration.getInstance();
        assertEquals(5000, config.getInt(Key.MAX_PIXELS));
        assertTrue(config.getBoolean(Key.BASIC_AUTH_ENABLED));
        assertEquals("dogs", config.getString(Key.BASIC_AUTH_USERNAME));
        assertEquals("foxes", config.getString(Key.BASIC_AUTH_SECRET));
        assertEquals("attachment",
                config.getString(Key.IIIF_CONTENT_DISPOSITION));
        assertEquals(75, config.getInt(Key.IIIF_MIN_SIZE));
        assertEquals(250, config.getInt(Key.IIIF_MIN_TILE_SIZE));
        assertTrue(config.getBoolean(Key.IIIF_1_ENDPOINT_ENABLED));
        assertTrue(config.getBoolean(Key.IIIF_2_ENDPOINT_ENABLED));
        assertTrue(config.getBoolean(Key.IIIF_2_RESTRICT_TO_SIZES));
        assertTrue(config.getBoolean(Key.API_ENABLED));
        assertEquals("cats", config.getString(Key.API_USERNAME));
        assertEquals("dogs", config.getString(Key.API_SECRET));
    }

    @Test
    public void testResolverSection() throws Exception {
        css("#cl-resolver-button > a").click();

        // Fill in the form
        selectNamed(Key.RESOLVER_DELEGATE).selectByValue("false");
        selectNamed(Key.RESOLVER_STATIC).
                selectByVisibleText("FilesystemResolver");
        // AmazonS3Resolver section
        css("#cl-resolver li > a[href=\"#AmazonS3Resolver\"]").click();
        inputNamed(Key.AMAZONS3RESOLVER_ACCESS_KEY_ID).sendKeys("123");
        inputNamed(Key.AMAZONS3RESOLVER_SECRET_KEY).sendKeys("456");
        inputNamed(Key.AMAZONS3RESOLVER_BUCKET_NAME).sendKeys("cats");
        inputNamed(Key.AMAZONS3RESOLVER_BUCKET_REGION).sendKeys("antarctica");
        selectNamed(Key.AMAZONS3RESOLVER_LOOKUP_STRATEGY).
                selectByValue("BasicLookupStrategy");
        // AzureStorageResolver
        css("#cl-resolver li > a[href=\"#AzureStorageResolver\"]").click();
        inputNamed(Key.AZURESTORAGERESOLVER_ACCOUNT_NAME).sendKeys("bla");
        inputNamed(Key.AZURESTORAGERESOLVER_ACCOUNT_KEY).sendKeys("cats");
        inputNamed(Key.AZURESTORAGERESOLVER_CONTAINER_NAME).sendKeys("bucket");
        selectNamed(Key.AZURESTORAGERESOLVER_LOOKUP_STRATEGY).
                selectByValue("BasicLookupStrategy");
        // FilesystemResolver
        css("#cl-resolver li > a[href=\"#FilesystemResolver\"]").click();
        selectNamed(Key.FILESYSTEMRESOLVER_LOOKUP_STRATEGY).
                selectByValue("BasicLookupStrategy");
        inputNamed(Key.FILESYSTEMRESOLVER_PATH_PREFIX).sendKeys("/prefix");
        inputNamed(Key.FILESYSTEMRESOLVER_PATH_SUFFIX).sendKeys("/suffix");
        // HttpResolver
        css("#cl-resolver li > a[href=\"#HttpResolver\"]").click();
        selectNamed(Key.HTTPRESOLVER_LOOKUP_STRATEGY).
                selectByValue("BasicLookupStrategy");
        inputNamed(Key.HTTPRESOLVER_TRUST_INVALID_CERTS).click();
        inputNamed(Key.HTTPRESOLVER_URL_PREFIX).sendKeys("http://prefix/");
        inputNamed(Key.HTTPRESOLVER_URL_SUFFIX).sendKeys("/suffix");
        inputNamed(Key.HTTPRESOLVER_BASIC_AUTH_USERNAME).sendKeys("username");
        inputNamed(Key.HTTPRESOLVER_BASIC_AUTH_SECRET).sendKeys("password");
        // JdbcResolver
        css("#cl-resolver li > a[href=\"#JdbcResolver\"]").click();
        inputNamed(Key.JDBCRESOLVER_JDBC_URL).sendKeys("cats://dogs");
        inputNamed(Key.JDBCRESOLVER_USER).sendKeys("user");
        inputNamed(Key.JDBCRESOLVER_PASSWORD).sendKeys("password");
        inputNamed(Key.JDBCRESOLVER_CONNECTION_TIMEOUT).sendKeys("5");

        // Submit the form
        css("#cl-resolver input[type=\"submit\"]").click();

        Thread.sleep(WAIT_AFTER_SUBMIT);

        // Assert that the application configuration has been updated correctly
        final Configuration config = Configuration.getInstance();
        assertFalse(config.getBoolean(Key.RESOLVER_DELEGATE));
        assertEquals("FilesystemResolver",
                config.getString(Key.RESOLVER_STATIC));
        // AmazonS3Resolver
        assertEquals("123",
                config.getString(Key.AMAZONS3RESOLVER_ACCESS_KEY_ID));
        assertEquals("456",
                config.getString(Key.AMAZONS3RESOLVER_SECRET_KEY));
        assertEquals("cats",
                config.getString(Key.AMAZONS3RESOLVER_BUCKET_NAME));
        assertEquals("antarctica",
                config.getString(Key.AMAZONS3RESOLVER_BUCKET_REGION));
        assertEquals("BasicLookupStrategy",
                config.getString(Key.AMAZONS3RESOLVER_LOOKUP_STRATEGY));
        // AzureStorageResolver
        assertEquals("bla",
                config.getString(Key.AZURESTORAGERESOLVER_ACCOUNT_NAME));
        assertEquals("cats",
                config.getString(Key.AZURESTORAGERESOLVER_ACCOUNT_KEY));
        assertEquals("bucket",
                config.getString(Key.AZURESTORAGERESOLVER_CONTAINER_NAME));
        assertEquals("BasicLookupStrategy",
                config.getString(Key.AZURESTORAGERESOLVER_LOOKUP_STRATEGY));
        // FilesystemResolver
        assertEquals("BasicLookupStrategy",
                config.getString(Key.FILESYSTEMRESOLVER_LOOKUP_STRATEGY));
        assertEquals("/prefix",
                config.getString(Key.FILESYSTEMRESOLVER_PATH_PREFIX));
        assertEquals("/suffix",
                config.getString(Key.FILESYSTEMRESOLVER_PATH_SUFFIX));
        // HttpResolver
        assertTrue(
                config.getBoolean(Key.HTTPRESOLVER_TRUST_INVALID_CERTS));
        assertEquals("BasicLookupStrategy",
                config.getString(Key.HTTPRESOLVER_LOOKUP_STRATEGY));
        assertEquals("http://prefix/",
                config.getString(Key.HTTPRESOLVER_URL_PREFIX));
        assertEquals("/suffix",
                config.getString(Key.HTTPRESOLVER_URL_SUFFIX));
        assertEquals("username",
                config.getString(Key.HTTPRESOLVER_BASIC_AUTH_USERNAME));
        assertEquals("password",
                config.getString(Key.HTTPRESOLVER_BASIC_AUTH_SECRET));
        // JdbcResolver
        assertEquals("cats://dogs",
                config.getString(Key.JDBCRESOLVER_JDBC_URL));
        assertEquals("user",
                config.getString(Key.JDBCRESOLVER_USER));
        assertEquals("password",
                config.getString(Key.JDBCRESOLVER_PASSWORD));
        assertEquals("5",
                config.getString(Key.JDBCRESOLVER_CONNECTION_TIMEOUT));
    }

    @Test
    public void testProcessorsSection() throws Exception {
        css("#cl-processors-button > a").click();

        // Fill in the form
        css("#cl-processors li > a[href=\"#cl-image-assignments\"]").click();
        selectNamed("processor.gif").selectByVisibleText("Java2dProcessor");
        selectNamed(Key.PROCESSOR_FALLBACK).selectByVisibleText("JaiProcessor");
        inputNamed(Key.PROCESSOR_DPI).sendKeys("300");
        inputNamed(Key.PROCESSOR_NORMALIZE).click();
        selectNamed(Key.PROCESSOR_BACKGROUND_COLOR).selectByValue("white");
        selectNamed(Key.PROCESSOR_UPSCALE_FILTER).
                selectByVisibleText("Triangle");
        selectNamed(Key.PROCESSOR_DOWNSCALE_FILTER).
                selectByVisibleText("Mitchell");
        inputNamed(Key.PROCESSOR_SHARPEN).sendKeys("0.2");
        inputNamed(Key.PROCESSOR_PRESERVE_METADATA).click();
        inputNamed(Key.PROCESSOR_LIMIT_TO_8_BITS).click();
        inputNamed(Key.PROCESSOR_JPG_PROGRESSIVE).click();
        inputNamed(Key.PROCESSOR_JPG_QUALITY).sendKeys("55");
        selectNamed(Key.PROCESSOR_TIF_COMPRESSION).selectByVisibleText("LZW");
        selectNamed(Key.STREAMPROCESSOR_RETRIEVAL_STRATEGY).
                selectByValue("StreamStrategy");
        // FfmpegProcessor
        css("#cl-processors li > a[href=\"#FfmpegProcessor\"]").click();
        inputNamed(Key.FFMPEGPROCESSOR_PATH_TO_BINARIES).sendKeys("/ffpath");
        // GraphicsMagickProcessor
        css("#cl-processors li > a[href=\"#GraphicsMagickProcessor\"]").click();
        inputNamed(Key.GRAPHICSMAGICKPROCESSOR_PATH_TO_BINARIES).sendKeys("/gmpath");
        // ImageMagickProcessor
        css("#cl-processors li > a[href=\"#ImageMagickProcessor\"]").click();
        inputNamed(Key.IMAGEMAGICKPROCESSOR_PATH_TO_BINARIES).sendKeys("/impath");
        // JaiProcessor
        css("#cl-processors li > a[href=\"#JaiProcessor\"]").click();
        // Java2dProcessor
        css("#cl-processors li > a[href=\"#Java2dProcessor\"]").click();
        // KakaduProcessor
        css("#cl-processors li > a[href=\"#KakaduProcessor\"]").click();
        inputNamed(Key.KAKADUPROCESSOR_PATH_TO_BINARIES).sendKeys("/kpath");
        // OpenJpegProcessor
        css("#cl-processors li > a[href=\"#OpenJpegProcessor\"]").click();
        inputNamed(Key.OPENJPEGPROCESSOR_PATH_TO_BINARIES).sendKeys("/ojpath");

        // Submit the form
        css("#cl-processors input[type=\"submit\"]").click();

        Thread.sleep(WAIT_AFTER_SUBMIT);

        // Assert that the application configuration has been updated correctly
        final Configuration config = Configuration.getInstance();
        assertEquals("Java2dProcessor", config.getString("processor.gif"));
        assertEquals("JaiProcessor", config.getString(Key.PROCESSOR_FALLBACK));
        assertEquals(300, config.getInt(Key.PROCESSOR_DPI));
        assertTrue(config.getBoolean(Key.PROCESSOR_NORMALIZE));
        assertEquals("white", config.getString(Key.PROCESSOR_BACKGROUND_COLOR));
        assertEquals("triangle",
                config.getString(Key.PROCESSOR_UPSCALE_FILTER));
        assertEquals("mitchell",
                config.getString(Key.PROCESSOR_DOWNSCALE_FILTER));
        assertEquals("0.2", config.getString(Key.PROCESSOR_SHARPEN));
        assertTrue(config.getBoolean(Key.PROCESSOR_PRESERVE_METADATA));
        assertTrue(config.getBoolean(Key.PROCESSOR_LIMIT_TO_8_BITS));
        assertEquals("true", config.getString(Key.PROCESSOR_JPG_PROGRESSIVE));
        assertEquals("55", config.getString(Key.PROCESSOR_JPG_QUALITY));
        assertEquals("LZW", config.getString(Key.PROCESSOR_TIF_COMPRESSION));
        assertEquals("StreamStrategy",
                config.getString(Key.STREAMPROCESSOR_RETRIEVAL_STRATEGY));
        // FfmpegProcessor
        assertEquals("/ffpath",
                config.getString(Key.FFMPEGPROCESSOR_PATH_TO_BINARIES));
        // GraphicsMagickProcessor
        assertEquals("/gmpath",
                config.getString(Key.GRAPHICSMAGICKPROCESSOR_PATH_TO_BINARIES));
        // ImageMagickProcessor
        assertEquals("/impath",
                config.getString(Key.IMAGEMAGICKPROCESSOR_PATH_TO_BINARIES));
        // JaiProcessor
        // Java2dProcessor
        // KakaduProcessor
        assertEquals("/kpath",
                config.getString(Key.KAKADUPROCESSOR_PATH_TO_BINARIES));
        // OpenJpegProcessor
        assertEquals("/ojpath",
                config.getString(Key.OPENJPEGPROCESSOR_PATH_TO_BINARIES));
    }

    @Test
    public void testCachesSection() throws Exception {
        css("#cl-caches-button > a").click();

        // Fill in the form
        inputNamed(Key.CLIENT_CACHE_ENABLED).click();
        inputNamed(Key.CLIENT_CACHE_MAX_AGE).sendKeys("250");
        inputNamed(Key.CLIENT_CACHE_SHARED_MAX_AGE).sendKeys("220");
        inputNamed(Key.CLIENT_CACHE_PUBLIC).click();
        inputNamed(Key.CLIENT_CACHE_PRIVATE).click();
        inputNamed(Key.CLIENT_CACHE_NO_CACHE).click();
        inputNamed(Key.CLIENT_CACHE_NO_STORE).click();
        inputNamed(Key.CLIENT_CACHE_MUST_REVALIDATE).click();
        inputNamed(Key.CLIENT_CACHE_PROXY_REVALIDATE).click();
        inputNamed(Key.CLIENT_CACHE_NO_TRANSFORM).click();
        selectNamed(Key.SOURCE_CACHE).selectByVisibleText("FilesystemCache");
        inputNamed(Key.SOURCE_CACHE_ENABLED).click();
        selectNamed(Key.DERIVATIVE_CACHE).selectByVisibleText("FilesystemCache");
        inputNamed(Key.DERIVATIVE_CACHE_ENABLED).click();
        inputNamed(Key.INFO_CACHE_ENABLED).click();
        inputNamed(Key.CACHE_SERVER_PURGE_MISSING).click();
        inputNamed(Key.CACHE_SERVER_RESOLVE_FIRST).click();
        inputNamed(Key.CACHE_SERVER_TTL).sendKeys("10");
        inputNamed(Key.CACHE_WORKER_ENABLED).click();
        inputNamed(Key.CACHE_WORKER_INTERVAL).sendKeys("25");
        // AmazonS3Cache
        css("#cl-caches li > a[href=\"#AmazonS3Cache\"]").click();
        inputNamed(Key.AMAZONS3CACHE_ACCESS_KEY_ID).sendKeys("cats");
        inputNamed(Key.AMAZONS3CACHE_SECRET_KEY).sendKeys("dogs");
        inputNamed(Key.AMAZONS3CACHE_BUCKET_NAME).sendKeys("bucket");
        inputNamed(Key.AMAZONS3CACHE_BUCKET_REGION).sendKeys("greenland");
        inputNamed(Key.AMAZONS3CACHE_OBJECT_KEY_PREFIX).sendKeys("obj");
        // AzureStorageCache
        css("#cl-caches li > a[href=\"#AzureStorageCache\"]").click();
        inputNamed(Key.AZURESTORAGECACHE_ACCOUNT_NAME).sendKeys("bees");
        inputNamed(Key.AZURESTORAGECACHE_ACCOUNT_KEY).sendKeys("birds");
        inputNamed(Key.AZURESTORAGECACHE_CONTAINER_NAME).sendKeys("badger");
        inputNamed(Key.AZURESTORAGECACHE_OBJECT_KEY_PREFIX).sendKeys("obj");
        // FilesystemCache
        css("#cl-caches li > a[href=\"#FilesystemCache\"]").click();
        inputNamed(Key.FILESYSTEMCACHE_PATHNAME).sendKeys("/path");
        inputNamed(Key.FILESYSTEMCACHE_DIRECTORY_DEPTH).sendKeys("8");
        inputNamed(Key.FILESYSTEMCACHE_DIRECTORY_NAME_LENGTH).sendKeys("4");
        // JdbcCache
        css("#cl-caches li > a[href=\"#JdbcCache\"]").click();
        inputNamed(Key.JDBCCACHE_JDBC_URL).sendKeys("jdbc://dogs");
        inputNamed(Key.JDBCCACHE_USER).sendKeys("person");
        inputNamed(Key.JDBCCACHE_PASSWORD).sendKeys("cats");
        inputNamed(Key.JDBCCACHE_CONNECTION_TIMEOUT).sendKeys("9");
        inputNamed(Key.JDBCCACHE_DERIVATIVE_IMAGE_TABLE).sendKeys("hula");
        inputNamed(Key.JDBCCACHE_INFO_TABLE).sendKeys("box");
        // HeapCache
        css("#cl-caches li > a[href=\"#HeapCache\"]").click();
        inputNamed(Key.HEAPCACHE_TARGET_SIZE).sendKeys("1234");
        inputNamed(Key.HEAPCACHE_PERSIST).click();
        inputNamed(Key.HEAPCACHE_PATHNAME).sendKeys("/tmp/cats");
        // RedisCache
        css("#cl-caches li > a[href=\"#RedisCache\"]").click();
        inputNamed(Key.REDISCACHE_HOST).sendKeys("localhost");
        inputNamed(Key.REDISCACHE_PORT).sendKeys("12398");
        inputNamed(Key.REDISCACHE_PASSWORD).sendKeys("redispass");
        inputNamed(Key.REDISCACHE_SSL).click();
        inputNamed(Key.REDISCACHE_DATABASE).sendKeys("5");

        // Submit the form
        css("#cl-caches input[type=\"submit\"]").click();

        Thread.sleep(WAIT_AFTER_SUBMIT);

        // Assert that the application configuration has been updated correctly
        final Configuration config = Configuration.getInstance();
        assertTrue(config.getBoolean(Key.CLIENT_CACHE_ENABLED));
        assertEquals("250", config.getString(Key.CLIENT_CACHE_MAX_AGE));
        assertEquals("220", config.getString(Key.CLIENT_CACHE_SHARED_MAX_AGE));
        assertTrue(config.getBoolean(Key.CLIENT_CACHE_PUBLIC));
        assertTrue(config.getBoolean(Key.CLIENT_CACHE_PRIVATE));
        assertTrue(config.getBoolean(Key.CLIENT_CACHE_NO_CACHE));
        assertTrue(config.getBoolean(Key.CLIENT_CACHE_NO_STORE));
        assertTrue(config.getBoolean(Key.CLIENT_CACHE_MUST_REVALIDATE));
        assertTrue(config.getBoolean(Key.CLIENT_CACHE_PROXY_REVALIDATE));
        assertTrue(config.getBoolean(Key.CLIENT_CACHE_NO_TRANSFORM));
        assertEquals("FilesystemCache", config.getString(Key.SOURCE_CACHE));
        assertTrue(config.getBoolean(Key.SOURCE_CACHE_ENABLED));
        assertEquals("FilesystemCache", config.getString(Key.DERIVATIVE_CACHE));
        assertTrue(config.getBoolean(Key.DERIVATIVE_CACHE_ENABLED));
        assertTrue(config.getBoolean(Key.INFO_CACHE_ENABLED));
        assertTrue(config.getBoolean(Key.CACHE_SERVER_PURGE_MISSING));
        assertTrue(config.getBoolean(Key.CACHE_SERVER_RESOLVE_FIRST));
        assertEquals(10, config.getInt(Key.CACHE_SERVER_TTL));
        assertTrue(config.getBoolean(Key.CACHE_WORKER_ENABLED));
        assertEquals(25, config.getInt(Key.CACHE_WORKER_INTERVAL));
        // AmazonS3Cache
        assertEquals("cats", config.getString(Key.AMAZONS3CACHE_ACCESS_KEY_ID));
        assertEquals("dogs", config.getString(Key.AMAZONS3CACHE_SECRET_KEY));
        assertEquals("bucket", config.getString(Key.AMAZONS3CACHE_BUCKET_NAME));
        assertEquals("greenland", config.getString(Key.AMAZONS3CACHE_BUCKET_REGION));
        assertEquals("obj", config.getString(Key.AMAZONS3CACHE_OBJECT_KEY_PREFIX));
        // AzureStorageCache
        assertEquals("bees", config.getString(Key.AZURESTORAGECACHE_ACCOUNT_NAME));
        assertEquals("birds", config.getString(Key.AZURESTORAGECACHE_ACCOUNT_KEY));
        assertEquals("badger", config.getString(Key.AZURESTORAGECACHE_CONTAINER_NAME));
        assertEquals("obj", config.getString(Key.AZURESTORAGECACHE_OBJECT_KEY_PREFIX));
        // FilesystemCache
        assertEquals("/path", config.getString(Key.FILESYSTEMCACHE_PATHNAME));
        assertEquals("8", config.getString(Key.FILESYSTEMCACHE_DIRECTORY_DEPTH));
        assertEquals("4", config.getString(Key.FILESYSTEMCACHE_DIRECTORY_NAME_LENGTH));
        // JdbcCache
        assertEquals("jdbc://dogs", config.getString(Key.JDBCCACHE_JDBC_URL));
        assertEquals("person", config.getString(Key.JDBCCACHE_USER));
        assertEquals("cats", config.getString(Key.JDBCCACHE_PASSWORD));
        assertEquals("9", config.getString(Key.JDBCCACHE_CONNECTION_TIMEOUT));
        assertEquals("hula", config.getString(Key.JDBCCACHE_DERIVATIVE_IMAGE_TABLE));
        assertEquals("box", config.getString(Key.JDBCCACHE_INFO_TABLE));
        // HeapCache
        assertEquals("1234", config.getString(Key.HEAPCACHE_TARGET_SIZE));
        assertTrue(config.getBoolean(Key.HEAPCACHE_PERSIST));
        assertEquals("/tmp/cats", config.getString(Key.HEAPCACHE_PATHNAME));
        // RedisCache
        assertEquals("localhost", config.getString(Key.REDISCACHE_HOST));
        assertEquals("12398", config.getString(Key.REDISCACHE_PORT));
        assertEquals("redispass", config.getString(Key.REDISCACHE_PASSWORD));
        assertTrue(config.getBoolean(Key.REDISCACHE_SSL));
        assertEquals("5", config.getString(Key.REDISCACHE_DATABASE));
    }

    @Test
    public void testOverlaysSection() throws Exception {
        css("#cl-overlays-button > a").click();

        // Fill in the form
        inputNamed(Key.OVERLAY_ENABLED).click();
        selectNamed(Key.OVERLAY_STRATEGY).selectByValue("BasicStrategy");
        selectNamed(Key.OVERLAY_POSITION).selectByValue("bottom left");
        inputNamed(Key.OVERLAY_INSET).sendKeys("35");
        inputNamed(Key.OVERLAY_OUTPUT_WIDTH_THRESHOLD).sendKeys("5");
        inputNamed(Key.OVERLAY_OUTPUT_HEIGHT_THRESHOLD).sendKeys("6");
        selectNamed(Key.OVERLAY_TYPE).selectByValue("string");
        inputNamed(Key.OVERLAY_IMAGE).sendKeys("/image.png");
        inputNamed(Key.OVERLAY_STRING_STRING).sendKeys("cats");
        selectNamed(Key.OVERLAY_STRING_FONT).selectByVisibleText("SansSerif");
        inputNamed(Key.OVERLAY_STRING_FONT_SIZE).sendKeys("13");
        inputNamed(Key.OVERLAY_STRING_FONT_MIN_SIZE).sendKeys("11");
        inputNamed(Key.OVERLAY_STRING_FONT_WEIGHT).sendKeys("1.2");
        inputNamed(Key.OVERLAY_STRING_GLYPH_SPACING).sendKeys("-0.1");
        inputNamed(Key.OVERLAY_STRING_COLOR).sendKeys("#d0d0d0");
        inputNamed(Key.OVERLAY_STRING_STROKE_COLOR).sendKeys("#e0e0e0");
        inputNamed(Key.OVERLAY_STRING_STROKE_WIDTH).sendKeys("5");
        inputNamed(Key.OVERLAY_STRING_BACKGROUND_COLOR).sendKeys("#ffd000");
        inputNamed(Key.REDACTION_ENABLED).click();

        // Submit the form
        css("#cl-overlays input[type=\"submit\"]").click();

        Thread.sleep(WAIT_AFTER_SUBMIT);

        // Assert that the application configuration has been updated correctly
        final Configuration config = Configuration.getInstance();
        assertTrue(config.getBoolean(Key.OVERLAY_ENABLED));
        assertEquals("BasicStrategy",
                config.getString(Key.OVERLAY_STRATEGY));
        assertEquals("bottom left",
                config.getString(Key.OVERLAY_POSITION));
        assertEquals("35",
                config.getString(Key.OVERLAY_INSET));
        assertEquals("5",
                config.getString(Key.OVERLAY_OUTPUT_WIDTH_THRESHOLD));
        assertEquals("6",
                config.getString(Key.OVERLAY_OUTPUT_HEIGHT_THRESHOLD));
        assertEquals("string",
                config.getString(Key.OVERLAY_TYPE));
        assertEquals("/image.png",
                config.getString(Key.OVERLAY_IMAGE));
        assertEquals("cats",
                config.getString(Key.OVERLAY_STRING_STRING));
        assertEquals("SansSerif",
                config.getString(Key.OVERLAY_STRING_FONT));
        assertEquals("13",
                config.getString(Key.OVERLAY_STRING_FONT_SIZE));
        assertEquals("11",
                config.getString(Key.OVERLAY_STRING_FONT_MIN_SIZE));
        assertEquals("1.2",
                config.getString(Key.OVERLAY_STRING_FONT_WEIGHT));
        assertEquals("-0.1",
                config.getString(Key.OVERLAY_STRING_GLYPH_SPACING));
        assertEquals("#d0d0d0",
                config.getString(Key.OVERLAY_STRING_COLOR));
        assertEquals("#e0e0e0",
                config.getString(Key.OVERLAY_STRING_STROKE_COLOR));
        assertEquals("5",
                config.getString(Key.OVERLAY_STRING_STROKE_WIDTH));
        assertEquals("#ffd000",
                config.getString(Key.OVERLAY_STRING_BACKGROUND_COLOR));
        assertTrue(config.getBoolean(Key.REDACTION_ENABLED));
    }

}
