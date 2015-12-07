package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.cache.Cache;
import edu.illinois.library.cantaloupe.cache.CacheFactory;
import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.processor.ProcessorFactory;
import edu.illinois.library.cantaloupe.processor.UnsupportedSourceFormatException;
import edu.illinois.library.cantaloupe.resolver.Resolver;
import edu.illinois.library.cantaloupe.resolver.ResolverFactory;
import org.apache.commons.lang3.StringUtils;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Handles the landing page.
 */
public class LandingResource extends AbstractResource {

    /**
     * @return Map of template variables common to most or all views, such as
     * variables that appear in a common header.
     */
    public static Map<String, Object> getCommonTemplateVars() {
        Map<String,Object> vars = new HashMap<>();
        // application version
        vars.put("version", Application.getVersion());
        // base URI
        String baseUri = Application.getConfiguration().getString("base_uri", "");
        baseUri = StringUtils.stripEnd(baseUri, "/");
        vars.put("baseUri", baseUri);
        return vars;
    }

    @Override
    protected void doInit() throws ResourceException {
        super.doInit();
        // add a "Cache-Control: no-cache" header because this page contains
        // dynamic information pertaining to the function of the application
        getResponseCacheDirectives().add(CacheDirective.noCache());
    }

    @Get
    public Representation doGet() throws Exception {
        Template template = Velocity.getTemplate("landing.vm");
        return new TemplateRepresentation(template, getTemplateVars(),
                MediaType.TEXT_HTML);
    }

    private Map<String,Object> getTemplateVars() throws Exception {
        Map<String, Object> vars = getCommonTemplateVars();

        // resolver name
        String resolverStr = "None";
        try {
            Resolver resolver = ResolverFactory.getResolver();
            if (resolver != null) {
                resolverStr = resolver.getClass().getSimpleName();
            }
        } catch (Exception e) {
            // noop
        }
        vars.put("resolverName", resolverStr);

        // cache name
        String cacheStr = "Disabled";
        try {
            Cache cache = CacheFactory.getInstance();
            if (cache != null) {
                cacheStr = cache.getClass().getSimpleName();
            }
        } catch (Exception e) {
            // noop
        }
        vars.put("cacheName", cacheStr);

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

        // source format assignments
        Map<SourceFormat,String> assignments = new TreeMap<>();
        for (SourceFormat sourceFormat : SourceFormat.values()) {
            try {
                assignments.put(sourceFormat,
                        ProcessorFactory.getProcessor(sourceFormat).getClass().getSimpleName());
            } catch (UnsupportedSourceFormatException e) {
                // noop
            }
        }
        vars.put("processorAssignments", assignments);

        class SourceFormatComparator implements Comparator<SourceFormat> {
            public int compare(SourceFormat o1, SourceFormat o2) {
                return o1.getName().compareTo(o2.getName());
            }
        }

        // image source formats
        List<SourceFormat> imageFormats = new ArrayList<>();
        for (SourceFormat sourceFormat : SourceFormat.values()) {
            if (sourceFormat.getType() != null &&
                    sourceFormat.getType().equals(SourceFormat.Type.IMAGE)) {
                imageFormats.add(sourceFormat);
            }
        }
        Collections.sort(imageFormats, new SourceFormatComparator());
        vars.put("imageSourceFormats", imageFormats);

        // video source formats
        List<SourceFormat> videoFormats = new ArrayList<>();
        for (SourceFormat sourceFormat : SourceFormat.values()) {
            if (sourceFormat.getType() != null &&
                    sourceFormat.getType().equals(SourceFormat.Type.VIDEO)) {
                videoFormats.add(sourceFormat);
            }
        }
        Collections.sort(videoFormats, new SourceFormatComparator());
        vars.put("videoSourceFormats", videoFormats);

        // processors
        class ProcessorComparator implements Comparator<Processor> {
            public int compare(Processor o1, Processor o2) {
                return o1.getClass().getSimpleName().
                        compareTo(o2.getClass().getSimpleName());
            }
        }
        List<Processor> sortedProcessors =
                new ArrayList<>(ProcessorFactory.getAllProcessors());
        Collections.sort(sortedProcessors, new ProcessorComparator());
        vars.put("processors", sortedProcessors);

        return vars;
    }

}
