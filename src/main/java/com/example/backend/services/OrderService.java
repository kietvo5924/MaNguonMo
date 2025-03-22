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
import com.example.backend.DTO.OrderItemDTO; // Make sure this DTO exists in your project
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

    // Constructor injection
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
        // Step 1: Retrieve user and check existence
        User user = userRepository.findById(orderRequest.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Step 2: Create the Order object
        Order order = new Order();
        order.setUser(user);
        order.setOrderDate(LocalDateTime.now());
        order.setStatus("PENDING");
        order.setShippingAddress(orderRequest.getShippingAddress());
        order.setTotalPrice(0.0);

        // Step 3: Save the order and initialize orderDetails list
        orderRepository.save(order);
        double totalOrderPrice = 0.0;
        List<OrderDetail> orderDetails = new ArrayList<>();

        // Step 4: Process each item in the orderRequest
        for (OrderItemDTO item : orderRequest.getItems()) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found"));

            ProductVersion productVersion = item.getProductVersionId() != null ?
                    productVersionRepository.findById(item.getProductVersionId()).orElse(null) : null;

            ProductColor productColor = item.getProductColorId() != null ?
                    productColorRepository.findById(item.getProductColorId()).orElse(null) : null;

            // Step 5: Calculate price including version and color options
            double price = product.getPrice();
            if (productVersion != null && productVersion.getExtraPrice() != null) {
                price += productVersion.getExtraPrice();
            }

            double totalAmount = price * item.getQuantity();
            totalOrderPrice += totalAmount;

            // Step 6: Create OrderDetail object
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

        // Step 7: Update the total price for the order and save it
        order.setTotalPrice(totalOrderPrice);
        orderRepository.save(order);
        orderDetailRepository.saveAll(orderDetails);

        // Step 8: Return the OrderDTO with mapped details
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

        // Cập nhật thông tin đơn hàng (ví dụ: địa chỉ giao hàng)
        order.setShippingAddress(orderRequest.getShippingAddress());
        order.setTotalPrice(0.0);

        List<OrderDetail> orderDetails = new ArrayList<>();
        double totalOrderPrice = 0.0;

        // Cập nhật các chi tiết đơn hàng
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

            // Tạo OrderDetail mới hoặc cập nhật nếu cần
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

        // Cập nhật giá trị tổng đơn hàng
        order.setTotalPrice(totalOrderPrice);
        orderRepository.save(order);
        orderDetailRepository.saveAll(orderDetails);

        return mapToOrderDTO(order, orderDetails);
    }

    @Transactional
    public void deleteOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // Xóa tất cả OrderDetails liên quan đến Order này
        orderDetailRepository.deleteByOrder(order);

        // Xóa đơn hàng
        orderRepository.delete(order);
    }


    private OrderDTO mapToOrderDTO(Order order, List<OrderDetail> orderDetails) {
        OrderDTO orderDTO = new OrderDTO();
        orderDTO.setId(order.getId());
        orderDTO.setUserId(order.getUser().getId());
        orderDTO.setOrderDate(order.getOrderDate());
        orderDTO.setStatus(order.getStatus());
        orderDTO.setTotalPrice(order.getTotalPrice());
        orderDTO.setShippingAddress(order.getShippingAddress());

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
