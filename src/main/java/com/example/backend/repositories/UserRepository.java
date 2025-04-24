package com.example.backend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.backend.models.User;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    // --- THÊM CÁC HÀM NÀY ---
    Optional<User> findByEmail(String email);
    Optional<User> findByGoogleUserId(String googleUserId);
    // ------------------------
}