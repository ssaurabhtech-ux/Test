package com.koerber.order.controller;

import com.koerber.order.client.InventoryClient;
import com.koerber.order.dto.InventoryResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class OrderControllerIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private InventoryClient inventoryClient;
    
    @Test
    void testPlaceOrder_Success() throws Exception {
        // Mock inventory client responses
        InventoryResponse inventoryResponse = new InventoryResponse();
        inventoryResponse.setProductId(1002L);
        inventoryResponse.setProductName("Smartphone");
        inventoryResponse.setBatches(Arrays.asList(
                new InventoryResponse.BatchInfo(3L, 50, LocalDate.now().plusMonths(1))
        ));
        
        when(inventoryClient.checkAvailability(1002L, 3)).thenReturn(true);
        when(inventoryClient.getInventory(1002L)).thenReturn(inventoryResponse);
        when(inventoryClient.reserveInventory(1002L, 3)).thenReturn(Arrays.asList(3L));
        doNothing().when(inventoryClient).updateInventory(any());
        
        String requestJson = "{\"productId\":1002,\"quantity\":3}";
        
        mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").exists())
                .andExpect(jsonPath("$.productId").value(1002))
                .andExpect(jsonPath("$.productName").value("Smartphone"))
                .andExpect(jsonPath("$.quantity").value(3))
                .andExpect(jsonPath("$.status").value("PLACED"))
                .andExpect(jsonPath("$.reservedFromBatchIds").isArray())
                .andExpect(jsonPath("$.message").value("Order placed. Inventory reserved."));
    }
    
    @Test
    void testPlaceOrder_InsufficientInventory() throws Exception {
        when(inventoryClient.checkAvailability(1002L, 100)).thenReturn(false);
        
        String requestJson = "{\"productId\":1002,\"quantity\":100}";
        
        mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("FAILED"));
    }
    
    @Test
    void testGetAllOrders() throws Exception {
        mockMvc.perform(get("/order"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
