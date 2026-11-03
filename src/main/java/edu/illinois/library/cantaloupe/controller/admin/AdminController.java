package edu.illinois.library.cantaloupe.controller.admin;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.auth.BasicAuth;
import edu.illinois.library.cantaloupe.cache.CacheFactory;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.MetaIdentifierTransformerFactory;
import edu.illinois.library.cantaloupe.operation.Scale;
import edu.illinois.library.cantaloupe.processor.InitializationException;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.processor.ProcessorFactory;
import edu.illinois.library.cantaloupe.processor.SourceFormatException;
import edu.illinois.library.cantaloupe.resource.EndpointDisabledException;
import edu.illinois.library.cantaloupe.resource.ResourceException;
import edu.illinois.library.cantaloupe.resource.TemplateVariables;
import edu.illinois.library.cantaloupe.source.Source;
import edu.illinois.library.cantaloupe.source.SourceFactory;
import edu.illinois.library.cantaloupe.util.StringUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.awt.GraphicsEnvironment;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Spring Boot controller for admin interface.
 * Replaces the previous admin.AdminResource class.
 */
@Controller
@RequestMapping("/admin")
public class AdminController {
    private final Configuration configuration;
    private final SourceFactory sourceFactory;

    private final MetaIdentifierTransformerFactory metaIdentifierTransformerFactory;

    @Autowired
    public AdminController(Configuration configuration,
                          SourceFactory sourceFactory,
                          MetaIdentifierTransformerFactory metaIdentifierTransformerFactory) {
        this.configuration = configuration;
        this.sourceFactory = sourceFactory;
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
    public String admin(Model model, HttpServletRequest request, HttpServletResponse response) throws ResourceException {
        beforeAll(request, response);
        response.setHeader("Content-Type", "text/html;charset=UTF-8");
        response.setHeader("Cache-Control", "no-cache");

        // Create request wrapper and get template variables
        TemplateVariables vars = getTemplateVars(request);

        model.addAllAttributes(vars.getVars());

        return "admin";
    }

    @RequestMapping(value = "", method = RequestMethod.OPTIONS)
    public void options(HttpServletRequest request, HttpServletResponse response) throws ResourceException {
        beforeAll(request, response);
        response.setStatus(HttpServletResponse.SC_NO_CONTENT);
        response.setHeader("Allow", "GET,OPTIONS");
    }

    /**
     * @return Map containing keys that will be used as variables in the admin
     *         interface's HTML template.
     */
    private TemplateVariables getTemplateVars(HttpServletRequest servletRequest) {
        final TemplateVariables vars = TemplateVariables.getDefault(servletRequest.getHeader("X-Forwarded-Path"));
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
            vars.put("xForwardedProtoHeader", Optional.ofNullable(servletRequest.getHeader("X-Forwarded-Proto")).orElse(""));
            vars.put("xForwardedHostHeader", Optional.ofNullable(servletRequest.getHeader("X-Forwarded-Host")).orElse(""));
            vars.put("xForwardedPortHeader", Optional.ofNullable(servletRequest.getHeader("X-Forwarded-Port")).orElse(""));
            vars.put("xForwardedPathHeader", Optional.ofNullable(servletRequest.getHeader("X-Forwarded-Path")).orElse(""));
            vars.put("xForwardedForHeader", Optional.ofNullable(servletRequest.getHeader("X-Forwarded-For")).orElse(""));
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

        List<ObjectProxy> sortedProxies = sourceFactory.getAllSources().
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
                new ProcessorFactory(configuration).getSelectionStrategy());

        // source format assignments
        Map<FormatProxy, ProcessorProxy> assignments = new TreeMap<>();
        for (Format format : Format.all()) {
            try (Processor proc = new ProcessorFactory(configuration).newProcessor(format)) {
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
        CacheFactory cacheFactory = new CacheFactory(configuration);
        {
            // source caches
            try {
                cacheFactory.getSourceCache().ifPresent(sc ->
                        vars.put("currentSourceCache", sc));
            } catch (Exception e) {
                // noop
            }

            sortedProxies = cacheFactory.getAllSourceCaches()
                    .stream()
                    .map(ObjectProxy::new)
                    .sorted(Comparator.comparing(ObjectProxy::getName))
                    .collect(Collectors.toList());
            vars.put("sourceCaches", sortedProxies);

            // derivative caches
            try {
                vars.put("currentDerivativeCache",
                        cacheFactory.getDerivativeCache());
            } catch (Exception e) {
                // noop
            }

            sortedProxies = cacheFactory.getAllDerivativeCaches()
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


    static final String BASIC_REALM = Application.getName() + " Control Panel";

    private void beforeAll(HttpServletRequest request, HttpServletResponse response) throws ResourceException {
        if (!configuration.getBoolean(Key.ADMIN_ENABLED, false)) {
            throw new EndpointDisabledException();
        }
        // Perform HTTP Basic Authentication
        BasicAuth.authenticateUsingBasic(BASIC_REALM, user -> {
            final String configUser = configuration.getString(Key.ADMIN_USERNAME, "");
            if (!configUser.isEmpty() && configUser.equals(user)) {
                return configuration.getString(Key.ADMIN_SECRET);
            }
            return null;
        }, request, response);
    }
}
