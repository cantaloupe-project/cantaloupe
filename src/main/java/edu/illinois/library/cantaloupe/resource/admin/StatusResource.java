package edu.illinois.library.cantaloupe.resource.admin;

import edu.illinois.library.cantaloupe.cache.InfoCache;
import edu.illinois.library.cantaloupe.cache.InfoService;
import edu.illinois.library.cantaloupe.http.Method;
import edu.illinois.library.cantaloupe.resource.JacksonRepresentation;
import edu.illinois.library.cantaloupe.script.DelegateProxy;
import edu.illinois.library.cantaloupe.script.InvocationCache;
import edu.illinois.library.cantaloupe.util.TimeUtils;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.HashMap;
import java.util.Map;

/**
 * Provides live status updates to the Control Panel via XHR.
 */
public class StatusResource extends AbstractAdminResource {

    static class Info {

        private static final long MEGABYTE = 1024 * 1024;

        public final Map<String,Object> delegateMethodInvocationCache = new HashMap<>();
        public final Map<String,Object> infoCache = new HashMap<>();
        public final Map<String,Object> vm = new HashMap<>();

        public Info() {
            try {
                InvocationCache cache = DelegateProxy.getInvocationCache();
                delegateMethodInvocationCache.put("size", cache.size());
                delegateMethodInvocationCache.put("maxSize", cache.maxSize());
            } catch (Exception e) {
                // If this is significant it will be noticed & handled
                // elsewhere.
            }

            InfoCache infoCache = InfoService.getInstance().getInfoCache();
            this.infoCache.put("size", infoCache.size());
            this.infoCache.put("maxSize", infoCache.maxSize());

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

    private static final Method[] SUPPORTED_METHODS =
            new Method[] { Method.GET, Method.OPTIONS };

    @Override
    public Method[] getSupportedMethods() {
        return SUPPORTED_METHODS;
    }

    @Override
    public void doGET() throws IOException {
        getResponse().setHeader("Content-Type",
                "application/json;charset=UTF-8");

        new JacksonRepresentation(new Info())
                .write(getResponse().getOutputStream());
    }

}
