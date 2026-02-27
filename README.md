# Koerber Java Microservices Assignment

A Spring Boot-based microservices architecture for e-commerce inventory and order management.

## 📋 Table of Contents
- [Overview](#overview)
- [Architecture](#architecture)
- [Prerequisites](#prerequisites)
- [Project Setup](#project-setup)
- [Running the Services](#running-the-services)
- [API Documentation](#api-documentation)
- [Testing](#testing)
- [Design Patterns](#design-patterns)
- [Database Schema](#database-schema)

## 🎯 Overview

This project implements two microservices that manage inventory and orders for an e-commerce platform:

1. **Inventory Service** (Port 8081) - Manages product inventory with batch tracking and expiry dates
2. **Order Service** (Port 8082) - Handles order placement and communicates with Inventory Service

### Key Features
- ✅ Batch inventory management with expiry date tracking
- ✅ FIFO (First Expiry First Out) strategy for inventory allocation
- ✅ Real-time inventory availability checking
- ✅ Inter-service communication using WebClient
- ✅ Factory Design Pattern for extensibility
- ✅ Automated database initialization with Liquibase
- ✅ Comprehensive unit and integration tests
- ✅ Swagger/OpenAPI documentation


## 🛠️ Technologies Used

- **Java 17**
- **Spring Boot 3.2.0**
  - Spring Web
  - Spring Data JPA
  - Spring WebFlux (for WebClient)
- **H2 Database** (In-memory)
- **Liquibase** (Database migration)
- **Lombok** (Reduce boilerplate)
- **JUnit 5 & Mockito** (Testing)
- **Swagger/OpenAPI** (API Documentation)
- **Maven** (Build tool)

## 📋 Prerequisites

- Java 17 or higher
- Maven 3.6+
- Git

## 🚀 Project Setup

### 1. Clone the Repository

```bash
git clone <repository-url>
cd koerber-microservices
```

### 2. Build Both Services

```bash
# Build Inventory Service
cd inventory-service
mvn clean install
cd ..

# Build Order Service
cd order-service
mvn clean install
cd ..
```

## ▶️ Running the Services

### Option 1: Using Maven (Recommended for Development)

**Terminal 1 - Start Inventory Service:**
```bash
cd inventory-service
mvn spring-boot:run
```

The service will start on `http://localhost:8081`

**Terminal 2 - Start Order Service:**
```bash
cd order-service
mvn spring-boot:run
```

The service will start on `http://localhost:8082`

### Option 2: Using JAR Files

```bash
# Build both services first
cd inventory-service && mvn clean package && cd ..
cd order-service && mvn clean package && cd ..

# Run Inventory Service
java -jar inventory-service/target/inventory-service-1.0.0.jar

# In another terminal, run Order Service
java -jar order-service/target/order-service-1.0.0.jar
```

### Verify Services are Running

```bash
# Check Inventory Service
curl http://localhost:8081/inventory/1001

# Check Order Service
curl http://localhost:8082/order
```

## 📚 API Documentation

### Inventory Service APIs (Port 8081)

#### 1. Get Inventory by Product ID
```bash
GET http://localhost:8081/inventory/{productId}
```

**Example Request:**
```bash
curl http://localhost:8081/inventory/1001
```

**Example Response:**
```json
{
  "productId": 1001,
  "productName": "Laptop",
  "batches": [
    {
      "batchId": 1,
      "quantity": 68,
      "expiryDate": "2026-06-25"
    }
  ]
}
```

#### 2. Check Inventory Availability
```bash
GET http://localhost:8081/inventory/check/{productId}?quantity={quantity}
```

**Example:**
```bash
curl "http://localhost:8081/inventory/check/1001?quantity=50"
```

**Response:** `true` or `false`

#### 3. Update Inventory
```bash
POST http://localhost:8081/inventory/update
Content-Type: application/json

{
  "productId": 1001,
  "quantity": 10,
  "reservedFromBatchIds": [1]
}
```

### Order Service APIs (Port 8082)

#### 1. Place an Order
```bash
POST http://localhost:8082/order
Content-Type: application/json

{
  "productId": 1002,
  "quantity": 3
}
```

**Example Response:**
```json
{
  "orderId": 5012,
  "productId": 1002,
  "productName": "Smartphone",
  "quantity": 3,
  "status": "PLACED",
  "reservedFromBatchIds": [9],
  "message": "Order placed. Inventory reserved."
}
```

#### 2. Get All Orders
```bash
GET http://localhost:8082/order
```

#### 3. Get Order by ID
```bash
GET http://localhost:8082/order/{orderId}
```

### Swagger UI

Access interactive API documentation:

- **Inventory Service:** http://localhost:8081/swagger-ui.html
- **Order Service:** http://localhost:8082/swagger-ui.html

### H2 Console

Access the H2 database consoles:

- **Inventory DB:** http://localhost:8081/h2-console
  - JDBC URL: `jdbc:h2:mem:inventorydb`
  - Username: `sa`
  - Password: (leave blank)

- **Order DB:** http://localhost:8082/h2-console
  - JDBC URL: `jdbc:h2:mem:orderdb`
  - Username: `sa`
  - Password: (leave blank)

## 🧪 Testing

### Run All Tests

```bash
# Test Inventory Service
cd inventory-service
mvn test

# Test Order Service
cd order-service
mvn test
```

### Test Coverage

Both services include:
- **Unit Tests** - Service layer logic with Mockito
- **Integration Tests** - Controller endpoints with @SpringBootTest
- **Repository Tests** - Data access layer validation

### Sample Test Scenarios

**Inventory Service:**
- ✅ Get inventory sorted by expiry date
- ✅ Check availability with sufficient stock
- ✅ Check availability with insufficient stock
- ✅ Update inventory successfully
- ✅ Handle inventory update failures

**Order Service:**
- ✅ Place order successfully
- ✅ Handle insufficient inventory
- ✅ Retrieve all orders
- ✅ Get order by ID
- ✅ Inter-service communication

## 🎨 Design Patterns

### Factory Pattern Implementation

The **Factory Design Pattern** is implemented in the Inventory Service to allow future extensibility:

```java
public interface InventoryHandler {
    InventoryResponse processInventory(Long productId, List<InventoryBatch> batches);
    List<Long> reserveInventory(Long productId, Integer quantity, List<InventoryBatch> batches);
    boolean supports(String inventoryType);
}
```

**Current Implementation:**
- `StandardInventoryHandler` - FIFO strategy for normal products

**Future Extensions (Examples):**
- `PerishableInventoryHandler` - Special handling for perishable items
- `BulkInventoryHandler` - Bulk order optimization
- `PriorityInventoryHandler` - Priority-based allocation

**Benefits:**
- ✅ Open for extension, closed for modification (SOLID principles)
- ✅ Easy to add new inventory strategies without changing existing code
- ✅ Loosely coupled architecture
- ✅ Spring automatically discovers and registers new handlers

## 🗄️ Database Schema

### Inventory Service

**Table: inventory_batch**
| Column | Type | Description |
|--------|------|-------------|
| batch_id | BIGINT | Primary key |
| product_id | BIGINT | Product identifier |
| product_name | VARCHAR(255) | Product name |
| quantity | INT | Available quantity |
| expiry_date | DATE | Batch expiry date |

### Order Service

**Table: orders**
| Column | Type | Description |
|--------|------|-------------|
| order_id | BIGINT | Primary key (auto-increment) |
| product_id | BIGINT | Product identifier |
| product_name | VARCHAR(255) | Product name |
| quantity | INT | Ordered quantity |
| status | VARCHAR(50) | Order status (PLACED, SHIPPED, DELIVERED, CANCELLED) |
| order_date | DATE | Order creation date |
| reserved_batch_ids | VARCHAR(500) | Comma-separated batch IDs |

## 📊 Sample Data

The application loads sample data automatically via Liquibase:

