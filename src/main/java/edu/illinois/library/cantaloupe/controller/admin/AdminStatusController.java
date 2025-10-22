package edu.illinois.library.cantaloupe.controller.admin;

import edu.illinois.library.cantaloupe.status.ApplicationStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * Spring Boot controller for admin status endpoints.
 * Replaces the previous admin.StatusResource class.
 */
@RestController
@RequestMapping("/admin/status")
public class AdminStatusController {

    /**
     * Provides live status updates for the admin interface.
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAdminStatus(HttpServletResponse response) {
        response.setHeader("Content-Type", "application/json;charset=UTF-8");

        Map<String, Object> statusMap = new ApplicationStatus().toMap();
        return ResponseEntity.ok(statusMap);
    }

    @RequestMapping(value = "", method = RequestMethod.OPTIONS)
    public ResponseEntity<Void> options() {
        return ResponseEntity.noContent()
                .header("Allow", "GET,OPTIONS")
                .build();
    }
}
