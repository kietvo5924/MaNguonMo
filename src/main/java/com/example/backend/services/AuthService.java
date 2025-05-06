package com.example.backend.services;

import com.example.backend.DTO.AuthRequest;
import com.example.backend.DTO.RegisterRequest;
import com.example.backend.DTO.UserResponse;
import com.example.backend.models.Role;
import com.example.backend.models.User;
import com.example.backend.models.UserRole; // Import nếu bạn dùng bảng UserRole riêng
import com.example.backend.repositories.RoleRepository;
import com.example.backend.repositories.UserRepository;
import com.example.backend.repositories.UserRoleRepository; // Import nếu bạn dùng bảng UserRole riêng

// --- Imports cho Google ---
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
import java.util.Set; // Import Set
import java.util.UUID; // Để tạo password giả
// ------------------------

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    // Bỏ UserRoleRepository nếu dùng @JoinTable và không cần thao tác trực tiếp với bảng join
    // private final UserRoleRepository userRoleRepository;

    @Autowired
    public AuthService(UserRepository userRepository, RoleRepository roleRepository /*, UserRoleRepository userRoleRepository */) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        // this.userRoleRepository = userRoleRepository;
    }

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

 // --- Đăng ký thường (Cập nhật gán Role) ---
    @Transactional // Thêm transactional
    public String register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username đã tồn tại");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email đã tồn tại");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword())); // Mã hóa mật khẩu
        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setAddress(request.getAddress());
        user.setActive(true); // Người dùng mới mặc định active

        // --- SỬA LỖI Ở ĐÂY ---
        User.Gender genderToSet = User.Gender.KHAC; // Giá trị mặc định
        if (request.getGender() != null) {
            try {
                // Cố gắng chuyển đổi String từ request thành Enum User.Gender
                // Đảm bảo giá trị String khớp với tên Enum (NAM, NU, KHAC) - phân biệt chữ hoa/thường
                genderToSet = User.Gender.valueOf(request.getGender().toUpperCase()); // Chuyển sang chữ hoa để khớp tên Enum
            } catch (IllegalArgumentException e) {
                // Nếu String không khớp với bất kỳ tên Enum nào, giữ nguyên giá trị mặc định KHAC
                System.err.println("Giá trị giới tính không hợp lệ từ request: " + request.getGender() + ". Sử dụng giá trị mặc định.");
                // Hoặc bạn có thể ném lỗi ở đây nếu muốn bắt buộc giới tính hợp lệ
                // throw new RuntimeException("Giá trị giới tính không hợp lệ: " + request.getGender());
            }
        }
        user.setGender(genderToSet); // Gán giá trị Enum đã xác định
        // --- KẾT THÚC SỬA LỖI ---

        // Lưu user trước để có ID
        User savedUser = userRepository.save(user);

        // Gán role mặc định
        assignDefaultRole(savedUser);

        return "Đăng ký người dùng thành công";
    }

    // --- Đăng nhập thường (Cập nhật kiểm tra và thêm kiểm tra Google ID) ---
    public UserResponse login(AuthRequest request) {
        Optional<User> userOpt = userRepository.findByUsername(request.getUsername());

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            // Chỉ cho phép đăng nhập thường nếu user không phải tạo từ Google
            // và mật khẩu khớp, và user đang active
            if (user.getGoogleUserId() == null && passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                 if (!user.isActive()) {
                    throw new RuntimeException("Tài khoản người dùng đã bị khóa");
                 }
                 return new UserResponse(user);
            }
        }
        // Nếu không tìm thấy user, hoặc là user Google, hoặc pass sai -> lỗi
        throw new RuntimeException("Tên đăng nhập hoặc mật khẩu không hợp lệ");
    }


    // --- HÀM MỚI: Xử lý Google Login/Register ---
    @Transactional
    public UserResponse loginOrRegisterGoogleUser(GoogleIdToken.Payload payload) {
        String googleUserId = payload.getSubject();
        String email = payload.getEmail();
        boolean emailVerified = payload.getEmailVerified();
        String name = (String) payload.get("name");
        // String pictureUrl = (String) payload.get("picture"); // Lấy ảnh nếu cần

        // Quan trọng: Chỉ chấp nhận email đã được Google xác thực
        if (!emailVerified) {
            throw new RuntimeException("Email Google chưa được xác thực");
        }

        // 1. Tìm user bằng Google ID trước
        Optional<User> userOpt = userRepository.findByGoogleUserId(googleUserId);
        if (userOpt.isPresent()) {
            // User đã tồn tại và liên kết với Google ID này -> Đăng nhập
            User existingUser = userOpt.get();
             if (!existingUser.isActive()) {
                 throw new RuntimeException("Tài khoản người dùng đã bị khóa");
             }
            System.out.println("Google User Found (by Google ID): " + email);
            return new UserResponse(existingUser);
        } else {
            // 2. Nếu không có Google ID, tìm bằng Email
            userOpt = userRepository.findByEmail(email);
            if (userOpt.isPresent()) {
                // User đã tồn tại với email này (có thể đăng ký thường trước đó)
                // -> Liên kết tài khoản này với Google ID
                User existingUser = userOpt.get();
                if (!existingUser.isActive()) {
                    throw new RuntimeException("Tài khoản người dùng đã bị khóa");
                }
                // Kiểm tra xem tài khoản này đã liên kết với Google ID khác chưa
                if (existingUser.getGoogleUserId() != null && !existingUser.getGoogleUserId().equals(googleUserId)) {
                     throw new RuntimeException("Email này đã được liên kết với một tài khoản Google khác.");
                }

                existingUser.setGoogleUserId(googleUserId); // Cập nhật Google ID
                // Cập nhật tên nếu tên hiện tại trống
                if (existingUser.getFullName() == null || existingUser.getFullName().isEmpty()) {
                    existingUser.setFullName(name);
                }
                userRepository.save(existingUser); // Lưu thay đổi
                System.out.println("Google User Found (by Email), Linked Google ID: " + email);
                return new UserResponse(existingUser);
            } else {
                // 3. User hoàn toàn mới -> Tạo user mới từ thông tin Google
                System.out.println("Creating new Google User: " + email);
                User newUser = new User();
                newUser.setEmail(email);
                newUser.setGoogleUserId(googleUserId);
                newUser.setFullName(name);
                newUser.setActive(true);
                newUser.setGender(User.Gender.KHAC); // Mặc định

                // Tạo username duy nhất từ email (ví dụ: "john.doe" từ "john.doe@example.com")
                String baseUsername = email.split("@")[0].replaceAll("[^a-zA-Z0-9_.]", ""); // Giữ lại dấu chấm và gạch dưới
                if (baseUsername.isEmpty()) { // Xử lý trường hợp email không hợp lệ để tạo username
                    baseUsername = "user_" + UUID.randomUUID().toString().substring(0, 8);
                }
                String finalUsername = baseUsername;
                int counter = 1;
                // Kiểm tra và thêm số nếu username đã tồn tại
                while (userRepository.existsByUsername(finalUsername)) {
                    finalUsername = baseUsername + counter++;
                }
                newUser.setUsername(finalUsername);

                // Đặt mật khẩu placeholder ngẫu nhiên, bảo mật và không dùng được
                // Người dùng này sẽ *chỉ* đăng nhập bằng Google
                newUser.setPassword(passwordEncoder.encode(UUID.randomUUID().toString() + System.currentTimeMillis()));

                 // Lưu user mới trước
                 User savedNewUser = userRepository.save(newUser);
                 // Gán role mặc định
                 assignDefaultRole(savedNewUser);

                 // Tạo UserResponse từ user đã có Role
                 // Cần load lại user từ DB để đảm bảo có roles (do save newUser chưa chắc có roles)
                 User userWithRoles = userRepository.findById(savedNewUser.getId()).orElseThrow();
                 return new UserResponse(userWithRoles);
            }
        }
    }

    // --- Hàm tách biệt để gán Role ---
    private void assignDefaultRole(User user) {
        // Tìm Role KHACH_HANG
        Role defaultRole = roleRepository.findByName(Role.RoleType.KHACH_HANG)
                .orElseThrow(() -> new RuntimeException("Lỗi cấu hình: Không tìm thấy Role KHACH_HANG"));

        // Nếu dùng @JoinTable, chỉ cần thêm Role vào Set của User và save User
        user.getRoles().add(defaultRole);
        userRepository.save(user);

        // Nếu dùng bảng UserRole riêng và UserRoleRepository:
        // UserRole userRole = new UserRole();
        // userRole.setUser(user);
        // userRole.setRole(defaultRole);
        // userRoleRepository.save(userRole);
    }
}