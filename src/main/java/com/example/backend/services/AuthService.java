package com.example.backend.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.example.backend.DTO.AuthRequest;
import com.example.backend.DTO.RegisterRequest;
import com.example.backend.DTO.UserResponse;
import com.example.backend.models.Role;
import com.example.backend.models.User;
import com.example.backend.models.UserRole;
import com.example.backend.repositories.RoleRepository;
import com.example.backend.repositories.UserRepository;
import com.example.backend.repositories.UserRoleRepository;

import java.util.Optional;

@Service
public class AuthService {
	
	private final UserRepository userRepository;
	private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
	@Autowired
    public AuthService(UserRepository userRepository, RoleRepository roleRepository, UserRoleRepository userRoleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
    }
	
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    
    public String register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setAddress(request.getAddress());
        user.setActive(true); // Mặc định người dùng mới là active
        user.setGender(User.Gender.KHAC);
        
        userRepository.save(user);
        
        Role role = roleRepository.findByName(Role.RoleType.KHACH_HANG)
                .orElseThrow(() -> new RuntimeException("Role KHACH_HANG not found"));
        
        UserRole userRole = new UserRole();
        userRole.setUser(user);
        userRole.setRole(role);

        userRoleRepository.save(userRole);
        
        return "User registered successfully";
    }

    public UserResponse login(AuthRequest request) {
        Optional<User> user = userRepository.findByUsername(request.getUsername());

        if (user.isPresent() && passwordEncoder.matches(request.getPassword(), user.get().getPassword())) {
            return new UserResponse(user.get());
        }
        
        throw new RuntimeException("Invalid username or password");
    }
}
