package edu.illinois.library.cantaloupe.resource.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.illinois.library.cantaloupe.cache.CacheFactory;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.operation.Scale;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.processor.ProcessorFactory;
import edu.illinois.library.cantaloupe.processor.UnsupportedSourceFormatException;
import edu.illinois.library.cantaloupe.resolver.ResolverFactory;
import edu.illinois.library.cantaloupe.resource.AbstractResource;
import edu.illinois.library.cantaloupe.resource.EndpointDisabledException;
import edu.illinois.library.cantaloupe.resource.JSONRepresentation;
import edu.illinois.library.cantaloupe.processor.ProcessorConnector;
import org.restlet.data.CacheDirective;
import org.restlet.data.Header;
import org.restlet.representation.EmptyRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;
import org.restlet.util.Series;
import org.slf4j.LoggerFactory;

import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Handles the web-based Control Panel.
 */
public class AdminResource extends AbstractResource {

    /**
     * <p>Resolvers, caches, etc. can't be accessed from the templates, so
     * instances of this class will proxy for them.</p>
     *
     * <p>N.B.: Velocity requires this class to be public.</p>
     */
    public static class ObjectProxy {
        protected Object object;

        ObjectProxy(Object object) {
            this.object = object;
        }

        public String getName() {
            return object.getClass().getSimpleName();
        }
    }

    /**
     * N.B.: Velocity requires this class to be public.
     */
    public static class ProcessorProxy extends ObjectProxy {

        ProcessorProxy(Processor proc) {
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

        public List<String> getWarnings() {
            return ((Processor) object).getWarnings();
        }
    }

    private class ObjectProxyComparator implements Comparator<ObjectProxy> {
        public int compare(ObjectProxy o1, ObjectProxy o2) {
            return o1.getName().compareTo(o2.getName());
        }
    }

    private static final org.slf4j.Logger logger = LoggerFactory.
            getLogger(AdminResource.class);

