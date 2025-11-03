package edu.illinois.library.cantaloupe.controller.iiif.v2;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.delegate.DelegateProxyService;
import edu.illinois.library.cantaloupe.image.FormatRegistry;
import edu.illinois.library.cantaloupe.image.FormatRegistryAccessor;
import edu.illinois.library.cantaloupe.image.MetaIdentifier;
import edu.illinois.library.cantaloupe.image.StandardMetaIdentifierTransformer;
import edu.illinois.library.cantaloupe.resource.InformationRequestHandlerFactory;
import edu.illinois.library.cantaloupe.source.SourceFactory;
import edu.illinois.library.cantaloupe.test.TestUtil;
import edu.illinois.library.cantaloupe.util.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Spring Boot test for IIIF v2 Information Controller using MockMvc.
 * Tests the information endpoint functionality and IIIF 2.x compliance.
 */
@WebMvcTest(InformationController.class)
@Import({FormatRegistry.class, FormatRegistryAccessor.class, DelegateProxyService.class,
         InformationRequestHandlerFactory.class, StringUtils.class, SourceFactory.class})
@TestPropertySource(properties = {
    "cantaloupe.config=test.properties"
})
class InformationControllerTest {

    private static final String IMAGE = "jpg-rgb-64x56x8-baseline.jpg";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private Configuration configuration;

    @BeforeEach
    void setUp() throws Exception {
        // Set up configuration system properties
        ConfigurationFactory.clearInstance();
        System.setProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT, "memory");
        System.setProperty(Application.TEST_VM_ARGUMENT, "true");

