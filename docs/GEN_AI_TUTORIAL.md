# Gen AI Tutorial - From Zero to Hero

## 🎯 What You'll Learn

By the end of this tutorial, you'll understand:
1. What Gen AI and LLMs are
2. How to use them in real applications
3. Key concepts (prompts, temperature, tokens)
4. Best practices and common pitfalls
5. How to integrate them into your code

---

## 📖 Part 1: What is Gen AI?

### **Generative AI (Gen AI)**
AI that can **create** new content (text, images, code) rather than just analyzing existing data.

**Examples:**
- **Text Generation**: ChatGPT writes emails, articles, code
- **Image Generation**: DALL-E creates images from descriptions
- **Code Generation**: GitHub Copilot suggests code

### **Large Language Model (LLM)**
A type of Gen AI specifically trained on text. It:
- Understands natural language
- Generates human-like text
- Can follow instructions
- Remembers context in a conversation

**Popular LLMs:**
- OpenAI GPT-4 (ChatGPT)
- Anthropic Claude (what you're using now!)
- Google Gemini

---

## 🏗️ Part 2: How Your Project Uses Gen AI

Your Creative Automation Pipeline uses LLMs to **automate marketing content creation**.

### **What it does:**

```
INPUT: Product info + Market + Tone
   ↓
LLM generates ad copy
   ↓
OUTPUT: "Shop now and discover the difference!"
```

### **Real-world value:**
- **Before**: Copywriter takes 30 minutes per ad
- **After**: LLM generates it in 2 seconds
- **Scale**: Generate 100s of variations instantly

---

## 🔑 Part 3: Key Concepts

### **1. Prompts (Instructions to AI)**

A prompt is what you tell the AI to do.

**Bad Prompt:**
```
Write something about moisturizer
```
Result: Vague, unpredictable

**Good Prompt:**
```
Create a 100-character professional ad for Eco Glow Moisturizer
targeting US millennials. Include a call-to-action.
Avoid these words: cure, miracle.
```
Result: Specific, predictable, useful

**In your code:**
```java
String prompt = String.format("""
    Create a %s campaign message for:
    Product: %s (%s category)
    Tagline: "%s"
    Market: %s

    Requirements:
    - Maximum 120 characters
    - Include call-to-action
    - Avoid: %s
    """, tone, product.getName(), ...);
```

### **2. Temperature (Creativity Control)**

Temperature controls randomness: 0.0 to 1.0

```
Temperature 0.0 (Deterministic)
├─ Same input → Same output
├─ Very predictable
└─ Use for: Facts, compliance, data extraction

Temperature 0.7 (Balanced)
├─ Good mix of predictable + creative
├─ Different but sensible outputs
└─ Use for: General content, summaries

Temperature 1.0 (Creative)
├─ Very random and diverse
├─ Can be surprising
└─ Use for: Brainstorming, creative writing
```

**Example in your project:**
```java
// For factual compliance analysis
LLMOptions.builder()
    .temperature(0.2)  // Low = consistent
    .build()

// For creative ad copy
LLMOptions.builder()
    .temperature(0.85)  // High = diverse
    .build()
```

### **3. Tokens (AI's "Words")**

Tokens are chunks of text the AI processes.

**Rough conversion:**
- 1 token ≈ 4 characters
- 1 token ≈ 0.75 words
- 100 tokens ≈ 75 words

**Why it matters:**
- **Cost**: APIs charge per token
- **Limits**: Models have max token limits
- **Speed**: More tokens = slower response

**In your code:**
```java
LLMOptions.builder()
    .maxTokens(100)  // Short response (tagline)
    .build()

LLMOptions.builder()
    .maxTokens(500)  // Longer response (analysis)
    .build()
```

### **4. System vs User Prompts**

**System Prompt**: Sets AI's role/behavior
```java
String systemPrompt = """
    You are an expert advertising copywriter.
    Create compelling, concise ad copy.
    """;
```

**User Prompt**: The actual task
```java
String userPrompt = """
    Write an ad for moisturizer.
    """;
```

**Result**: AI acts like a copywriter, not a general assistant.

---

## 💻 Part 4: Hands-On Examples

### **Example 1: Basic Text Generation**

```java
@Autowired
private LLMService llmService;

public void example1() throws IOException {
    // Simple generation
    String response = llmService.generate(
        "Write a tagline for eco-friendly skincare"
    );

    System.out.println(response);
    // Output: "Nature's glow, naturally yours"
}
```

**What's happening:**
1. You send a prompt to the LLM
2. LLM processes it and generates text
3. You get the response back

### **Example 2: Controlling Creativity**

```java
public void example2() throws IOException {
    String prompt = "Write a product tagline for moisturizer";

    // Try 1: Low temperature (consistent)
    String boring = llmService.generate(
        prompt,
        LLMOptions.builder().temperature(0.2).build()
    );
    System.out.println("Consistent: " + boring);
    // Output: "Hydrate your skin daily"

    // Try 2: High temperature (creative)
    String creative = llmService.generate(
        prompt,
        LLMOptions.builder().temperature(0.9).build()
    );
    System.out.println("Creative: " + creative);
    // Output: "Unlock your skin's radiant potential"
}
```

### **Example 3: Market-Specific Ad Copy**

```java
public void example3() throws IOException {
    Product moisturizer = Product.builder()
        .name("Eco Glow Moisturizer")
        .category("Skincare")
        .tagline("Radiance from nature")
        .build();

    Market usMarket = Market.builder()
        .region("US")
        .locale("en_US")
        .prohibitedWords(List.of("cure", "miracle"))
        .build();

    // Generate professional copy
    String copy = copyService.generateCampaignMessage(
        moisturizer,
        usMarket,
        "professional"
    );

    System.out.println(copy);
    // Output: "Discover natural radiance with Eco Glow. Shop now!"
}
```

**Why this is powerful:**
- Automatic compliance (avoids "cure", "miracle")
- Market-specific tone
- Brand-consistent messaging
- Generates in seconds

### **Example 4: A/B Testing Variations**

```java
public void example4() throws IOException {
    // Generate 3 different versions to test
    List<String> variations = copyService.generateVariations(
        product,
        market,
        3  // number of variations
    );

    variations.forEach(v -> System.out.println("→ " + v));

    // Output:
    // → Shop now and save 20% on Eco Glow Moisturizer!
    // → Transform your skin with nature's best. Try Eco Glow today.
    // → Join thousands who trust Eco Glow for radiant skin.
}
```

**Business value:**
- Test which message performs best
- No need for multiple copywriters
- Data-driven marketing decisions

---

## 🎓 Part 5: Advanced Concepts

### **Prompt Engineering**

The art of writing prompts that get the best results.

**Technique 1: Be Specific**
```
❌ "Write about moisturizer"
✅ "Write a 50-character urgent ad for moisturizer targeting millennials"
```

**Technique 2: Provide Context**
```
❌ "Translate this"
✅ "Translate this US skincare ad to French, maintaining urgency and avoiding medical claims"
```

**Technique 3: Give Examples**
```
Good taglines:
- "Just Do It" (Nike)
- "Think Different" (Apple)

Now write one for eco skincare.
```

**Technique 4: Set Constraints**
```
Requirements:
- Maximum 100 characters
- Include brand name
- Avoid words: cure, miracle, guaranteed
- Professional tone
```

### **Chain of Thought**

Break complex tasks into steps:

```java
// Step 1: Analyze the product
String analysis = llmService.generate(
    "Analyze key benefits of eco-friendly moisturizer"
);

// Step 2: Identify target audience
String audience = llmService.generate(
    "Who would buy eco-friendly skincare?"
);

// Step 3: Generate copy using insights
String finalCopy = llmService.generate(
    "Write ad copy for eco moisturizer targeting: " + audience +
    " highlighting: " + analysis
);
```

### **Error Handling**

LLMs can fail. Handle gracefully:

```java
public String generateWithFallback(String prompt) {
    try {
        return llmService.generate(prompt);
    } catch (IOException e) {
        log.error("LLM failed: {}", e.getMessage());

        // Fallback to safe default
        return "Shop now! Limited time offer.";
    }
}
```

### **Validation**

Always validate LLM output:

```java
String copy = llmService.generate(prompt);

// Check length
if (copy.length() > 120) {
    copy = copy.substring(0, 120);
}

// Check prohibited words
for (String word : prohibitedWords) {
    if (copy.toLowerCase().contains(word.toLowerCase())) {
        throw new ComplianceException("Contains: " + word);
    }
}

// Check has call-to-action
if (!copy.matches(".*(?i)(shop|buy|discover|try).*")) {
    log.warn("No CTA found: {}", copy);
}
```

---

## 🚀 Part 6: Practical Exercises

### **Exercise 1: Your First LLM Call**

Let's generate a tagline:

```bash
curl -X POST http://localhost:8080/api/llm/generate \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "Create a 10-word tagline for sustainable fashion",
    "temperature": 0.7,
    "maxTokens": 50
  }'
```

**Try it!** Change:
- The prompt
- Temperature (0.1 to 0.9)
- See how results change

### **Exercise 2: Generate Ad Copy**

Create an ad for YOUR product:

```bash
curl -X POST http://localhost:8080/api/llm/copy/generate \
  -H "Content-Type: application/json" \
  -d '{
    "product": {
      "id": "my_product",
      "name": "YOUR PRODUCT NAME",
      "category": "YOUR CATEGORY",
      "tagline": "YOUR TAGLINE",
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
  }'
```

**Experiment:**
- Try "urgent", "playful", "elegant" tones
- Add prohibited words
- Change the product details

### **Exercise 3: A/B Test Variations**

Generate 5 variations and compare:

```bash
curl -X POST http://localhost:8080/api/llm/copy/variations \
  -H "Content-Type: application/json" \
  -d '{
    "product": {...},
    "market": {...},
    "numVariations": 5
  }'
```

**Question:** Which variation would YOU click on?

---

## 🎯 Part 7: Real-World Use Cases

### **Use Case 1: E-commerce Product Descriptions**

```java
public String generateProductDescription(Product product) {
    return llmService.generate(
        String.format("""
            Write a compelling 200-word product description for:
            Name: %s
            Category: %s
            Key Features: %s
            Target Audience: %s

            Make it persuasive and SEO-friendly.
            """,
            product.getName(),
            product.getCategory(),
            product.getFeatures(),
            product.getTargetAudience()
        ),
        LLMOptions.builder()
            .temperature(0.7)
            .maxTokens(300)
            .build()
    );
}
```

### **Use Case 2: Email Marketing**

```java
public String generateEmailSubject(Campaign campaign) {
    return llmService.generate(
        String.format("""
            Create 3 email subject lines for:
            Offer: %s
            Urgency: %s
            Target: %s

            Requirements:
            - Under 50 characters
            - Create urgency
            - Personalized tone

            Format as numbered list.
            """,
            campaign.getOffer(),
            campaign.getDeadline(),
            campaign.getAudience()
        ),
        LLMOptions.creative()
    );
}
```

### **Use Case 3: Social Media Posts**

```java
public List<String> generateSocialPosts(Product product, int count) {
    String prompt = String.format("""
        Create %d Instagram captions for %s.

        Style: Fun, engaging, with emojis
        Length: 100-150 characters
        Include: Call-to-action and hashtags

        Format as numbered list.
        """, count, product.getName());

    return copyService.generateVariations(product, market, count);
}
```

---

## 💡 Part 8: Tips & Best Practices

### **DO's ✅**

1. **Be Specific**: Clear prompts = better results
2. **Validate Output**: Always check what the LLM returns
3. **Use Appropriate Temperature**: Low for facts, high for creativity
4. **Set Token Limits**: Control cost and response length
5. **Handle Errors**: APIs can fail, have fallbacks
6. **Test Different Prompts**: Iterate to find what works
7. **Cache Results**: Don't regenerate the same thing

### **DON'Ts ❌**

1. **Don't Trust Blindly**: LLMs can hallucinate (make things up)
2. **Don't Expose Secrets**: Never include API keys in prompts
3. **Don't Ignore Costs**: API calls cost money
4. **Don't Skip Validation**: Check for compliance, length, quality
5. **Don't Forget Rate Limits**: APIs have request limits
6. **Don't Use for Critical Decisions**: LLMs can be wrong

---

## 🧪 Part 9: Testing Your Understanding

### **Quiz 1: Temperature**
Which temperature for generating legal disclaimers?
- A) 0.0-0.2 ✅ (Need consistency)
- B) 0.5-0.7
- C) 0.8-1.0

