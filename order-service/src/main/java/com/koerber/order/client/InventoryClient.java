package com.koerber.order.client;

import com.koerber.order.dto.InventoryResponse;
import com.koerber.order.dto.UpdateInventoryRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryClient {
    
    private final WebClient.Builder webClientBuilder;
    
    @Value("${inventory.service.url}")
    private String inventoryServiceUrl;
    
    /**
     * Get inventory details for a product
     */
    public InventoryResponse getInventory(Long productId) {
        log.info("Fetching inventory for product: {} from {}", productId, inventoryServiceUrl);
        
        try {
            return webClientBuilder.build()
                    .get()
                    .uri(inventoryServiceUrl + "/inventory/{productId}", productId)
                    .retrieve()
                    .bodyToMono(InventoryResponse.class)
                    .block();
        } catch (Exception e) {
            log.error("Error fetching inventory for product: {}", productId, e);
            throw new RuntimeException("Failed to fetch inventory: " + e.getMessage());
        }
    }
    
    /**
     * Check if product has sufficient quantity available
     */
    public boolean checkAvailability(Long productId, Integer quantity) {
        log.info("Checking availability for product: {}, quantity: {}", productId, quantity);
        
        try {
            Boolean available = webClientBuilder.build()
                    .get()
                    .uri(inventoryServiceUrl + "/inventory/check/{productId}?quantity={quantity}", 
                            productId, quantity)
                    .retrieve()
                    .bodyToMono(Boolean.class)
                    .block();
            
            return Boolean.TRUE.equals(available);
        } catch (Exception e) {
            log.error("Error checking availability for product: {}", productId, e);
            return false;
        }
    }
    
    /**
     * Update inventory after order placement
     */
    public void updateInventory(UpdateInventoryRequest request) {
        log.info("Updating inventory for product: {}", request.getProductId());
        
        try {
            webClientBuilder.build()
                    .post()
                    .uri(inventoryServiceUrl + "/inventory/update")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            
            log.info("Inventory updated successfully for product: {}", request.getProductId());
        } catch (Exception e) {
            log.error("Error updating inventory for product: {}", request.getProductId(), e);
            throw new RuntimeException("Failed to update inventory: " + e.getMessage());
        }
    }
    
    /**
     * Reserve inventory and get batch IDs (using FIFO strategy from inventory service)
     */
    public List<Long> reserveInventory(Long productId, Integer quantity) {
        InventoryResponse inventory = getInventory(productId);
        
        List<Long> reservedBatchIds = new ArrayList<>();
        int remainingQuantity = quantity;
        
        for (InventoryResponse.BatchInfo batch : inventory.getBatches()) {
            if (remainingQuantity <= 0) {
                break;
            }
            
            if (batch.getQuantity() > 0) {
                reservedBatchIds.add(batch.getBatchId());
                remainingQuantity -= batch.getQuantity();
            }
        }
        
        if (remainingQuantity > 0) {
            throw new RuntimeException("Insufficient inventory for product: " + productId);
        }
        
        return reservedBatchIds;
    }
}
