# LLM Integration Guide

## Overview

The Creative Automation Pipeline now includes comprehensive LLM (Large Language Model) integration for AI-powered content generation and analysis.

## Features

### 🎨 **Ad Copy Generation**
- Market-specific messaging
- Tone control (professional, playful, urgent, elegant)
- Compliance-aware (respects prohibited words)
- Character limit enforcement

### 🖼️ **Image Prompt Enhancement**
- Transforms basic descriptions into detailed prompts
- Includes lighting, composition, and style guidance
- Optimized for different platforms (Instagram, Facebook, etc.)

### 🌍 **Translation & Localization**
- Culturally adapts copy for different markets
- Maintains tone and marketing effectiveness
- Respects regional regulations

### ✅ **AI-Powered Compliance Analysis**
- Goes beyond keyword matching
- Detects implied claims and sensitivities
- Provides fix suggestions

### 🔄 **Creative Variations**
- Generates A/B test variations automatically
- Tests different psychological triggers
- Maintains brand consistency

---

## Configuration

### Application Configuration

Edit `src/main/resources/application.yml`:

```yaml
pipeline:
  llm-provider: mock         # mock | openai | anthropic
  openai-api-key: ${OPENAI_API_KEY:}
  anthropic-api-key: ${ANTHROPIC_API_KEY:}
```

### Environment Variables

**For OpenAI:**
```bash
export OPENAI_API_KEY="sk-..."
```

**For Anthropic:**
```bash
export ANTHROPIC_API_KEY="sk-ant-..."
```

### Provider Options

| Provider | Models | Best For |
|----------|--------|----------|
| **mock** | N/A | Testing, CI/CD, development without API costs |
| **openai** | GPT-4, GPT-4 Turbo | General purpose, structured output |
| **anthropic** | Claude Sonnet/Opus 4.6 | Long-form content, analysis, compliance |

---

## API Endpoints

### Health Check
```bash
curl http://localhost:8080/api/llm/health
```

### Generate Ad Copy
```bash
curl -X POST http://localhost:8080/api/llm/copy/generate \
  -H "Content-Type: application/json" \
  -d '{
    "product": {
      "id": "eco_moisturizer",
      "name": "Eco Glow Moisturizer",
      "category": "Skincare",
      "tagline": "Radiance from nature",
      "brandColors": {
        "primary": "#2EC4B6",
        "secondary": "#FFFFFF",
        "accent": "#FF9F1C"
      }
    },
    "market": {
      "region": "US",
      "locale": "en_US",
      "campaignMessage": "",
      "prohibitedWords": ["cure", "miracle", "guaranteed"]
    },
    "tone": "professional"
  }'
```

**Response:**
```json
{
  "copy": "Discover Eco Glow Moisturizer – natural radiance for healthy skin. Shop now!",
  "characterCount": 78,
  "tone": "professional"
}
```

### Generate Variations
```bash
curl -X POST http://localhost:8080/api/llm/copy/variations \
  -H "Content-Type: application/json" \
  -d '{
    "product": { ... },
    "market": { ... },
    "numVariations": 3
  }'
```

**Response:**
```json
{
  "variations": [
    "Eco Glow Moisturizer: Nature's gift to your skin. Limited time offer!",
    "Transform your skincare routine with Eco Glow. Shop the collection now.",
    "Join thousands who trust Eco Glow for radiant, healthy skin."
  ],
  "count": 3
}
```

### Enhance Image Prompt
```bash
curl -X POST http://localhost:8080/api/llm/prompt/enhance \
  -H "Content-Type: application/json" \
  -d '{
    "product": { ... },
    "platform": "Instagram"
  }'
```

**Response:**
```json
{
  "enhancedPrompt": "Professional product photography of Eco Glow Moisturizer, studio lighting with soft diffusion, clean white background, commercial advertising style, centered composition, vibrant teal and orange accent colors, high resolution 4K, shallow depth of field, beauty product photography aesthetic",
  "characterCount": 287
}
```

