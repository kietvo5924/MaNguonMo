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
import org.springframework.transaction.annotation.Transactional;
import com.example.backend.DTO.OrderDTO;
import com.example.backend.DTO.OrderDetailDTO;
import com.example.backend.DTO.OrderRequestDTO;
import com.example.backend.DTO.OrderItemDTO;
import com.example.backend.DTO.PaymentRequestDTO;
import com.example.backend.models.*;
import com.example.backend.repositories.*;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ProductVersionRepository productVersionRepository;
    private final ProductColorRepository productColorRepository;

    @Value("${stripe.secret.key}")
    private String stripeSecretKey;

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

    @PostConstruct
    public void initStripe() {
        Stripe.apiKey = stripeSecretKey;
        log.info("Stripe API Key configured (simplified setup).");
    }

    @Transactional
    public OrderDTO createOrder(OrderRequestDTO orderRequest) {
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
    public OrderDTO updateOrderStatus(Long orderId, String newStatus) {
        log.info("Updating status for order {} to {}", orderId, newStatus);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found: " + orderId));

        order.setStatus(newStatus);

        if ("DELIVERED".equalsIgnoreCase(newStatus)
                && "COD".equals(order.getPaymentMethod())
                && "UNPAID".equals(order.getPaymentStatus())) {
            order.setPaymentStatus("PAID");
            order.setPaymentDate(LocalDateTime.now());
            log.info("Order ID {} (COD) marked as PAID upon delivery.", orderId);
        }

        Order updatedOrder = orderRepository.save(order);
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
    public OrderDTO processPayment(Long orderId, PaymentRequestDTO paymentRequest) {
        log.info("Processing payment/confirmation for order ID: {} with method: {}", orderId, paymentRequest.getPaymentMethod());
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found: " + orderId));

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
            log.info("Processing COD for order ID {}.", orderId);
            order.setPaymentStatus("UNPAID");
            order.setPaymentDate(null);
            order.setStatus("PENDING");

        } else if ("CREDIT_CARD".equals(backendPaymentMethod)) {
            log.info("Processing Credit Card for order ID {} (Stripe Check).", orderId);
            String stripePaymentMethodId = paymentRequest.getStripePaymentMethodId();

            if (stripePaymentMethodId == null || stripePaymentMethodId.isBlank()) {
                log.error("Stripe PaymentMethod ID is missing for order ID {}", orderId);
                throw new IllegalArgumentException("Thông tin thanh toán thẻ bị thiếu.");
            }

            try {
                 long amountInSmallestUnit;
                 String currency = "vnd"; // *** THAY ĐỔI TIỀN TỆ CỦA BẠN Ở ĐÂY ***
                 if ("vnd".equalsIgnoreCase(currency)) {
                     amountInSmallestUnit = order.getTotalPrice().longValue();
                 } else {
                     amountInSmallestUnit = (long) (order.getTotalPrice() * 100);
                 }

                 Map<String, String> metadata = new HashMap<>();
                 metadata.put("order_id", order.getId().toString());
                 metadata.put("user_id", order.getUser() != null ? order.getUser().getId().toString() : "unknown");

                log.debug("Attempting Stripe PaymentIntent creation (check) for order {}", orderId);
                PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amountInSmallestUnit)
                    .setCurrency(currency)
                    .setPaymentMethod(stripePaymentMethodId)
                    .setConfirmationMethod(PaymentIntentCreateParams.ConfirmationMethod.MANUAL)
                    .setConfirm(true)
                    .putAllMetadata(metadata)
                    .setReturnUrl("http://localhost:3000/checkout/success?order_id=" + orderId)
                    .build();

                PaymentIntent paymentIntent = PaymentIntent.create(params);
                log.info("Stripe check result for order {}: PI Status = {}", orderId, paymentIntent.getStatus());

                if ("succeeded".equals(paymentIntent.getStatus())) {
                    log.info("Stripe check SUCCEEDED for order {}. Applying original 'PAID' status.", orderId);
                    order.setPaymentStatus("PAID");
                    order.setPaymentDate(LocalDateTime.now());
                    order.setStatus("PENDING");
                } else {
                    log.warn("Stripe check FAILED or requires action for order {}. PI Status: {}", orderId, paymentIntent.getStatus());
                    throw new RuntimeException("Thanh toán thẻ không thành công hoặc cần xác thực thêm. Vui lòng thử lại hoặc dùng COD.");
                }

            } catch (StripeException e) {
                log.error("Stripe API check error for order {}: {}", orderId, e.getMessage());
                throw new RuntimeException("Lỗi xử lý thanh toán thẻ: " + e.getMessage());
            } catch (Exception e) {
                 log.error("Unexpected error during Stripe check for order {}: {}", orderId, e.getMessage(), e);
                 throw new RuntimeException("Lỗi hệ thống khi kiểm tra thanh toán thẻ.");
             }

        } else {
             log.warn("Unhandled payment method '{}'. Setting UNPAID/PENDING.", backendPaymentMethod);
             order.setPaymentStatus("UNPAID");
             order.setPaymentDate(null);
             order.setStatus("PENDING");
        }

        try {
             Order processedOrder = orderRepository.save(order);
             log.info("Order ID {} state saved. Status: {}, PaymentStatus: {}",
                      processedOrder.getId(), processedOrder.getStatus(), processedOrder.getPaymentStatus());
             return mapToOrderDTO(processedOrder, orderDetailRepository.findByOrder(processedOrder));
        } catch (Exception e) {
             log.error("DB Error saving order ID {} after payment processing: {}", orderId, e.getMessage(), e);
             throw new RuntimeException("Lỗi lưu đơn hàng sau khi xử lý thanh toán cho ID " + orderId, e);
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
                .filter(Objects::nonNull)
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

    private String mapFrontendPaymentMethodToBackend(String frontendMethod) {
        if (frontendMethod == null) return null;
        String upperCaseMethod = frontendMethod.trim().toUpperCase();
        switch (upperCaseMethod) {
            case "COD":
                return "COD";
            case "CREDITCARD":
            case "CREDIT_CARD":
                 return "CREDIT_CARD";
            default:
                log.warn("Unrecognized payment method received: {}", frontendMethod);
                return null;
        }
    }
}