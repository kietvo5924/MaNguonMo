package com.example.backend.DTO;

public class PaymentRequestDTO {
    private String paymentMethod; // Phương thức thanh toán: "CREDIT_CARD", "PAYPAL", "CASH_ON_DELIVERY"
    private Double amount; // Số tiền thanh toán

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }
}