### Analyze Compliance
```bash
curl -X POST http://localhost:8080/api/llm/compliance/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "copy": "This miracle product is guaranteed to cure dry skin!",
    "market": {
      "region": "US",
      "locale": "en_US",
      "prohibitedWords": ["cure", "miracle", "guaranteed"]
    }
  }'
```

**Response:**
```json
{
  "passed": false,
  "confidence": 0.95,
  "issues": [
    "Contains prohibited word: miracle",
    "Contains prohibited word: guaranteed",
    "Makes medical claim: cure"
  ],
  "suggestions": [
    "Replace 'miracle' with 'effective' or 'powerful'",
    "Remove 'guaranteed' or replace with 'designed to'",
    "Change 'cure' to 'help reduce' or 'improve'"
  ]
}
```

### Translate & Localize
```bash
curl -X POST http://localhost:8080/api/llm/translate \
  -H "Content-Type: application/json" \
  -d '{
    "copy": "Shop now and save 20%!",
    "sourceMarket": {
      "region": "US",
      "locale": "en_US",
      "prohibitedWords": []
    },
    "targetMarket": {
      "region": "FR",
      "locale": "fr_FR",
      "prohibitedWords": []
    }
  }'
```

**Response:**
```json
{
  "originalCopy": "Shop now and save 20%!",
  "localizedCopy": "Achetez maintenant et économisez 20% !",
  "sourceRegion": "US",
  "targetRegion": "FR"
}
```

---

## Java SDK Usage

### Basic Generation

```java
@Autowired
private LLMService llmService;

String response = llmService.generate(
    "Create a tagline for eco-friendly skincare"
);
```

### With Options

```java
String response = llmService.generate(
    "Write a compelling ad headline",
    LLMOptions.builder()
        .temperature(0.8)
        .maxTokens(100)
        .build()
);
```

### Using Presets

```java
// Creative copy (high temperature)
String creative = llmService.generate(prompt, LLMOptions.creative());

// Factual analysis (low temperature)
String analysis = llmService.generate(prompt, LLMOptions.factual());

// Concise response (short)
String tagline = llmService.generate(prompt, LLMOptions.concise());

// JSON output (structured)
String json = llmService.generate(prompt, LLMOptions.json());
```

### System Prompts

```java
String response = llmService.generateWithSystem(
    "You are an expert advertising copywriter.",
    "Create a 100-character ad for moisturizer",
    LLMOptions.creative()
);
```

### Copy Generation Service

```java
@Autowired
private CopyGenerationService copyService;

// Generate campaign message
String copy = copyService.generateCampaignMessage(
    product,
    market,
    "professional"
);

// Generate variations
List<String> variations = copyService.generateVariations(
    product,
    market,
    3
);

// Enhance image prompt
String enhanced = copyService.enhanceImagePrompt(
    product,
    "Instagram"
);

// Analyze compliance
ComplianceAnalysis analysis = copyService.analyzeCompliance(
    "Shop now!",
    market
);
```

---

## LLMOptions Presets

| Preset | Temperature | Max Tokens | Use Case |
|--------|-------------|------------|----------|
| `defaults()` | 0.7 | 500 | General purpose |
| `creative()` | 0.85 | 300 | Ad copy, brainstorming |
| `factual()` | 0.2 | 500 | Analysis, compliance |
| `concise()` | 0.5 | 100 | Taglines, headlines |
| `translation()` | 0.4 | 400 | Localization |
| `json()` | 0.1 | 800 | Structured output |

---

## Testing

### Run Tests
```bash
mvn test -Dtest=LLMServiceTest
mvn test -Dtest=CopyGenerationServiceTest
```

### Test with Real Providers

**OpenAI:**
```bash
export OPENAI_API_KEY="sk-..."
mvn test -Dtest=LLMServiceTest#testOpenAIIntegration
```

**Anthropic:**
```bash
export ANTHROPIC_API_KEY="sk-ant-..."
mvn test -Dtest=LLMServiceTest#testAnthropicIntegration
```

---

## Mock Mode

The mock provider returns intelligent responses without API calls:

