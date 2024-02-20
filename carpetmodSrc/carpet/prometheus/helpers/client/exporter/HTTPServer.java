package carpet.prometheus.helpers.client.exporter;

import carpet.prometheus.helpers.client.CollectorRegistry;
import carpet.prometheus.helpers.client.Predicate;
import carpet.prometheus.helpers.client.SampleNameFilter;
import carpet.prometheus.helpers.client.Supplier;
import carpet.prometheus.helpers.client.exporter.common.TextFormat;
import com.sun.net.httpserver.*;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPOutputStream;

public class HTTPServer implements Closeable {
    protected final HttpServer server;
    protected final ExecutorService executorService;

    protected static boolean shouldUseCompression(HttpExchange exchange) {
        List<String> encodingHeaders = exchange.getRequestHeaders().get("Accept-Encoding");
        if (encodingHeaders == null) {
            return false;
        }
        else {
            Iterator var2 = encodingHeaders.iterator();

            while (var2.hasNext()) {
                String encodingHeader = (String) var2.next();
                String[] encodings = encodingHeader.split(",");
                String[] var5 = encodings;
                int var6 = encodings.length;

                for (int var7 = 0; var7 < var6; ++var7) {
                    String encoding = var5[var7];
                    if (encoding.trim().equalsIgnoreCase("gzip")) {
                        return true;
                    }
                }
            }

            return false;
        }
    }

