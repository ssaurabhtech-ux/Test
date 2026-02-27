package com.koerber.order.service;

import com.koerber.order.client.InventoryClient;
import com.koerber.order.dto.InventoryResponse;
import com.koerber.order.dto.OrderRequest;
import com.koerber.order.dto.OrderResponse;
import com.koerber.order.dto.UpdateInventoryRequest;
import com.koerber.order.model.Order;
import com.koerber.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {
    
    private final OrderRepository orderRepository;
    private final InventoryClient inventoryClient;
    
    /**
     * Place a new order
     * 1. Check inventory availability
     * 2. Reserve inventory batches
     * 3. Create order
     * 4. Update inventory
     */
    @Transactional
    public OrderResponse placeOrder(OrderRequest request) {
        log.info("Placing order for product: {}, quantity: {}", 
                request.getProductId(), request.getQuantity());
        
        // Step 1: Check availability
        boolean available = inventoryClient.checkAvailability(
                request.getProductId(), 
                request.getQuantity()
        );
        
        if (!available) {
            throw new RuntimeException("Insufficient inventory for product: " + request.getProductId());
        }
        
        // Step 2: Get product details and reserve batches
        InventoryResponse inventoryResponse = inventoryClient.getInventory(request.getProductId());
        List<Long> reservedBatchIds = inventoryClient.reserveInventory(
                request.getProductId(), 
                request.getQuantity()
        );
        
        // Step 3: Create order
        Order order = new Order();
        order.setProductId(request.getProductId());
        order.setProductName(inventoryResponse.getProductName());
        order.setQuantity(request.getQuantity());
        order.setStatus(Order.OrderStatus.PLACED);
        order.setOrderDate(LocalDate.now());
        order.setReservedBatchIds(reservedBatchIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(",")));
        
        Order savedOrder = orderRepository.save(order);
        log.info("Order created with ID: {}", savedOrder.getOrderId());
        
        // Step 4: Update inventory
        UpdateInventoryRequest updateRequest = new UpdateInventoryRequest(
                request.getProductId(),
                request.getQuantity(),
                reservedBatchIds
        );
        
        inventoryClient.updateInventory(updateRequest);
        log.info("Inventory updated for order: {}", savedOrder.getOrderId());
        
        // Step 5: Return response
        return new OrderResponse(
                savedOrder.getOrderId(),
                savedOrder.getProductId(),
                savedOrder.getProductName(),
                savedOrder.getQuantity(),
                savedOrder.getStatus().name(),
                reservedBatchIds,
                "Order placed. Inventory reserved."
        );
    }
    
    /**
     * Get all orders
     */
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }
    
    /**
     * Get order by ID
     */
    public Order getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
    }
}
