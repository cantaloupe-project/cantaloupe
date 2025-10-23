package edu.illinois.library.cantaloupe.controller.admin;

import java.awt.GraphicsEnvironment;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import edu.illinois.library.cantaloupe.cache.CacheFactory;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.http.Headers;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.MetaIdentifierTransformerFactory;
import edu.illinois.library.cantaloupe.operation.Scale;
import edu.illinois.library.cantaloupe.processor.InitializationException;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.processor.ProcessorFactory;
import edu.illinois.library.cantaloupe.processor.SourceFormatException;
import edu.illinois.library.cantaloupe.resource.Request;
import edu.illinois.library.cantaloupe.resource.TemplateVariables;
import edu.illinois.library.cantaloupe.source.Source;
import edu.illinois.library.cantaloupe.source.SourceFactory;
import edu.illinois.library.cantaloupe.util.StringUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Spring Boot controller for admin interface.
 * Replaces the previous admin.AdminResource class.
 */
@Controller
@RequestMapping("/admin")
public class AdminController {
    private final Configuration configuration;

    private final MetaIdentifierTransformerFactory metaIdentifierTransformerFactory;

    @Autowired
    public AdminController(Configuration configuration,
                          MetaIdentifierTransformerFactory metaIdentifierTransformerFactory) {
        this.configuration = configuration;
        this.metaIdentifierTransformerFactory = metaIdentifierTransformerFactory;
    }

    /**
     * Sources, caches, etc. can't be accessed from the templates, so
     * instances of this class will proxy for them.
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
     * Proxy for Format objects in templates.
     */
    public static class FormatProxy extends ObjectProxy implements Comparable<FormatProxy> {

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
     * Proxy for Processor objects in templates.
     */
    public static class ProcessorProxy extends ObjectProxy {

        ProcessorProxy(Processor proc) {
            super(proc);
        }

        public boolean supports(FormatProxy format) {
            return ((Processor) object).supportsSourceFormat((Format) format.object);
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

    @GetMapping
    public String admin(Model model, HttpServletRequest request, HttpServletResponse response) {
        response.setHeader("Content-Type", "text/html;charset=UTF-8");

        // Create request wrapper and get template variables
        Request requestWrapper = new Request(request, Collections.emptyList());
        TemplateVariables vars = getTemplateVars(requestWrapper);

        model.addAllAttributes(vars.getVars());

        return "admin";
    }

    @RequestMapping(value = "", method = RequestMethod.OPTIONS)
    public void options(HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_NO_CONTENT);
        response.setHeader("Allow", "GET,OPTIONS");
    }

    /**
     * @return Map containing keys that will be used as variables in the admin
     *         interface's HTML template.
     */
    private TemplateVariables getTemplateVars(Request request) {
        final TemplateVariables vars = TemplateVariables.getDefault(request);
        vars.put("adminUri", StringUtils.stripEnd((String) vars.get("basePath"), "/") + "/admin");

        ////////////////////////////////////////////////////////////////////
        //////////////////////// status section ////////////////////////////
        ////////////////////////////////////////////////////////////////////
        {
            // VM info
            RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
            vars.put("vmArguments", runtimeMxBean.getInputArguments());
            vars.put("vmName", runtimeMxBean.getVmName());
            vars.put("vmVendor", runtimeMxBean.getVmVendor());
            vars.put("vmVersion", runtimeMxBean.getVmVersion());
            vars.put("javaVersion", runtimeMxBean.getSpecVersion());

            // Reverse-Proxy headers
            final Headers headers = request.getHeaders();
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
        }

        ////////////////////////////////////////////////////////////////////
        /////////////////////// endpoints section //////////////////////////
        ////////////////////////////////////////////////////////////////////
        {
            vars.put("currentMetaIdentifierTransformer",
                    metaIdentifierTransformerFactory
                            .newInstance(null)
                            .getClass().getSimpleName());
            List<String> impls = MetaIdentifierTransformerFactory.allImplementations()
                    .stream()
                    .map(Class::getSimpleName)
                    .collect(Collectors.toList());
            vars.put("metaIdentifierTransformers", impls);
        }

        ////////////////////////////////////////////////////////////////////
        //////////////////////// sources section ///////////////////////////
        ////////////////////////////////////////////////////////////////////

        SourceFactory.SelectionStrategy selectionStrategy =
                new SourceFactory(configuration).getSelectionStrategy();
        vars.put("sourceSelectionStrategy", selectionStrategy);

        if (selectionStrategy.equals(SourceFactory.SelectionStrategy.STATIC)) {
            try {
                Source source = new SourceFactory(configuration).newSource(
                        new Identifier("irrelevant"),
                        null);
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
        Map<FormatProxy, ProcessorProxy> assignments = new TreeMap<>();
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
        {
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
        }

        ////////////////////////////////////////////////////////////////////
        /////////////////////// overlays section ///////////////////////////
        ////////////////////////////////////////////////////////////////////
        {
            vars.put("fonts", GraphicsEnvironment.getLocalGraphicsEnvironment().
                    getAvailableFontFamilyNames());
            vars.put("currentOverlayFont", configuration.
                    getString(Key.OVERLAY_STRING_FONT, ""));
        }
        return vars;
    }
}
