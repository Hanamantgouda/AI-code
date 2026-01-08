package org.aicode.aicode.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@RestController
@RequestMapping("/api")
public class AskAIController {

    @Value("${openrouter.api.key}")
    private String OPENROUTER_API_KEY;

    private static final String MODEL = "mistralai/mistral-7b-instruct";
    private static final String API_URL = "https://openrouter.ai/api/v1/chat/completions";

    @PostMapping("/ask-ai/fix-code")
    public Map<String, String> fixCode(@RequestBody Map<String, String> req) {

        Map<String, String> res = new HashMap<>();

        try {
            String language = req.get("language");
            String code = req.get("code");
            String error = req.getOrDefault("error", "Unknown error");

            String prompt = """
You are a compiler.

Fix the following %s code.

CRITICAL RULES:
- Output ONLY valid %s source code
- Do NOT include explanations
- Do NOT include markdown
- Do NOT include ``` or any text
- Output must start with the first line of code
- Output must end with the last line of code

Error:
%s

Code:
%s
""".formatted(language, language, error, code);

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
            String raw = message.get("content").toString();

            String fixedCode = cleanCode(raw);

            res.put("fixedCode", fixedCode);

        } catch (Exception e) {
            res.put("fixedCode", "");
            res.put("error", e.toString());
        }

        return res;
    }

    private String cleanCode(String aiResponse) {

        return aiResponse
                .replaceAll("```java", "")
                .replaceAll("```", "")
                .replaceAll("(?s)^.*?(import|public|#include)", "$1")
                .trim();
    }
}
