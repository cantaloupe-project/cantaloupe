package edu.illinois.library.cantaloupe.resource.admin;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.WebApplication;
import edu.illinois.library.cantaloupe.WebServer;
import edu.illinois.library.cantaloupe.cache.Cache;
import edu.illinois.library.cantaloupe.cache.CacheFactory;
import edu.illinois.library.cantaloupe.cache.CacheWorker;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.redaction.RedactionService;
import edu.illinois.library.cantaloupe.image.watermark.WatermarkService;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.processor.ProcessorFactory;
import edu.illinois.library.cantaloupe.processor.UnsupportedSourceFormatException;
import edu.illinois.library.cantaloupe.resolver.ResolverFactory;
import edu.illinois.library.cantaloupe.resource.AbstractResource;
import edu.illinois.library.cantaloupe.resource.iiif.IiifResource;
import edu.illinois.library.cantaloupe.script.ScriptEngineFactory;
import org.apache.commons.configuration.Configuration;
import org.apache.velocity.Template;
import org.apache.velocity.app.Velocity;
import org.restlet.data.CacheDirective;
import org.restlet.data.Header;
import org.restlet.data.MediaType;
import org.restlet.ext.velocity.TemplateRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;
import org.restlet.util.Series;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class AdminResource extends AbstractResource {

    /**
     * <p>Processors can't be used in the templates directly, so instances of
     * this class will proxy for them.</p>
     *
     * <p>Velocity requires this class to be public.</p>
     */
    public static class ProcessorProxy {
        private Processor processor;

        public ProcessorProxy(Processor proc) {
            processor = proc;
        }

        public String getName() {
            return processor.getClass().getSimpleName();
        }

        public boolean supports(Format format) {
            try {
                processor.setSourceFormat(format);
                return true;
            } catch (UnsupportedSourceFormatException e) {
                return false;
            }
        }
    }

    @Override
    protected void doInit() throws ResourceException {
        super.doInit();
        // Add a "Cache-Control: no-cache" header because this page contains
        // live information pertaining to the function of the application.
        getResponseCacheDirectives().add(CacheDirective.noCache());
    }

    @Get
    public Representation doGet() throws Exception {
        Template template = Velocity.getTemplate("admin.vm");
        return new TemplateRepresentation(template, getTemplateVars(),
                MediaType.TEXT_HTML);
    }

    private Map<String,Object> getTemplateVars() throws Exception {
        final Configuration config = Application.getConfiguration();
        final Map<String, Object> vars = getCommonTemplateVars(getRequest());

        vars.put("configFilePath",
                Application.getConfigurationFile().getAbsolutePath());

        ////////////////////////////////////////////////////////////////////
        //////////////////////// status section ////////////////////////////
        ////////////////////////////////////////////////////////////////////

        // VM arguments
        RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
        vars.put("vmArguments", runtimeMxBean.getInputArguments());

        // memory
        final int mb = 1024 * 1024;
        Runtime runtime = Runtime.getRuntime();
        vars.put("usedHeap", (runtime.totalMemory() - runtime.freeMemory()) / mb);
        vars.put("freeHeap", runtime.freeMemory() / mb);
        vars.put("totalHeap", runtime.totalMemory() / mb);
        vars.put("maxHeap", runtime.maxMemory() / mb);
        final double usedPercent = (runtime.totalMemory() - runtime.freeMemory()) /
                (double) runtime.maxMemory();
        vars.put("memoryBarValue", usedPercent * 100);
        String memoryBarClass = "progress-bar-success";
        if (usedPercent > 0.8) {
            memoryBarClass = "progress-bar-danger";
        } else if (usedPercent > 0.7) {
            memoryBarClass = "progress-bar-warning";
        }
        vars.put("memoryBarClass", memoryBarClass);

        ////////////////////////////////////////////////////////////////////
        //////////////////////// server section ////////////////////////////
        ////////////////////////////////////////////////////////////////////

        // HTTP/HTTPS
        vars.put("httpEnabled",
                config.getBoolean(WebServer.HTTP_ENABLED_CONFIG_KEY, false));
        vars.put("httpPort",
                config.getInt(WebServer.HTTP_PORT_CONFIG_KEY, -1));
        vars.put("httpsEnabled",
                config.getBoolean(WebServer.HTTPS_ENABLED_CONFIG_KEY, false));
        vars.put("httpsKeyStorePassword",
                config.getString(WebServer.HTTPS_KEY_STORE_PASSWORD_CONFIG_KEY, ""));
        vars.put("httpsKeyStorePath",
                config.getString(WebServer.HTTPS_KEY_STORE_PATH_CONFIG_KEY, ""));
        vars.put("httpsKeyStoreType",
                config.getString(WebServer.HTTPS_KEY_STORE_TYPE_CONFIG_KEY, ""));
        vars.put("httpsPort",
                config.getInt(WebServer.HTTPS_PORT_CONFIG_KEY, -1));

        // Basic auth
        vars.put("basicAuthEnabled",
                config.getBoolean(WebApplication.BASIC_AUTH_ENABLED_CONFIG_KEY, false));
        vars.put("basicAuthUsername",
                config.getString(WebApplication.BASIC_AUTH_USERNAME_CONFIG_KEY, ""));
        vars.put("basicAuthSecret",
                config.getString(WebApplication.BASIC_AUTH_SECRET_CONFIG_KEY, ""));

        // Reverse-Proxy headers
        final Series<Header> headers = getRequest().getHeaders();
        vars.put("xForwardedProtoHeader",
                headers.getFirstValue("X-Forwarded-Proto", true, ""));
        vars.put("xForwardedHostHeader",
                headers.getFirstValue("X-Forwarded-Host", true, ""));
        vars.put("xForwardedPortHeader",
                headers.getFirstValue("X-Forwarded-Port", true, ""));
        vars.put("xForwardedPathHeader",
                headers.getFirstValue("X-Forwarded-Path", true, ""));
        vars.put("xForwardedForHeader",
                headers.getFirstValue("X-Forwarded-For", true, ""));

        vars.put("baseUriConfig",
                config.getString(AbstractResource.BASE_URI_CONFIG_KEY, ""));
        vars.put("uriSlashSubstitute",
                config.getString(AbstractResource.SLASH_SUBSTITUTE_CONFIG_KEY, ""));
        vars.put("printStackTraceOnErrorPages",
                config.getBoolean("print_stack_trace_on_error_pages", false));
        final File delegateScript = ScriptEngineFactory.getScript();
        vars.put("delegateScript",
                delegateScript.exists() ? delegateScript.getAbsolutePath() : "");

        ////////////////////////////////////////////////////////////////////
        ////////////////////// endpoints section ///////////////////////////
        ////////////////////////////////////////////////////////////////////

        vars.put("maxPixels",
                config.getString(AbstractResource.MAX_PIXELS_CONFIG_KEY, "0"));
        vars.put("iiifContentDisposition",
                config.getString(IiifResource.CONTENT_DISPOSITION_CONFIG_KEY, "inline"));
        vars.put("iiifMinTileSize",
                config.getString("endpoint.iiif.min_tile_size", "0"));
        vars.put("iiif1EndpointEnabled",
                config.getBoolean("endpoint.iiif.1.enabled", false));
        vars.put("iiif2EndpointEnabled",
                config.getBoolean("endpoint.iiif.2.enabled", false));
        vars.put("iiif2RestrictToSizes",
                config.getBoolean("endpoint.iiif.2.restrict_to_sizes", false));

        ////////////////////////////////////////////////////////////////////
        /////////////////////// resolver section ///////////////////////////
        ////////////////////////////////////////////////////////////////////

        // resolver name
        String resolverStr = config.getString(
                ResolverFactory.STATIC_RESOLVER_CONFIG_KEY);
        if (config.getBoolean(ResolverFactory.DELEGATE_RESOLVER_CONFIG_KEY, false)) {
            resolverStr = "Delegate Script";
        }
        vars.put("resolverName", resolverStr);

        vars.put("amazonS3ResolverAccessKeyId",
                config.getString("AmazonS3Resolver.access_key_id", ""));
        vars.put("amazonS3ResolverSecretKey",
                config.getString("AmazonS3Resolver.secret_key", ""));
        vars.put("amazonS3ResolverBucketName",
                config.getString("AmazonS3Resolver.bucket.name", ""));
        vars.put("amazonS3ResolverBucketRegion",
                config.getString("AmazonS3Resolver.bucket.region", ""));
        vars.put("amazonS3ResolverLookupStrategy",
                config.getString("AmazonS3Resolver.lookup_strategy", ""));

        vars.put("azureStorageResolverAccountName",
                config.getString("AzureStorageResolver.account_name", ""));
        vars.put("azureStorageResolverAccountKey",
                config.getString("AzureStorageResolver.account_key", ""));
        vars.put("azureStorageResolverContainerName",
                config.getString("AzureStorageResolver.container_name", ""));
        vars.put("azureStorageResolverLookupStrategy",
                config.getString("AzureStorageResolver.lookup_strategy", ""));

        vars.put("filesystemResolverLookupStrategy",
                config.getString("FilesystemResolver.lookup_strategy", ""));
        vars.put("filesystemResolverPathPrefix",
                config.getString("FilesystemResolver.BasicLookupStrategy.path_prefix", ""));
        vars.put("filesystemResolverPathSuffix",
                config.getString("FilesystemResolver.BasicLookupStrategy.path_suffix", ""));

        vars.put("httpResolverLookupStrategy",
                config.getString("HttpResolver.lookup_strategy", ""));
        vars.put("httpResolverUrlPrefix",
                config.getString("HttpResolver.BasicLookupStrategy.url_prefix", ""));
        vars.put("httpResolverUrlSuffix",
                config.getString("HttpResolver.BasicLookupStrategy.url_suffix", ""));
        vars.put("httpResolverBasicUsername",
                config.getString("HttpResolver.auth.basic.username", ""));
        vars.put("httpResolverBasicSecret",
                config.getString("HttpResolver.auth.basic.secret", ""));

        vars.put("jdbcResolverUrl", config.getString("JdbcResolver.url", ""));
        vars.put("jdbcResolverUser", config.getString("JdbcResolver.user", ""));
        vars.put("jdbcResolverPassword",
                config.getString("JdbcResolver.password", ""));
        vars.put("jdbcResolverMaxPoolSize",
                config.getString("JdbcResolver.max_pool_size", ""));
        vars.put("jdbcResolverConnectionTimeout",
                config.getString("JdbcResolver.connection_timeout", ""));

        ////////////////////////////////////////////////////////////////////
        ////////////////////// processors section //////////////////////////
        ////////////////////////////////////////////////////////////////////

        // source format assignments
        Map<Format,String> assignments = new TreeMap<>();
        for (Format format : Format.values()) {
            try {
                assignments.put(format,
                        ProcessorFactory.getProcessor(format).getClass().getSimpleName());
            } catch (UnsupportedSourceFormatException e) {
                // noop
            }
        }
        vars.put("processorAssignments", assignments);

        class SourceFormatComparator implements Comparator<Format> {
            public int compare(Format o1, Format o2) {
                return o1.getName().compareTo(o2.getName());
            }
        }

        // image source formats
        List<Format> imageFormats = new ArrayList<>();
        for (Format format : Format.values()) {
            if (format.getType() != null &&
                    format.getType().equals(Format.Type.IMAGE)) {
                imageFormats.add(format);
            }
        }
        Collections.sort(imageFormats, new SourceFormatComparator());
        vars.put("imageSourceFormats", imageFormats);

        // video source formats
        List<Format> videoFormats = new ArrayList<>();
        for (Format format : Format.values()) {
            if (format.getType() != null &&
                    format.getType().equals(Format.Type.VIDEO)) {
                videoFormats.add(format);
            }
        }
        Collections.sort(videoFormats, new SourceFormatComparator());
        vars.put("videoSourceFormats", videoFormats);

        // processors
        class ProcessorProxyComparator implements Comparator<ProcessorProxy> {
            public int compare(ProcessorProxy o1, ProcessorProxy o2) {
                return o1.getName().compareTo(o2.getName());
            }
        }

        List<ProcessorProxy> sortedProxies = new ArrayList<>();
        for (Processor proc : ProcessorFactory.getAllProcessors()) {
            sortedProxies.add(new ProcessorProxy(proc));
        }

        Collections.sort(sortedProxies, new ProcessorProxyComparator());
        vars.put("processors", sortedProxies);

        vars.put("ffmpegProcessorPathToBinaries",
                config.getString("FfmpegProcessor.path_to_binaries", ""));

        vars.put("graphicsMagickProcessorPathToBinaries",
                config.getString("GraphicsMagickProcessor.path_to_binaries", ""));
        vars.put("graphicsMagickProcessorBackgroundColor",
                config.getString("GraphicsMagickProcessor.background_color", ""));

        vars.put("imageMagickProcessorPathToBinaries",
                config.getString("ImageMagickProcessor.path_to_binaries", ""));
        vars.put("imageMagickProcessorBackgroundColor",
                config.getString("ImageMagickProcessor.background_color", ""));

        vars.put("jaiProcessorJpegQuality",
                config.getString("JaiProcessor.jpg.quality", ""));
        vars.put("jaiProcessorTiffCompression",
                config.getString("JaiProcessor.tif.compression", ""));

        vars.put("java2dProcessorScaleMode",
                config.getString("Java2dProcessor.scale_mode", ""));
        vars.put("java2dProcessorJpegQuality",
                config.getString("Java2dProcessor.jpg.quality", ""));
        vars.put("java2dProcessorTiffCompression",
                config.getString("Java2dProcessor.tif.compression", ""));

        vars.put("kakaduProcessorPathToBinaries",
                config.getString("KakaduProcessor.path_to_binaries", ""));
        vars.put("kakaduProcessorPostProcessor",
                config.getString("KakaduProcessor.post_processor", ""));
        vars.put("kakaduProcessorPostProcessorJava2dScaleMode",
                config.getString("KakaduProcessor.post_processor.java2d.scale_mode", ""));

        vars.put("openJpegProcessorPathToBinaries",
                config.getString("OpenJpegProcessor.path_to_binaries", ""));
        vars.put("openJpegProcessorPostProcessor",
                config.getString("OpenJpegProcessor.post_processor", ""));
        vars.put("openJpegProcessorPostProcessorJava2dScaleMode",
                config.getString("OpenJpegProcessor.post_processor.java2d.scale_mode", ""));

        vars.put("pdfBoxProcessorDpi",
                config.getString("PdfBoxProcessor.dpi", ""));
        vars.put("pdfBoxProcessorPostProcessorJava2dScaleMode",
                config.getString("PdfBoxProcessor.post_processor.java2d.scale_mode", ""));

        ////////////////////////////////////////////////////////////////////
        //////////////////////// caches section ////////////////////////////
        ////////////////////////////////////////////////////////////////////

        // Cache-Control header
        vars.put("clientCacheEnabled",
                config.getBoolean(AbstractResource.CLIENT_CACHE_ENABLED_CONFIG_KEY, false));
        vars.put("clientCacheMaxAge",
                config.getString(AbstractResource.CLIENT_CACHE_MAX_AGE_CONFIG_KEY, ""));
        vars.put("clientCacheSharedMaxAge",
                config.getString(AbstractResource.CLIENT_CACHE_SHARED_MAX_AGE_CONFIG_KEY, ""));
        vars.put("clientCachePublic",
                config.getBoolean(AbstractResource.CLIENT_CACHE_PUBLIC_CONFIG_KEY, false));
        vars.put("clientCachePrivate",
                config.getBoolean(AbstractResource.CLIENT_CACHE_PRIVATE_CONFIG_KEY, false));
        vars.put("clientCacheNoCache",
                config.getBoolean(AbstractResource.CLIENT_CACHE_NO_CACHE_CONFIG_KEY, false));
        vars.put("clientCacheNoStore",
                config.getBoolean(AbstractResource.CLIENT_CACHE_NO_STORE_CONFIG_KEY, false));
        vars.put("clientCacheMustRevalidate",
                config.getBoolean(AbstractResource.CLIENT_CACHE_MUST_REVALIDATE_CONFIG_KEY, false));
        vars.put("clientCacheProxyRevalidate",
                config.getBoolean(AbstractResource.CLIENT_CACHE_PROXY_REVALIDATE_CONFIG_KEY, false));
        vars.put("clientCacheNoTransform",
                config.getBoolean(AbstractResource.CLIENT_CACHE_NO_TRANSFORM_CONFIG_KEY, false));

        // source cache name
        String sourceCacheStr = "Disabled";
        try {
            Cache cache = CacheFactory.getSourceCache();
            if (cache != null) {
                sourceCacheStr = cache.getClass().getSimpleName();
            }
        } catch (Exception e) {
            // noop
        }
        vars.put("sourceCacheName", sourceCacheStr);

        // derivative cache name
        String derivativeCacheStr = "Disabled";
        try {
            Cache cache = CacheFactory.getDerivativeCache();
            if (cache != null) {
                derivativeCacheStr = cache.getClass().getSimpleName();
            }
        } catch (Exception e) {
            // noop
        }
        vars.put("derivativeCacheName", derivativeCacheStr);

        vars.put("cachePurgeMissing",
                config.getBoolean("cache.server.purge_missing", false));
        vars.put("cacheResolveFirst",
                config.getBoolean("cache.server.resolve_first", false));

        vars.put("cacheWorkerEnabled",
                config.getBoolean(CacheWorker.ENABLED_CONFIG_KEY, false));
        vars.put("cacheWorkerInterval",
                config.getString(CacheWorker.INTERVAL_CONFIG_KEY, "0"));

        vars.put("amazonS3CacheAccessKeyId",
                config.getString("AmazonS3Cache.access_key_id", ""));
        vars.put("amazonS3CacheSecretKey",
                config.getString("AmazonS3Cache.secret_key", ""));
        vars.put("amazonS3CacheBucketName",
                config.getString("AmazonS3Cache.bucket.name", ""));
        vars.put("amazonS3CacheBucketRegion",
                config.getString("AmazonS3Cache.bucket.region", ""));
        vars.put("amazonS3CacheObjectKeyPrefix",
                config.getString("AmazonS3Cache.object_key_prefix", ""));
        vars.put("amazonS3CacheTimeToLive",
                config.getString("AmazonS3Cache.ttl_seconds", ""));

        vars.put("azureStorageCacheAccountName",
                config.getString("AzureStorageCache.account_name", ""));
        vars.put("azureStorageCacheAccountKey",
                config.getString("AzureStorageCache.account_key", ""));
        vars.put("azureStorageCacheContainerName",
                config.getString("AzureStorageCache.container_name", ""));
        vars.put("azureStorageCacheObjectKeyPrefix",
                config.getString("AzureStorageCache.object_key_prefix", ""));
        vars.put("azureStorageCacheTimeToLive",
                config.getString("AzureStorageCache.ttl_seconds", ""));

        vars.put("filesystemCachePathname",
                config.getString("FilesystemCache.pathname", ""));
        vars.put("filesystemCacheTimeToLive",
                config.getString("FilesystemCache.ttl_seconds", ""));
        vars.put("filesystemCacheDirectoryDepth",
                config.getString("FilesystemCache.dir_depth", ""));
        vars.put("filesystemCacheDirectoryName",
                config.getString("FilesystemCache.dir_name", ""));

        vars.put("jdbcCacheUrl",
                config.getString("JdbcCache.url", ""));
        vars.put("jdbcCacheUser",
                config.getString("JdbcCache.user", ""));
        vars.put("jdbcCachePassword",
                config.getString("JdbcCache.password", ""));
        vars.put("jdbcCacheMaxPoolSize",
                config.getString("JdbcCache.max_pool_size", ""));
        vars.put("jdbcCacheConnectionTimeout",
                config.getString("JdbcCache.connection_timeout", ""));
        vars.put("jdbcCacheSourceImageTable",
                config.getString("JdbcCache.source_image_table", ""));
        vars.put("jdbcCacheDerivativeImageTable",
                config.getString("JdbcCache.derivative_image_table", ""));
        vars.put("jdbcCacheInfoTable",
                config.getString("JdbcCache.info_table", ""));
        vars.put("jdbcCacheTimeToLive",
                config.getString("JdbcCache.ttl_seconds", ""));

        ////////////////////////////////////////////////////////////////////
        ///////////////////// watermarking section /////////////////////////
        ////////////////////////////////////////////////////////////////////

        vars.put("watermarkEnabled",
                config.getBoolean(WatermarkService.WATERMARK_ENABLED_CONFIG_KEY, false));
        vars.put("watermarkStrategy",
                config.getString(WatermarkService.WATERMARK_STRATEGY_CONFIG_KEY, ""));
        vars.put("watermarkBasicStrategyImage",
                config.getString(WatermarkService.WATERMARK_FILE_CONFIG_KEY, ""));
        vars.put("watermarkBasicStrategyPosition",
                config.getString(WatermarkService.WATERMARK_POSITION_CONFIG_KEY, ""));
        vars.put("watermarkBasicStrategyInset",
                config.getString(WatermarkService.WATERMARK_INSET_CONFIG_KEY, ""));
        vars.put("watermarkBasicStrategyOutputWidthThreshold",
                config.getString(WatermarkService.WATERMARK_OUTPUT_WIDTH_THRESHOLD_CONFIG_KEY, ""));
        vars.put("watermarkBasicStrategyOutputHeightThreshold",
                config.getString(WatermarkService.WATERMARK_OUTPUT_HEIGHT_THRESHOLD_CONFIG_KEY, ""));

        ////////////////////////////////////////////////////////////////////
        /////////////////////// redaction section //////////////////////////
        ////////////////////////////////////////////////////////////////////

        vars.put("redactionEnabled",
                config.getBoolean(RedactionService.REDACTION_ENABLED_CONFIG_KEY, false));

        ////////////////////////////////////////////////////////////////////
        //////////////////////// logging section ///////////////////////////
        ////////////////////////////////////////////////////////////////////

        vars.put("applicationLogLevel",
                config.getString("log.application.level"));

        vars.put("applicationLogConsoleAppenderEnabled",
                config.getBoolean("log.application.ConsoleAppender.enabled", false));

        vars.put("applicationLogFileAppenderEnabled",
                config.getBoolean("log.application.FileAppender.enabled", false));
        vars.put("applicationLogFileAppenderPathname",
                config.getString("log.application.FileAppender.pathname", ""));

        vars.put("applicationLogRollingFileAppenderEnabled",
                config.getBoolean("log.application.RollingFileAppender.enabled", false));
        vars.put("applicationLogRollingFileAppenderPathname",
                config.getString("log.application.RollingFileAppender.pathname", ""));
        vars.put("applicationLogRollingFileAppenderFilenamePattern",
                config.getString("log.application.RollingFileAppender.TimeBasedRollingPolicy.filename_pattern", ""));
        vars.put("applicationLogRollingFileAppenderMaxHistory",
                config.getString("log.application.RollingFileAppender.TimeBasedRollingPolicy.max_history", ""));

        vars.put("applicationLogSyslogAppenderEnabled",
                config.getBoolean("log.application.SyslogAppender.enabled", false));
        vars.put("applicationLogSyslogAppenderHost",
                config.getString("log.application.SyslogAppender.host", ""));
        vars.put("applicationLogSyslogAppenderPort",
                config.getString("log.application.SyslogAppender.port", ""));
        vars.put("applicationLogSyslogAppenderFacility",
                config.getString("log.application.SyslogAppender.facility", ""));

        vars.put("accessLogConsoleAppenderEnabled",
                config.getBoolean("log.access.ConsoleAppender.enabled", false));

        vars.put("accessLogFileAppenderEnabled",
                config.getBoolean("log.access.FileAppender.enabled", false));
        vars.put("accessLogFileAppenderPathname",
                config.getString("log.access.FileAppender.pathname", ""));

        vars.put("accessLogRollingFileAppenderEnabled",
                config.getBoolean("log.access.RollingFileAppender.enabled", false));
        vars.put("accessLogRollingFileAppenderPathname",
                config.getString("log.access.RollingFileAppender.pathname", ""));
        vars.put("accessLogRollingFileAppenderFilenamePattern",
                config.getString("log.access.RollingFileAppender.TimeBasedRollingPolicy.filename_pattern", ""));
        vars.put("accessLogRollingFileAppenderMaxHistory",
                config.getString("log.access.RollingFileAppender.TimeBasedRollingPolicy.max_history", ""));

        vars.put("accessLogSyslogAppenderEnabled",
                config.getBoolean("log.access.SyslogAppender.enabled", false));
        vars.put("accessLogSyslogAppenderHost",
                config.getString("log.access.SyslogAppender.host", ""));
        vars.put("accessLogSyslogAppenderPort",
                config.getString("log.access.SyslogAppender.port", ""));
        vars.put("accessLogSyslogAppenderFacility",
                config.getString("log.access.SyslogAppender.facility", ""));

        return vars;
    }

}
