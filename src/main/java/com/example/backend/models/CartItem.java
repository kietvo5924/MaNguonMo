package com.example.backend.models;

//CartItem.java (Entity)
import jakarta.persistence.*; // Đổi sang jakarta nếu dùng Spring Boot 3+

@Entity
@Table(name = "cart_items")
public class CartItem {

 @Id
 @GeneratedValue(strategy = GenerationType.IDENTITY)
 private Long id;

 @Column(name = "user_id", nullable = false) // Thêm nullable=false nếu bắt buộc
 private Long userId;

 @Column(name = "product_id", nullable = false) // Thêm nullable=false nếu bắt buộc
 private Long productId;

 @Column(name = "product_version_id") // Lưu ID của phiên bản
 private Long productVersionId;

 @Column(name = "product_color_id") // Lưu ID của màu sắc
 private Long productColorId;

 @Column(name = "product_name")
 private String productName; // Tên sản phẩm (lấy từ Product)

 @Column(name = "price")
 private Double price; // Giá cuối cùng (base + extra)

 @Column(name = "quantity", nullable = false) // Thêm nullable=false nếu bắt buộc
 private Integer quantity;

 @Column(name = "version_name") // Tên phiên bản (lấy từ ProductVersion)
 private String version; // Đổi tên cột nếu cần

 @Column(name = "color_name") // Tên màu sắc (lấy từ ProductColor)
 private String color; // Đổi tên cột nếu cần

 @Column(name = "color_code") // Mã màu hex (lấy từ ProductColor)
 private String colorCode;

 // Getters and Setters cho tất cả các trường (bao gồm cả ID mới)
 // Ví dụ:
 public Long getId() { return id; }
 public void setId(Long id) { this.id = id; }
 public Long getUserId() { return userId; }
 public void setUserId(Long userId) { this.userId = userId; }
 public Long getProductId() { return productId; }
 public void setProductId(Long productId) { this.productId = productId; }
 public Long getProductVersionId() { return productVersionId; }
 public void setProductVersionId(Long productVersionId) { this.productVersionId = productVersionId; }
 public Long getProductColorId() { return productColorId; }
 public void setProductColorId(Long productColorId) { this.productColorId = productColorId; }
 public String getProductName() { return productName; }
 public void setProductName(String productName) { this.productName = productName; }
 public Double getPrice() { return price; }
 public void setPrice(Double price) { this.price = price; }
 public Integer getQuantity() { return quantity; }
 public void setQuantity(Integer quantity) { this.quantity = quantity; }
 public String getVersion() { return version; }
 public void setVersion(String version) { this.version = version; }
 public String getColor() { return color; }
 public void setColor(String color) { this.color = color; }
 public String getColorCode() { return colorCode; }
 public void setColorCode(String colorCode) { this.colorCode = colorCode; }

 // toString(), equals(), hashCode() nếu cần
}