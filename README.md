# Creative Automation Pipeline

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.4-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Maven](https://img.shields.io/badge/Maven-3.6+-blue.svg)](https://maven.apache.org/)

A Spring Boot microservice that automates social media ad campaign creation using AI-driven workflows.

## Overview

Marketing teams need to create dozens of ad variations for campaigns across multiple products, markets, and platforms. This pipeline automates that process: input a YAML campaign brief, output compliance-checked, localized creatives in seconds.

**Example:** 2 products × 3 markets × 3 formats = **18 unique creatives** in **~7 seconds**

## Project Documentation

- Full documentation with architecture and flow diagrams: `docs/PROJECT_DOCUMENTATION.md`

## Features

- 🚀 **REST API** for programmatic access
- 📚 **OpenAPI/Swagger Documentation** at `/swagger-ui.html`
- 🎨 **AI Image Generation** (OpenAI, Firefly, Stability AI)
- 🤖 **LLM Integration** for AI-powered ad copy & content generation
- 🌍 **Multi-Market Support** with localized messaging
- ✅ **Compliance Validation** for market regulations (rule-based + AI-powered)
- 📊 **Audit Reports** in JSON format
- 🔌 **Pluggable Architecture** for easy provider swaps
- 🏥 **Health Monitoring** via Spring Actuator
- 🐳 **Docker Support** with multi-stage builds
- 🔄 **CI/CD Pipeline** with GitHub Actions
- ✨ **Request Validation** with bean validation

## Quick Start

### Prerequisites
- Java 17+
- Maven 3.6+
- Docker (optional, for containerized deployment)

### Option 1: Run with Docker (Recommended)

```bash
# Build and start with docker-compose
docker-compose up -d

# View logs
docker-compose logs -f

# Stop
docker-compose down
```

The application starts on `http://localhost:8080`

### Option 2: Build & Run Locally

```bash
# Set Java 17
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
export PATH="$JAVA_HOME/bin:$PATH"

# Build
mvn clean package -DskipTests

# Run on default port 8080
java -jar target/creative-automation-1.0.0.jar

# Or run on a different port (e.g., 8081 if 8080 is in use)
java -jar target/creative-automation-1.0.0.jar --server.port=8081
```

The application starts on `http://localhost:8080` (or your specified port)

Alternatively, run with Maven:
```bash
# Default port 8080
mvn spring-boot:run

# Custom port (recommended if 8080 is in use)
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

### Configuration

Create `config/campaign_brief.yaml`:

```yaml
campaign_id: DEMO_2026_Q1
campaign_name: Spring Product Launch
target_audience: Millennials and Gen Z
output_dir: outputs

products:
  - id: eco_moisturizer
    name: Eco Glow Moisturizer
    category: Skincare
    tagline: Radiance from nature
    brand_colors:
      primary: "#2EC4B6"
      secondary: "#FFFFFF"
      accent: "#FF9F1C"

markets:
  - region: US
    locale: en_US
    campaign_message: "Shop now and save 20%!"
    prohibited_words: [cure, miracle, guaranteed]

aspect_ratios:
  - { name: instagram_square, width: 1080, height: 1080, platform: Instagram }
  - { name: instagram_story,  width: 1080, height: 1920, platform: Instagram }
  - { name: facebook_feed,    width: 1200, height: 628,  platform: Facebook }
```

## API Documentation

**Interactive API docs:** http://localhost:8080/swagger-ui.html
**OpenAPI spec:** http://localhost:8080/v3/api-docs

> **💡 Note:** If port 8080 is already in use, replace `8080` with your chosen port (e.g., `8081`)

## LLM & AI Features 🤖

The pipeline now includes comprehensive LLM integration for AI-powered content:

### Ad Copy Generation
```bash
curl -X POST http://localhost:8080/api/llm/copy/generate \
  -H "Content-Type: application/json" \
  -d '{"product": {...}, "market": {...}, "tone": "professional"}'
```

### Creative Variations (A/B Testing)
```bash
curl -X POST http://localhost:8080/api/llm/copy/variations \
  -H "Content-Type: application/json" \
  -d '{"product": {...}, "market": {...}, "numVariations": 3}'
```

### Image Prompt Enhancement
```bash
curl -X POST http://localhost:8080/api/llm/prompt/enhance \
  -H "Content-Type: application/json" \
  -d '{"product": {...}, "platform": "Instagram"}'
```

### AI-Powered Compliance Analysis
```bash
curl -X POST http://localhost:8080/api/llm/compliance/analyze \
  -H "Content-Type: application/json" \
  -d '{"copy": "Your ad text", "market": {...}}'
```

**Full LLM Documentation:** [docs/LLM_INTEGRATION.md](docs/LLM_INTEGRATION.md)

---

## API Endpoints

### Health Check
```bash
curl http://localhost:8080/api/pipeline/health
```

Response:
```json
{
  "status": "UP",
  "service": "creative-automation-pipeline",
  "version": "1.0.0",
  "timestamp": "2026-02-23T21:15:22.245689Z"
}
```

### Validate Brief
```bash
curl -X POST http://localhost:8080/api/pipeline/validate \
  -H "Content-Type: application/json" \
  -d '{"briefFilePath": "config/campaign_brief.yaml"}'
```

Response:
```json
{
  "valid": true,
  "campaignId": "DEMO_2026_Q1",
  "campaignName": "Spring Product Launch Campaign",
  "products": 2,
  "markets": 3,
  "aspectRatios": 3,
  "totalAssets": 18
}
```

### Run Pipeline
```bash
curl -X POST http://localhost:8080/api/pipeline/run \
  -H "Content-Type: application/json" \
  -d '{"briefFilePath": "config/campaign_brief.yaml"}'
```

Response:
```json
{
  "campaignId": "DEMO_2026_Q1",
  "totalAssets": 18,
  "passed": 18,
  "failed": 0,
  "durationSeconds": 6.834,
  "results": [
    {
      "productId": "eco_moisturizer",
      "region": "US",
      "aspectRatio": "instagram_square",
      "outputPath": "outputs/eco_moisturizer/US/instagram_square.png",
      "compliance": {
        "passed": true,
        "warnings": [],
        "errors": []
      },
      "durationMs": 406
    }
    // ... 17 more results
  ]
}
```

### Upload Brief
```bash
curl -X POST http://localhost:8080/api/pipeline/upload \
  -F "brief=@config/campaign_brief.yaml"
```

## Architecture

```
REST API → Pipeline Orchestrator → Image Generator
                                 → Creative Composer
                                 → Compliance Checker
                                 → Output Storage
```

### Key Components

| Component | Purpose | Location |
|-----------|---------|----------|
| **PipelineController** | REST API endpoints | `api/PipelineController.java` |
| **PipelineOrchestrator** | Workflow coordination | `orchestrator/PipelineOrchestrator.java` |
| **ImageGenerator** | AI image generation (pluggable) | `orchestrator/ImageGenerator.java` |
| **CreativeComposer** | Brand overlay & composition | `orchestrator/CreativeComposer.java` |
| **ComplianceChecker** | Regulatory validation | `compliance/ComplianceChecker.java` |

## Output

Generated assets are organized by product and market:

```
outputs/
├── run_report.json              # Audit trail
├── eco_moisturizer/
│   ├── US/
│   │   ├── instagram_square.png
│   │   ├── instagram_story.png
│   │   └── facebook_feed.png
│   ├── UK/
│   └── DE/
└── energy_drink/
    ├── US/
    ├── UK/
    └── DE/
```

**run_report.json** includes:
- Total assets generated
- Compliance pass/fail
- Execution time
- Individual asset details

## Configuration Options

Edit `src/main/resources/application.yml`:

```yaml
server:
  port: 8080

pipeline:
  image-provider: mock         # mock | openai | firefly | stability
  llm-provider: mock           # mock | openai | anthropic
  storage-provider: local      # local | s3 | azure
  output-dir: outputs

  # API Keys (use environment variables in production)
  openai-api-key: ${OPENAI_API_KEY:}
  anthropic-api-key: ${ANTHROPIC_API_KEY:}
  stability-api-key: ${STABILITY_API_KEY:}
  firefly-client-id: ${FIREFLY_CLIENT_ID:}
  firefly-client-secret: ${FIREFLY_CLIENT_SECRET:}
```

## Development

### Run Tests
```bash
mvn test
```

### Build Without Tests
```bash
mvn clean package -DskipTests
```

### Run with Different Port
```bash
java -jar target/creative-automation-1.0.0.jar --server.port=8081
```

## Project Structure

```
src/
├── main/
│   ├── java/com/adobe/creative/
│   │   ├── CreativeAutomationApplication.java    # Main entry point
│   │   ├── api/                                  # REST controllers
│   │   │   ├── PipelineController.java
│   │   │   ├── RunPipelineRequest.java
│   │   │   └── GlobalExceptionHandler.java
│   │   ├── orchestrator/                         # Pipeline logic
│   │   │   ├── PipelineOrchestrator.java
│   │   │   ├── ImageGenerator.java
│   │   │   └── CreativeComposer.java
│   │   ├── parser/                               # YAML parsing
│   │   │   └── CampaignBriefParser.java
│   │   ├── compliance/                           # Validation
│   │   │   └── ComplianceChecker.java
│   │   ├── model/                                # Data models
│   │   │   ├── CampaignBrief.java
│   │   │   ├── Product.java
│   │   │   ├── Market.java
│   │   │   └── ...
│   │   └── config/                               # Configuration
│   │       └── PipelineConfig.java
│   └── resources/
│       └── application.yml
└── test/java/                                     # Unit tests
```

## Technology Stack

- **Java 17** - Modern Java features
- **Spring Boot 3.2.4** - Microservice framework
- **Maven** - Build management
- **Lombok** - Boilerplate reduction
- **Jackson** - YAML/JSON processing
- **Spring Actuator** - Health & metrics

## Design Patterns

- **Builder Pattern** - Clean object creation
- **Strategy Pattern** - Pluggable providers
- **Dependency Injection** - Spring-managed components
- **Template Method** - Pipeline workflow

## Key Design Decisions

1. **Thin Controller Layer** - Business logic delegated to orchestrator/services
2. **Hero Image Optimization** - Generated once per product, reused across markets (9x efficiency)
3. **Structured Error Responses** - `GlobalExceptionHandler` prevents stack trace leaks
4. **Config-Driven Providers** - Easy switching between mock/real AI services
5. **Dual Mode Operation** - Works as REST service or CLI tool
6. **Compliance-First** - Every asset validated before delivery

## Performance

- **Hero Image Optimization:** Generated once per product, reused across markets/formats
- **Example:** 2-product campaign needs 2 API calls, not 18 (9x reduction)
- **Scalability:** Sequential for POC; production uses parallel processing

## Production Enhancements

Current implementation includes Docker, CI/CD, and comprehensive testing. Additional production improvements:

- [ ] Parallel processing with `ExecutorService`
- [ ] Message queue integration (RabbitMQ/Kafka)
- [ ] Cloud storage (S3/Azure Blob)
- [ ] Database for campaign history
- [ ] Authentication/Authorization (OAuth2/JWT)
- [ ] Rate limiting
- [ ] Caching layer (Redis)
- [ ] Kubernetes deployment manifests
- [x] ✅ Docker containerization
- [x] ✅ CI/CD pipeline with GitHub Actions

## Assumptions and Limitations

- Default provider settings use mock generation (not production-grade)
- Assumes local filesystem access for brief files with `briefFilePath`
- Root route (`/`) not implemented; use `/api/pipeline/*` endpoints
- Sequential processing in POC; production would use parallel execution

## Troubleshooting

### Port Already in Use

If you see `Port 8080 is already in use`, you have two options:

**Option 1: Stop the conflicting application**
```bash
# Find what's using port 8080
lsof -i :8080

# Kill the process (use the PID from above)
kill -9 <PID>
```

**Option 2: Run on a different port (Recommended)**
```bash
# Run on port 8081 with JAR
java -jar target/creative-automation-1.0.0.jar --server.port=8081

# Or with Maven
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

Then access at `http://localhost:8081`

### Java Version Issues
```bash
# Verify Java 17 is active
java --version

# Set Java 17
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
```

## License

MIT License

## Author

Built as a demonstration of modern microservice architecture with Spring Boot.
