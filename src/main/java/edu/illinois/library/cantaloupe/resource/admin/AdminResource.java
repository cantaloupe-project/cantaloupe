package edu.illinois.library.cantaloupe.resource.admin;

import edu.illinois.library.cantaloupe.cache.CacheFactory;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.http.Headers;
import edu.illinois.library.cantaloupe.http.Method;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.operation.Scale;
import edu.illinois.library.cantaloupe.processor.InitializationException;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.processor.ProcessorFactory;
import edu.illinois.library.cantaloupe.processor.SourceFormatException;
import edu.illinois.library.cantaloupe.resource.Route;
import edu.illinois.library.cantaloupe.resource.VelocityRepresentation;
import edu.illinois.library.cantaloupe.source.Source;
import edu.illinois.library.cantaloupe.source.SourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.GraphicsEnvironment;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
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
     * <p>Sources, caches, etc. can't be accessed from the templates, so
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
    public static class FormatProxy extends ObjectProxy
            implements Comparable<FormatProxy> {

        FormatProxy(Format format) {
            super(format);
        }

        @Override
        public int compareTo(FormatProxy o) {
            return ((Format) object).compareTo((Format) o.object);
        }

        @Override
        public String getName() {
            return ((Format) object).getName();
        }

        public boolean isImage() {
            return !((Format) object).isVideo();
        }

        public boolean isVideo() {
            return ((Format) object).isVideo();
        }

        @Override
        public String toString() {
            return object.toString();
        }

    }

    /**
     * N.B.: Velocity requires this class to be public.
     */
    public static class ProcessorProxy extends ObjectProxy {

        ProcessorProxy(Processor proc) {
            super(proc);
        }

        public boolean supports(FormatProxy format) {
            try {
                ((Processor) object).setSourceFormat((Format) format.object);
                return true;
            } catch (SourceFormatException e) {
                return false;
            }
        }

        /**
         * @return List of all processor warnings, plus the message of the
         *         return value of {@link Processor#getInitializationError()},
         *         if any.
         */
        public List<String> getWarnings() {
            Processor proc = (Processor) object;

            List<String> warnings = new ArrayList<>();

            // Add the InitializationException message
            String msg = proc.getInitializationError();
            if (msg != null) {
                warnings.add(msg);
            }

            // Add warnings
            warnings.addAll(proc.getWarnings());

            return warnings;
        }
    }

    private static final Logger LOGGER =
            LoggerFactory.getLogger(AdminResource.class);

    private static final Method[] SUPPORTED_METHODS =
            new Method[] { Method.GET, Method.OPTIONS };

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

    @Override
    public Method[] getSupportedMethods() {
        return SUPPORTED_METHODS;
    }

    @Override
    public void doGET() throws Exception {
        getResponse().setHeader("Content-Type", "text/html;charset=UTF-8");

        new VelocityRepresentation("/admin.vm", getTemplateVars())
                .write(getResponse().getOutputStream());
    }

    /**
     * @return Map containing keys that will be used as variables in the admin
     *         interface's HTML template.
     */
    private Map<String,Object> getTemplateVars() {
        final Map<String, Object> vars = getCommonTemplateVars();
        vars.put("adminUri", vars.get("baseUri") + Route.ADMIN_PATH);

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
        final Headers headers = getRequest().getHeaders();
        vars.put("xForwardedProtoHeader",
                headers.getFirstValue("X-Forwarded-Proto", ""));
        vars.put("xForwardedHostHeader",
                headers.getFirstValue("X-Forwarded-Host", ""));
        vars.put("xForwardedPortHeader",
                headers.getFirstValue("X-Forwarded-Port", ""));
        vars.put("xForwardedPathHeader",
                headers.getFirstValue("X-Forwarded-Path", ""));
        vars.put("xForwardedForHeader",
                headers.getFirstValue("X-Forwarded-For", ""));

        ////////////////////////////////////////////////////////////////////
        //////////////////////// sources section ///////////////////////////
        ////////////////////////////////////////////////////////////////////

        SourceFactory.SelectionStrategy selectionStrategy =
                new SourceFactory().getSelectionStrategy();
        vars.put("sourceSelectionStrategy", selectionStrategy);

        if (selectionStrategy.equals(SourceFactory.SelectionStrategy.STATIC)) {
            try {
                Source source = new SourceFactory().newSource(
                        new Identifier("irrelevant"),
                        getDelegateProxy());
                vars.put("currentSource", new ObjectProxy(source));
            } catch (Exception e) {
                // nothing we can do
            }
        }

        List<ObjectProxy> sortedProxies = SourceFactory.getAllSources().
                stream().
                map(ObjectProxy::new).
                sorted(Comparator.comparing(ObjectProxy::getName)).
                collect(Collectors.toList());
        vars.put("sources", sortedProxies);

        ////////////////////////////////////////////////////////////////////
        ////////////////////// processors section //////////////////////////
        ////////////////////////////////////////////////////////////////////

        // selection strategy
        vars.put("processorSelectionStrategy",
                new ProcessorFactory().getSelectionStrategy());

        // source format assignments
        Map<FormatProxy,ProcessorProxy> assignments = new TreeMap<>();
        for (Format format : Format.all()) {
            try (Processor proc = new ProcessorFactory().newProcessor(format)) {
                assignments.put(new FormatProxy(format), new ProcessorProxy(proc));
            } catch (SourceFormatException |
                    InitializationException |
                    ReflectiveOperationException e) {
                // nothing we can do
            }
        }
        vars.put("processorAssignments", assignments);

        // image source formats
        List<FormatProxy> imageFormats = Format.all()
                .stream()
                .filter(f -> !f.isVideo())
                .sorted(Comparator.comparing(Format::getName))
                .map(FormatProxy::new)
                .collect(Collectors.toUnmodifiableList());
        vars.put("imageSourceFormats", imageFormats);

        // video source formats
        List<FormatProxy> videoFormats = Format.all()
                .stream()
                .filter(Format::isVideo)
                .sorted(Comparator.comparing(Format::getName))
                .map(FormatProxy::new)
                .collect(Collectors.toUnmodifiableList());
        vars.put("videoSourceFormats", videoFormats);

        // source format assignments
        vars.put("sourceFormats", Format.all()
                .stream()
                .map(FormatProxy::new)
                .collect(Collectors.toUnmodifiableList()));

        List<ProcessorProxy> sortedProcessorProxies =
                ProcessorFactory.getAllProcessors().stream().
                        map(ProcessorProxy::new).
                        sorted(Comparator.comparing(ObjectProxy::getName)).
                        collect(Collectors.toUnmodifiableList());

        // warnings
        vars.put("anyWarnings", sortedProcessorProxies
                .stream()
                .anyMatch(p -> !p.getWarnings().isEmpty()));

        vars.put("processors", sortedProcessorProxies);

        // source formats
        vars.put("scaleFilters", Scale.Filter.values());

        ////////////////////////////////////////////////////////////////////
        //////////////////////// caches section ////////////////////////////
        ////////////////////////////////////////////////////////////////////

        // source caches
        try {
            CacheFactory.getSourceCache().ifPresent(sc ->
                    vars.put("currentSourceCache", sc));
        } catch (Exception e) {
            // noop
        }

        sortedProxies = CacheFactory.getAllSourceCaches()
                .stream()
                .map(ObjectProxy::new)
                .sorted(Comparator.comparing(ObjectProxy::getName))
                .collect(Collectors.toList());
        vars.put("sourceCaches", sortedProxies);

        // derivative caches
        try {
            vars.put("currentDerivativeCache",
                    CacheFactory.getDerivativeCache());
        } catch (Exception e) {
            // noop
        }

        sortedProxies = CacheFactory.getAllDerivativeCaches()
                .stream()
                .map(ObjectProxy::new)
                .sorted(Comparator.comparing(ObjectProxy::getName))
                .collect(Collectors.toList());
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
