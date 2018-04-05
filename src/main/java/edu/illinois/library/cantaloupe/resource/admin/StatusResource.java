package edu.illinois.library.cantaloupe.resource.admin;

import edu.illinois.library.cantaloupe.cache.InfoService;
import edu.illinois.library.cantaloupe.resource.JSONRepresentation;
import edu.illinois.library.cantaloupe.script.DelegateProxy;
import edu.illinois.library.cantaloupe.script.InvocationCache;
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
                InvocationCache cache = DelegateProxy.getInvocationCache();
                delegateMethodInvocationCache.put("size", cache.size());
                delegateMethodInvocationCache.put("maxSize", cache.maxSize());
            } catch (Exception e) {
                // If this is significant it will be noticed & handled
                // elsewhere.
            }

            this.infoCache.put("size",
                    InfoService.getInstance().getInfoCache().size());
            this.infoCache.put("maxSize",
                    InfoService.getInstance().getInfoCache().maxSize());

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
    public Representation getConfiguration() {
        return new JSONRepresentation(new Status());
    }

}
