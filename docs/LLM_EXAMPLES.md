# LLM Integration - Quick Examples

## 🚀 Quick Start

The LLM service works in **mock mode** by default (no API keys required). Perfect for testing!

### 1. Start the Application

```bash
mvn spring-boot:run
```

### 2. Test LLM Health

```bash
curl http://localhost:8080/api/llm/health
```

**Response:**
```json
{
  "status": "UP",
  "service": "llm-service",
  "capabilities": [
    "ad-copy-generation",
    "prompt-enhancement",
    "compliance-analysis",
    "translation"
  ]
}
```

---

## 📝 Ad Copy Generation

### Generate Campaign Copy

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
  "copy": "Shop now and discover the difference! Limited time offer.",
  "characterCount": 57,
  "tone": "professional"
}
```

### Try Different Tones

**Playful:**
```bash
# Change "tone": "professional" to "tone": "playful"
```

**Urgent:**
```bash
# Change "tone": "professional" to "tone": "urgent"
```

**Elegant:**
```bash
# Change "tone": "professional" to "tone": "elegant"
```

---

## 🔄 A/B Testing Variations

Generate multiple variations for testing:

```bash
curl -X POST http://localhost:8080/api/llm/copy/variations \
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
      "prohibitedWords": ["cure", "miracle"]
    },
    "numVariations": 3
  }'
```

**Response:**
```json
{
  "variations": [
    "Shop now and discover the difference! Limited time offer.",
    "Transform your routine with our premium products today.",
    "Join thousands of satisfied customers. Shop the collection now."
  ],
  "count": 3
}
```

---

## 🎨 Image Prompt Enhancement

Transform basic descriptions into detailed prompts for better AI-generated images:

```bash
curl -X POST http://localhost:8080/api/llm/prompt/enhance \
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
    "platform": "Instagram"
  }'
```

**Response:**
```json
{
  "enhancedPrompt": "Professional product photography, studio lighting, clean white background, commercial advertising style, high resolution, vibrant colors, centered composition",
  "characterCount": 158
}
```

---

## ✅ AI-Powered Compliance Analysis

Go beyond keyword matching with contextual analysis:

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

---

## 🌍 Translation & Localization

Culturally adapt copy for different markets:

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

## 🔧 Using Real LLM Providers

### OpenAI Setup

1. Get your API key from https://platform.openai.com/api-keys

2. Set environment variable:
```bash
export OPENAI_API_KEY="sk-..."
```

3. Configure `application.yml`:
```yaml
pipeline:
  llm-provider: openai
```

4. Restart the application

### Anthropic (Claude) Setup

1. Get your API key from https://console.anthropic.com/

2. Set environment variable:
```bash
export ANTHROPIC_API_KEY="sk-ant-..."
```

3. Configure `application.yml`:
```yaml
pipeline:
  llm-provider: anthropic
```

4. Restart the application

---

## 💡 Java SDK Examples

### Basic Generation

```java
@Autowired
private LLMService llmService;

public void generateTagline() throws IOException {
    String tagline = llmService.generate(
        "Create a catchy tagline for eco-friendly skincare",
        LLMOptions.concise()
    );
    System.out.println("Tagline: " + tagline);
}
```

### Ad Copy Generation

```java
@Autowired
private CopyGenerationService copyService;

public void generateCampaign(Product product, Market market) throws IOException {
    // Single message
    String copy = copyService.generateCampaignMessage(
        product,
        market,
        "professional"
    );

    // Multiple variations
    List<String> variations = copyService.generateVariations(
        product,
        market,
        3
    );

    // Enhanced image prompt
    String enhancedPrompt = copyService.enhanceImagePrompt(
        product,
        "Instagram"
    );
}
```

### Custom Options

```java
// Creative copy (high temperature)
String creative = llmService.generate(
    "Write an exciting ad headline",
    LLMOptions.builder()
        .temperature(0.9)
        .maxTokens(50)
        .build()
);

