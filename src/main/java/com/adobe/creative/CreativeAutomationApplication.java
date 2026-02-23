package com.adobe.creative;

import com.adobe.creative.model.CampaignBrief;
import com.adobe.creative.model.PipelineRun;
import com.adobe.creative.orchestrator.PipelineOrchestrator;
import com.adobe.creative.parser.CampaignBriefParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Spring Boot application entry point.
 *
 * <p>Two modes of operation:
 * <ol>
 *   <li><b>Server mode (default)</b> – starts on port 8080, exposes REST API</li>
 *   <li><b>CLI mode</b> – if {@code config/campaign_brief.yaml} exists locally,
 *       runs the pipeline once on startup (useful for demo / local development)</li>
 * </ol>
 */
@Slf4j
@SpringBootApplication
@EnableConfigurationProperties
@RequiredArgsConstructor
public class CreativeAutomationApplication {

    public static void main(String[] args) {
        SpringApplication.run(CreativeAutomationApplication.class, args);
    }

    /**
     * On startup: if a default brief file exists, run the pipeline once.
     * This makes the app work as a self-contained CLI tool as well as a server.
     */
    @Bean
    CommandLineRunner onStartup(
            CampaignBriefParser parser,
            PipelineOrchestrator orchestrator) {

        return args -> {
            String defaultBrief = "config/campaign_brief.yaml";
            if (Files.exists(Paths.get(defaultBrief))) {
                log.info("Found default brief at {} – running pipeline …", defaultBrief);
                CampaignBrief brief = parser.parseFromFile(defaultBrief);
                PipelineRun run     = orchestrator.run(brief);
                run.printSummary();
            } else {
                log.info("No default brief found. Server running at http://localhost:8080");
                log.info("POST /api/pipeline/run with {{ \"briefFilePath\": \"...\" }}");
            }
        };
    }
}
