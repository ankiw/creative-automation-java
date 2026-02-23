# Creative Automation Pipeline (Java)

Spring Boot service that parses a campaign brief (YAML), generates creative assets, runs compliance checks, and returns a structured pipeline result.

## How to Run

### Prerequisites

- Java 17+ (project compiles with release 17)
- Maven 3.8+

### Start the service

From project root:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
export PATH="$JAVA_HOME/bin:$PATH"
mvn -Dmaven.test.skip=true spring-boot:run
```

Notes:
- `-Dmaven.test.skip=true` is currently useful because one test does not compile (`toBuilder()` usage in `ImageGeneratorTest`).
- Server starts at `http://localhost:8080`.

### Useful API endpoints

- `GET /api/pipeline/health`
- `POST /api/pipeline/validate`
- `POST /api/pipeline/run`
- `POST /api/pipeline/upload`

## Example Input and Output

### Example input brief

The sample brief file is at `config/campaign_brief.yaml`.

Example excerpt:

```yaml
campaign_id: DEMO_2026_Q1
campaign_name: Spring Product Launch Campaign
target_audience: Millennials and Gen Z
output_dir: outputs
products:
  - id: eco_moisturizer
    name: Eco Glow Moisturizer
markets:
  - region: US
    locale: en_US
aspect_ratios:
  - { name: instagram_square, width: 1080, height: 1080, platform: Instagram }
```

### Example request: validate brief inline

```bash
curl -X POST http://localhost:8080/api/pipeline/validate \
  -H "Content-Type: application/json" \
  --data-raw '{
    "briefYaml":"campaign_id: TEST_001\ncampaign_name: Test Campaign\ntarget_audience: Millennials\noutput_dir: outputs\nproducts:\n  - id: product_a\n    name: Product A\n    category: Skincare\n    tagline: Feel the glow\n    brand_colors:\n      primary: \"#F4A261\"\n      secondary: \"#FFFFFF\"\n      accent: \"#264653\"\n  - id: product_b\n    name: Product B\n    category: Beverage\n    tagline: Feel alive\n    brand_colors:\n      primary: \"#2EC4B6\"\n      secondary: \"#011627\"\n      accent: \"#FF9F1C\"\nmarkets:\n  - region: US\n    locale: en_US\n    campaign_message: \"Shop now.\"\n    prohibited_words: [cure, miracle]\n  - region: DE\n    locale: de_DE\n    campaign_message: \"Jetzt shoppen.\"\naspect_ratios:\n  - { name: square, width: 1080, height: 1080, platform: Instagram }\n  - { name: story, width: 1080, height: 1920, platform: TikTok }\n  - { name: landscape, width: 1920, height: 1080, platform: Facebook }"
  }'
```

Example response:

```json
{
  "valid": true,
  "campaignName": "Test Campaign",
  "aspectRatios": 3,
  "totalAssets": 12,
  "campaignId": "TEST_001",
  "products": 2,
  "markets": 2
}
```

### Example request: run full pipeline from file

```bash
curl -X POST http://localhost:8080/api/pipeline/run \
  -H "Content-Type: application/json" \
  -d '{"briefFilePath":"config/campaign_brief.yaml"}'
```

Expected result shape includes:
- `campaignId`
- `totalAssets`
- `passed`
- `failed`
- `durationSeconds`
- `results` (per-asset output and compliance details)

Generated files are written under `outputs/` by default.

## Key Design Decisions

- Thin controller layer: API handlers in `PipelineController` delegate business logic to orchestrator/parser services.
- Structured error responses: centralized handling through `GlobalExceptionHandler` to avoid leaking stack traces to clients.
- Config-driven providers: image, LLM, and storage backends are selected via `application.yml` (`mock` defaults for local dev).
- Dual startup behavior: app runs as REST service; on startup it can also execute one run automatically when `config/campaign_brief.yaml` exists.

## Assumptions and Limitations

- Root route (`/`) is not implemented as a UI endpoint; use `/api/pipeline/*`.
- App currently assumes local filesystem access for brief files when using `briefFilePath`.
- Default provider settings are mock-based for local testing, not production-grade generation.
- Missing route errors are currently transformed by global exception handling into JSON error responses.
- Automated `mvn test` currently fails because of a test compile issue in `ImageGeneratorTest` (`Product.toBuilder()`).