// Factual analysis (low temperature)
String analysis = llmService.generate(
    "Analyze this ad copy for compliance issues",
    LLMOptions.builder()
        .temperature(0.2)
        .maxTokens(500)
        .build()
);
```

---

## 🧪 Testing with cURL

### Save Request to File

Create `request.json`:
```json
{
  "product": {
    "id": "test_product",
    "name": "Test Product",
    "category": "Test",
    "tagline": "Test tagline",
    "brandColors": {
      "primary": "#FF0000",
      "secondary": "#FFFFFF",
      "accent": "#0000FF"
    }
  },
  "market": {
    "region": "US",
    "locale": "en_US",
    "prohibitedWords": []
  },
  "tone": "professional"
}
```

Run:
```bash
curl -X POST http://localhost:8080/api/llm/copy/generate \
  -H "Content-Type: application/json" \
  -d @request.json
```

---

## 📊 Cost Optimization Tips

### 1. Use Mock Mode for Development
```yaml
pipeline:
  llm-provider: mock  # Free!
```

### 2. Choose Appropriate Models
```java
// Cheap & fast for simple tasks
LLMOptions.builder()
    .model("gpt-3.5-turbo")  // or "claude-haiku-4-5"
    .build()

// Powerful for complex tasks
LLMOptions.builder()
    .model("gpt-4")  // or "claude-sonnet-4-6"
    .build()
```

### 3. Limit Token Usage
```java
LLMOptions.builder()
    .maxTokens(100)  // Short responses only
    .build()
```

### 4. Implement Caching
```java
@Cacheable(value = "adCopy", key = "#product.id + #market.region")
public String generateCampaignMessage(Product product, Market market, String tone) {
    return copyService.generateCampaignMessage(product, market, tone);
}
```

---

## 🎯 Best Practices

### 1. **Validate Output**
```java
String copy = copyService.generateCampaignMessage(product, market, "professional");

// Check length
if (copy.length() > 120) {
    copy = copy.substring(0, 120);
}

// Check compliance
ComplianceAnalysis analysis = copyService.analyzeCompliance(copy, market);
if (!analysis.passed()) {
    // Regenerate or use fallback
}
```

### 2. **Handle Errors Gracefully**
```java
try {
    String copy = llmService.generate(prompt);
} catch (IOException e) {
    log.error("LLM generation failed: {}", e.getMessage());
    // Use fallback copy
    copy = "Shop now! Limited time offer.";
}
```

### 3. **Use Appropriate Temperatures**
- **0.0-0.3**: Factual, consistent (compliance, analysis)
- **0.4-0.7**: Balanced (general purpose)
- **0.8-1.0**: Creative, diverse (ad copy, brainstorming)

### 4. **Test Variations**
Always generate multiple versions and A/B test to find what works best.

---

## 🔍 Troubleshooting

### "LLM API error (HTTP 401)"
Check your API key:
```bash
echo $OPENAI_API_KEY
# or
echo $ANTHROPIC_API_KEY
```

### "Response is empty"
Increase max tokens:
```java
LLMOptions.builder().maxTokens(1000).build()
```

### "Too random/inconsistent"
Lower temperature:
```java
LLMOptions.builder().temperature(0.3).build()
```

### "Rate limit exceeded"
Implement retry with exponential backoff or use mock mode temporarily.

---

## 📚 More Resources

- **Full Documentation:** [LLM_INTEGRATION.md](LLM_INTEGRATION.md)
- **Main README:** [../README.md](../README.md)
- **OpenAPI Docs:** http://localhost:8080/swagger-ui.html

## 🎉 Next Steps

1. ✅ Try the examples above
2. ✅ Explore the Swagger UI at http://localhost:8080/swagger-ui.html
3. ✅ Read the full [LLM Integration Guide](LLM_INTEGRATION.md)
4. ✅ Configure real providers (OpenAI/Anthropic) when ready
5. ✅ Build your own creative workflows!

---

**Happy building! 🚀**
