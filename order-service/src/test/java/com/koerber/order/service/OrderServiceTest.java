package com.koerber.order.service;

import com.koerber.order.client.InventoryClient;
import com.koerber.order.dto.InventoryResponse;
import com.koerber.order.dto.OrderRequest;
import com.koerber.order.dto.OrderResponse;
import com.koerber.order.model.Order;
import com.koerber.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    
    @Mock
    private OrderRepository orderRepository;
    
    @Mock
    private InventoryClient inventoryClient;
    
    @InjectMocks
    private OrderService orderService;
    
    private OrderRequest orderRequest;
    private InventoryResponse inventoryResponse;
    private Order savedOrder;
    
    @BeforeEach
    void setUp() {
        orderRequest = new OrderRequest(1002L, 3);
        
        inventoryResponse = new InventoryResponse();
        inventoryResponse.setProductId(1002L);
        inventoryResponse.setProductName("Smartphone");
        inventoryResponse.setBatches(Arrays.asList(
                new InventoryResponse.BatchInfo(3L, 50, LocalDate.now().plusMonths(1))
        ));
        
        savedOrder = new Order();
        savedOrder.setOrderId(5012L);
        savedOrder.setProductId(1002L);
        savedOrder.setProductName("Smartphone");
        savedOrder.setQuantity(3);
        savedOrder.setStatus(Order.OrderStatus.PLACED);
        savedOrder.setOrderDate(LocalDate.now());
        savedOrder.setReservedBatchIds("3");
    }
    
    @Test
    void testPlaceOrder_Success() {
        // Arrange
        when(inventoryClient.checkAvailability(1002L, 3)).thenReturn(true);
        when(inventoryClient.getInventory(1002L)).thenReturn(inventoryResponse);
        when(inventoryClient.reserveInventory(1002L, 3)).thenReturn(Arrays.asList(3L));
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        doNothing().when(inventoryClient).updateInventory(any());
        
        // Act
        OrderResponse response = orderService.placeOrder(orderRequest);
        
        // Assert
        assertNotNull(response);
        assertEquals(5012L, response.getOrderId());
        assertEquals(1002L, response.getProductId());
        assertEquals("Smartphone", response.getProductName());
        assertEquals(3, response.getQuantity());
        assertEquals("PLACED", response.getStatus());
        assertEquals(Arrays.asList(3L), response.getReservedFromBatchIds());
        assertEquals("Order placed. Inventory reserved.", response.getMessage());
        
        verify(inventoryClient).checkAvailability(1002L, 3);
        verify(inventoryClient).getInventory(1002L);
        verify(inventoryClient).reserveInventory(1002L, 3);
        verify(orderRepository).save(any(Order.class));
        verify(inventoryClient).updateInventory(any());
    }
    
    @Test
    void testPlaceOrder_InsufficientInventory() {
        // Arrange
        when(inventoryClient.checkAvailability(1002L, 3)).thenReturn(false);
        
        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, 
                () -> orderService.placeOrder(orderRequest));
        
        assertTrue(exception.getMessage().contains("Insufficient inventory"));
        verify(inventoryClient).checkAvailability(1002L, 3);
        verify(orderRepository, never()).save(any());
    }
    
    @Test
    void testGetAllOrders() {
        // Arrange
        List<Order> orders = Arrays.asList(savedOrder);
        when(orderRepository.findAll()).thenReturn(orders);
        
        // Act
        List<Order> result = orderService.getAllOrders();
        
        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(5012L, result.get(0).getOrderId());
    }
    
    @Test
    void testGetOrderById_Success() {
        // Arrange
        when(orderRepository.findById(5012L)).thenReturn(Optional.of(savedOrder));
        
        // Act
        Order result = orderService.getOrderById(5012L);
        
        // Assert
        assertNotNull(result);
        assertEquals(5012L, result.getOrderId());
    }
    
    @Test
    void testGetOrderById_NotFound() {
        // Arrange
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(RuntimeException.class, () -> orderService.getOrderById(999L));
    }
}
