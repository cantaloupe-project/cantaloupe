package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.cache.Cache;
import edu.illinois.library.cantaloupe.cache.CacheFactory;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.processor.ProcessorFactory;
import edu.illinois.library.cantaloupe.processor.UnsupportedSourceFormatException;
import edu.illinois.library.cantaloupe.resolver.ResolverFactory;
import org.apache.commons.configuration.Configuration;
import org.apache.velocity.Template;
import org.apache.velocity.app.Velocity;
import org.restlet.data.CacheDirective;
import org.restlet.data.MediaType;
import org.restlet.ext.velocity.TemplateRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;

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
        // add a "Cache-Control: no-cache" header because this page contains
        // live information pertaining to the function of the application
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

        vars.put("iiif1EndpointEnabled",
                config.getBoolean("endpoint.iiif.1.enabled", true));
        vars.put("iiif2EndpointEnabled",
                config.getBoolean("endpoint.iiif.2.enabled", true));

        // resolver name
        String resolverStr = config.getString(
                ResolverFactory.STATIC_RESOLVER_CONFIG_KEY);
        if (config.getBoolean(ResolverFactory.DELEGATE_RESOLVER_CONFIG_KEY, false)) {
            resolverStr = "Delegate Script";
        }
        vars.put("resolverName", resolverStr);

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

        return vars;
    }

}
