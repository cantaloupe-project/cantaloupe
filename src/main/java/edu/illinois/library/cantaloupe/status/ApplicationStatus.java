package edu.illinois.library.cantaloupe.status;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.cache.InfoCache;
import edu.illinois.library.cantaloupe.cache.InfoService;

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
     * @return Number of available processor cores.
     */
    public int getNumProcessors() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.availableProcessors();
    }

    /**
     * @return Free VM heap in bytes.
     */
    public long getVMFreeHeap() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.freeMemory();
    }

    /**
     * @return JVM info.
     */
    public String getVMInfo() {
        return System.getProperty("java.vm.info");
    }

    /**
     * @return Max VM heap in bytes.
     */
    public long getVMMaxHeap() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.maxMemory();
    }

    /**
     * @return JVM name.
     */
    public String getVMName() {
        return System.getProperty("java.vm.name");
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

    /**
     * @return JVM vendor.
     */
    public String getVMVendor() {
        return System.getProperty("java.vendor");
    }

    /**
     * @return JVM version.
     */
    public String getVMVersion() {
        return System.getProperty("java.version");
    }

    public Map<String,Object> toMap() {
        final Map<String,Object> status = new LinkedHashMap<>();

        { // Application
            var section = new LinkedHashMap<>();
            section.put("version", getApplicationVersion());
            status.put("application", section);
        }
        { // Info cache
            var section = new LinkedHashMap<>();
            section.put("size", getInfoCacheSize());
            section.put("maxSize", getInfoCacheMaxSize());
            status.put("infoCache", section);
        }
        { // VM
            var section = new LinkedHashMap<>();
            section.put("vendor", getVMVendor());
            section.put("name", getVMName());
            section.put("version", getVMVersion());
            section.put("info", getVMInfo());
            section.put("numProcessors", getNumProcessors());
            section.put("usedHeapBytes", getVMTotalHeap() - getVMFreeHeap());
            section.put("freeHeapBytes", getVMFreeHeap());
            section.put("totalHeapBytes", getVMTotalHeap());
            section.put("maxHeapBytes", getVMMaxHeap());
            section.put("usedHeapPercent", getVMUsedHeap() / (double) getVMMaxHeap());
            section.put("uptimeMsec", getVMUptime());
            status.put("vm", section);
        }
        return status;
    }

}
