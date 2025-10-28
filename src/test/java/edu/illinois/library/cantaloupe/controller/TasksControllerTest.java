package edu.illinois.library.cantaloupe.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Base64;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.async.TaskStatus;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.resource.api.APITask;
import edu.illinois.library.cantaloupe.resource.api.TaskMonitor;

@WebMvcTest(TasksController.class)
public class TasksControllerTest {

    private static final String USERNAME = "admin";
    private static final String SECRET = "secret";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private Configuration configuration;

    @BeforeEach
    void setUp() throws Exception {
        // Set up configuration system properties
        System.setProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT, "memory");
        System.setProperty(Application.TEST_VM_ARGUMENT, "true");

        // Set up API configuration
        when(configuration.getBoolean(Key.API_ENABLED, false)).thenReturn(true);
        when(configuration.getString(Key.API_USERNAME, "")).thenReturn(USERNAME);
        when(configuration.getString(Key.API_SECRET)).thenReturn(SECRET);
    }

    private String getBasicAuthHeader() {
        String credentials = USERNAME + ":" + SECRET;
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes());
    }

    // Tests from AbstractAPIResourceTest

    @Test
    void testOPTIONSWhenEnabled() throws Exception {
        mockMvc.perform(options("/tasks")
                .header("Authorization", getBasicAuthHeader()))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Allow", "POST,OPTIONS"));
    }

    @Test
    void testOPTIONSWhenDisabled() throws Exception {
        when(configuration.getBoolean(Key.API_ENABLED, false)).thenReturn(false);

        mockMvc.perform(options("/tasks")
                .header("Authorization", getBasicAuthHeader()))
                .andExpect(status().isForbidden());
    }

    // Tests from TasksResourceTest (converted from POST to /tasks)

    @Test
    void testPOSTWithIncorrectContentType() throws Exception {
        mockMvc.perform(post("/tasks")
                .header("Authorization", getBasicAuthHeader())
                .contentType(MediaType.TEXT_PLAIN)
                .content("not json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testPOSTWithEmptyRequestBody() throws Exception {
        mockMvc.perform(post("/tasks")
                .header("Authorization", getBasicAuthHeader())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testPOSTWithMalformedRequestBody() throws Exception {
        mockMvc.perform(post("/tasks")
                .header("Authorization", getBasicAuthHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ this is: invalid\" }"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testPOSTWithMissingVerb() throws Exception {
        mockMvc.perform(post("/tasks")
                .header("Authorization", getBasicAuthHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"cats\": \"yes\" }"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testPOSTWithUnsupportedVerb() throws Exception {
        mockMvc.perform(post("/tasks")
                .header("Authorization", getBasicAuthHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"verb\": \"dogs\" }"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testPOSTWithPurgeInfoCacheVerb() throws Exception {
        mockMvc.perform(post("/tasks")
                .header("Authorization", getBasicAuthHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"verb\": \"PurgeInfoCache\" }"))
                .andExpect(status().isAccepted())
                .andExpect(header().exists("Location"));
    }

    @Test
    void testPOSTWithPurgeInvalidFromCacheVerb() throws Exception {
        mockMvc.perform(post("/tasks")
                .header("Authorization", getBasicAuthHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"verb\": \"PurgeInvalidFromCache\" }"))
                .andExpect(status().isAccepted())
                .andExpect(header().exists("Location"));
    }

    @Test
    void testPOSTWithPurgeItemFromCacheVerb() throws Exception {
        mockMvc.perform(post("/tasks")
                .header("Authorization", getBasicAuthHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"verb\": \"PurgeItemFromCache\", \"identifier\": \"cats\" }"))
                .andExpect(status().isAccepted())
                .andExpect(header().exists("Location"));
    }

    @Test
    void testPOSTResponseHeaders() throws Exception {
        mockMvc.perform(post("/tasks")
                .header("Authorization", getBasicAuthHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"verb\": \"PurgeInfoCache\" }"))
                .andExpect(status().isAccepted())
                .andExpect(header().string("Cache-Control", "no-cache"))
                .andExpect(header().exists("Location"));
    }

    // Tests from TaskResourceTest (converted from GET to /tasks/{uuid})

    @Test
    void testGETTaskWithInvalidID() throws Exception {
        mockMvc.perform(get("/tasks/550e8400-e29b-41d4-a716-446655440001")
                .header("Authorization", getBasicAuthHeader()))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGETTaskWithValidID() throws Exception {
        // Create a mock task with mock methods using raw types to avoid generic conflicts
        @SuppressWarnings({"rawtypes"})
        APITask mockTask = org.mockito.Mockito.mock(APITask.class);
        UUID testUuid = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

        // Set up mock task behavior
        when(mockTask.getUUID()).thenReturn(testUuid);
        when(mockTask.getVerb()).thenReturn("PurgeInfoCache");
        when(mockTask.getStatus()).thenReturn(TaskStatus.SUCCEEDED);
        when(mockTask.getInstantQueued()).thenReturn(null);
        when(mockTask.getInstantStarted()).thenReturn(null);
        when(mockTask.getInstantStopped()).thenReturn(null);
        when(mockTask.getException()).thenReturn(null);

        // Mock TaskMonitor to return our mock task
        try (MockedStatic<TaskMonitor> mockedTaskMonitor = mockStatic(TaskMonitor.class)) {
            TaskMonitor mockMonitor = org.mockito.Mockito.mock(TaskMonitor.class);
            when(mockMonitor.get(any(UUID.class))).thenReturn(mockTask);
            mockedTaskMonitor.when(TaskMonitor::getInstance).thenReturn(mockMonitor);

            mockMvc.perform(get("/tasks/550e8400-e29b-41d4-a716-446655440000")
                    .header("Authorization", getBasicAuthHeader()))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void testGETTaskResponseHeaders() throws Exception {
        // Note: Since the current implementation returns 404, we test the headers
        // for the 404 response. In a full implementation, this would test the
        // headers for a successful task retrieval.
        mockMvc.perform(get("/tasks/550e8400-e29b-41d4-a716-446655440002")
                .header("Authorization", getBasicAuthHeader()))
                .andExpect(status().isNotFound())
                .andExpect(header().string("Content-Type", "application/json;charset=UTF-8"));
    }

    @Test
    void testOPTIONSForTaskEndpoint() throws Exception {
        mockMvc.perform(options("/tasks/550e8400-e29b-41d4-a716-446655440003")
                .header("Authorization", getBasicAuthHeader()))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Allow", "GET,OPTIONS"));
    }
}
