package carpet.prometheus.helpers.client.exporter;

import carpet.prometheus.helpers.client.Predicate;
import carpet.prometheus.helpers.client.Supplier;

public class SampleNameFilterSupplier implements Supplier<Predicate<String>> {
    private final Predicate<String> sampleNameFilter;

    public static SampleNameFilterSupplier of(Predicate<String> sampleNameFilter) {
        return new SampleNameFilterSupplier(sampleNameFilter);
    }

    private SampleNameFilterSupplier(Predicate<String> sampleNameFilter) {
        this.sampleNameFilter = sampleNameFilter;
    }

    public Predicate<String> get() {
        return this.sampleNameFilter;
    }
}

