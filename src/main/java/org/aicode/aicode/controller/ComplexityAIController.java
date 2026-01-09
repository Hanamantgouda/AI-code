package org.aicode.aicode.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@RestController
@RequestMapping("/api")
public class ComplexityAIController {

    @Value("${openrouter.api.key}")
    private String OPENROUTER_API_KEY;

    private static final String MODEL = "mistralai/mistral-7b-instruct";
    private static final String API_URL = "https://openrouter.ai/api/v1/chat/completions";

    @PostMapping("/complexity")
    public Map<String, String> analyzeComplexity(@RequestBody Map<String, String> req) {

        Map<String, String> res = new HashMap<>();

        try {
            String language = req.get("language");
            String code = req.get("code");

            String prompt = """
You are a senior software engineer.

Analyze the following %s code and determine its time and space complexity.

CRITICAL RULES:
- Respond in plain text only
- Do NOT include markdown
- Do NOT include code blocks
- Do NOT include explanations beyond complexity
- Use Big-O notation only
- Keep response short and precise

FORMAT:
Time Complexity: O(...)
Space Complexity: O(...)

Code:
%s
""".formatted(language, code);

            Map<String, Object> body = new HashMap<>();
            body.put("model", MODEL);

            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of(
                    "role", "user",
                    "content", prompt
            ));
            body.put("messages", messages);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(OPENROUTER_API_KEY);
            headers.set("HTTP-Referer", "http://localhost");
            headers.set("X-Title", "Multi Language Compiler");

            HttpEntity<Map<String, Object>> entity =
                    new HttpEntity<>(body, headers);

            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<Map> response =
                    restTemplate.postForEntity(API_URL, entity, Map.class);

            List choices = (List) response.getBody().get("choices");
            if (choices == null || choices.isEmpty()) {
                throw new RuntimeException("No AI response");
            }

            Map message = (Map) ((Map) choices.get(0)).get("message");
            String content = message.get("content").toString().trim();

            res.put("complexity", content);

        } catch (Exception e) {
            res.put("complexity", "");
            res.put("error", e.toString());
        }

        return res;
    }
}
