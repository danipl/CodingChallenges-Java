# E-Commerce Platform

> CQRS architecture, product filtering, cart management, and order processing.

## Requirements

- Product catalog with search and filters
- Shopping cart management
- Order placement and processing
- User reviews and ratings
- Inventory management

## Architecture: CQRS (Command Query Responsibility Segregation)

```
┌─────────────┐     ┌──────────────┐
│   Commands   │     │    Queries    │
│  (Write)     │     │   (Read)      │
├─────────────┤     ├──────────────┤
│ PlaceOrder  │     │ GetProducts   │
│ AddToCart   │     │ SearchProducts│
│ UpdateStock │     │ GetOrder      │
└──────┬──────┘     └──────┬───────┘
       │                   │
  ┌────▼────┐         ┌────▼────┐
  │ Write   │         │ Read    │
  │ Model   │         │ Model   │
  │ (DB)    │         │ (Cache) │
  └─────────┘         └─────────┘
```

## Key Patterns

### Criteria Pattern (Product Filtering)
```java
// See Criteria pattern doc for full implementation
Criteria<Product> filter = new QueryBuilder<Product>()
    .and(new PriceRangeCriteria(min, max))
    .and(new CategoryCriteria("Electronics"))
    .and(new RatingCriteria(4))
    .or(new BrandCriteria("Apple"))
    .or(new BrandCriteria("Samsung"))
    .execute(products);
```

### Observer (Order Events)
```java
class OrderEventBus extends EventBus {
    void orderPlaced(Order order) {
        publish(new Event(ORDER_PLACED, order));
    }
}

// Subscribers: InventoryManager, EmailNotifier, AnalyticsTracker
```

### Strategy (Shipping)
```java
interface ShippingStrategy {
    BigDecimal calculateCost(Order order);
    Duration estimateDelivery(Order order);
}

class StandardShipping implements ShippingStrategy { /* 5-7 days, $5 */ }
class ExpressShipping implements ShippingStrategy { /* 1-2 days, $15 */ }
class OvernightShipping implements ShippingStrategy { /* next day, $25 */ }
```

## Core Implementation

```java
class ShoppingCart {
    private final Map<ProductId, Integer> items = new HashMap<>();
    private final InventoryService inventory;

    synchronized void addItem(Product product, int quantity) {
        if (!inventory.isAvailable(product.getId(), quantity)) {
            throw new InsufficientStockException();
        }
        items.merge(product.getId(), quantity, Integer::sum);
    }

    BigDecimal getTotal() {
        return items.entrySet().stream()
            .map(e -> e.getKey().getPrice().multiply(BigDecimal.valueOf(e.getValue())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}

class OrderService {
    private final EventBus eventBus;

    Order placeOrder(User user, ShoppingCart cart, ShippingStrategy shipping) {
        Order order = new Order(user, new ArrayList<>(cart.getItems()), shipping);
        order.place();

        // Deduct inventory
        for (var entry : cart.getItems().entrySet()) {
            inventory.deduct(entry.getKey().getId(), entry.getValue());
        }

        // Publish event
        eventBus.publish(new Event(ORDER_PLACED, order));

        return order;
    }
}
```

## Interview Tips

1. **CQRS**: Separate read and write models — reads can be cached, writes must be consistent
2. **Inventory concurrency**: `synchronized` or database-level locking
3. **Filtering**: Criteria pattern for composable, nested filters
4. **Event-driven**: Order placement triggers multiple downstream actions
5. **Scalability**: Read replicas for product catalog, single writer for orders
