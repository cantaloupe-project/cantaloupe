package edu.illinois.library.cantaloupe.resource.admin;

import edu.illinois.library.cantaloupe.WebApplication;
import edu.illinois.library.cantaloupe.cache.Cache;
import edu.illinois.library.cantaloupe.cache.CacheFactory;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.operation.Scale;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.processor.ProcessorFactory;
import edu.illinois.library.cantaloupe.processor.UnsupportedSourceFormatException;
import edu.illinois.library.cantaloupe.resolver.Resolver;
import edu.illinois.library.cantaloupe.resolver.ResolverFactory;
import edu.illinois.library.cantaloupe.resource.SourceImageWrangler;
import org.restlet.data.CacheDirective;
import org.restlet.data.Header;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;
import org.restlet.util.Series;

import java.awt.GraphicsEnvironment;
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

    private static class ObjectProxyComparator
            implements Comparator<ObjectProxy> {
        public int compare(ObjectProxy o1, ObjectProxy o2) {
            return o1.getName().compareTo(o2.getName());
        }
    }

    @Override
    protected void doInit() throws ResourceException {
        super.doInit();

        // Add a "Cache-Control: no-cache" header because this page contains
        // live information pertaining to the function of the application.
        getResponseCacheDirectives().add(CacheDirective.noCache());
    }

    /**
     * @return HTML representation of the admin interface.
     */
    @Get("html")
    public Representation doGet() throws Exception {
        return template("/admin.vm", getTemplateVars());
    }

    /**
     * @return Map containing variables for use in the admin interface HTML
     *         template.
     */
    private Map<String,Object> getTemplateVars() throws Exception {
        final Map<String, Object> vars = getCommonTemplateVars(getRequest());
        vars.put("adminUri", vars.get("baseUri") + WebApplication.ADMIN_PATH);

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
        vars.put("xIiifIdHeader",
                headers.getFirstValue("X-IIIF-ID", true, ""));

        final File configFile =
                ConfigurationFactory.getInstance().getFile();
        vars.put("configFilePath", (configFile != null) ?
                configFile.getAbsolutePath() : "None");

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
        /////////////////////// overlays section ///////////////////////////
        ////////////////////////////////////////////////////////////////////

        vars.put("fonts", GraphicsEnvironment.getLocalGraphicsEnvironment().
                getAvailableFontFamilyNames());
        vars.put("currentOverlayFont", ConfigurationFactory.getInstance().
                getString("overlays.BasicStrategy.string.font", ""));

        return vars;
    }

}
