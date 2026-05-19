# 4.2 Software Testing

> Write tests that catch real bugs. Unit, integration, and mocking.

## Testing Pyramid

```
        /\
       /  \      E2E Tests (few)
      /────\
     /      \    Integration Tests (some)
    /────────\
   /          \  Unit Tests (many)
  /────────────\
```

## Unit Testing with JUnit 5

```java
class ShoppingCartTest {

    @Test
    void shouldCalculateTotalCorrectly() {
        ShoppingCart cart = new ShoppingCart();
        cart.addItem(new Product("Book", new BigDecimal("19.99")), 2);
        cart.addItem(new Product("Pen", new BigDecimal("1.50")), 3);

        BigDecimal total = cart.getTotal();

        assertEquals(new BigDecimal("44.48"), total);
    }

    @Test
    void shouldNotAllowNegativeQuantity() {
        ShoppingCart cart = new ShoppingCart();
        assertThrows(IllegalArgumentException.class, () ->
            cart.addItem(new Product("Book", new BigDecimal("10")), -1));
    }

    @Test
    void shouldApplyDiscount() {
        ShoppingCart cart = new ShoppingCart();
        cart.addItem(new Product("Book", new BigDecimal("100")), 1);
        cart.applyDiscount(new PercentageDiscount(10));  // 10% off

        assertEquals(new BigDecimal("90"), cart.getTotal());
    }
}
```

## Mockito for Dependencies

```java
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private PaymentGateway paymentGateway;

    @Mock
    private InventoryService inventoryService;

    @InjectMocks
    private OrderService orderService;

    @Test
    void shouldProcessOrderSuccessfully() {
        // Arrange
        when(inventoryService.isAvailable("PROD-1", 2)).thenReturn(true);
        when(paymentGateway.process(any(BigDecimal.class)))
            .thenReturn(PaymentResult.success());

        // Act
        Order order = orderService.placeOrder(user, cart);

        // Assert
        assertNotNull(order);
        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
        verify(inventoryService).deduct("PROD-1", 2);
    }

    @Test
    void shouldRollbackOnPaymentFailure() {
        when(inventoryService.isAvailable("PROD-1", 2)).thenReturn(true);
        when(paymentGateway.process(any())).thenReturn(PaymentResult.failure("Declined"));

        assertThrows(PaymentFailedException.class, () ->
            orderService.placeOrder(user, cart));

        verify(inventoryService, never()).deduct(anyString(), anyInt());
    }
}
```

## Test Structure: AAA Pattern

```java
@Test
void testName() {
    // Arrange — set up the test
    ShoppingCart cart = new ShoppingCart();
    cart.addItem(product, 2);

    // Act — execute the behavior being tested
    BigDecimal total = cart.getTotal();

    // Assert — verify the result
    assertEquals(expected, total);
}
```

## Parameterized Tests

```java
@ParameterizedTest
@ValueSource(strings = {"email@test.com", "user@domain.org", "a@b.co"})
void shouldAcceptValidEmails(String email) {
    assertTrue(EmailValidator.isValid(email));
}

@ParameterizedTest
@CsvSource({
    "10, 20, 30",
    "0, 0, 0",
    "-5, 10, 5"
})
void shouldAddNumbersCorrectly(int a, int b, int expected) {
    assertEquals(expected, a + b);
}
```

## Testing Concurrency

```java
@Test
void shouldHandleConcurrentAccess() throws InterruptedException {
    Counter counter = new Counter();
    int threadCount = 100;
    int incrementsPerThread = 1000;

    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    CountDownLatch latch = new CountDownLatch(threadCount);

    for (int i = 0; i < threadCount; i++) {
        executor.submit(() -> {
            for (int j = 0; j < incrementsPerThread; j++) {
                counter.increment();
            }
            latch.countDown();
        });
    }

    latch.await();
    executor.shutdown();

    assertEquals(threadCount * incrementsPerThread, counter.getCount());
}
```

## Interview Testing Checklist

- [ ] Write tests using AAA pattern
- [ ] Mock dependencies with Mockito
- [ ] Test edge cases (null, empty, boundary values)
- [ ] Test exception paths (`assertThrows`)
- [ ] Parameterize tests for multiple inputs
- [ ] Test concurrent code with `CountDownLatch`
- [ ] Know when to unit test vs integration test
