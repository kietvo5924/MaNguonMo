package com.example.backend.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional; // Import Optional

@Service
public class GeminiHttpService {

    private static final Logger logger = LoggerFactory.getLogger(GeminiHttpService.class);

    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String apiUrl;
    private final ObjectMapper objectMapper;
    private final String modelName = "gemini-1.5-flash-latest"; // Hoặc gemini-pro

    public GeminiHttpService(@Value("${google.gemini.api.key}") String apiKey,
                             RestTemplate restTemplate,
                             ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.apiUrl = String.format("https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s", modelName, apiKey);

        // Kiểm tra API key khi khởi tạo
        if (this.apiKey == null || this.apiKey.isEmpty() || this.apiKey.equals("YOUR_GOOGLE_API_KEY_HERE")) {
            logger.error("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            logger.error("!!! Google Gemini API key chưa được cấu hình đúng trong application.properties (google.gemini.api.key) !!!");
            logger.error("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        }
        logger.info("GeminiHttpService initialized for model: {}", modelName);
    }

    /**
     * Sends a prompt to the Gemini API and returns the text response.
     * @param prompt The user prompt or query.
     * @return The text response from Gemini, or an error message string starting with "Lỗi:".
     */
    public String askGemini(String prompt) {
        if (this.apiKey == null || this.apiKey.isEmpty() || this.apiKey.equals("YOUR_GOOGLE_API_KEY_HERE")) {
             logger.error("Cannot call Gemini API: API Key is missing or not configured.");
             return "Lỗi: Cấu hình API Key bị thiếu.";
        }

        String requestBodyJson = buildRequestBody(prompt);
        if (requestBodyJson == null) {
            return "Lỗi: Không thể tạo request body JSON."; // Error logged in buildRequestBody
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> requestEntity = new HttpEntity<>(requestBodyJson, headers);

        logger.info("Sending request to Gemini API endpoint...");
        // Avoid logging the full request body in production unless necessary for debugging specific issues
        // logger.debug("Request Body: {}", requestBodyJson);

        try {
            ResponseEntity<String> responseEntity = restTemplate.postForEntity(apiUrl, requestEntity, String.class);

            if (responseEntity.getStatusCode() == HttpStatus.OK && responseEntity.getBody() != null) {
                logger.info("Received successful response from Gemini API (HTTP {}).", responseEntity.getStatusCodeValue());
                return parseResponse(responseEntity.getBody())
                        .orElse("Lỗi: Không thể trích xuất nội dung từ phản hồi Gemini hợp lệ."); // Handle empty Optional
            } else {
                logger.error("Received non-OK status code from Gemini API: {} - Body: {}",
                             responseEntity.getStatusCode(), responseEntity.getBody());
                return "Lỗi: Nhận mã trạng thái không thành công từ Gemini API: " + responseEntity.getStatusCode();
            }

        } catch (HttpClientErrorException e) {
            logger.error("HTTP Client Error calling Gemini API: {} - Response Body: {}",
                         e.getStatusCode(), e.getResponseBodyAsString(), e);
            // Try to parse the error response body for a more specific message
            String specificError = parseErrorFromBody(e.getResponseBodyAsString());
            return String.format("Lỗi Client khi gọi Gemini API: %s - %s", e.getStatusCode(), specificError);
        } catch (HttpServerErrorException e) {
            logger.error("HTTP Server Error calling Gemini API: {} - Response Body: {}",
                         e.getStatusCode(), e.getResponseBodyAsString(), e);
            String specificError = parseErrorFromBody(e.getResponseBodyAsString());
            return String.format("Lỗi Server từ Gemini API: %s - %s", e.getStatusCode(), specificError);
        } catch (RestClientException e) {
            logger.error("RestClientException calling Gemini API: {}", e.getMessage(), e);
            return "Lỗi kết nối đến Gemini API: " + e.getMessage();
        } catch (Exception e) {
            logger.error("Unexpected error processing Gemini request/response: {}", e.getMessage(), e);
            return "Lỗi không mong muốn khi xử lý yêu cầu Gemini: " + e.getMessage();
        }
    }

    /** Builds the JSON request body for the Gemini API. */
    private String buildRequestBody(String prompt) {
        Map<String, Object> part = Map.of("text", prompt);
        Map<String, Object> content = Map.of("parts", List.of(part));
        Map<String, Object> requestBody = new HashMap<>(); // Use HashMap to allow adding more keys
        requestBody.put("contents", List.of(content));

        // Optional: Add generation configuration
        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", 0.7f);
        // generationConfig.put("maxOutputTokens", 2048); // Example
        requestBody.put("generationConfig", generationConfig);

        try {
            return objectMapper.writeValueAsString(requestBody);
        } catch (JsonProcessingException e) {
            logger.error("Error building Gemini request body JSON: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Parses the JSON response body from Gemini API to extract the text content.
     * Returns an Optional<String> to handle cases where text is not found or blocked.
     */
    private Optional<String> parseResponse(String responseBody) {
        try {
            logger.debug("Parsing Gemini response body..."); // Keep debug log for parsing start
            JsonNode rootNode = objectMapper.readTree(responseBody);

            // Check for top-level API errors first
            JsonNode errorNode = rootNode.path("error");
            if (!errorNode.isMissingNode()) {
                String errorMessage = errorNode.path("message").asText("Unknown API error");
                logger.error("Gemini API returned an error in response body: {}", errorMessage);
                // Return specific error message instead of empty Optional
                return Optional.of("Lỗi từ Gemini API: " + errorMessage);
            }

            JsonNode candidates = rootNode.path("candidates");
            if (candidates.isArray() && !candidates.isEmpty()) {
                JsonNode firstCandidate = candidates.get(0);

                // Check finishReason for blocking or other issues
                String finishReason = firstCandidate.path("finishReason").asText("UNKNOWN");
                if (!"STOP".equalsIgnoreCase(finishReason) && !"MAX_TOKENS".equalsIgnoreCase(finishReason)) {
                    logger.warn("Gemini response finished with non-STOP reason: {}", finishReason);
                    // Check safety ratings for block details
                    JsonNode safetyRatings = firstCandidate.path("safetyRatings");
                     if (safetyRatings.isArray() && !safetyRatings.isEmpty()) {
                         for (JsonNode rating : safetyRatings) {
                             if ("BLOCKED".equalsIgnoreCase(rating.path("probability").asText())) {
                                 String category = rating.path("category").asText("UNKNOWN_CATEGORY");
                                 logger.warn("Response blocked by safety filter: {}", category);
                                 return Optional.of("Phản hồi bị chặn bởi bộ lọc an toàn: " + category);
                             }
                         }
                     }
                     // Return a generic message if not specifically blocked by safetyRatings
                     return Optional.of("Phản hồi từ Gemini không hoàn chỉnh (Reason: " + finishReason + ").");
                }

                // Extract text if finishReason is OK
                JsonNode content = firstCandidate.path("content");
                JsonNode parts = content.path("parts");
                if (parts.isArray() && !parts.isEmpty()) {
                    JsonNode firstPart = parts.get(0);
                    if (firstPart.has("text")) {
                        String resultText = firstPart.get("text").asText();
                        logger.info("Successfully extracted text from Gemini response.");
                        return Optional.of(resultText);
                    } else {
                        logger.warn("First part of Gemini response content does not contain 'text' field.");
                    }
                } else {
                    logger.warn("Could not find 'parts' array in Gemini response content.");
                }
            } else {
                // Check if the prompt itself was blocked
                JsonNode promptFeedback = rootNode.path("promptFeedback");
                if (!promptFeedback.isMissingNode() && "BLOCK_REASON_UNSPECIFIED".equals(promptFeedback.path("blockReason").asText(""))) {
                     logger.warn("Prompt was blocked by Gemini safety filters.");
                     return Optional.of("Yêu cầu của bạn đã bị chặn bởi bộ lọc an toàn.");
                }
                logger.warn("Could not find 'candidates' array in Gemini response or it was empty. Response: {}", responseBody);
            }

            // If text could not be extracted through normal paths
            logger.warn("Failed to extract text using standard parsing logic.");
            return Optional.empty(); // Return empty if no text found

        } catch (JsonProcessingException e) {
            logger.error("Error parsing Gemini response JSON: {}", e.getMessage(), e);
            // Return specific error message instead of empty Optional
            return Optional.of("Lỗi: Phản hồi JSON từ Gemini không hợp lệ.");
        }
    }

    /** Helper to try and parse a more specific error message from an error response body. */
    private String parseErrorFromBody(String errorBody) {
        if (errorBody == null || errorBody.isEmpty()) {
            return "(No error body)";
        }
        try {
            JsonNode errorRoot = objectMapper.readTree(errorBody);
            JsonNode errorDetails = errorRoot.path("error");
            if (!errorDetails.isMissingNode()) {
                return errorDetails.path("message").asText(errorBody); // Return specific message or original body
            }
        } catch (JsonProcessingException e) {
            logger.warn("Could not parse error response body as JSON: {}", e.getMessage());
        }
        return errorBody; // Return original body if parsing fails
    }
}