- **Ad copy requests** → "Shop now and discover the difference!"
- **Prompt enhancement** → Detailed photography descriptions
- **Compliance analysis** → JSON with pass/fail and confidence
- **Translation** → Placeholder localized text

Perfect for:
- Development without API costs
- CI/CD pipelines
- Unit testing
- Demos

---

## Cost Optimization

### Caching Strategy

```java
// The LLM service doesn't cache by default
// Implement caching in your application layer:

@Cacheable(value = "adCopy", key = "#product.id + #market.region + #tone")
public String generateCampaignMessage(Product product, Market market, String tone) {
    return copyService.generateCampaignMessage(product, market, tone);
}
```

### Batch Processing

```java
List<String> prompts = List.of("prompt1", "prompt2", "prompt3");
List<String> responses = llmService.generateBatch(prompts, LLMOptions.defaults());
```

### Model Selection

```java
// Use faster/cheaper models for simple tasks
LLMOptions cheap = LLMOptions.builder()
    .model("gpt-3.5-turbo")  // OpenAI
    .temperature(0.7)
    .build();

// Use powerful models for complex tasks
LLMOptions powerful = LLMOptions.builder()
    .model("gpt-4")  // OpenAI
    .temperature(0.7)
    .build();
```

---

## Error Handling

```java
try {
    String copy = copyService.generateCampaignMessage(product, market, "professional");
} catch (IOException e) {
    // API error (network, rate limit, auth)
    log.error("LLM generation failed: {}", e.getMessage());
    // Fallback to default copy or retry with exponential backoff
}
```

---

## Architecture

```
PipelineController
    ↓
LLMController ← REST API
    ↓
CopyGenerationService ← Business Logic
    ↓
LLMService ← Provider Abstraction
    ↓
├─ Mock Provider (Java)
├─ OpenAI Provider (HTTP)
└─ Anthropic Provider (HTTP)
```

---

## Best Practices

### 1. **Use Appropriate Temperatures**
```java
// Factual tasks → low temperature
LLMOptions.factual()  // 0.2

// Creative tasks → high temperature
LLMOptions.creative()  // 0.85
```

### 2. **Set Reasonable Token Limits**
```java
// Short responses (taglines)
.maxTokens(50)

// Medium responses (ad copy)
.maxTokens(200)

// Long responses (analysis)
.maxTokens(500)
```

### 3. **Validate LLM Output**
```java
String copy = copyService.generateCampaignMessage(...);

// Check length
if (copy.length() > 120) {
    copy = copy.substring(0, 120);
}

// Check for prohibited words
ComplianceAnalysis analysis = copyService.analyzeCompliance(copy, market);
if (!analysis.passed()) {
    // Regenerate or use fallback
}
```

### 4. **Implement Retry Logic**
```java
@Retryable(
    value = IOException.class,
    maxAttempts = 3,
    backoff = @Backoff(delay = 1000, multiplier = 2)
)
public String generateWithRetry(String prompt) throws IOException {
    return llmService.generate(prompt);
}
```

---

## Troubleshooting

### Issue: "LLM API error (HTTP 401)"
**Solution:** Check API key configuration
```bash
# Verify environment variable
echo $OPENAI_API_KEY
echo $ANTHROPIC_API_KEY
```

### Issue: "Response is empty or truncated"
**Solution:** Increase max tokens
```java
LLMOptions.builder().maxTokens(1000).build()
```

### Issue: "Responses are too random/inconsistent"
**Solution:** Lower temperature
```java
LLMOptions.builder().temperature(0.3).build()
```

### Issue: "API rate limit exceeded"
**Solution:** Implement retry with exponential backoff and caching

---

## Future Enhancements

- [ ] Streaming responses for real-time UI updates
- [ ] Multi-modal support (image + text analysis)
- [ ] Fine-tuned models for brand-specific copy
- [ ] Conversation history for iterative refinement
- [ ] Performance prediction models
- [ ] Automated A/B test result analysis

---

## License

MIT License

## Questions?

Check the main [README.md](../README.md) or open an issue on GitHub.
