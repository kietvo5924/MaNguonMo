package com.example.backend.services;

import com.example.backend.DTO.UserResponse;
import com.example.backend.DTO.UserUpdateDTO;
import com.example.backend.models.Role;
import com.example.backend.models.User;
import com.example.backend.repositories.RoleRepository;
import com.example.backend.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserService {

	@Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;// Để mã hóa password nếu cần

    public List<UserResponse> getAllUsers() {
        List<User> users = userRepository.findAll();
        return users.stream().map(UserResponse::new).collect(Collectors.toList());
    }

    public Optional<UserResponse> getUserById(Long id) {
        return userRepository.findById(id).map(UserResponse::new);
    }

    public Optional<UserResponse> getUserByUsername(String username) {
        return userRepository.findByUsername(username).map(UserResponse::new);
    }

    public UserResponse createUser(User user) {
        if (userRepository.existsByUsername(user.getUsername()) || userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Username or email already exists");
        }
        if (user.getRoles() != null) {
            Set<Role> persistedRoles = user.getRoles().stream()
                .map(role -> {
                    Optional<Role> existingRole = roleRepository.findByName(role.getName());
                    if (existingRole.isPresent()) {
                        return existingRole.get();
                    } else {
                        return roleRepository.save(role);
                    }
                })
                .collect(Collectors.toSet());
            user.setRoles(persistedRoles);
        }
        // Mã hóa password trước khi lưu
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        User savedUser = userRepository.save(user);
        return new UserResponse(savedUser);
    }

    public UserResponse updateUser(Long id, UserUpdateDTO userDTO) {
        Optional<User> existingUserOpt = userRepository.findById(id);
        if (existingUserOpt.isPresent()) {
            User existingUser = existingUserOpt.get();

            // Kiểm tra username và email không trùng với user khác
            if (userDTO.getUsername() != null && !userDTO.getUsername().equals(existingUser.getUsername())) {
                if (userRepository.existsByUsername(userDTO.getUsername())) {
                    throw new RuntimeException("Username already exists");
                }
                existingUser.setUsername(userDTO.getUsername());
            }
            if (userDTO.getEmail() != null && !userDTO.getEmail().equals(existingUser.getEmail())) {
                if (userRepository.existsByEmail(userDTO.getEmail())) {
                    throw new RuntimeException("Email already exists");
                }
                existingUser.setEmail(userDTO.getEmail());
            }

            // Cập nhật các trường khác
            if (userDTO.getFullName() != null) {
                existingUser.setFullName(userDTO.getFullName());
            }
            if (userDTO.getPhoneNumber() != null) {
                existingUser.setPhoneNumber(userDTO.getPhoneNumber());
            }
            if (userDTO.getAddress() != null) {
                existingUser.setAddress(userDTO.getAddress());
            }
            if (userDTO.getGender() != null) {
                existingUser.setGender(User.Gender.valueOf(userDTO.getGender()));
            }
            existingUser.setActive(userDTO.isActive());

            // Cập nhật password nếu có
            if (userDTO.getPassword() != null && !userDTO.getPassword().isEmpty()) {
                existingUser.setPassword(passwordEncoder.encode(userDTO.getPassword()));
            }

            // Cập nhật roles
            if (userDTO.getRoles() != null && !userDTO.getRoles().isEmpty()) {
                Set<Role> roles = userDTO.getRoles().stream()
                    .map(roleName -> {
                        Role.RoleType roleType = Role.RoleType.valueOf(roleName);
                        Optional<Role> existingRole = roleRepository.findByName(roleType);
                        if (existingRole.isPresent()) {
                            return existingRole.get();
                        } else {
                            Role newRole = new Role();
                            newRole.setName(roleType);
                            return roleRepository.save(newRole);
                        }
                    })
                    .collect(Collectors.toSet());
                existingUser.setRoles(roles);
            }

            User savedUser = userRepository.save(existingUser);
            return new UserResponse(savedUser);
        }
        return null;
    }

    public boolean deleteUser(Long id) {
        Optional<User> user = userRepository.findById(id);
        if (user.isPresent()) {
            userRepository.delete(user.get());
            return true;
        }
        return false;
    }

    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
}