### **Quiz 2: Tokens**
You need a 5-word tagline. How many tokens?
- A) 50
- B) 100
- C) 10 ✅ (5 words ≈ 7 tokens, round up to 10)

### **Quiz 3: Prompts**
Best prompt for generating an ad?
- A) "Write an ad"
- B) "Create a 100-char professional ad for moisturizer targeting millennials with CTA" ✅
- C) "Make marketing content"

---

## 🎓 Part 10: Next Steps

### **Learning Path:**

**Week 1: Basics**
- ✅ Complete this tutorial
- ✅ Try all exercises
- ✅ Read documentation

**Week 2: Experimentation**
- Generate 100 ad variations
- Compare different temperatures
- Test different prompts
- Measure quality

**Week 3: Integration**
- Connect real LLM (OpenAI/Anthropic)
- Build your own use case
- Implement error handling
- Add caching

**Week 4: Production**
- Deploy to production
- Monitor performance
- Track costs
- Optimize prompts

### **Resources:**

1. **OpenAI Cookbook**: https://github.com/openai/openai-cookbook
2. **Anthropic Prompt Engineering**: https://docs.anthropic.com/claude/docs
3. **Your Project Docs**:
   - docs/LLM_INTEGRATION.md
   - docs/LLM_EXAMPLES.md

