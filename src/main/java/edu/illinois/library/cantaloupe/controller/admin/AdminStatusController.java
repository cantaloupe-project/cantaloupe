package edu.illinois.library.cantaloupe.controller.admin;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.auth.BasicAuth;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.resource.EndpointDisabledException;
import edu.illinois.library.cantaloupe.resource.ResourceException;
import edu.illinois.library.cantaloupe.resource.api.TaskMonitor;
import edu.illinois.library.cantaloupe.status.ApplicationStatus;
import edu.illinois.library.cantaloupe.util.TimeUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Spring Boot controller for admin status endpoints.
 * Replaces the previous admin.StatusResource class.
 */
@RestController
@RequestMapping("/admin/status")
public class AdminStatusController {
    private Configuration configuration;
    static final String BASIC_REALM = Application.getName() + " Control Panel";

    @Autowired
    public AdminStatusController(Configuration configuration) {
        this.configuration = configuration;
    }

    /**
     * Provides live status updates for the admin interface.
     * @throws ResourceException 
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getStatus(HttpServletRequest request, HttpServletResponse response) throws ResourceException {
        beforeAll(request, response);
  
        response.setHeader("Content-Type", "application/json;charset=UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        return ResponseEntity.ok(getStatus());
    }

    @RequestMapping(value = "", method = RequestMethod.OPTIONS)
    public ResponseEntity<Void> options(HttpServletRequest request, HttpServletResponse response) throws ResourceException {
        beforeAll(request, response);
        
        return ResponseEntity.noContent()
                .header("Allow", "GET,OPTIONS")
                .build();
    }

    private static final long MEGABYTE = 1024 * 1024;

    @SuppressWarnings("unchecked")
    private Map<String,Object> getStatus() {
        final ApplicationStatus status = new ApplicationStatus(configuration);
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

