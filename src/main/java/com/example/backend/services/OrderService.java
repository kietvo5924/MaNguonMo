package com.example.backend.services;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.backend.DTO.OrderDTO;
import com.example.backend.DTO.OrderDetailDTO;
import com.example.backend.DTO.OrderRequestDTO;
import com.example.backend.DTO.OrderItemDTO;
import com.example.backend.DTO.PaymentRequestDTO; // Thêm DTO mới cho thanh toán
import com.example.backend.models.Order;
import com.example.backend.models.OrderDetail;
import com.example.backend.models.Product;
import com.example.backend.models.ProductColor;
import com.example.backend.models.ProductVersion;
import com.example.backend.models.User;
import com.example.backend.repositories.OrderDetailRepository;
import com.example.backend.repositories.OrderRepository;
import com.example.backend.repositories.ProductColorRepository;
import com.example.backend.repositories.ProductRepository;
import com.example.backend.repositories.ProductVersionRepository;
import com.example.backend.repositories.UserRepository;

import jakarta.transaction.Transactional;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ProductVersionRepository productVersionRepository;
    private final ProductColorRepository productColorRepository;

    @Autowired
    public OrderService(OrderRepository orderRepository, OrderDetailRepository orderDetailRepository,
                        UserRepository userRepository, ProductRepository productRepository,
                        ProductVersionRepository productVersionRepository, ProductColorRepository productColorRepository) {
        this.orderRepository = orderRepository;
        this.orderDetailRepository = orderDetailRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.productVersionRepository = productVersionRepository;
        this.productColorRepository = productColorRepository;
    }

    @Transactional
    public OrderDTO createOrder(OrderRequestDTO orderRequest) {
        User user = userRepository.findById(orderRequest.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Order order = new Order();
        order.setUser(user);
        order.setOrderDate(LocalDateTime.now());
        order.setStatus("PENDING");
        order.setPaymentStatus("UNPAID"); // Thêm trạng thái thanh toán mặc định
        order.setShippingAddress(orderRequest.getShippingAddress());
        order.setTotalPrice(0.0);

        orderRepository.save(order);
        double totalOrderPrice = 0.0;
        List<OrderDetail> orderDetails = new ArrayList<>();

        for (OrderItemDTO item : orderRequest.getItems()) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found"));

            ProductVersion productVersion = item.getProductVersionId() != null ?
                    productVersionRepository.findById(item.getProductVersionId()).orElse(null) : null;

            ProductColor productColor = item.getProductColorId() != null ?
                    productColorRepository.findById(item.getProductColorId()).orElse(null) : null;

            double price = product.getPrice();
            if (productVersion != null && productVersion.getExtraPrice() != null) {
                price += productVersion.getExtraPrice();
            }

            double totalAmount = price * item.getQuantity();
            totalOrderPrice += totalAmount;

            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setOrder(order);
            orderDetail.setProduct(product);
            orderDetail.setProductVersion(productVersion);
            orderDetail.setProductColor(productColor);
            orderDetail.setQuantity(item.getQuantity());
            orderDetail.setPrice(price);
            orderDetail.setTotalAmount(totalAmount);

            orderDetails.add(orderDetail);
        }

        order.setTotalPrice(totalOrderPrice);
        orderRepository.save(order);
        orderDetailRepository.saveAll(orderDetails);

        return mapToOrderDTO(order, orderDetails);
    }

    public List<OrderDTO> getOrdersByUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Order> orders = orderRepository.findByUser(user);
        return orders.stream()
                .map(order -> mapToOrderDTO(order, orderDetailRepository.findByOrder(order)))
                .collect(Collectors.toList());
    }

    @Transactional
    public OrderDTO updateOrder(Long orderId, OrderRequestDTO orderRequest) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        order.setShippingAddress(orderRequest.getShippingAddress());
        order.setTotalPrice(0.0);

        List<OrderDetail> orderDetails = new ArrayList<>();
        double totalOrderPrice = 0.0;

        for (OrderItemDTO item : orderRequest.getItems()) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found"));

            ProductVersion productVersion = item.getProductVersionId() != null ?
                    productVersionRepository.findById(item.getProductVersionId()).orElse(null) : null;

            ProductColor productColor = item.getProductColorId() != null ?
                    productColorRepository.findById(item.getProductColorId()).orElse(null) : null;

            double price = product.getPrice();
            if (productVersion != null && productVersion.getExtraPrice() != null) {
                price += productVersion.getExtraPrice();
            }

            double totalAmount = price * item.getQuantity();
            totalOrderPrice += totalAmount;

            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setOrder(order);
            orderDetail.setProduct(product);
            orderDetail.setProductVersion(productVersion);
            orderDetail.setProductColor(productColor);
            orderDetail.setQuantity(item.getQuantity());
            orderDetail.setPrice(price);
            orderDetail.setTotalAmount(totalAmount);

            orderDetails.add(orderDetail);
        }

        order.setTotalPrice(totalOrderPrice);
        orderRepository.save(order);
        orderDetailRepository.saveAll(orderDetails);

        return mapToOrderDTO(order, orderDetails);
    }

    @Transactional
    public void deleteOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        orderDetailRepository.deleteByOrder(order);
        orderRepository.delete(order);
    }

    @Transactional
    public OrderDTO processPayment(Long orderId, PaymentRequestDTO paymentRequest) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!"PENDING".equals(order.getStatus()) || "PAID".equals(order.getPaymentStatus())) {
            throw new RuntimeException("Order cannot be paid. Current status: " + order.getStatus() + ", Payment status: " + order.getPaymentStatus());
        }

        if (paymentRequest.getAmount() == null || paymentRequest.getAmount() < order.getTotalPrice()) {
            throw new RuntimeException("Payment amount is insufficient. Required: " + order.getTotalPrice());
        }

        order.setPaymentMethod(paymentRequest.getPaymentMethod());
        order.setPaymentDate(LocalDateTime.now());
        order.setPaymentStatus("PAID");
        order.setStatus("SHIPPED"); // Chuyển trạng thái sau khi thanh toán
        orderRepository.save(order);

        List<OrderDetail> orderDetails = orderDetailRepository.findByOrder(order);
        return mapToOrderDTO(order, orderDetails);
    }

    private OrderDTO mapToOrderDTO(Order order, List<OrderDetail> orderDetails) {
        OrderDTO orderDTO = new OrderDTO();
        orderDTO.setId(order.getId());
        orderDTO.setUserId(order.getUser().getId());
        orderDTO.setOrderDate(order.getOrderDate());
        orderDTO.setStatus(order.getStatus());
        orderDTO.setTotalPrice(order.getTotalPrice());
        orderDTO.setShippingAddress(order.getShippingAddress());
        orderDTO.setPaymentMethod(order.getPaymentMethod()); // Thêm thông tin thanh toán
        orderDTO.setPaymentDate(order.getPaymentDate());
        orderDTO.setPaymentStatus(order.getPaymentStatus());

        List<OrderDetailDTO> detailDTOs = orderDetails.stream().map(detail -> {
            OrderDetailDTO detailDTO = new OrderDetailDTO();
            detailDTO.setProductId(detail.getProduct().getId());
            detailDTO.setProductName(detail.getProduct().getName());
            detailDTO.setQuantity(detail.getQuantity());
            detailDTO.setPrice(detail.getPrice());
            detailDTO.setTotalAmount(detail.getTotalAmount());

            if (detail.getProductVersion() != null) {
                detailDTO.setVersionName(detail.getProductVersion().getVersionName());
            }

            if (detail.getProductColor() != null) {
                detailDTO.setColorName(detail.getProductColor().getColorName());
            }

            return detailDTO;
        }).collect(Collectors.toList());

        orderDTO.setOrderDetails(detailDTOs);
        return orderDTO;
    }
}