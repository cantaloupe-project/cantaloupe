package edu.illinois.library.cantaloupe.resource.admin;

import edu.illinois.library.cantaloupe.cache.InfoService;
import edu.illinois.library.cantaloupe.resource.JSONRepresentation;
import edu.illinois.library.cantaloupe.script.ScriptEngineFactory;
import edu.illinois.library.cantaloupe.util.TimeUtils;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.HashMap;
import java.util.Map;

/**
 * Provides live status updates to the Control Panel via XHR.
 */
public class StatusResource extends AbstractAdminResource {

    static class Status {

        private static final long MEGABYTE = 1024 * 1024;

        public final Map<String,Object> delegateMethodInvocationCache = new HashMap<>();
        public final Map<String,Object> infoCache = new HashMap<>();
        public final Map<String,Object> vm = new HashMap<>();

        public Status() {
            try {
                delegateMethodInvocationCache.put("size",
                        ScriptEngineFactory.getScriptEngine().getInvocationCache().size());
                delegateMethodInvocationCache.put("maxSize",
                        ScriptEngineFactory.getScriptEngine().getInvocationCache().maxSize());
            } catch (Exception e) {
                // Not a big deal; if there's a significant problem it will be
                // handled elsewhere.
            }

            this.infoCache.put("size",
                    InfoService.getInstance().getObjectCacheSize());
            this.infoCache.put("maxSize",
                    InfoService.getInstance().getObjectCacheMaxSize());

            Runtime runtime = Runtime.getRuntime();
            RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
            this.vm.put("usedHeap", (runtime.totalMemory() - runtime.freeMemory()) / MEGABYTE);
            this.vm.put("freeHeap", runtime.freeMemory() / MEGABYTE);
            this.vm.put("totalHeap", runtime.totalMemory() / MEGABYTE);
            this.vm.put("maxHeap", runtime.maxMemory() / MEGABYTE);
            this.vm.put("usedPercent", (runtime.totalMemory() - runtime.freeMemory()) /
                    (double) runtime.maxMemory());
            this.vm.put("uptime",
                    TimeUtils.millisecondsToHumanTime(runtimeMxBean.getUptime()));
        }
    }

    @Get("json")
    public Representation getConfiguration() throws Exception {
        return new JSONRepresentation(new Status());
    }

}
