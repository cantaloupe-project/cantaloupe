package edu.illinois.library.cantaloupe.resource.admin;

import edu.illinois.library.cantaloupe.RestletApplication;
import edu.illinois.library.cantaloupe.cache.CacheFactory;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.operation.Scale;
import edu.illinois.library.cantaloupe.processor.InitializationException;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.processor.ProcessorConnector;
import edu.illinois.library.cantaloupe.processor.ProcessorFactory;
import edu.illinois.library.cantaloupe.processor.UnsupportedSourceFormatException;
import edu.illinois.library.cantaloupe.resolver.ResolverFactory;
import org.restlet.data.CacheDirective;
import org.restlet.data.Header;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;
import org.restlet.util.Series;

import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Handles the web-based Control Panel.
 */
public class AdminResource extends AbstractAdminResource {

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

    private static class ObjectProxyComparator
            implements Comparator<ObjectProxy>, Serializable {
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
     * @return Map containing keys that will be used as variables in the admin
     *         interface's HTML template.
     */
    private Map<String,Object> getTemplateVars() throws Exception {
        final Map<String, Object> vars = getCommonTemplateVars(getRequest());
        vars.put("adminUri", vars.get("baseUri") + RestletApplication.ADMIN_PATH);

        ////////////////////////////////////////////////////////////////////
        //////////////////////// status section ////////////////////////////
        ////////////////////////////////////////////////////////////////////

        // VM info
        RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
        vars.put("vmArguments", runtimeMxBean.getInputArguments());
        vars.put("vmName", runtimeMxBean.getVmName());
        vars.put("vmVendor", runtimeMxBean.getVmVendor());
        vars.put("vmVersion", runtimeMxBean.getVmVersion());
        vars.put("javaVersion", runtimeMxBean.getSpecVersion());

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
                headers.getFirstValue(PUBLIC_IDENTIFIER_HEADER, true, ""));

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
                    new ResolverFactory().newResolver(new Identifier("irrelevant"))));
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
                        new ProcessorProxy(new ProcessorFactory().newProcessor(format)));
            } catch (UnsupportedSourceFormatException e) {
                // nothing we can do
            } catch (InitializationException e) {
                // nothing we can do
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