    protected static Set<String> parseQuery(String query) throws IOException {
        Set<String> names = new HashSet();
        if (query != null) {
            String[] pairs = query.split("&");
            String[] var3 = pairs;
            int var4 = pairs.length;

            for (int var5 = 0; var5 < var4; ++var5) {
                String pair = var3[var5];
                int idx = pair.indexOf("=");
                if (idx != -1 && URLDecoder.decode(pair.substring(0, idx), "UTF-8").equals("name[]")) {
                    names.add(URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
                }
            }
        }

        return names;
    }

    public HTTPServer(HttpServer httpServer, CollectorRegistry registry, boolean daemon) throws IOException {
        this(httpServer, registry, daemon, (Supplier) null, (Authenticator) null);
    }

    public HTTPServer(InetSocketAddress addr, CollectorRegistry registry, boolean daemon) throws IOException {
        this(HttpServer.create(addr, 3), registry, daemon);
    }

    public HTTPServer(InetSocketAddress addr, CollectorRegistry registry) throws IOException {
        this(addr, registry, false);
    }

    public HTTPServer(int port, boolean daemon) throws IOException {
        this(new InetSocketAddress(port), CollectorRegistry.defaultRegistry, daemon);
    }

    public HTTPServer(int port) throws IOException {
        this(port, false);
    }

    public HTTPServer(String host, int port, boolean daemon) throws IOException {
        this(new InetSocketAddress(host, port), CollectorRegistry.defaultRegistry, daemon);
    }

    public HTTPServer(String host, int port) throws IOException {
        this(new InetSocketAddress(host, port), CollectorRegistry.defaultRegistry, false);
    }

    private HTTPServer(HttpServer httpServer, CollectorRegistry registry, boolean daemon, Supplier<Predicate<String>> sampleNameFilterSupplier, Authenticator authenticator) {
        if (httpServer.getAddress() == null) {
            throw new IllegalArgumentException("HttpServer hasn't been bound to an address");
        }
        else {
            this.server = httpServer;
            HttpHandler mHandler = new HTTPMetricHandler(registry, sampleNameFilterSupplier);
            HttpContext mContext = this.server.createContext("/", mHandler);
            if (authenticator != null) {
                mContext.setAuthenticator(authenticator);
            }

            mContext = this.server.createContext("/metrics", mHandler);
            if (authenticator != null) {
                mContext.setAuthenticator(authenticator);
            }

            mContext = this.server.createContext("/-/healthy", mHandler);
            if (authenticator != null) {
                mContext.setAuthenticator(authenticator);
            }

            this.executorService = Executors.newFixedThreadPool(5, NamedDaemonThreadFactory.defaultThreadFactory(daemon));
            this.server.setExecutor(this.executorService);
            this.start(daemon);
        }
    }

    private void start(boolean daemon) {
        if (daemon == Thread.currentThread().isDaemon()) {
            this.server.start();
        }
        else {
            FutureTask<Void> startTask = new FutureTask(new Runnable() {
                public void run() {
                    HTTPServer.this.server.start();
                }
            }, (Object) null);
            NamedDaemonThreadFactory.defaultThreadFactory(daemon).newThread(startTask).start();

            try {
                startTask.get();
            } catch (ExecutionException var4) {
                throw new RuntimeException("Unexpected exception on starting HTTPSever", var4);
            } catch (InterruptedException var5) {
                Thread.currentThread().interrupt();
            }
        }

    }

    /**
     * @deprecated
     */
    public void stop() {
        this.close();
    }

    public void close() {
        this.server.stop(0);
        this.executorService.shutdown();
    }

    public int getPort() {
        return this.server.getAddress().getPort();
    }

    static {
        if (!System.getProperties().containsKey("sun.net.httpserver.maxReqTime")) {
            System.setProperty("sun.net.httpserver.maxReqTime", "60");
        }

        if (!System.getProperties().containsKey("sun.net.httpserver.maxRspTime")) {
            System.setProperty("sun.net.httpserver.maxRspTime", "600");
        }

    }

    public static class Builder {
        private int port = 0;
        private String hostname = null;
        private InetAddress inetAddress = null;
        private InetSocketAddress inetSocketAddress = null;
        private HttpServer httpServer = null;
        private CollectorRegistry registry;
        private boolean daemon;
        private Predicate<String> sampleNameFilter;
        private Supplier<Predicate<String>> sampleNameFilterSupplier;
        private Authenticator authenticator;

        public Builder() {
            this.registry = CollectorRegistry.defaultRegistry;
            this.daemon = false;
        }

        public Builder withPort(int port) {
            this.port = port;
            return this;
        }

        public Builder withHostname(String hostname) {
            this.hostname = hostname;
            return this;
        }

        public Builder withInetAddress(InetAddress address) {
            this.inetAddress = address;
            return this;
        }

        public Builder withInetSocketAddress(InetSocketAddress address) {
            this.inetSocketAddress = address;
            return this;
        }

        public Builder withHttpServer(HttpServer httpServer) {
            this.httpServer = httpServer;
            return this;
        }

        public Builder withDaemonThreads(boolean daemon) {
            this.daemon = daemon;
            return this;
        }

        public Builder withSampleNameFilter(Predicate<String> sampleNameFilter) {
            this.sampleNameFilter = sampleNameFilter;
            return this;
        }

        public Builder withSampleNameFilterSupplier(Supplier<Predicate<String>> sampleNameFilterSupplier) {
            this.sampleNameFilterSupplier = sampleNameFilterSupplier;
            return this;
        }

        public Builder withRegistry(CollectorRegistry registry) {
            this.registry = registry;
            return this;
        }

        public Builder withAuthenticator(Authenticator authenticator) {
            this.authenticator = authenticator;
            return this;
        }

        public HTTPServer build() throws IOException {
            if (this.sampleNameFilter != null) {
                this.assertNull(this.sampleNameFilterSupplier, "cannot configure 'sampleNameFilter' and 'sampleNameFilterSupplier' at the same time");
                this.sampleNameFilterSupplier = SampleNameFilterSupplier.of(this.sampleNameFilter);
            }

            if (this.httpServer != null) {
                this.assertZero(this.port, "cannot configure 'httpServer' and 'port' at the same time");
                this.assertNull(this.hostname, "cannot configure 'httpServer' and 'hostname' at the same time");
                this.assertNull(this.inetAddress, "cannot configure 'httpServer' and 'inetAddress' at the same time");
                this.assertNull(this.inetSocketAddress, "cannot configure 'httpServer' and 'inetSocketAddress' at the same time");
                return new HTTPServer(this.httpServer, this.registry, this.daemon, this.sampleNameFilterSupplier, this.authenticator);
            }
            else {
                if (this.inetSocketAddress != null) {
                    this.assertZero(this.port, "cannot configure 'inetSocketAddress' and 'port' at the same time");
                    this.assertNull(this.hostname, "cannot configure 'inetSocketAddress' and 'hostname' at the same time");
                    this.assertNull(this.inetAddress, "cannot configure 'inetSocketAddress' and 'inetAddress' at the same time");
                }
                else if (this.inetAddress != null) {
                    this.assertNull(this.hostname, "cannot configure 'inetAddress' and 'hostname' at the same time");
                    this.inetSocketAddress = new InetSocketAddress(this.inetAddress, this.port);
                }
                else if (this.hostname != null) {
                    this.inetSocketAddress = new InetSocketAddress(this.hostname, this.port);
                }
                else {
                    this.inetSocketAddress = new InetSocketAddress(this.port);
                }

                return new HTTPServer(HttpServer.create(this.inetSocketAddress, 3), this.registry, this.daemon, this.sampleNameFilterSupplier, this.authenticator);
            }
        }

        private void assertNull(Object o, String msg) {
            if (o != null) {
                throw new IllegalStateException(msg);
            }
        }

        private void assertZero(int i, String msg) {
            if (i != 0) {
                throw new IllegalStateException(msg);
            }
        }
    }

    static class NamedDaemonThreadFactory implements ThreadFactory {
        private static final AtomicInteger POOL_NUMBER = new AtomicInteger(1);
        private final int poolNumber;
        private final AtomicInteger threadNumber;
        private final ThreadFactory delegate;
        private final boolean daemon;

        NamedDaemonThreadFactory(ThreadFactory delegate, boolean daemon) {
            this.poolNumber = POOL_NUMBER.getAndIncrement();
            this.threadNumber = new AtomicInteger(1);
            this.delegate = delegate;
            this.daemon = daemon;
        }

        public Thread newThread(Runnable r) {
            Thread t = this.delegate.newThread(r);
            t.setName(String.format("prometheus-http-%d-%d", this.poolNumber, this.threadNumber.getAndIncrement()));
            t.setDaemon(this.daemon);
            return t;
        }

        static ThreadFactory defaultThreadFactory(boolean daemon) {
            return new NamedDaemonThreadFactory(Executors.defaultThreadFactory(), daemon);
        }
    }

    public static class HTTPMetricHandler implements HttpHandler {
        private final CollectorRegistry registry;
        private final LocalByteArray response;
        private final Supplier<Predicate<String>> sampleNameFilterSupplier;
        private static final String HEALTHY_RESPONSE = "Exporter is Healthy.";

        HTTPMetricHandler(CollectorRegistry registry) {
            this(registry, (Supplier) null);
        }

        HTTPMetricHandler(CollectorRegistry registry, Supplier<Predicate<String>> sampleNameFilterSupplier) {
            this.response = new LocalByteArray();
            this.registry = registry;
            this.sampleNameFilterSupplier = sampleNameFilterSupplier;
        }

        public void handle(HttpExchange t) throws IOException {
            String query = t.getRequestURI().getRawQuery();
            String contextPath = t.getHttpContext().getPath();
            ByteArrayOutputStream response = (ByteArrayOutputStream) this.response.get();
            response.reset();
            OutputStreamWriter osw = new OutputStreamWriter(response, Charset.forName("UTF-8"));
            if ("/-/healthy".equals(contextPath)) {
                osw.write("Exporter is Healthy.");
            }
            else {
                String contentType = TextFormat.chooseContentType(t.getRequestHeaders().getFirst("Accept"));
                t.getResponseHeaders().set("Content-Type", contentType);
                Predicate<String> filter = this.sampleNameFilterSupplier == null ? null : (Predicate) this.sampleNameFilterSupplier.get();
                filter = SampleNameFilter.restrictToNamesEqualTo(filter, HTTPServer.parseQuery(query));
                if (filter == null) {
                    TextFormat.writeFormat(contentType, osw, this.registry.metricFamilySamples());
                }
                else {
                    TextFormat.writeFormat(contentType, osw, this.registry.filteredMetricFamilySamples(filter));
                }
            }

            osw.close();
            if (HTTPServer.shouldUseCompression(t)) {
                t.getResponseHeaders().set("Content-Encoding", "gzip");
                t.sendResponseHeaders(200, 0L);
                GZIPOutputStream os = new GZIPOutputStream(t.getResponseBody());

                try {
                    response.writeTo(os);
                } finally {
                    os.close();
                }
            }
            else {
                t.getResponseHeaders().set("Content-Length", String.valueOf(response.size()));
                t.sendResponseHeaders(200, (long) response.size());
                response.writeTo(t.getResponseBody());
            }

            t.close();
        }
    }

    private static class LocalByteArray extends ThreadLocal<ByteArrayOutputStream> {
        private LocalByteArray() {
        }

        protected ByteArrayOutputStream initialValue() {
            return new ByteArrayOutputStream(1048576);
        }
    }
}
