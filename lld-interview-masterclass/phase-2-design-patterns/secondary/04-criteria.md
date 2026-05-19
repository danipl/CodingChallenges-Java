# Criteria Pattern

> Build complex, nested filters dynamically. Essential for query builders and search systems.

## Why?

When you need to combine filters (AND, OR, NOT) in arbitrary, runtime-determined combinations.

## Where?

- **JPA/Hibernate**: `CriteriaBuilder` for dynamic queries
- **Elasticsearch**: Bool queries with must/should/must_not
- **E-commerce**: Product filters (price AND brand OR category)
- **Admin panels**: Dynamic search with multiple filter conditions

## How

```java
// 1. Criteria interface
interface Criteria<T> {
    List<T> meet(List<T> items);
}

// 2. Atomic criteria
class PriceRangeCriteria implements Criteria<Product> {
    private final BigDecimal min, max;
    PriceRangeCriteria(BigDecimal min, BigDecimal max) { this.min = min; this.max = max; }
    public List<Product> meet(List<Product> items) {
        return items.stream()
            .filter(p -> p.getPrice().compareTo(min) >= 0 && p.getPrice().compareTo(max) <= 0)
            .toList();
    }
}

class BrandCriteria implements Criteria<Product> {
    private final String brand;
    BrandCriteria(String brand) { this.brand = brand; }
    public List<Product> meet(List<Product> items) {
        return items.stream().filter(p -> p.getBrand().equals(brand)).toList();
    }
}

// 3. Composite criteria (AND, OR)
class AndCriteria<T> implements Criteria<T> {
    private final List<Criteria<T>> criteria;
    AndCriteria(Criteria<T>... c) { this.criteria = Arrays.asList(c); }
    public List<T> meet(List<T> items) {
        for (Criteria<T> c : criteria) {
            items = c.meet(items);
        }
        return items;
    }
}

class OrCriteria<T> implements Criteria<T> {
    private final List<Criteria<T>> criteria;
    OrCriteria(Criteria<T>... c) { this.criteria = Arrays.asList(c); }
    public List<T> meet(List<T> items) {
        Set<T> result = new LinkedHashSet<>();
        for (Criteria<T> c : criteria) {
            result.addAll(c.meet(new ArrayList<>(items)));
        }
        return new ArrayList<>(result);
    }
}

// 4. Usage — nested filters
Criteria<Product> filter = new AndCriteria<>(
    new PriceRangeCriteria(new BigDecimal("10"), new BigDecimal("100")),
    new OrCriteria<>(
        new BrandCriteria("Nike"),
        new BrandCriteria("Adidas")
    )
);
List<Product> results = filter.match(allProducts);
// Products priced $10-$100 AND (Nike OR Adidas)
```

## Interview Application

- **E-commerce product search**: Price + brand + rating + availability
- **User search**: Age range + location + interests + activity status
- **Log filtering**: Level + service + time range + keyword

## Building a Query Builder

```java
class QueryBuilder<T> {
    private final List<Criteria<T>> andCriteria = new ArrayList<>();
    private final List<Criteria<T>> orCriteria = new ArrayList<>();

    QueryBuilder<T> and(Criteria<T> c) { andCriteria.add(c); return this; }
    QueryBuilder<T> or(Criteria<T> c) { orCriteria.add(c); return this; }

    List<T> execute(List<T> items) {
        List<T> result = items;
        for (Criteria<T> c : andCriteria) result = c.meet(result);
        if (!orCriteria.isEmpty()) {
            result = new OrCriteria<>(orCriteria.toArray(new Criteria[0])).meet(result);
        }
        return result;
    }
}

// Fluent API
List<Product> results = new QueryBuilder<Product>()
    .and(new PriceRangeCriteria(min, max))
    .and(new RatingCriteria(4))
    .or(new BrandCriteria("Nike"))
    .or(new BrandCriteria("Adidas"))
    .execute(products);
```