        // Mock the default configuration similar to ResourceTest.setUp()
        when(configuration.getBoolean(Key.IIIF_2_ENDPOINT_ENABLED, true)).thenReturn(true);
        when(configuration.getDouble(Key.MAX_SCALE, 0.0)).thenReturn(0.0);
        when(configuration.getBoolean(Key.ADMIN_ENABLED, false)).thenReturn(true);
        when(configuration.getBoolean(Key.DELEGATE_SCRIPT_ENABLED, false)).thenReturn(true);
        when(configuration.getString(Key.DELEGATE_SCRIPT_PATHNAME, "")).thenReturn(TestUtil.getFixture("delegates.rb").toString());
        when(configuration.getString(Key.PROCESSOR_SELECTION_STRATEGY, "")).thenReturn("ManualSelectionStrategy");
        when(configuration.getString("processor.ManualSelectionStrategy.jpg")).thenReturn("Java2dProcessor");
        when(configuration.getString("processor.ManualSelectionStrategy.pdf")).thenReturn("PdfBoxProcessor");
        when(configuration.getString(Key.PROCESSOR_FALLBACK, "")).thenReturn("Java2dProcessor");
        when(configuration.getString(Key.SOURCE_STATIC)).thenReturn("FilesystemSource");
        when(configuration.getString(Key.FILESYSTEMSOURCE_LOOKUP_STRATEGY, "")).thenReturn("BasicLookupStrategy");
        when(configuration.getString(Key.FILESYSTEMSOURCE_PATH_PREFIX, "")).thenReturn(TestUtil.getFixturePath() + "/images/");
        when(configuration.getString(Key.FILESYSTEMSOURCE_PATH_SUFFIX, "")).thenReturn("");
        when(configuration.getString(Key.BASE_URI, "")).thenReturn("");
        when(configuration.getString(Key.SLASH_SUBSTITUTE, "")).thenReturn("");
        when(configuration.getString(Key.DERIVATIVE_CACHE, "")).thenReturn("");
        when(configuration.getString(Key.SOURCE_CACHE, "")).thenReturn("");
        when(configuration.getBoolean(Key.CLIENT_CACHE_ENABLED, false)).thenReturn(false);
    }

    private void stubCacheControlHeaders() {
        when(configuration.getBoolean(Key.CLIENT_CACHE_ENABLED, false)).thenReturn(true);
        when(configuration.getBoolean(Key.CLIENT_CACHE_PUBLIC, true)).thenReturn(true);
        when(configuration.getBoolean(Key.CLIENT_CACHE_PRIVATE, false)).thenReturn(false);
        when(configuration.getBoolean(Key.CLIENT_CACHE_NO_CACHE, false)).thenReturn(false);
        when(configuration.getBoolean(Key.CLIENT_CACHE_NO_STORE, false)).thenReturn(false);
        when(configuration.getBoolean(Key.CLIENT_CACHE_MUST_REVALIDATE, false)).thenReturn(false);
        when(configuration.getBoolean(Key.CLIENT_CACHE_PROXY_REVALIDATE, false)).thenReturn(false);
        when(configuration.getBoolean(Key.CLIENT_CACHE_NO_TRANSFORM, false)).thenReturn(true);
        when(configuration.getString(Key.CLIENT_CACHE_MAX_AGE, "")).thenReturn("1234");
        when(configuration.getString(Key.CLIENT_CACHE_SHARED_MAX_AGE, "")).thenReturn("4567");
    }


    @Test
    void testGetInformationBasic() throws Exception {
        mockMvc.perform(get("/iiif/2/{identifier}/info.json", IMAGE))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$['@context']", is("http://iiif.io/api/image/2/context.json")))
                .andExpect(jsonPath("$.protocol", is("http://iiif.io/api/image")))
                .andExpect(jsonPath("$.width").exists())
                .andExpect(jsonPath("$.height").exists());
    }

    @Test
    void testGetInformationWithJsonLdContentType() throws Exception {
        mockMvc.perform(get("/iiif/2/{identifier}/info.json", IMAGE)
                .accept("application/ld+json"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/ld+json;charset=UTF-8"))
                .andExpect(jsonPath("$['@context']", is("http://iiif.io/api/image/2/context.json")));
    }

    @Test
    void testGetInformationAuthorizationWhenUnauthorized() throws Exception {
        mockMvc.perform(get("/iiif/2/unauthorized.jpg/info.json"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$['@context']", is("http://iiif.io/api/image/2/context.json")))
                .andExpect(jsonPath("$.protocol", is("http://iiif.io/api/image")))
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.message", is("Unauthorized")))
                .andExpect(jsonPath("$.attribution", is("Copyright My Great Organization. All rights reserved.")))
                .andExpect(jsonPath("$.license", is("http://example.org/license.html")))
                .andExpect(jsonPath("$.service['@context']", is("http://iiif.io/api/annex/services/physdim/1/context.json")))
                .andExpect(jsonPath("$.service.profile", is("http://iiif.io/api/annex/services/physdim")))
                .andExpect(jsonPath("$.service.physicalScale", is(0.0025)))
                .andExpect(jsonPath("$.service.physicalUnits", is("in")));
    }

    @Test
    void testGetInformationAuthorizationWhenForbidden() throws Exception {
        mockMvc.perform(get("/iiif/2/forbidden.jpg/info.json"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)));
    }

    @Test
    void testGetInformationEndpointEnabled() throws Exception {
        when(configuration.getBoolean(Key.IIIF_2_ENDPOINT_ENABLED, true)).thenReturn(true);

        mockMvc.perform(get("/iiif/2/{identifier}/info.json", IMAGE))
                .andExpect(status().isOk());
    }

    @Test
    void testGetInformationEndpointDisabled() throws Exception {
        when(configuration.getBoolean(Key.IIIF_2_ENDPOINT_ENABLED, true)).thenReturn(false);

        mockMvc.perform(get("/iiif/2/{identifier}/info.json", IMAGE))
                .andExpect(status().isForbidden());
    }

    @Test
    void testGetInformationWithForwardSlashInIdentifier() throws Exception {
        // override the filesystem prefix to one folder level up so we can use
        // a slash in the identifier
        File directory = new File(".");
        String cwd = directory.getCanonicalPath();
        Path path = Paths.get(cwd, "src", "test", "resources");
        when(configuration.getString(Key.FILESYSTEMSOURCE_PATH_PREFIX, "")).thenReturn(path + File.separator);

        final String identifier = "images%2F" + IMAGE;

        mockMvc.perform(get("/iiif/2/{identifier}/info.json", identifier))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['@context']", is("http://iiif.io/api/image/2/context.json")));
    }

    @Test
    void testGetInformationWithBackslashInIdentifier() throws Exception {
        // override the filesystem prefix to one folder level up so we can use
        // a backslash in the identifier
        File directory = new File(".");
        String cwd = directory.getCanonicalPath();
        Path path = Paths.get(cwd, "src", "test", "resources");
        when(configuration.getString(Key.FILESYSTEMSOURCE_PATH_PREFIX, "")).thenReturn(path + File.separator);

        final String identifier = "images%5C" + IMAGE;

        mockMvc.perform(get("/iiif/2/{identifier}/info.json", identifier))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['@context']", is("http://iiif.io/api/image/2/context.json")));
    }

    @Test
    void testGetInformationNotFound() throws Exception {
        mockMvc.perform(get("/iiif/2/bogus/info.json"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));
    }

    @Test
    void testGetInformationWithCacheHeaders() throws Exception {
        stubCacheControlHeaders();

        mockMvc.perform(get("/iiif/2/{identifier}/info.json", IMAGE))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "max-age=1234, s-maxage=4567, public, no-transform"));
    }

    @Test
    void testGetInformationWithCachingDisabled() throws Exception {
        when(configuration.getBoolean(Key.CLIENT_CACHE_ENABLED, false)).thenReturn(false);

        mockMvc.perform(get("/iiif/2/{identifier}/info.json", IMAGE))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String cacheControl = result.getResponse().getHeader("Cache-Control");
                    if (cacheControl != null) {
                        assert !cacheControl.contains("max-age");
                    }
                });
    }

    @Test
    void testGetInformationWithCacheQueryParameter() throws Exception {
        when(configuration.getBoolean(Key.CLIENT_CACHE_ENABLED, false)).thenReturn(true);

        mockMvc.perform(get("/iiif/2/{identifier}/info.json?cache=nocache", IMAGE))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String cacheControl = result.getResponse().getHeader("Cache-Control");
                    if (cacheControl != null) {
                        assert !cacheControl.contains("max-age");
                    }
                });
    }

    @Test
    void testGetInformationWithCacheRecacheQueryParameter() throws Exception {
        stubCacheControlHeaders();

        mockMvc.perform(get("/iiif/2/{identifier}/info.json?cache=recache", IMAGE))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "max-age=1234, s-maxage=4567, public, no-transform"));
    }

    @Test
    void testGetInformationCorsHeaders() throws Exception {
        mockMvc.perform(get("/iiif/2/{identifier}/info.json", IMAGE))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "*"));
    }

    @Test
    void testGetInformationWithPageIndex() throws Exception {
        final String multiPageImage = "pdf-multipage.pdf";

        mockMvc.perform(get("/iiif/2/{identifier}/info.json", multiPageImage))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['@context']", is("http://iiif.io/api/image/2/context.json")))
                .andExpect(jsonPath("$.protocol", is("http://iiif.io/api/image")));
    }

    /**
     * Tests that a scale constraint of {@literal 1:1} is redirected to no
     * scale constraint.
     * @throws Exception
     */
    @Test
    void testGetInformation_RedirectToNormalizedScaleConstraint1() throws Exception {
        MetaIdentifier metaIdentifier = MetaIdentifier.builder()
                .withIdentifier(IMAGE)
                .withScaleConstraint(1, 1)
                .build();
        String metaIdentifierString = new StandardMetaIdentifierTransformer()
                .serialize(metaIdentifier, false);

        mockMvc.perform(get("/iiif/2/{identifier}/info.json", metaIdentifierString))
                .andExpect(redirectedUrl("http://localhost/iiif/2/" + IMAGE + "/info.json"));
    }

    @Test
    void testOptionsInformation() throws Exception {
        mockMvc.perform(options("/iiif/2/{identifier}/info.json", IMAGE))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Allow", "GET,OPTIONS"))
                .andExpect(header().string("Access-Control-Allow-Origin", "*"));
    }

    @Test
    void testOptionsInformationEndpointDisabled() throws Exception {
        when(configuration.getBoolean(Key.IIIF_2_ENDPOINT_ENABLED, true)).thenReturn(false);

        mockMvc.perform(options("/iiif/2/{identifier}/info.json", IMAGE))
                .andExpect(status().isForbidden());
    }

    @Test
    void testGetInformationWithLastModifiedHeader() throws Exception {
        mockMvc.perform(get("/iiif/2/{identifier}/info.json", IMAGE))
                .andExpect(status().isOk())
                .andExpect(header().exists("Last-Modified"));
    }

    @Test
    void testGetInformationJsonFormat() throws Exception {
        mockMvc.perform(get("/iiif/2/{identifier}/info.json", IMAGE))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$['@context']").exists())
                .andExpect(jsonPath("$['@id']").exists())
                .andExpect(jsonPath("$.protocol").exists())
                .andExpect(jsonPath("$.width").exists())
                .andExpect(jsonPath("$.height").exists())
                .andExpect(jsonPath("$.profile").exists());
    }

    @Test
    void testGetInformationWithEmptyProfile() throws Exception {
        mockMvc.perform(get("/iiif/2/{identifier}/info.json", IMAGE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile").isArray());
    }

    @Test
    void testGetInformationComplianceLevel() throws Exception {
        mockMvc.perform(get("/iiif/2/{identifier}/info.json", IMAGE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile[0]", containsString("http://iiif.io/api/image/2/level")));
    }
}
