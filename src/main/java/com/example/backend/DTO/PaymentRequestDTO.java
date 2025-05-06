package com.example.backend.DTO;

public class PaymentRequestDTO {

    private String paymentMethod; // "COD" hoặc "CreditCard"
    private Double amount;
    private String stripePaymentMethodId; // ID từ Stripe Elements khi thanh toán thẻ

    // --- Getters ---
    public String getPaymentMethod() {
        return paymentMethod;
    }

    public Double getAmount() {
        return amount;
    }

    public String getStripePaymentMethodId() {
        return stripePaymentMethodId;
    }

    // --- Setters ---
    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public void setStripePaymentMethodId(String stripePaymentMethodId) {
        this.stripePaymentMethodId = stripePaymentMethodId;
    }
}