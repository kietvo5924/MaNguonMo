package com.example.backend.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.DTO.CartItemDTO;
import com.example.backend.models.CartItem;
import com.example.backend.repositories.CartRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CartService {

    private static final Logger logger = LoggerFactory.getLogger(CartService.class);

    @Autowired
    private CartRepository cartRepository;

    public List<CartItemDTO> getCartByUserId(Long userId) {
        logger.info("Lấy giỏ hàng cho userId: {}", userId);
        return cartRepository.findByUserId(userId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public CartItemDTO addToCart(CartItemDTO cartItemDTO) {
        logger.info("Thêm sản phẩm vào giỏ hàng: {}", cartItemDTO);
        List<CartItem> existingItems = cartRepository.findByUserId(cartItemDTO.getUserId());

        // Tìm sản phẩm có cùng productId, version, và color
        CartItem existingItem = existingItems.stream()
                .filter(item -> item.getProductId().equals(cartItemDTO.getProductId())
                        && (item.getVersion() == null ? cartItemDTO.getVersion() == null : item.getVersion().equals(cartItemDTO.getVersion()))
                        && (item.getColor() == null ? cartItemDTO.getColor() == null : item.getColor().equals(cartItemDTO.getColor())))
                .findFirst()
                .orElse(null);

        CartItem cartItem;
        if (existingItem != null) {
            // Nếu sản phẩm đã tồn tại (cùng productId, version, và color), tăng số lượng
            existingItem.setQuantity(existingItem.getQuantity() + cartItemDTO.getQuantity());
            cartItem = cartRepository.save(existingItem);
        } else {
            // Nếu không, tạo một mục mới
            cartItem = convertToEntity(cartItemDTO);
            cartItem = cartRepository.save(cartItem);
        }
        return convertToDTO(cartItem);
    }

    @Transactional
    public void removeFromCart(Long userId, Long itemId) {
        logger.info("Xóa sản phẩm {} khỏi giỏ hàng của userId: {}", itemId, userId);
        CartItem cartItem = cartRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm trong giỏ hàng với ID: " + itemId));
        
        if (!cartItem.getUserId().equals(userId)) {
            throw new RuntimeException("Sản phẩm không thuộc giỏ hàng của người dùng này.");
        }

        cartRepository.deleteByUserIdAndId(userId, itemId);
    }

    @Transactional
    public void clearCart(Long userId) {
        logger.info("Xóa toàn bộ giỏ hàng của userId: {}", userId);
        cartRepository.deleteByUserId(userId);
    }

    private CartItemDTO convertToDTO(CartItem cartItem) {
        CartItemDTO dto = new CartItemDTO();
        dto.setId(cartItem.getId());
        dto.setUserId(cartItem.getUserId());
        dto.setProductId(cartItem.getProductId());
        dto.setProductName(cartItem.getProductName());
        dto.setPrice(cartItem.getPrice());
        dto.setQuantity(cartItem.getQuantity());
        dto.setVersion(cartItem.getVersion()); // Thêm version
        dto.setColor(cartItem.getColor()); // Thêm color
        dto.setColorCode(cartItem.getColorCode()); // Thêm colorCode
        return dto;
    }

    private CartItem convertToEntity(CartItemDTO dto) {
        CartItem cartItem = new CartItem();
        cartItem.setId(dto.getId());
        cartItem.setUserId(dto.getUserId());
        cartItem.setProductId(dto.getProductId());
        cartItem.setProductName(dto.getProductName());
        cartItem.setPrice(dto.getPrice());
        cartItem.setQuantity(dto.getQuantity());
        cartItem.setVersion(dto.getVersion()); // Thêm version
        cartItem.setColor(dto.getColor()); // Thêm color
        cartItem.setColorCode(dto.getColorCode()); // Thêm colorCode
        return cartItem;
    }
}