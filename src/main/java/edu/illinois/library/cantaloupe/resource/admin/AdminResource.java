package edu.illinois.library.cantaloupe.resource.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.cache.Cache;
import edu.illinois.library.cantaloupe.cache.CacheFactory;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.processor.ProcessorFactory;
import edu.illinois.library.cantaloupe.processor.UnsupportedSourceFormatException;
import edu.illinois.library.cantaloupe.resolver.Resolver;
import edu.illinois.library.cantaloupe.resolver.ResolverFactory;
import edu.illinois.library.cantaloupe.resource.AbstractResource;
import edu.illinois.library.cantaloupe.resource.EndpointDisabledException;
import edu.illinois.library.cantaloupe.resource.SourceImageWrangler;
import edu.illinois.library.cantaloupe.script.ScriptEngineFactory;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.FileConfiguration;
import org.apache.commons.io.FileUtils;
import org.apache.velocity.Template;
import org.apache.velocity.app.Velocity;
import org.restlet.data.CacheDirective;
import org.restlet.data.Header;
import org.restlet.data.MediaType;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.ext.velocity.TemplateRepresentation;
import org.restlet.representation.EmptyRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;
import org.restlet.util.Series;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class AdminResource extends AbstractResource {

    /**
     * <p>Resolver, caches, etc. can't be accessed from the templates, so
     * instances of this class will proxy for them.</p>
     *
     * <p>Velocity requires this class to be public.</p>
     */
    public static class ObjectProxy {
        protected Object object;

        public ObjectProxy(Object object) {
            this.object = object;
        }

        public String getName() {
            return object.getClass().getSimpleName();
        }
    }

    public static class ProcessorProxy extends ObjectProxy {

        public ProcessorProxy(Processor proc) {
            super(proc);
        }

        public boolean supports(Format format) {
            try {
                ((Processor) object).setSourceFormat(format);
                return true;
            } catch (UnsupportedSourceFormatException e) {
                return false;
            }
        }
    }

    private class ObjectProxyComparator implements Comparator<ObjectProxy> {
        public int compare(ObjectProxy o1, ObjectProxy o2) {
            return o1.getName().compareTo(o2.getName());
        }
    }

    private static org.slf4j.Logger logger = LoggerFactory.
            getLogger(AdminResource.class);

    public static final String CONTROL_PANEL_ENABLED_CONFIG_KEY =
            "admin.enabled";

    @Override
    protected void doInit() throws ResourceException {
        if (!Application.getConfiguration().
                getBoolean(CONTROL_PANEL_ENABLED_CONFIG_KEY, true)) {
            throw new EndpointDisabledException();
        }
        super.doInit();

        // Add a "Cache-Control: no-cache" header because this page contains
        // live information pertaining to the function of the application.
        getResponseCacheDirectives().add(CacheDirective.noCache());
    }

    /**
     * @return HTML representation of the admin interface.
     * @throws Exception
     */
    @Get("html")
    public Representation doGetAsHtml() throws Exception {
        Template template = Velocity.getTemplate("admin.vm");
        return new TemplateRepresentation(template, getTemplateVars(),
                MediaType.TEXT_HTML);
    }

    /**
     * @return JSON application configuration. <strong>This may contain
     *         sensitive info and must be protected.</strong>
     * @throws Exception
     */
    @Get("application/json")
    public Representation doGetAsJson() throws Exception {
        return new JacksonRepresentation<>(configurationAsMap());
    }

    /**
     * Deserializes submitted JSON data and updates the application
     * configuration instance with it.
     *
     * @param rep
     * @throws IOException
     * @throws ConfigurationException
     */
    @Post("application/json")
    public Representation doPost(Representation rep)
            throws IOException, ConfigurationException {
        final Configuration config = Application.getConfiguration();
        final Map submittedConfig = new ObjectMapper().readValue(
                rep.getStream(), HashMap.class);

        // Copy configuration keys and values from the request JSON payload to
        // the application configuration.
        for (final Object key : submittedConfig.keySet()) {
            // Some POSTed keys are not actually configuration keys and will
            // need to be handled separately.
            if (!key.equals("delegate_script_contents")) {
                final Object value = submittedConfig.get(key);
                logger.debug("Setting {} = {}", key, value);
                config.setProperty((String) key, value);
            }
        }

        // Handle certain submitted keys differently.
        final String scriptContents =
                (String) submittedConfig.get("delegate_script_contents");
        if (scriptContents != null) {
            final File scriptFile = ScriptEngineFactory.getScript();
            if (scriptFile != null) {
                logger.info("Updating delegate script contents");
                FileUtils.writeStringToFile(scriptFile, scriptContents);
            } else {
                logger.info("No delegate script file; skipping update.");
            }
        }

        // If the application configuration is file-based, save it.
        if (config instanceof FileConfiguration) {
            final FileConfiguration fileConfig = (FileConfiguration) config;
            final File configFile = Application.getConfigurationFile();
            if (configFile != null) {
                logger.debug("Saving {}", configFile);
                fileConfig.save();
            } else {
                logger.debug("No configuration file; nothing to save.");
            }
        }

        return new EmptyRepresentation();
    }

    /**
     * @return Map representation of the application configuration.
     */
    private Map<String,Object> configurationAsMap() {
        final Configuration config = Application.getConfiguration();
        final Map<String,Object> configMap = new HashMap<>();
        final Iterator it = config.getKeys();
        while (it.hasNext()) {
            final String key = (String) it.next();
            final Object value = config.getProperty(key);
            configMap.put(key, value);
        }
        return configMap;
    }

    /**
     * @return Map containing variables for use in the admin interface HTML
     *         template.
     * @throws Exception
     */
    private Map<String,Object> getTemplateVars() throws Exception {
        final Configuration config = Application.getConfiguration();
        final Map<String, Object> vars = getCommonTemplateVars(getRequest());

        final File configFile = Application.getConfigurationFile();
        if (configFile != null) {
            vars.put("configFilePath", configFile.getAbsolutePath());
        }

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

        ////////////////////////////////////////////////////////////////////
        /////////////////////// resolver section ///////////////////////////
        ////////////////////////////////////////////////////////////////////

        ResolverFactory.SelectionStrategy selectionStrategy =
                ResolverFactory.getSelectionStrategy();
        vars.put("resolverSelectionStrategy", selectionStrategy);

        if (selectionStrategy.equals(ResolverFactory.SelectionStrategy.STATIC)) {
            vars.put("currentResolver", new ObjectProxy(
                    ResolverFactory.getResolver(new Identifier("irrelevant"))));
        }

        List<ObjectProxy> sortedProxies = new ArrayList<>();
        for (Resolver resolver : ResolverFactory.getAllResolvers()) {
            sortedProxies.add(new ObjectProxy(resolver));
        }

        Collections.sort(sortedProxies, new ObjectProxyComparator());
        vars.put("resolvers", sortedProxies);

        ////////////////////////////////////////////////////////////////////
        ////////////////////// processors section //////////////////////////
        ////////////////////////////////////////////////////////////////////

        // source format assignments
        Map<Format,ProcessorProxy> assignments = new TreeMap<>();
        for (Format format : Format.values()) {
            try {
                assignments.put(format,
                        new ProcessorProxy(ProcessorFactory.getProcessor(format)));
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

        // source format assignments
        vars.put("sourceFormats", Format.values());

        List<ProcessorProxy> sortedProcessorProxies = new ArrayList<>();
        for (Processor proc : ProcessorFactory.getAllProcessors()) {
            sortedProcessorProxies.add(new ProcessorProxy(proc));
        }

        Collections.sort(sortedProcessorProxies, new ObjectProxyComparator());
        vars.put("processors", sortedProcessorProxies);

        vars.put("streamProcessorRetrievalStrategy",
                SourceImageWrangler.getStreamProcessorRetrievalStrategy());

        ////////////////////////////////////////////////////////////////////
        //////////////////////// caches section ////////////////////////////
        ////////////////////////////////////////////////////////////////////

        // source caches
        try {
            vars.put("currentSourceCache", CacheFactory.getSourceCache());
        } catch (Exception e) {
            // noop
        }

        sortedProxies = new ArrayList<>();
        for (Cache cache : CacheFactory.getAllSourceCaches()) {
            sortedProxies.add(new ObjectProxy(cache));
        }

        Collections.sort(sortedProxies, new ObjectProxyComparator());
        vars.put("sourceCaches", sortedProxies);

        // derivative caches
        try {
            vars.put("currentDerivativeCache",
                    CacheFactory.getDerivativeCache());
        } catch (Exception e) {
            // noop
        }

        sortedProxies = new ArrayList<>();
        for (Cache cache : CacheFactory.getAllDerivativeCaches()) {
            sortedProxies.add(new ObjectProxy(cache));
        }

        Collections.sort(sortedProxies, new ObjectProxyComparator());
        vars.put("derivativeCaches", sortedProxies);

        ////////////////////////////////////////////////////////////////////
        //////////////////// delegate script section ///////////////////////
        ////////////////////////////////////////////////////////////////////

        String scriptData = "";
        try {
            File scriptFile = ScriptEngineFactory.getScript();
            if (scriptFile != null) {
                scriptData = FileUtils.readFileToString(scriptFile);
            }
        } catch (FileNotFoundException e) {
            logger.debug(e.getMessage());
        }
        vars.put("delegateScriptContents", scriptData);

        return vars;
    }

}
