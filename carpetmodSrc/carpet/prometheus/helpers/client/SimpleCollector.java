package carpet.prometheus.helpers.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public abstract class SimpleCollector<Child> extends Collector {
    protected final String fullname;
    protected final String help;
    protected final String unit;
    protected final List<String> labelNames;
    protected final ConcurrentMap<List<String>, Child> children = new ConcurrentHashMap();
    protected Child noLabelsChild;

    public Child labels(String... labelValues) {
        if (labelValues.length != this.labelNames.size()) {
            throw new IllegalArgumentException("Incorrect number of labels.");
        } else {
            String[] var2 = labelValues;
            int var3 = labelValues.length;

            for(int var4 = 0; var4 < var3; ++var4) {
                String label = var2[var4];
                if (label == null) {
                    throw new IllegalArgumentException("Label cannot be null.");
                }
            }

            List<String> key = Arrays.asList(labelValues);
            Child c = this.children.get(key);
            if (c != null) {
                return c;
            } else {
                Child c2 = this.newChild();
                Child tmp = this.children.putIfAbsent(key, c2);
                return tmp == null ? c2 : tmp;
            }
        }
    }

    public void remove(String... labelValues) {
        this.children.remove(Arrays.asList(labelValues));
        this.initializeNoLabelsChild();
    }

    public void clear() {
        this.children.clear();
        this.initializeNoLabelsChild();
    }

    protected void initializeNoLabelsChild() {
        if (this.labelNames.size() == 0) {
            this.noLabelsChild = this.labels();
        }

    }

    public <T extends Collector> SimpleCollector<Child> setChild(Child child, String... labelValues) {
        if (labelValues.length != this.labelNames.size()) {
            throw new IllegalArgumentException("Incorrect number of labels.");
        } else {
            this.children.put(Arrays.asList(labelValues), child);
            return this;
        }
    }

    protected abstract Child newChild();

    protected List<MetricFamilySamples> familySamplesList(Type type, List<MetricFamilySamples.Sample> samples) {
        MetricFamilySamples mfs = new MetricFamilySamples(this.fullname, this.unit, type, this.help, samples);
        List<MetricFamilySamples> mfsList = new ArrayList(1);
        mfsList.add(mfs);
        return mfsList;
    }

    protected SimpleCollector(Builder b) {
        if (b.name.isEmpty()) {
            throw new IllegalStateException("Name hasn't been set.");
        } else {
            String name = b.name;
            if (!b.subsystem.isEmpty()) {
                name = b.subsystem + '_' + name;
            }

            if (!b.namespace.isEmpty()) {
                name = b.namespace + '_' + name;
            }

            this.unit = b.unit;
            if (!this.unit.isEmpty() && !name.endsWith("_" + this.unit)) {
                name = name + "_" + this.unit;
            }

            this.fullname = name;
            checkMetricName(this.fullname);
            if (b.help != null && b.help.isEmpty()) {
                throw new IllegalStateException("Help hasn't been set.");
            } else {
                this.help = b.help;
                this.labelNames = Arrays.asList(b.labelNames);
                Iterator var3 = this.labelNames.iterator();

                while(var3.hasNext()) {
                    String n = (String)var3.next();
                    checkMetricLabelName(n);
                }

                if (!b.dontInitializeNoLabelsChild) {
                    this.initializeNoLabelsChild();
                }

            }
        }
    }

    public abstract static class Builder<B extends Builder<B, C>, C extends SimpleCollector> {
        String namespace = "";
        String subsystem = "";
        String name = "";
        String fullname = "";
        String unit = "";
        String help = "";
        String[] labelNames = new String[0];
        boolean dontInitializeNoLabelsChild;

        public Builder() {
        }

        public Builder<B, C> name(String name) {
            this.name = name;
            return this;
        }

        public Builder<B, C> subsystem(String subsystem) {
            this.subsystem = subsystem;
            return this;
        }

        public Builder<B, C> namespace(String namespace) {
            this.namespace = namespace;
            return this;
        }

        public Builder<B, C> unit(String unit) {
            this.unit = unit;
            return this;
        }

        public Builder<B, C> help(String help) {
            this.help = help;
            return this;
        }

        public Builder<B, C> labelNames(String... labelNames) {
            this.labelNames = labelNames;
            return this;
        }

        public abstract C create();

        public C register() {
            return this.register(CollectorRegistry.defaultRegistry);
        }

        public C register(CollectorRegistry registry) {
            C sc = this.create();
            registry.register(sc);
            return sc;
        }
    }
}

