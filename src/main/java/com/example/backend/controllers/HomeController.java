package com.example.backend.controllers;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.util.StringUtils;

import com.example.backend.services.GeminiHttpService;
import com.example.backend.services.ProductContextService;

import jakarta.persistence.EntityNotFoundException;

@RestController
@RequestMapping("/api/chat")
public class HomeController {

    private static final Logger logger = LoggerFactory.getLogger(HomeController.class);

    private final GeminiHttpService geminiHttpService;
    private final ProductContextService productContextService;

    @Autowired
    public HomeController(GeminiHttpService geminiHttpService,
                          ProductContextService productContextService) {
        this.geminiHttpService = geminiHttpService;
        this.productContextService = productContextService;
    }

    @PostMapping
    public ResponseEntity<?> handleChatRequest(@RequestBody Map<String, String> request) {
        String userInput = request.get("message");

        if (!StringUtils.hasText(userInput)) {
            logger.warn("Received chat request with empty or null message.");
            return ResponseEntity.badRequest().body(Map.of("error", "Nội dung tin nhắn không được để trống."));
        }

        try {
            logger.info("Received chat request with message: '{}'", userInput);

            logger.info("Fetching product context from ProductContextService...");
            String relatedData = productContextService.getProductInfoForPrompt(userInput);
            // Context đã được log trong ProductContextService

            // *** PROMPT RÕ RÀNG HƠN ***
            String fullPrompt = String.format(
                 "Bạn là trợ lý ảo của cửa hàng. Nhiệm vụ của bạn là trả lời câu hỏi của người dùng dựa trên thông tin sản phẩm được cung cấp dưới đây và kiến thức chung của bạn.\n" +
                 "**HƯỚNG DẪN QUAN TRỌNG:**\n" +
                 "1.  **Ưu tiên thông tin sản phẩm:** Luôn kiểm tra kỹ phần 'Thông tin sản phẩm liên quan' trước khi trả lời. Trả lời dựa trên thông tin này nếu có liên quan đến câu hỏi.\n" +
                 "2.  **Trả lời về giá:** Nếu người dùng hỏi về giá, hãy tìm dòng 'Giá gốc' hoặc 'Giá phiên bản' trong thông tin sản phẩm và nêu rõ giá đó. Nếu thông tin ghi '(Chưa cập nhật)' hoặc '(Lỗi định dạng)', hãy nói rằng giá chưa có hoặc đang bị lỗi.\n" +
                 "3.  **Thông tin khác:** Trả lời các câu hỏi khác (mô tả, số lượng, màu sắc, phiên bản, danh mục) dựa trên thông tin tương ứng trong context.\n" +
                 "4.  **Nếu không có thông tin:** Nếu thông tin sản phẩm báo 'Không tìm thấy' hoặc không chứa chi tiết người dùng hỏi, hãy trả lời một cách lịch sự rằng bạn không có thông tin đó và gợi ý người dùng cung cấp thêm chi tiết hoặc kiểm tra lại.\n\n" +
                 "--- Thông tin sản phẩm liên quan ---\n" +
                 "%s\n" + // Dữ liệu context từ ProductContextService
                 "--- Hết thông tin sản phẩm ---\n\n" +
                 "Câu hỏi của người dùng: %s\n\n" +
                 "Câu trả lời của bạn:", // Yêu cầu Gemini viết câu trả lời
                 relatedData,
                 userInput
            );
            logger.info("Constructed detailed prompt for Gemini.");
            // logger.debug("Full prompt for Gemini: {}", fullPrompt);

            logger.info("Sending prompt to GeminiHttpService...");
            String geminiResponseText = geminiHttpService.askGemini(fullPrompt);
            logger.info("Received response from GeminiHttpService.");

            if (geminiResponseText == null) {
                logger.error("GeminiHttpService returned null response.");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Lỗi không mong muốn: Không nhận được phản hồi từ dịch vụ AI."));
            } else if (geminiResponseText.startsWith("Lỗi:")) {
                logger.warn("GeminiHttpService returned an error message: {}", geminiResponseText);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", geminiResponseText));
            } else {
                logger.info("Sending successful reply to client.");
                return ResponseEntity.ok(Map.of("reply", geminiResponseText));
            }

        } catch (Exception e) {
            logger.error("An unexpected error occurred in chat endpoint: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Đã xảy ra lỗi không mong muốn phía máy chủ."));
        }
    }
}
