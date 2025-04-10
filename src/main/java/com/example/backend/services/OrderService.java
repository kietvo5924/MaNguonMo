package com.example.backend.services;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Recommended

import com.example.backend.DTO.OrderDTO;
import com.example.backend.DTO.OrderDetailDTO;
import com.example.backend.DTO.OrderRequestDTO;
import com.example.backend.DTO.OrderItemDTO;
import com.example.backend.DTO.PaymentRequestDTO;
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

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ProductVersionRepository productVersionRepository;
    private final ProductColorRepository productColorRepository;

    // Constructor Injection
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
    public OrderDTO createOrder(@Valid OrderRequestDTO orderRequest) {
        log.info("Creating order for user ID: {}", orderRequest.getUserId());
        User user = userRepository.findById(orderRequest.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + orderRequest.getUserId()));

        Order order = new Order();
        order.setUser(user);
        order.setOrderDate(LocalDateTime.now());
        order.setStatus("PENDING");
        order.setPaymentStatus("UNPAID");
        order.setShippingAddress(orderRequest.getShippingAddress());
        order.setTotalPrice(0.0);

        // Save order first to get ID for details
        Order savedOrderWithId = orderRepository.save(order);

        double totalOrderPrice = 0.0;
        List<OrderDetail> orderDetails = new ArrayList<>();

        if (orderRequest.getItems() == null || orderRequest.getItems().isEmpty()) {
            throw new IllegalArgumentException("Order must contain at least one item.");
        }

        for (OrderItemDTO item : orderRequest.getItems()) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new EntityNotFoundException("Product not found: " + item.getProductId()));
            ProductVersion productVersion = item.getProductVersionId() != null ?
                    productVersionRepository.findById(item.getProductVersionId()).orElse(null) : null;
            ProductColor productColor = item.getProductColorId() != null ?
                    productColorRepository.findById(item.getProductColorId()).orElse(null) : null;

            double price = product.getPrice();
            if (productVersion != null && productVersion.getExtraPrice() != null) {
                price += productVersion.getExtraPrice();
            }
             int quantity = item.getQuantity();
             if (quantity <= 0) {
                 throw new IllegalArgumentException("Item quantity must be positive for product: " + item.getProductId());
             }
            double totalAmount = price * quantity;
            totalOrderPrice += totalAmount;

            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setOrder(savedOrderWithId);
            orderDetail.setProduct(product);
            orderDetail.setProductVersion(productVersion);
            orderDetail.setProductColor(productColor);
            orderDetail.setQuantity(quantity);
            orderDetail.setPrice(price);
            orderDetail.setTotalAmount(totalAmount);
            orderDetails.add(orderDetail);
        }

        savedOrderWithId.setTotalPrice(totalOrderPrice);
        Order updatedOrderWithPrice = orderRepository.save(savedOrderWithId);
        List<OrderDetail> savedDetails = orderDetailRepository.saveAll(orderDetails);

        return mapToOrderDTO(updatedOrderWithPrice, savedDetails);
    }

    public List<OrderDTO> getOrdersByUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));
        List<Order> orders = orderRepository.findByUser(user);
        // Eagerly fetch details to avoid lazy loading issues if mapping requires it
        return orders.stream()
                .map(order -> mapToOrderDTO(order, orderDetailRepository.findByOrder(order)))
                .collect(Collectors.toList());
    }

    public OrderDTO getOrderById(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found: " + orderId));
        List<OrderDetail> orderDetails = orderDetailRepository.findByOrder(order);
        return mapToOrderDTO(order, orderDetails);
    }

    @Transactional
    public OrderDTO updateOrder(Long orderId, @Valid OrderRequestDTO orderRequest) {
         log.info("Updating order ID: {}", orderId);
         Order order = orderRepository.findById(orderId)
                 .orElseThrow(() -> new EntityNotFoundException("Order not found: " + orderId));

         // Consider adding more checks here, e.g., only allow updates for PENDING orders
         if (!"PENDING".equals(order.getStatus())) {
             log.warn("Attempted to update order ID {} which is not in PENDING status ({})", orderId, order.getStatus());
         }

        // Delete old details first
        orderDetailRepository.deleteByOrder(order);
        order.getOrderDetails().clear();


        order.setShippingAddress(orderRequest.getShippingAddress());
        order.setTotalPrice(0.0); // Recalculate

        List<OrderDetail> newOrderDetails = new ArrayList<>();
        double totalOrderPrice = 0.0;

         if (orderRequest.getItems() == null || orderRequest.getItems().isEmpty()) {
             throw new IllegalArgumentException("Updated order must contain at least one item.");
         }

        for (OrderItemDTO item : orderRequest.getItems()) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new EntityNotFoundException("Product not found: " + item.getProductId()));
            ProductVersion productVersion = item.getProductVersionId() != null ?
                    productVersionRepository.findById(item.getProductVersionId()).orElse(null) : null;
            ProductColor productColor = item.getProductColorId() != null ?
                    productColorRepository.findById(item.getProductColorId()).orElse(null) : null;

            double price = product.getPrice();
            if (productVersion != null && productVersion.getExtraPrice() != null) {
                price += productVersion.getExtraPrice();
            }
             int quantity = item.getQuantity();
              if (quantity <= 0) {
                  throw new IllegalArgumentException("Item quantity must be positive for product: " + item.getProductId());
              }
            double totalAmount = price * quantity;
            totalOrderPrice += totalAmount;

            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setOrder(order); // Link to the existing order
            orderDetail.setProduct(product);
            orderDetail.setProductVersion(productVersion);
            orderDetail.setProductColor(productColor);
            orderDetail.setQuantity(quantity);
            orderDetail.setPrice(price);
            orderDetail.setTotalAmount(totalAmount);
            newOrderDetails.add(orderDetail);
        }

        order.setTotalPrice(totalOrderPrice);
        // Add new details before saving Order if Cascade includes PERSIST/MERGE
        order.getOrderDetails().addAll(newOrderDetails);

        Order savedOrder = orderRepository.save(order);

        // Fetch the details again to be sure they are linked correctly for the DTO mapping
        List<OrderDetail> finalDetails = orderDetailRepository.findByOrder(savedOrder);

        return mapToOrderDTO(savedOrder, finalDetails);
    }

    @Transactional
    public OrderDTO updateOrderStatus(Long orderId, String newStatus) {
        log.info("Updating status for order {} to {}", orderId, newStatus);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found: " + orderId));

        order.setStatus(newStatus);

        // Automatically mark COD as PAID upon successful delivery
        if ("DELIVERED".equalsIgnoreCase(newStatus)
                && "COD".equals(order.getPaymentMethod())
                && "UNPAID".equals(order.getPaymentStatus())) {
            order.setPaymentStatus("PAID");
            order.setPaymentDate(LocalDateTime.now());
            log.info("Order ID {} (COD) marked as PAID upon delivery.", orderId);
        }

        Order updatedOrder = orderRepository.save(order);
        // Fetch details again for the DTO map
        List<OrderDetail> orderDetails = orderDetailRepository.findByOrder(updatedOrder);
        return mapToOrderDTO(updatedOrder, orderDetails);
    }

    @Transactional
    public void deleteOrder(Long orderId) {
        log.info("Deleting order ID: {}", orderId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found: " + orderId));
        orderRepository.delete(order); 
    }

    @Transactional
    public OrderDTO processPayment(Long orderId, @Valid PaymentRequestDTO paymentRequest) {
        log.info("Processing payment/confirmation for order ID: {}", orderId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found: " + orderId));

        // Prevent processing if already processed or not in PENDING state
        if (order.getPaymentMethod() != null || !"PENDING".equals(order.getStatus())) {
             log.warn("Skipping payment processing for order ID {}. PaymentMethod: {}, Status: {}",
                      orderId, order.getPaymentMethod(), order.getStatus());
             return mapToOrderDTO(order, orderDetailRepository.findByOrder(order));
         }

        String frontendMethod = paymentRequest.getPaymentMethod();
        String backendPaymentMethod = mapFrontendPaymentMethodToBackend(frontendMethod);

        if (backendPaymentMethod == null) {
             log.error("Invalid payment method '{}' for order ID {}", frontendMethod, orderId);
             throw new IllegalArgumentException("Invalid payment method: " + frontendMethod);
        }

        order.setPaymentMethod(backendPaymentMethod);

        if ("COD".equals(backendPaymentMethod)) {
            order.setPaymentStatus("UNPAID");
            order.setPaymentDate(null);
            order.setStatus("PENDING");
        } else if ("CREDIT_CARD".equals(backendPaymentMethod)) {
            if (paymentRequest.getAmount() == null || paymentRequest.getAmount() < order.getTotalPrice()) {
                 log.error("Insufficient payment amount for order {}. Required: {}, Provided: {}", orderId, order.getTotalPrice(), paymentRequest.getAmount());
                 throw new IllegalArgumentException("Payment amount insufficient. Required: " + order.getTotalPrice());
             }
            // Simulate successful online payment
            order.setPaymentStatus("PAID");
            order.setPaymentDate(LocalDateTime.now());
            order.setStatus("PENDING"); // Status remains PENDING as per last request
        } else {
             log.warn("Unhandled payment method '{}'. Setting UNPAID/PENDING.", backendPaymentMethod);
             order.setPaymentStatus("UNPAID");
             order.setPaymentDate(null);
             order.setStatus("PENDING");
        }

        try {
             Order processedOrder = orderRepository.save(order);
             log.info("Order ID {} processed. Final Status: {}, PaymentStatus: {}",
                      processedOrder.getId(), processedOrder.getStatus(), processedOrder.getPaymentStatus());
             return mapToOrderDTO(processedOrder, orderDetailRepository.findByOrder(processedOrder));
        } catch (Exception e) {
             log.error("DB Error saving order ID {} after payment processing: {}", orderId, e.getMessage(), e);
             throw new RuntimeException("Failed to save order after payment processing for order ID " + orderId, e);
        }
    }

    private OrderDTO mapToOrderDTO(Order order, List<OrderDetail> orderDetails) {
        if (order == null) return null;
        OrderDTO orderDTO = new OrderDTO();
        orderDTO.setId(order.getId());
        if (order.getUser() != null) {
           orderDTO.setUserId(order.getUser().getId());
        }
        orderDTO.setOrderDate(order.getOrderDate());
        orderDTO.setStatus(order.getStatus());
        orderDTO.setTotalPrice(order.getTotalPrice());
        orderDTO.setShippingAddress(order.getShippingAddress());
        orderDTO.setPaymentMethod(order.getPaymentMethod());
        orderDTO.setPaymentDate(order.getPaymentDate());
        orderDTO.setPaymentStatus(order.getPaymentStatus());

        if (orderDetails != null) {
             List<OrderDetailDTO> detailDTOs = orderDetails.stream()
                .filter(Objects::nonNull) // Filter out null details just in case
                .map(detail -> {
                    OrderDetailDTO detailDTO = new OrderDetailDTO();
                    if (detail.getProduct() != null) {
                        detailDTO.setProductId(detail.getProduct().getId());
                        detailDTO.setProductName(detail.getProduct().getName());
                    }
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
                })
                .collect(Collectors.toList());
             orderDTO.setOrderDetails(detailDTOs);
        } else {
            orderDTO.setOrderDetails(new ArrayList<>());
        }
        return orderDTO;
    }

    public List<OrderDTO> getAllOrders() {
        List<Order> orders = orderRepository.findAll();
        return orders.stream()
                .map(order -> mapToOrderDTO(order, orderDetailRepository.findByOrder(order)))
                .collect(Collectors.toList());
    }

    // Helper to map frontend payment method names to backend standard names
    private String mapFrontendPaymentMethodToBackend(String frontendMethod) {
        if (frontendMethod == null) return null;
        String upperCaseMethod = frontendMethod.trim().toUpperCase();
        switch (upperCaseMethod) {
            case "COD":
                return "COD";
            case "CREDITCARD": // Allow "CreditCard" from frontend
            case "CREDIT_CARD": // Allow "CREDIT_CARD" from frontend
                 return "CREDIT_CARD"; // Standardize to "CREDIT_CARD"
            default:
                log.warn("Unrecognized payment method received: {}", frontendMethod);
                return null; // Or throw exception if only specific methods are allowed
        }
    }
}