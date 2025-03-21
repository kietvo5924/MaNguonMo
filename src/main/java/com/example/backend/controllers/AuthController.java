package com.example.backend.controllers;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.DTO.AuthRequest;
import com.example.backend.DTO.RegisterRequest;
import com.example.backend.DTO.UserResponse;
import com.example.backend.services.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@RestController
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
@RequestMapping("/api/auth")
public class AuthController {
	
	private final AuthService authService;
	@Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

	// Đăng ký
	@PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody @Valid RegisterRequest request, BindingResult result) {
        if (result.hasErrors()) {
            // Lấy các lỗi và trả lại lỗi chi tiết
            List<String> errors = result.getAllErrors().stream()
                .map(ObjectError::getDefaultMessage)
                .collect(Collectors.toList());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Lỗi đăng ký: " + String.join(", ", errors));
        }
        
        try {
            String response = authService.register(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Lỗi đăng ký: " + e.getMessage());
        }
    }


    // Đăng nhập
    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(@RequestBody AuthRequest request, HttpSession session) {
        // Xử lý logic đăng nhập (ví dụ: kiểm tra thông tin đăng nhập)
        UserResponse userResponse = authService.login(request);
        
        // Lưu thông tin người dùng vào session
        session.setAttribute("user", userResponse);

        // Trả về phản hồi
        return ResponseEntity.ok(userResponse);
    }

    @GetMapping("/check-session")
    public ResponseEntity<String> checkSession(HttpSession session) {
        // Kiểm tra xem có thông tin người dùng trong session không
        if (session.getAttribute("user") != null) {
            return ResponseEntity.ok("Đã đăng nhập: " + session.getAttribute("user"));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Chưa đăng nhập");
        }
    }
    
    @PostMapping("/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response) {
        new SecurityContextLogoutHandler().logout(request, response, SecurityContextHolder.getContext().getAuthentication());
        return "Logged out successfully";
    }
}
