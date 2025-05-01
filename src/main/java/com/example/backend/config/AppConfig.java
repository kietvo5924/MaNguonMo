package com.example.backend.config; // Đảm bảo đúng package

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import java.time.Duration;

@Configuration
public class AppConfig {

    /**
     * Cung cấp một bean RestTemplate để thực hiện các cuộc gọi HTTP.
     * Sử dụng RestTemplateBuilder để dễ dàng cấu hình timeout.
     * @param builder RestTemplateBuilder được Spring Boot tự động cung cấp.
     * @return Một instance RestTemplate đã được cấu hình.
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        // Cấu hình timeout quan trọng khi gọi các service khác hoặc API bên ngoài
        return builder
                .setConnectTimeout(Duration.ofSeconds(10)) // Timeout kết nối
                .setReadTimeout(Duration.ofSeconds(45))   // Timeout đọc (tăng lên cho Gemini)
                .build();
    }

    /**
     * Cung cấp một bean ObjectMapper để xử lý JSON.
     * @return Một instance ObjectMapper.
     */
    @Bean
    public ObjectMapper objectMapper() {
        // Có thể tùy chỉnh ObjectMapper ở đây nếu cần (ví dụ: định dạng ngày tháng)
        ObjectMapper objectMapper = new ObjectMapper();
        // objectMapper.findAndRegisterModules(); // Tự động tìm các module như JavaTimeModule
        return objectMapper;
    }
}
