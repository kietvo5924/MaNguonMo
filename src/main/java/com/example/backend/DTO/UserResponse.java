package com.example.backend.DTO;

import com.example.backend.models.Role; // Import Role
import com.example.backend.models.User;

import java.util.Collections; // Import Collections
import java.util.Set;
import java.util.stream.Collectors;

public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String phoneNumber;
    private String address;
    private String gender; // Giữ là String
    private boolean active;
    private Set<String> roles; // Giữ là Set<String>

    // Không trả về password hoặc googleUserId

    public UserResponse(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.fullName = user.getFullName();
        this.phoneNumber = user.getPhoneNumber();
        this.address = user.getAddress();
        // Xử lý trường hợp gender là null
        this.gender = (user.getGender() != null) ? user.getGender().name() : User.Gender.KHAC.name(); // Hoặc trả về null nếu muốn
        this.active = user.isActive();

        // Xử lý trường hợp roles là null hoặc rỗng
        if (user.getRoles() != null && !user.getRoles().isEmpty()) {
            this.roles = user.getRoles().stream()
                         // Đảm bảo Role và getName() không null trước khi gọi name()
                         .filter(role -> role != null && role.getName() != null)
                         .map(role -> role.getName().name()) // Lấy tên Enum dạng String
                         .collect(Collectors.toSet());
        } else {
             this.roles = Collections.emptySet(); // Trả về Set rỗng nếu không có role
        }
    }

    // Getters (BẮT BUỘC phải có để Jackson serialize)
    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getFullName() { return fullName; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getAddress() { return address; }
    public String getGender() { return gender; }
    public boolean isActive() { return active; }
    public Set<String> getRoles() { return roles; }

     // Không cần Setters nếu chỉ dùng để trả về dữ liệu
}