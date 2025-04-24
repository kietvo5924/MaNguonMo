package com.example.backend.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.backend.services.EmailService;

import java.util.Map;
import java.util.HashMap; 

@RestController
@RequestMapping("/api") 
public class ContactController {

    @Autowired
    private EmailService emailService; // Inject EmailService đã tạo

    @PostMapping("/contact")
    @CrossOrigin(origins = "http://localhost:3000", methods = {RequestMethod.POST}, allowedHeaders = {"Content-Type"})
    public ResponseEntity<Map<String, Object>> handleContactForm(@RequestBody Map<String, String> formData) {
        // Tạo map để trả về response JSON
        Map<String, Object> response = new HashMap<>();

        // Lấy dữ liệu từ Map (key phải khớp với state trong React)
        String name = formData.get("name");
        String email = formData.get("email");
        String subject = formData.get("subject"); // Trường này có thể null/rỗng
        String message = formData.get("message");

        // --- Validation cơ bản ---
        if (name == null || name.trim().isEmpty()) {
            response.put("success", false);
            response.put("message", "Vui lòng nhập họ tên.");
            return ResponseEntity.badRequest().body(response); // 400 Bad Request
        }
        if (email == null || email.trim().isEmpty() || !email.contains("@")) { // Kiểm tra email đơn giản
            response.put("success", false);
            response.put("message", "Vui lòng nhập địa chỉ email hợp lệ.");
            return ResponseEntity.badRequest().body(response);
        }
        if (message == null || message.trim().isEmpty()) {
            response.put("success", false);
            response.put("message", "Vui lòng nhập nội dung tin nhắn.");
            return ResponseEntity.badRequest().body(response);
        }
        // --- Kết thúc Validation ---

        try {
            // Gọi service để gửi email
            emailService.sendContactFormEmail(name, email, subject, message);

            // Gửi phản hồi thành công về cho frontend
            response.put("success", true);
            response.put("message", "Tin nhắn của bạn đã được gửi thành công!");
            return ResponseEntity.ok(response); // 200 OK

        } catch (Exception e) {
            // Ghi log lỗi ở phía server
            System.err.println("API Error - Không thể gửi email: " + e.getMessage());
             e.printStackTrace(); // In stack trace để debug

            // Gửi phản hồi lỗi về cho frontend
            response.put("success", false);
            response.put("message", "Đã xảy ra lỗi phía máy chủ khi gửi tin nhắn. Vui lòng thử lại sau.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response); // 500 Internal Server Error
        }
    }
}