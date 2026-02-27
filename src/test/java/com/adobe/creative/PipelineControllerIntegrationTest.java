package com.adobe.creative;

import com.adobe.creative.api.PipelineController;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for {@link PipelineController}.
 * These tests run with the full Spring context loaded.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PipelineControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("GET /api/pipeline/health returns 200 with service info")
    void healthEndpoint_returnsServiceInfo() throws Exception {
        mockMvc.perform(get("/api/pipeline/health"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("creative-automation-pipeline"))
                .andExpect(jsonPath("$.version").value("1.0.0"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("POST /api/pipeline/validate with valid file path returns 200")
    void validateEndpoint_withValidFilePath_returnsValid() throws Exception {
        String requestBody = objectMapper.writeValueAsString(
                Map.of("briefFilePath", "config/campaign_brief.yaml")
        );

        mockMvc.perform(post("/api/pipeline/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.campaignId").exists())
                .andExpect(jsonPath("$.totalAssets").isNumber());
    }

    @Test
    @DisplayName("POST /api/pipeline/validate with missing brief returns 400")
    void validateEndpoint_withMissingBrief_returns400() throws Exception {
        String requestBody = objectMapper.writeValueAsString(
                Map.of("outputDir", "outputs")
        );

        mockMvc.perform(post("/api/pipeline/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("POST /api/pipeline/run with valid brief executes pipeline")
    void runEndpoint_withValidBrief_executesPipeline() throws Exception {
        String requestBody = objectMapper.writeValueAsString(
                Map.of("briefFilePath", "config/campaign_brief.yaml")
        );

        mockMvc.perform(post("/api/pipeline/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.campaignId").exists())
                .andExpect(jsonPath("$.totalAssets").isNumber())
                .andExpect(jsonPath("$.durationSeconds").isNumber())
                .andExpect(jsonPath("$.results").isArray());
    }

    @Test
    @DisplayName("POST /api/pipeline/run with invalid file path returns error")
    void runEndpoint_withInvalidFilePath_returnsError() throws Exception {
        String requestBody = objectMapper.writeValueAsString(
                Map.of("briefFilePath", "nonexistent/file.yaml")
        );

        mockMvc.perform(post("/api/pipeline/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().is5xxServerError())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("OpenAPI documentation is accessible at /v3/api-docs")
    void openApiDocs_accessible() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.openapi").exists())
                .andExpect(jsonPath("$.info.title").value("Creative Automation Pipeline API"));
    }

    @Test
    @DisplayName("Swagger UI is accessible at /swagger-ui/index.html")
    void swaggerUI_accessible() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }
}
