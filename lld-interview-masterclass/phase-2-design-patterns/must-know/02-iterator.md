# Iterator Pattern

> Traverse collections without exposing internal structure.

## Why?

Clients need to walk through a collection but shouldn't know if it's a list, tree, graph, or stream.

## Where?

- `java.util.Iterator` — every Java collection
- Database cursor iteration
- File system traversal
- Pagination APIs

## How

```java
// 1. Iterator interface
interface Iterator<T> {
    boolean hasNext();
    T next();
}

// 2. Iterable collection
interface IterableCollection<T> {
    Iterator<T> iterator();
}

// 3. Concrete implementation
class BookCollection implements IterableCollection<Book> {
    private final List<Book> books = new ArrayList<>();

    @Override
    public Iterator<Book> iterator() {
        return new BookIterator();
    }

    private class BookIterator implements Iterator<Book> {
        private int index = 0;
        public boolean hasNext() { return index < books.size(); }
        public Book next() { return books.get(index++); }
    }
}

// 4. Usage
BookCollection library = new BookCollection();
for (Book book : library) {  // Works with for-each
    System.out.println(book.getTitle());
}
```

## Interview Application

- **Tree traversal**: In-order, pre-order, post-order iterators
- **Merged sorted lists**: Iterator that merges multiple sorted streams
- **Filtered iteration**: Iterator that skips elements matching a predicate
