package com.example.backend.DTO;

//CartItemDTO.java
//Thêm thư viện Lombok nếu muốn dùng @Data, @Getter, @Setter...
//import lombok.Data;

//@Data // Lombok (tự tạo getter, setter, toString,...)
public class CartItemDTO {

 private Long id;
 private Long userId; // Frontend gửi lên
 private Long productId; // Frontend gửi lên
 private Long productVersionId; // Frontend gửi lên (ID phiên bản)
 private Long productColorId; // Frontend gửi lên (ID màu)

 private String productName; // Backend trả về / Frontend có thể gửi (sẽ bị ghi đè)
 private Double price; // Backend trả về / Frontend có thể gửi (sẽ bị ghi đè)
 private Integer quantity; // Frontend gửi lên
 private String version; // Backend trả về / Frontend có thể gửi (sẽ bị ghi đè) (Tên phiên bản)
 private String color; // Backend trả về / Frontend có thể gửi (sẽ bị ghi đè) (Tên màu)
 private String colorCode; // Backend trả về / Frontend có thể gửi (sẽ bị ghi đè)

 // Getters and Setters (tạo thủ công hoặc dùng Lombok)
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

 // toString() nếu cần
 @Override
 public String toString() {
     return "CartItemDTO{" +
             "id=" + id +
             ", userId=" + userId +
             ", productId=" + productId +
             ", productVersionId=" + productVersionId +
             ", productColorId=" + productColorId +
             ", productName='" + productName + '\'' +
             ", price=" + price +
             ", quantity=" + quantity +
             ", version='" + version + '\'' +
             ", color='" + color + '\'' +
             ", colorCode='" + colorCode + '\'' +
             '}';
 }
}