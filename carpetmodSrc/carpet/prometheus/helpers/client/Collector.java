package carpet.prometheus.helpers.client;

import carpet.prometheus.helpers.client.exemplars.Exemplar;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

public abstract class Collector {
    public static final double NANOSECONDS_PER_SECOND = 1.0E9D;
    public static final double MILLISECONDS_PER_SECOND = 1000.0D;
    private static final Pattern METRIC_NAME_RE = Pattern.compile("[a-zA-Z_:][a-zA-Z0-9_:]*");
    private static final Pattern METRIC_LABEL_NAME_RE = Pattern.compile("[a-zA-Z_][a-zA-Z0-9_]*");
    private static final Pattern RESERVED_METRIC_LABEL_NAME_RE = Pattern.compile("__.*");
    private static final Pattern SANITIZE_PREFIX_PATTERN = Pattern.compile("^[^a-zA-Z_:]");
    private static final Pattern SANITIZE_BODY_PATTERN = Pattern.compile("[^a-zA-Z0-9_:]");

    public Collector() {
    }

    public abstract List<MetricFamilySamples> collect();

    public List<MetricFamilySamples> collect(Predicate<String> sampleNameFilter) {
        List<MetricFamilySamples> all = this.collect();
        if (sampleNameFilter == null) {
            return all;
        }
        else {
            List<MetricFamilySamples> remaining = new ArrayList(all.size());
            Iterator var4 = all.iterator();

            while (true) {
                while (var4.hasNext()) {
                    MetricFamilySamples mfs = (MetricFamilySamples) var4.next();
                    String[] var6 = mfs.getNames();
                    int var7 = var6.length;

                    for (int var8 = 0; var8 < var7; ++var8) {
                        String name = var6[var8];
                        if (sampleNameFilter.test(name)) {
                            remaining.add(mfs);
                            break;
                        }
                    }
                }

                return remaining;
            }
        }
    }
    public <T extends Collector> Collector unregister() {
        unregister(CollectorRegistry.defaultRegistry);
        return this;
    }
    public <T extends Collector> Collector unregister(CollectorRegistry registry) {
        registry.unregister(this);
        return this;
    }

    public <T extends Collector> Collector register() {//Changed T with Collector
        return this.register(CollectorRegistry.defaultRegistry);
    }

    public <T extends Collector> Collector register(CollectorRegistry registry) {//Changed T with Collector
        registry.register(this);
        return this;
    }

    protected static void checkMetricName(String name) {
        if (!METRIC_NAME_RE.matcher(name).matches()) {
            throw new IllegalArgumentException("Invalid metric name: " + name);
        }
    }

    public static String sanitizeMetricName(String metricName) {
        return SANITIZE_BODY_PATTERN.matcher(SANITIZE_PREFIX_PATTERN.matcher(metricName).replaceFirst("_")).replaceAll("_");
    }

    protected static void checkMetricLabelName(String name) {
        if (!METRIC_LABEL_NAME_RE.matcher(name).matches()) {
            throw new IllegalArgumentException("Invalid metric label name: " + name);
        }
        else if (RESERVED_METRIC_LABEL_NAME_RE.matcher(name).matches()) {
            throw new IllegalArgumentException("Invalid metric label name, reserved for internal use: " + name);
        }
    }

    public static String doubleToGoString(double d) {
        if (d == 1.0D / 0.0) {
            return "+Inf";
        }
        else {
            return d == -1.0D / 0.0 ? "-Inf" : Double.toString(d);
        }
    }

    public interface Describable {
        List<MetricFamilySamples> describe();
    }

    public static class MetricFamilySamples {
        public final String name;
        public final String unit;
        public final Type type;
        public final String help;
        public final List<Sample> samples;

        public MetricFamilySamples(String name, Type type, String help, List<Sample> samples) {
            this(name, "", type, help, samples);
        }

        public MetricFamilySamples(String name, String unit,Type type, String help, List<Sample> samples) {
            if (!unit.isEmpty() && !name.endsWith("_" + unit)) {
                throw new IllegalArgumentException("Metric's unit is not the suffix of the metric name: " + name);
            }
            else if ((type ==Type.INFO || type == Type.STATE_SET) && !unit.isEmpty()) {
                throw new IllegalArgumentException("Metric is of a type that cannot have a unit: " + name);
            }
            else {
                List<Sample> mungedSamples = samples;
                if (type == Type.COUNTER) {
                    if (name.endsWith("_total")) {
                        name = name.substring(0, name.length() - 6);
                    }

                    String withTotal = name + "_total";
                    mungedSamples = new ArrayList(samples.size());

                    Sample s;
                    String n;
                    for (Iterator var8 = samples.iterator(); var8.hasNext(); ((List) mungedSamples).add(new Sample(n, s.labelNames, s.labelValues, s.value, s.exemplar, s.timestampMs))) {
                        s = (Sample) var8.next();
                        n = s.name;
                        if (name.equals(n)) {
                            n = withTotal;
                        }
                    }
                }

                this.name = name;
                this.unit = unit;
                this.type = type;
                this.help = help;
                this.samples = (List) mungedSamples;
            }
        }

