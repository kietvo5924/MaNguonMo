package com.example.backend.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.DTO.CartItemDTO;
import com.example.backend.models.CartItem;
import com.example.backend.models.Product; // Giả sử import Product
import com.example.backend.models.ProductColor; // Giả sử import ProductColor
import com.example.backend.models.ProductVersion; // Giả sử import ProductVersion
import com.example.backend.repositories.CartRepository;
import com.example.backend.repositories.ProductColorRepository; // Import repository
import com.example.backend.repositories.ProductRepository; // Import repository
import com.example.backend.repositories.ProductVersionRepository; // Import repository
import jakarta.persistence.EntityNotFoundException; // Import exception phù hợp

import java.util.List;
import java.util.Objects; // Dùng để so sánh ID (có thể null)
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CartService {

    private static final Logger logger = LoggerFactory.getLogger(CartService.class);

    @Autowired
    private CartRepository cartRepository;

    // --- Inject các repository cần thiết ---
    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductVersionRepository productVersionRepository; // Giả sử tên repository

    @Autowired
    private ProductColorRepository productColorRepository; // Giả sử tên repository
    // ---------------------------------------

    public List<CartItemDTO> getCartByUserId(Long userId) {
        logger.info("Lấy giỏ hàng cho userId: {}", userId);
        List<CartItem> cartItems = cartRepository.findByUserId(userId);
        // --- CẬP NHẬT THÔNG TIN TRƯỚC KHI TRẢ VỀ (Tùy chọn nhưng nên làm) ---
        List<CartItem> updatedCartItems = cartItems.stream()
                .map(this::updateCartItemDetailsIfNeeded) // Hàm helper để cập nhật
                .filter(Objects::nonNull) // Loại bỏ item null nếu updateCartItemDetailsIfNeeded trả về null
                .collect(Collectors.toList());

        return updatedCartItems.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional // Đảm bảo các thao tác DB là nhất quán
    public CartItemDTO addToCart(CartItemDTO cartItemDTO) {
        logger.info("Yêu cầu thêm vào giỏ hàng: {}", cartItemDTO);
        Long userId = cartItemDTO.getUserId();
        Long productId = cartItemDTO.getProductId();
        Long versionId = cartItemDTO.getProductVersionId(); // Lấy version ID từ DTO
        Long colorId = cartItemDTO.getProductColorId();     // Lấy color ID từ DTO
        int quantityToAdd = cartItemDTO.getQuantity() != null ? cartItemDTO.getQuantity() : 1; // Mặc định là 1 nếu null

        if (userId == null || productId == null || quantityToAdd <= 0) {
            throw new IllegalArgumentException("Thông tin userId, productId, và quantity không hợp lệ.");
        }

        // 1. Lấy thông tin Product
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy sản phẩm với ID: " + productId));

        // 2. Lấy thông tin ProductVersion (nếu có versionId)
        ProductVersion version = null;
        if (versionId != null) {
            version = productVersionRepository.findById(versionId)
                    // Kiểm tra version có thuộc product không (quan trọng)
                    .filter(v -> v.getProduct().getId().equals(productId))
                    .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy phiên bản với ID: " + versionId + " cho sản phẩm ID: " + productId));
        }

        // 3. Lấy thông tin ProductColor (nếu có colorId)
        ProductColor color = null;
        if (colorId != null) {
             // Kiểm tra xem version có tồn tại không nếu colorId được cung cấp
             if (version == null) {
                 throw new IllegalArgumentException("Không thể chọn màu sắc khi không có phiên bản.");
             }
            color = productColorRepository.findById(colorId)
                    // Kiểm tra color có thuộc version không (quan trọng)
                    .filter(c -> c.getProductVersion().getId().equals(versionId))
                    .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy màu sắc với ID: " + colorId + " cho phiên bản ID: " + versionId));
        }

        // 4. Tính toán giá cuối cùng
        double basePrice = product.getPrice() != null ? product.getPrice() : 0.0;
        double extraPrice = (version != null && version.getExtraPrice() != null) ? version.getExtraPrice() : 0.0;
        double finalPrice = basePrice + extraPrice;

        // 5. Tìm CartItem hiện có với cùng userId, productId, versionId, colorId
        Optional<CartItem> existingItemOpt = cartRepository.findByUserId(userId).stream()
                .filter(item -> item.getProductId().equals(productId)
                        && Objects.equals(item.getProductVersionId(), versionId) // So sánh ID (có thể null)
                        && Objects.equals(item.getProductColorId(), colorId))   // So sánh ID (có thể null)
                .findFirst();

        CartItem savedCartItem;
        if (existingItemOpt.isPresent()) {
            // 6. Nếu có: Cập nhật quantity VÀ các thông tin khác
            logger.info("Sản phẩm đã tồn tại, cập nhật số lượng và thông tin.");
            CartItem existingItem = existingItemOpt.get();
            existingItem.setQuantity(existingItem.getQuantity() + quantityToAdd);
            // Cập nhật lại thông tin từ DB phòng trường hợp thay đổi
            existingItem.setProductName(product.getName());
            existingItem.setPrice(finalPrice);
            existingItem.setVersion(version != null ? version.getVersionName() : null);
            existingItem.setColor(color != null ? color.getColorName() : null);
            existingItem.setColorCode(color != null ? color.getColorCode() : null);
            // ID, productVersionId và productColorId không đổi

            savedCartItem = cartRepository.save(existingItem);
        } else {
            // 7. Nếu không: Tạo CartItem mới với đầy đủ thông tin
             logger.info("Sản phẩm chưa tồn tại, tạo mới.");
            CartItem newItem = new CartItem();
            newItem.setUserId(userId);
            newItem.setProductId(productId);
            newItem.setQuantity(quantityToAdd);
            // Set thông tin lấy từ DB
            newItem.setProductName(product.getName());
            newItem.setPrice(finalPrice);
            newItem.setProductVersionId(versionId); // Lưu ID version
            newItem.setVersion(version != null ? version.getVersionName() : null); // Lưu tên version
            newItem.setProductColorId(colorId); // Lưu ID color
            newItem.setColor(color != null ? color.getColorName() : null); // Lưu tên color
            newItem.setColorCode(color != null ? color.getColorCode() : null); // Lưu code color

            savedCartItem = cartRepository.save(newItem);
        }

        // 8. Trả về DTO
        return convertToDTO(savedCartItem);
    }

    // Hàm helper để cập nhật thông tin item khi getCart (tùy chọn)
    private CartItem updateCartItemDetailsIfNeeded(CartItem cartItem) {
        boolean updated = false;
        try {
            Product product = productRepository.findById(cartItem.getProductId()).orElse(null);
            if (product == null) {
                 logger.warn("Sản phẩm với ID {} không còn tồn tại, giữ nguyên thông tin cũ cho cart item ID {}", cartItem.getProductId(), cartItem.getId());
                 return cartItem; // Giữ nguyên thông tin cũ
                 // Hoặc bạn có thể chọn xóa cart item này nếu sản phẩm không còn tồn tại:
                 // logger.warn("Sản phẩm với ID {} không còn tồn tại, xóa cart item ID {}", cartItem.getProductId(), cartItem.getId());
                 // cartRepository.delete(cartItem);
                 // return null;
            }

            ProductVersion version = null;
            if (cartItem.getProductVersionId() != null) {
                version = productVersionRepository.findById(cartItem.getProductVersionId())
                    .filter(v -> v.getProduct().getId().equals(cartItem.getProductId()))
                    .orElse(null);
                 if (version == null) {
                     logger.warn("Phiên bản với ID {} không còn tồn tại hoặc không thuộc sản phẩm ID {}, reset thông tin version/color cho cart item ID {}", cartItem.getProductVersionId(), cartItem.getProductId(), cartItem.getId());
                     cartItem.setVersion(null);
                     cartItem.setProductVersionId(null);
                     cartItem.setColor(null);
                     cartItem.setColorCode(null);
                     cartItem.setProductColorId(null);
                     updated = true;
                 }
            }


            ProductColor color = null;
            if (cartItem.getProductColorId() != null && version != null) {
                 color = productColorRepository.findById(cartItem.getProductColorId())
                    .filter(c -> c.getProductVersion().getId().equals(cartItem.getProductVersionId()))
                    .orElse(null);
                 if (color == null) {
                     logger.warn("Màu với ID {} không còn tồn tại hoặc không thuộc phiên bản ID {}, reset thông tin color cho cart item ID {}", cartItem.getProductColorId(), cartItem.getProductVersionId(), cartItem.getId());
                      cartItem.setColor(null);
                      cartItem.setColorCode(null);
                      cartItem.setProductColorId(null);
                      updated = true;
                 }
            }

            double basePrice = product.getPrice() != null ? product.getPrice() : 0.0;
            double extraPrice = (version != null && version.getExtraPrice() != null) ? version.getExtraPrice() : 0.0;
            double currentFinalPrice = basePrice + extraPrice;

            if (!Objects.equals(cartItem.getProductName(), product.getName())) {
                cartItem.setProductName(product.getName());
                updated = true;
            }
            if (cartItem.getPrice() == null || Math.abs(cartItem.getPrice() - currentFinalPrice) > 0.001) {
                cartItem.setPrice(currentFinalPrice);
                updated = true;
            }
             String currentVersionName = version != null ? version.getVersionName() : null;
             if (!Objects.equals(cartItem.getVersion(), currentVersionName)) {
                 cartItem.setVersion(currentVersionName);
                 updated = true;
             }
             String currentColorName = color != null ? color.getColorName() : null;
             if (!Objects.equals(cartItem.getColor(), currentColorName)) {
                 cartItem.setColor(currentColorName);
                 updated = true;
             }
             String currentColorCode = color != null ? color.getColorCode() : null;
             if (!Objects.equals(cartItem.getColorCode(), currentColorCode)) {
                 cartItem.setColorCode(currentColorCode);
                 updated = true;
             }

            if (updated) {
                logger.info("Cập nhật thông tin cho cart item ID {}", cartItem.getId());
                return cartRepository.save(cartItem);
            }

        } catch (Exception e) {
            logger.error("Lỗi khi cập nhật chi tiết cho cart item ID {}: {}", cartItem.getId(), e.getMessage(), e); // Log cả exception
        }
        return cartItem;
    }

    @Transactional
    public void removeFromCart(Long userId, Long itemId) {
        logger.info("Xóa sản phẩm {} khỏi giỏ hàng của userId: {}", itemId, userId);
        cartRepository.findByUserIdAndId(userId, itemId)
             .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với ID " + itemId + " trong giỏ hàng của người dùng ID " + userId));
        cartRepository.deleteByUserIdAndId(userId, itemId);
    }

    // --- ĐÃ SỬA PHƯƠNG THỨC NÀY ---
    @Transactional
    public void clearCart(Long userId) {
        logger.info("Xóa toàn bộ giỏ hàng của userId: {}", userId);
        // Chỉ gọi phương thức xóa, không cần gán kết quả vì nó trả về void
        cartRepository.deleteByUserId(userId);
        // Có thể log một thông báo khác nếu cần
        logger.info("Đã gửi yêu cầu xóa giỏ hàng cho userId: {}", userId);
    }
    // -----------------------------

    private CartItemDTO convertToDTO(CartItem cartItem) {
        if (cartItem == null) return null;
        CartItemDTO dto = new CartItemDTO();
        dto.setId(cartItem.getId());
        dto.setUserId(cartItem.getUserId());
        dto.setProductId(cartItem.getProductId());
        dto.setProductName(cartItem.getProductName());
        dto.setPrice(cartItem.getPrice());
        dto.setQuantity(cartItem.getQuantity());
        dto.setProductVersionId(cartItem.getProductVersionId());
        dto.setVersion(cartItem.getVersion());
        dto.setProductColorId(cartItem.getProductColorId());
        dto.setColor(cartItem.getColor());
        dto.setColorCode(cartItem.getColorCode());
        return dto;
    }

    private CartItem convertToEntity(CartItemDTO dto) {
         if (dto == null) return null;
        CartItem cartItem = new CartItem();
        cartItem.setUserId(dto.getUserId());
        cartItem.setProductId(dto.getProductId());
        cartItem.setProductName(dto.getProductName());
        cartItem.setPrice(dto.getPrice());
        cartItem.setQuantity(dto.getQuantity());
        cartItem.setProductVersionId(dto.getProductVersionId());
        cartItem.setVersion(dto.getVersion());
        cartItem.setProductColorId(dto.getProductColorId());
        cartItem.setColor(dto.getColor());
        cartItem.setColorCode(dto.getColorCode());
        return cartItem;
    }
}