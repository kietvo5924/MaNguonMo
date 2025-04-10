package com.example.backend.models;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "orders")
public class Order {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull(message = "User cannot be null")
    private User user; // Người đặt hàng

    @Column(nullable = false)
    @NotNull(message = "Order date cannot be null")
    private LocalDateTime orderDate; // Ngày đặt hàng

    @Column(nullable = false, length = 50)
    @NotNull(message = "Status cannot be null")
    @Size(max = 50, message = "Status must not exceed 50 characters")
    private String status; // Trạng thái đơn hàng: "PENDING", "SHIPPED", "DELIVERED", "CANCELED"

    @Column(nullable = false)
    @NotNull(message = "Total price cannot be null")
    private Double totalPrice; // Tổng giá trị đơn hàng

    @Column(length = 255)
    @Size(max = 255, message = "Shipping address must not exceed 255 characters")
    private String shippingAddress; // Địa chỉ giao hàng

    @Column(length = 50)
    @Size(max = 50, message = "Payment method must not exceed 50 characters")
    private String paymentMethod; // Phương thức thanh toán: "CREDIT_CARD", "CASH_ON_DELIVERY"

    @Column
    private LocalDateTime paymentDate; // Thời điểm thanh toán (có thể null nếu chưa thanh toán)

    @Column(nullable = false, length = 20)
    @NotNull(message = "Payment status cannot be null")
    @Size(max = 20, message = "Payment status must not exceed 20 characters")
    private String paymentStatus = "UNPAID"; // Trạng thái thanh toán: "UNPAID", "PAID"

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<OrderDetail> orderDetails = new HashSet<>();

    // Getters và Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public LocalDateTime getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(LocalDateTime orderDate) {
        this.orderDate = orderDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(Double totalPrice) {
        this.totalPrice = totalPrice;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public LocalDateTime getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDateTime paymentDate) {
        this.paymentDate = paymentDate;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public Set<OrderDetail> getOrderDetails() {
        return orderDetails;
    }

    public void setOrderDetails(Set<OrderDetail> orderDetails) {
        this.orderDetails = orderDetails;
    }
}