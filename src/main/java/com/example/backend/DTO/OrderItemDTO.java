package com.example.backend.DTO;

public class OrderItemDTO {
    private Long productId;
    private Long productVersionId;
    private Long productColorId;
    private Integer quantity;

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Long getProductVersionId() {
        return productVersionId;
    }

    public void setProductVersionId(Long productVersionId) {
        this.productVersionId = productVersionId;
    }

    public Long getProductColorId() {
        return productColorId;
    }

    public void setProductColorId(Long productColorId) {
        this.productColorId = productColorId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}