    @Override
    protected void doInit() throws ResourceException {
        if (!Configuration.getInstance().getBoolean(Key.ADMIN_ENABLED, false)) {
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
        return template("/admin.vm", getTemplateVars());
    }

    /**
     * @return JSON application configuration. <strong>This may contain
     *         sensitive info and must be protected.</strong>
     * @throws Exception
     */
    @Get("json")
    public Representation doGetAsJson() throws Exception {
        return new JSONRepresentation(configurationAsMap());
    }

    /**
     * Deserializes submitted JSON data and updates the application
     * configuration instance with it.
     *
     * @param rep
     * @throws IOException
     */
    @Post("json")
    public Representation doPost(Representation rep) throws IOException {
        final Configuration config = Configuration.getInstance();
        final Map<?, ?> submittedConfig = new ObjectMapper().readValue(
                rep.getStream(), HashMap.class);

        // Copy configuration keys and values from the request JSON payload to
        // the application configuration.
        for (final Object key : submittedConfig.keySet()) {
            final Object value = submittedConfig.get(key);
            logger.debug("Setting {} = {}", key, value);
            config.setProperty((String) key, value);

        }

        config.save();

        return new EmptyRepresentation();
    }

    /**
     * @return Map representation of the application configuration.
     */
    private Map<String,Object> configurationAsMap() {
        final Configuration config = Configuration.getInstance();
        final Map<String,Object> configMap = new HashMap<>();
        final Iterator<String> it = config.getKeys();
        while (it.hasNext()) {
            final String key = it.next();
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
        final Map<String, Object> vars = getCommonTemplateVars(getRequest());

        ////////////////////////////////////////////////////////////////////
        //////////////////////// status section ////////////////////////////
        ////////////////////////////////////////////////////////////////////

        // VM info
        RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
        vars.put("vmArguments", runtimeMxBean.getInputArguments());
        vars.put("vmName", runtimeMxBean.getVmName());
        vars.put("vmVendor", runtimeMxBean.getVmVendor());
        vars.put("vmVersion", runtimeMxBean.getVmVersion());
        vars.put("uptime", TimeUtils.millisecondsToHumanTime(runtimeMxBean.getUptime()));

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
        vars.put("xIiifIdHeader",
                headers.getFirstValue("X-IIIF-ID", true, ""));

        final File configFile = Configuration.getInstance().getFile();
        vars.put("configFilePath", (configFile != null) ?
                configFile.getAbsolutePath() : "None");

        ////////////////////////////////////////////////////////////////////
        /////////////////////// resolvers section //////////////////////////
        ////////////////////////////////////////////////////////////////////

        ResolverFactory.SelectionStrategy selectionStrategy =
                new ResolverFactory().getSelectionStrategy();
        vars.put("resolverSelectionStrategy", selectionStrategy);

        if (selectionStrategy.equals(ResolverFactory.SelectionStrategy.STATIC)) {
            vars.put("currentResolver", new ObjectProxy(
                    new ResolverFactory().getResolver(new Identifier("irrelevant"))));
        }

        List<ObjectProxy> sortedProxies = ResolverFactory.getAllResolvers().
                stream().
                map(ObjectProxy::new).
                collect(Collectors.toList());
        sortedProxies.sort(new ObjectProxyComparator());
        vars.put("resolvers", sortedProxies);

        ////////////////////////////////////////////////////////////////////
        ////////////////////// processors section //////////////////////////
        ////////////////////////////////////////////////////////////////////

        // source format assignments
        Map<Format,ProcessorProxy> assignments = new TreeMap<>();
        for (Format format : Format.values()) {
            try {
                assignments.put(format,
                        new ProcessorProxy(new ProcessorFactory().getProcessor(format)));
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
        List<Format> imageFormats = Arrays.stream(Format.values()).
                filter(f -> Format.Type.IMAGE.equals(f.getType())).
                collect(Collectors.toList());
        imageFormats.sort(new SourceFormatComparator());
        vars.put("imageSourceFormats", imageFormats);

        // video source formats
        List<Format> videoFormats = Arrays.stream(Format.values()).
                filter(f -> Format.Type.VIDEO.equals(f.getType())).
                collect(Collectors.toList());
        videoFormats.sort(new SourceFormatComparator());
        vars.put("videoSourceFormats", videoFormats);

        // source format assignments
        vars.put("sourceFormats", Format.values());

        List<ProcessorProxy> sortedProcessorProxies =
                ProcessorFactory.getAllProcessors().stream().
                        map(ProcessorProxy::new).
                        collect(Collectors.toList());

        // warnings
        vars.put("anyWarnings", sortedProcessorProxies.stream().
                anyMatch(p -> !p.getWarnings().isEmpty()));

        sortedProcessorProxies.sort(new ObjectProxyComparator());
        vars.put("processors", sortedProcessorProxies);

        vars.put("streamProcessorRetrievalStrategy",
                ProcessorConnector.getStreamProcessorRetrievalStrategy());

        // source formats
        vars.put("scaleFilters", Scale.Filter.values());

        ////////////////////////////////////////////////////////////////////
        //////////////////////// caches section ////////////////////////////
        ////////////////////////////////////////////////////////////////////

        // source caches
        try {
            vars.put("currentSourceCache", CacheFactory.getSourceCache());
        } catch (Exception e) {
            // noop
        }

        sortedProxies = CacheFactory.getAllSourceCaches().stream().
                map(ObjectProxy::new).
                collect(Collectors.toList());
        sortedProxies.sort(new ObjectProxyComparator());
        vars.put("sourceCaches", sortedProxies);

        // derivative caches
        try {
            vars.put("currentDerivativeCache",
                    CacheFactory.getDerivativeCache());
        } catch (Exception e) {
            // noop
        }

        sortedProxies = CacheFactory.getAllDerivativeCaches().stream().
                map(ObjectProxy::new).
                collect(Collectors.toList());
        sortedProxies.sort(new ObjectProxyComparator());
        vars.put("derivativeCaches", sortedProxies);

        ////////////////////////////////////////////////////////////////////
        /////////////////////// overlays section ///////////////////////////
        ////////////////////////////////////////////////////////////////////

        vars.put("fonts", GraphicsEnvironment.getLocalGraphicsEnvironment().
                getAvailableFontFamilyNames());
        vars.put("currentOverlayFont", Configuration.getInstance().
                getString(Key.OVERLAY_STRING_FONT, ""));

        return vars;
    }

}
