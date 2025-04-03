package com.example.backend.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "order_details")
public class OrderDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    @NotNull(message = "Order cannot be null")
    private Order order; // Đơn hàng chứa sản phẩm này

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    @NotNull(message = "Product cannot be null")
    private Product product; // Sản phẩm đặt hàng

    @ManyToOne
    @JoinColumn(name = "product_version_id", nullable = true)
    private ProductVersion productVersion; // Phiên bản của sản phẩm (nếu có)

    @ManyToOne
    @JoinColumn(name = "product_color_id", nullable = true)
    private ProductColor productColor; // Màu sắc sản phẩm (nếu có)

    @Column(nullable = false)
    @NotNull(message = "Quantity cannot be null")
    private Integer quantity; // Số lượng sản phẩm

    @Column(nullable = false)
    @NotNull(message = "Price cannot be null")
    private Double price; // Giá tại thời điểm đặt hàng

    @Column(nullable = false)
    @NotNull(message = "Total amount cannot be null")
    private Double totalAmount; // Tổng tiền của mục này (price * quantity)

    // Getters và Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public ProductVersion getProductVersion() {
        return productVersion;
    }

    public void setProductVersion(ProductVersion productVersion) {
        this.productVersion = productVersion;
    }

    public ProductColor getProductColor() {
        return productColor;
    }

    public void setProductColor(ProductColor productColor) {
        this.productColor = productColor;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Double totalAmount) {
        this.totalAmount = totalAmount;
    }
}