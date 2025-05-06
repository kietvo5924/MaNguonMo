package com.example.backend.repositories;

import com.example.backend.models.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository; // Import annotation @Repository
import org.springframework.transaction.annotation.Transactional; // Import annotation @Transactional

import java.util.List;
import java.util.Optional; // Import Optional

@Repository // <-- Thêm @Repository
public interface CartRepository extends JpaRepository<CartItem, Long> {
    List<CartItem> findByUserId(Long userId);
    Optional<CartItem> findByUserIdAndId(Long userId, Long id); // <-- Thêm phương thức này
    @Transactional // <-- Thêm @Transactional
    void deleteByUserIdAndId(Long userId, Long id);
 // Trong CartRepository.java
    @Transactional
    void deleteByUserId(Long userId); // <-- Kiểu trả về là void
}