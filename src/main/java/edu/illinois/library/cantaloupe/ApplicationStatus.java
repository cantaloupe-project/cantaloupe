package edu.illinois.library.cantaloupe;

import edu.illinois.library.cantaloupe.cache.InfoCache;
import edu.illinois.library.cantaloupe.cache.InfoService;
import edu.illinois.library.cantaloupe.script.DelegateProxy;
import edu.illinois.library.cantaloupe.script.InvocationCache;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Provides views into various application status data points. All accessors
 * provide "live" values.
 */
public final class ApplicationStatus {

    public String getApplicationVersion() {
        return Application.getVersion();
    }

    /**
     * @return Max delegate method invocation cache size.
     */
    public long getDMICMaxSize() {
        InvocationCache cache = DelegateProxy.getInvocationCache();
        return cache.maxSize();
    }

    /**
     * @return Current delegate method invocation cache size.
     */
    public long getDMICSize() {
        InvocationCache cache = DelegateProxy.getInvocationCache();
        return cache.size();
    }

    /**
     * @return Max {@link InfoCache} size in bytes.
     */
    public long getInfoCacheMaxSize() {
        InfoCache infoCache = InfoService.getInstance().getInfoCache();
        return infoCache.maxSize();
    }

    /**
     * @return Number of {@link InfoCache cached infos}.
     */
    public long getInfoCacheSize() {
        InfoCache infoCache = InfoService.getInstance().getInfoCache();
        return infoCache.size();
    }

    /**
     * @return Free VM heap in bytes.
     */
    public long getVMFreeHeap() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.freeMemory();
    }

    /**
     * @return Max VM heap in bytes.
     */
    public long getVMMaxHeap() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.maxMemory();
    }

    /**
     * @return Total VM heap in bytes.
     */
    public long getVMTotalHeap() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory();
    }

    /**
     * @return VM uptime in epoch milliseconds.
     */
    public long getVMUptime() {
        RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
        return runtimeMxBean.getUptime();
    }

    /**
     * @return Used VM heap in bytes.
     */
    public long getVMUsedHeap() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    public Map<String,Object> toMap() {
        final Map<String,Object> status = new LinkedHashMap<>();

        // Application
        Map<String,Object> section = new LinkedHashMap<>();
        section.put("version", getApplicationVersion());
        status.put("application", section);

        // DMIC
        section = new LinkedHashMap<>();
        section.put("size", getDMICSize());
        section.put("maxSize", getDMICMaxSize());
        status.put("delegateMethodInvocationCache", section);

        // Info cache
        section = new LinkedHashMap<>();
        section.put("size", getInfoCacheSize());
        section.put("maxSize", getInfoCacheMaxSize());
        status.put("infoCache", section);

        // Memory
        section = new LinkedHashMap<>();
        section.put("usedHeapBytes", getVMTotalHeap() - getVMFreeHeap());
        section.put("freeHeapBytes", getVMFreeHeap());
        section.put("totalHeapBytes", getVMTotalHeap());
        section.put("maxHeapBytes", getVMMaxHeap());
        section.put("usedHeapPercent", getVMUsedHeap() / (double) getVMMaxHeap());
        section.put("uptimeMsec", getVMUptime());
        status.put("vm", section);

        return status;
    }

}
