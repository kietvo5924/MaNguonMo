package com.example.backend.controllers;

import com.example.backend.DTO.AuthRequest;
import com.example.backend.DTO.RegisterRequest;
import com.example.backend.DTO.UserResponse;
import com.example.backend.services.AuthService;

// --- Imports cho Google Auth ---
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory; // Hoặc JacksonFactory nếu dùng Jackson
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
// ----------------------------------

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
// Bỏ import SecurityContextHolder nếu không dùng Spring Security đầy đủ
// import org.springframework.security.core.context.SecurityContextHolder;
// import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.io.IOException;
import java.security.GeneralSecurityException;


@RestController
// Đảm bảo CORS cấu hình đúng
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    // --- Inject Google Client ID từ application.properties ---
    @Value("${google.client.id}") // Đảm bảo có property này trong application.properties
    private String googleClientId;
    // -----------------------------------------------------

    // --- Google Verifier (Nên tạo Bean nếu có thể, nhưng tạo trực tiếp cũng được) ---
    private GoogleIdTokenVerifier verifier;

    @Autowired
    public AuthController(AuthService authService, @Value("${google.client.id}") String googleClientId) {
        this.authService = authService;
        this.googleClientId = googleClientId; // Gán giá trị đã inject

        // Khởi tạo Verifier một lần (hoặc tạo Bean)
        // Cần xử lý Exception ở đây nếu có lỗi khởi tạo Transport/Factory
        try {
            this.verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(this.googleClientId))
                .build();
        } catch (Exception e) {
             System.err.println("FATAL: Could not initialize GoogleIdTokenVerifier: " + e.getMessage());
             // Có thể ném RuntimeException ở đây để ngăn ứng dụng khởi động nếu verifier là bắt buộc
             // throw new RuntimeException("Failed to initialize GoogleIdTokenVerifier", e);
             this.verifier = null; // Đặt là null để kiểm tra sau
        }
    }
    // -------------------------------------------------------------


    // --- Endpoint Đăng ký (Cập nhật Response) ---
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody @Valid RegisterRequest request, BindingResult result) {
        if (result.hasErrors()) {
            List<String> errors = result.getAllErrors().stream()
                .map(ObjectError::getDefaultMessage)
                .collect(Collectors.toList());
             // Trả về cấu trúc JSON chuẩn
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("success", false, "message", "Lỗi đăng ký: " + String.join(", ", errors)));
        }
        try {
            String responseMessage = authService.register(request);
             // Trả về cấu trúc JSON chuẩn
            return ResponseEntity.ok(Map.of("success", true, "message", responseMessage));
        } catch (RuntimeException e) {
             // Trả về cấu trúc JSON chuẩn
             return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("success", false, "message", "Lỗi đăng ký: " + e.getMessage()));
        }
    }

    // --- Endpoint Đăng nhập thường (Cập nhật Response) ---
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody AuthRequest request, HttpSession session) {
        try {
            UserResponse userResponse = authService.login(request);
            session.setAttribute("user", userResponse); // Lưu user vào session
            // Trả về cấu trúc JSON chuẩn giống Google login
            return ResponseEntity.ok(Map.of("success", true, "data", userResponse));
        } catch (RuntimeException e) {
            // Trả về cấu trúc JSON chuẩn
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // --- ENDPOINT MỚI: Google Login ---
    @PostMapping("/google")
    public ResponseEntity<Map<String, Object>> handleGoogleAuth(@RequestBody Map<String, String> payload, HttpSession session) {
        String idTokenString = payload.get("token");

        if (idTokenString == null || idTokenString.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Thiếu Google ID token"));
        }
        if (this.verifier == null) {
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("success", false, "message", "Lỗi cấu hình Google Verifier phía server."));
        }


        GoogleIdToken idToken;
        try {
            // Xác thực token với Google
            idToken = verifier.verify(idTokenString);
        } catch (GeneralSecurityException | IOException | IllegalArgumentException e) {
            // IllegalArgumentException thường xảy ra nếu token không đúng định dạng hoặc hết hạn
            System.err.println("Google Token Verification Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Token Google không hợp lệ: " + e.getMessage()));
        }

        if (idToken != null) {
            GoogleIdToken.Payload tokenPayload = idToken.getPayload();
            try {
                // Gọi service để xử lý đăng nhập hoặc đăng ký user Google
                UserResponse userResponse = authService.loginOrRegisterGoogleUser(tokenPayload);

                // Lưu thông tin user vào session (quan trọng)
                session.setAttribute("user", userResponse);

                // Trả về thành công cùng dữ liệu user
                return ResponseEntity.ok(Map.of("success", true, "data", userResponse));

            } catch (RuntimeException e) {
                 // Lỗi từ AuthService (vd: email chưa verify, user bị inactive...)
                 System.err.println("Google Auth Service Error: " + e.getMessage());
                 return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                         .body(Map.of("success", false, "message", e.getMessage()));
             } catch (Exception e) {
                 // Lỗi không mong muốn khác
                 System.err.println("Unexpected Google Auth Error: " + e.getMessage());
                 e.printStackTrace(); // In stack trace để debug
                 return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                         .body(Map.of("success", false, "message", "Lỗi server trong quá trình xác thực Google."));
             }
        } else {
            // verifier.verify trả về null -> token không hợp lệ (có thể sai audience, hết hạn...)
            System.err.println("Invalid ID token (verify returned null). Audience configured: " + googleClientId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Token Google không hợp lệ (Xác thực thất bại)"));
        }
    }
    // --- Kết thúc Endpoint Google Login ---


    // --- Endpoint Kiểm tra Session (Cập nhật Response) ---
    @GetMapping("/check-session")
    public ResponseEntity<Map<String, Object>> checkSession(HttpSession session) {
        UserResponse user = (UserResponse) session.getAttribute("user");
        if (user != null) {
            // Trả về thông tin user nếu đã đăng nhập, cấu trúc chuẩn
            return ResponseEntity.ok(Map.of("loggedIn", true, "user", user));
        } else {
             // Trả về trạng thái chưa đăng nhập, cấu trúc chuẩn
            return ResponseEntity.ok(Map.of("loggedIn", false));
        }
    }

    // --- Endpoint Đăng xuất (Sửa lại dùng session.invalidate()) ---
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(HttpServletRequest request, HttpServletResponse response, HttpSession session) {
         // Cách đơn giản và hiệu quả khi quản lý session thủ công
         if (session != null) {
            System.out.println("Invalidating session for user: " + session.getAttribute("user"));
            session.invalidate(); // Hủy session phía server
         }

        // Cách dùng SecurityContextLogoutHandler chỉ hiệu quả nếu Spring Security được cấu hình đầy đủ
        // Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        // if (auth != null) {
        //    new SecurityContextLogoutHandler().logout(request, response, auth);
        // }
        System.out.println("User logged out, session invalidated.");
         // Trả về cấu trúc JSON chuẩn
        return ResponseEntity.ok(Map.of("success", true, "message", "Đăng xuất thành công"));
    }
}