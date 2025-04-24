package com.example.backend.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    // Email nhận mặc định từ Mailtrap inbox của bạn
    private String mailtrapInbox = "bb1585c742-27610b@inbox.mailtrap.io";

    public void sendContactFormEmail(String name, String fromEmail, String subject, String message) {
        try {
            SimpleMailMessage mailMessage = new SimpleMailMessage();

            mailMessage.setTo(mailtrapInbox); // Gửi đến inbox Mailtrap
            mailMessage.setSubject(subject != null && !subject.isEmpty() ? "[Contact Form] " + subject : "[Contact Form] Tin nhắn từ " + name);
            mailMessage.setFrom("contact-form@yourdomain.com"); // Địa chỉ From (có thể tùy chỉnh)
            mailMessage.setReplyTo(fromEmail); // Quan trọng: Để nút Reply trong mail client trỏ về email người gửi

            String emailBody = "Bạn nhận được tin nhắn liên hệ từ:\n\n" +
                               "Tên: " + name + "\n" +
                               "Email: " + fromEmail + "\n" +
                               "Chủ đề: " + (subject != null && !subject.isEmpty() ? subject : "(Không có)") + "\n\n" +
                               "Nội dung:\n" + message;

            mailMessage.setText(emailBody);

            mailSender.send(mailMessage);
            System.out.println("Đã gửi email liên hệ thành công tới Mailtrap.");

        } catch (Exception e) {
            System.err.println("Lỗi khi gửi email liên hệ: " + e.getMessage());
            // Có thể throw một exception tùy chỉnh ở đây để Controller xử lý
            throw new RuntimeException("Gửi email thất bại", e);
        }
    }
}