        public MetricFamilySamples filter(Predicate<String> sampleNameFilter) {
            if (sampleNameFilter == null) {
                return this;
            }
            else {
                List<Sample> remainingSamples = new ArrayList(this.samples.size());
                Iterator var3 = this.samples.iterator();

                while (var3.hasNext()) {
                    Sample sample = (Sample) var3.next();
                    if (sampleNameFilter.test(sample.name)) {
                        remainingSamples.add(sample);
                    }
                }

                if (remainingSamples.isEmpty()) {
                    return null;
                }
                else {
                    return new MetricFamilySamples(this.name, this.unit, this.type, this.help, remainingSamples);
                }
            }
        }

        public String[] getNames() {
            switch (this.type) {
                case COUNTER:
                    return new String[]{this.name + "_total", this.name + "_created", this.name};
                case SUMMARY:
                    return new String[]{this.name + "_count", this.name + "_sum", this.name + "_created", this.name};
                case HISTOGRAM:
                    return new String[]{this.name + "_count", this.name + "_sum", this.name + "_bucket", this.name + "_created", this.name};
                case GAUGE_HISTOGRAM:
                    return new String[]{this.name + "_gcount", this.name + "_gsum", this.name + "_bucket", this.name};
                case INFO:
                    return new String[]{this.name + "_info", this.name};
                default:
                    return new String[]{this.name};
            }
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof MetricFamilySamples)) {
                return false;
            }
            else {
                MetricFamilySamples other = (MetricFamilySamples) obj;
                return other.name.equals(this.name) && other.unit.equals(this.unit) && other.type.equals(this.type) && other.help.equals(this.help) && other.samples.equals(this.samples);
            }
        }

        public int hashCode() {
            int hash = 1;
            hash = 37 * hash + this.name.hashCode();
            hash = 37 * hash + this.unit.hashCode();
            hash = 37 * hash + this.type.hashCode();
            hash = 37 * hash + this.help.hashCode();
            hash = 37 * hash + this.samples.hashCode();
            return hash;
        }

        public String toString() {
            return "Name: " + this.name + " Unit:" + this.unit + " Type: " + this.type + " Help: " + this.help + " Samples: " + this.samples;
        }

        public static class Sample {
            public final String name;
            public final List<String> labelNames;
            public final List<String> labelValues;
            public final double value;
            public final Exemplar exemplar;
            public final Long timestampMs;

            public Sample(String name, List<String> labelNames, List<String> labelValues, double value, Exemplar exemplar, Long timestampMs) {
                this.name = name;
                this.labelNames = labelNames;
                this.labelValues = labelValues;
                this.value = value;
                this.exemplar = exemplar;
                this.timestampMs = timestampMs;
            }

            public Sample(String name, List<String> labelNames, List<String> labelValues, double value, Long timestampMs) {
                this(name, labelNames, labelValues, value, (Exemplar) null, timestampMs);
            }

            public Sample(String name, List<String> labelNames, List<String> labelValues, double value, Exemplar exemplar) {
                this(name, labelNames, labelValues, value, exemplar, (Long) null);
            }

            public Sample(String name, List<String> labelNames, List<String> labelValues, double value) {
                this(name, labelNames, labelValues, value, (Exemplar) null, (Long) null);
            }

            public boolean equals(Object obj) {
                if (!(obj instanceof Sample)) {
                    return false;
                }
                else {
                    Sample other = (Sample) obj;
                    return other.name.equals(this.name) && other.labelNames.equals(this.labelNames) && other.labelValues.equals(this.labelValues) && other.value == this.value && (this.exemplar == null && other.exemplar == null || other.exemplar != null && other.exemplar.equals(this.exemplar)) && (this.timestampMs == null && other.timestampMs == null || other.timestampMs != null && other.timestampMs.equals(this.timestampMs));
                }
            }

            public int hashCode() {
                int hash = 1;
                hash = 37 * hash + this.name.hashCode();
                hash = 37 * hash + this.labelNames.hashCode();
                hash = 37 * hash + this.labelValues.hashCode();
                long d = Double.doubleToLongBits(this.value);
                hash = 37 * hash + (int) (d ^ d >>> 32);
                if (this.timestampMs != null) {
                    hash = 37 * hash + this.timestampMs.hashCode();
                }

                if (this.exemplar != null) {
                    hash = 37 * this.exemplar.hashCode();
                }

                return hash;
            }

            public String toString() {
                return "Name: " + this.name + " LabelNames: " + this.labelNames + " labelValues: " + this.labelValues + " Value: " + this.value + " TimestampMs: " + this.timestampMs;
            }
        }
    }

    public static enum Type {
        UNKNOWN,
        COUNTER,
        GAUGE,
        STATE_SET,
        INFO,
        HISTOGRAM,
        GAUGE_HISTOGRAM,
        SUMMARY;

        private Type() {
        }
    }
}

