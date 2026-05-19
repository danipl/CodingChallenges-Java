# Decorator Pattern

> Add behavior to objects dynamically without subclassing.

## Why?

Subclassing explodes combinatorially when you need to mix features.

## Where?

- **Java I/O**: `BufferedInputStream(new FileInputStream(...))`
- **Servlet**: `HttpServletRequestWrapper`
- **Caching**: Add caching layer around any service
- **Logging**: Wrap service with logging decorator

## How

```java
// 1. Component interface
interface DataSource {
    String read();
    void write(String data);
}

// 2. Concrete component
class FileDataSource implements DataSource {
    public String read() { return "file content"; }
    public void write(String data) { /* write to file */ }
}

// 3. Decorator base
abstract class DataSourceDecorator implements DataSource {
    protected final DataSource wrapped;
    DataSourceDecorator(DataSource wrapped) { this.wrapped = wrapped; }
    public String read() { return wrapped.read(); }
    public void write(String data) { wrapped.write(data); }
}

// 4. Concrete decorators
class CompressionDecorator extends DataSourceDecorator {
    CompressionDecorator(DataSource w) { super(w); }
    public String read() { return decompress(super.read()); }
    public void write(String data) { super.write(compress(data)); }
    private String compress(String d) { return "COMPRESSED[" + d + "]"; }
    private String decompress(String d) { return d.replace("COMPRESSED[", "").replace("]", ""); }
}

class EncryptionDecorator extends DataSourceDecorator {
    EncryptionDecorator(DataSource w) { super(w); }
    public String read() { return decrypt(super.read()); }
    public void write(String data) { super.write(encrypt(data)); }
    private String encrypt(String d) { return "ENCRYPTED[" + d + "]"; }
    private String decrypt(String d) { return d.replace("ENCRYPTED[", "").replace("]", ""); }
}

// 5. Compose dynamically
DataSource source = new FileDataSource();
source = new EncryptionDecorator(source);
source = new CompressionDecorator(source);  // Encrypt then compress
source.write("secret data");
```

## Interview Application

- **HTTP middleware**: Add auth, logging, rate limiting to any endpoint
- **Stream processing**: Add buffering, compression, encryption to any stream
- **UI components**: Add borders, scrollbars, shadows to any widget