### **Community:**

- OpenAI Forum
- Anthropic Discord
- r/MachineLearning (Reddit)

---

## 🎉 Congratulations!

You now understand:
✅ What Gen AI and LLMs are
✅ Key concepts (prompts, temperature, tokens)
✅ How to use them in your code
✅ Best practices and pitfalls
✅ Real-world applications

**You're ready to build AI-powered applications!** 🚀

---

## 📝 Quick Reference Card

```
┌─────────────────────────────────────────────────┐
│         GEN AI QUICK REFERENCE                  │
├─────────────────────────────────────────────────┤
│                                                 │
│ Temperature:                                    │
│   0.0-0.3  → Factual, consistent               │
│   0.4-0.7  → Balanced                          │
│   0.8-1.0  → Creative, diverse                 │
│                                                 │
│ Tokens:                                         │
│   1 token ≈ 4 characters                       │
│   1 token ≈ 0.75 words                         │
│   100 tokens ≈ 75 words                        │
│                                                 │
│ Prompt Engineering:                             │
│   ✓ Be specific                                │
│   ✓ Provide context                            │
│   ✓ Set constraints                            │
│   ✓ Give examples                              │
│                                                 │
│ Best Practices:                                 │
│   ✓ Validate output                            │
│   ✓ Handle errors                              │
│   ✓ Control costs                              │
│   ✓ Test prompts                               │
│                                                 │
└─────────────────────────────────────────────────┘
```

---

**Need help?** Check:
- docs/LLM_EXAMPLES.md (practical examples)
- docs/LLM_INTEGRATION.md (technical details)
- Swagger UI: http://localhost:8080/swagger-ui.html

Happy learning! 🎓
