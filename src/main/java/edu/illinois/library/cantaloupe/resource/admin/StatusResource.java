package edu.illinois.library.cantaloupe.resource.admin;

import edu.illinois.library.cantaloupe.ApplicationStatus;
import edu.illinois.library.cantaloupe.http.Method;
import edu.illinois.library.cantaloupe.resource.JacksonRepresentation;
import edu.illinois.library.cantaloupe.resource.api.TaskMonitor;
import edu.illinois.library.cantaloupe.util.TimeUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Provides live status updates to the Control Panel via XHR.
 */
public class StatusResource extends AbstractAdminResource {

    private static final long MEGABYTE = 1024 * 1024;

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
        new JacksonRepresentation(getStatus())
                .write(getResponse().getOutputStream());
    }

    @SuppressWarnings("unchecked")
    private Map<String,Object> getStatus() {
        final ApplicationStatus status = new ApplicationStatus();
        final Map<String,Object> map = new HashMap<>(status.toMap());

        // Reformat various values for human consumption
        Map<String,Object> vmSection = (Map<String, Object>) map.get("vm");
        vmSection.put("uptime", TimeUtils.millisecondsToHumanTime(status.getVMUptime()));
        vmSection.put("usedHeapBytes", Math.round(status.getVMUsedHeap() / (double) MEGABYTE));
        vmSection.put("freeHeapBytes", Math.round(status.getVMFreeHeap() / (double) MEGABYTE));
        vmSection.put("totalHeapBytes", Math.round(status.getVMTotalHeap() / (double) MEGABYTE));
        vmSection.put("maxHeapBytes", Math.round(status.getVMMaxHeap() / (double) MEGABYTE));

        // Add tasks section
        map.put("tasks", TaskMonitor.getInstance().getAll());

        return map;
    }

}
