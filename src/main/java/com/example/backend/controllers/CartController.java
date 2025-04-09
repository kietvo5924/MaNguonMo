package com.example.backend.controllers;

import com.example.backend.DTO.CartItemDTO;
import com.example.backend.services.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    @Autowired
    private CartService cartService;

    @GetMapping("/{userId}")
    public ResponseEntity<List<CartItemDTO>> getCart(@PathVariable Long userId) {
        List<CartItemDTO> cart = cartService.getCartByUserId(userId);
        return ResponseEntity.ok(cart);
    }

    @PostMapping
    public ResponseEntity<CartItemDTO> addToCart(@RequestBody CartItemDTO cartItemDTO) {
        CartItemDTO savedItem = cartService.addToCart(cartItemDTO);
        return ResponseEntity.ok(savedItem);
    }

    @DeleteMapping("/{userId}/{itemId}")
    public ResponseEntity<String> removeFromCart(@PathVariable Long userId, @PathVariable Long itemId) {
        try {
            cartService.removeFromCart(userId, itemId);
            return ResponseEntity.ok("Xóa sản phẩm thành công.");
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(e.getMessage());
        }
    }

    @DeleteMapping("/{userId}/clear")
    public ResponseEntity<String> clearCart(@PathVariable Long userId) {
        try {
            cartService.clearCart(userId);
            return ResponseEntity.ok("Xóa giỏ hàng thành công.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Lỗi khi xóa giỏ hàng: " + e.getMessage());
        }
    }
}