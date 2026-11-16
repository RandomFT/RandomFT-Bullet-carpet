package carpet.prometheus.helpers.client;

import carpet.prometheus.helpers.client.Collector.Describable;

import java.io.Closeable;
import java.util.*;
import java.util.concurrent.Callable;

public class Gauge extends SimpleCollector<Gauge.Child> implements Describable {
    Gauge(Builder b) {
        super(b);
    }

    public static Builder build(String name, String help) {
        return (Builder)((Builder)(new Builder()).name(name)).help(help);
    }

    public static Builder build() {
        return new Builder();
    }

    protected Child newChild() {
        return new Child();
    }

    public void inc() {
        this.inc(1.0D);
    }

    public void inc(double amt) {
        ((Child)this.noLabelsChild).inc(amt);
    }

    public void dec() {
        this.dec(1.0D);
    }

    public void dec(double amt) {
        ((Child)this.noLabelsChild).dec(amt);
    }

    public void set(double val) {
        ((Child)this.noLabelsChild).set(val);
    }

    public void setToCurrentTime() {
        ((Child)this.noLabelsChild).setToCurrentTime();
    }

    public Timer startTimer() {
        return ((Child)this.noLabelsChild).startTimer();
    }

    public double setToTime(Runnable timeable) {
        return ((Child)this.noLabelsChild).setToTime(timeable);
    }

    public <Object> Object setToTime(Callable<Object> timeable) {//Chamged <>
        return ((Child)this.noLabelsChild).setToTime(timeable);
    }

    public double get() {
        return ((Child)this.noLabelsChild).get();
    }

    public List<MetricFamilySamples> collect() {
        List<MetricFamilySamples.Sample> samples = new ArrayList(this.children.size());
        Iterator var2 = this.children.entrySet().iterator();

        while(var2.hasNext()) {
            Map.Entry<List<String>, Child> c = (Map.Entry)var2.next();
            samples.add(new MetricFamilySamples.Sample(this.fullname, this.labelNames, (List)c.getKey(), ((Child)c.getValue()).get()));
        }

        return this.familySamplesList(Type.GAUGE, samples);
    }

    public List<MetricFamilySamples> describe() {
        return Collections.singletonList(new GaugeMetricFamily(this.fullname, this.help, this.labelNames));
    }

    static class TimeProvider {
        TimeProvider() {
        }

        long currentTimeMillis() {
            return System.currentTimeMillis();
        }

        long nanoTime() {
            return System.nanoTime();
        }
    }

    public static class Child {
        private final DoubleAdder value = new DoubleAdder();
        static TimeProvider timeProvider = new TimeProvider();

        public Child() {
        }

        public void inc() {
            this.inc(1.0D);
        }

        public void inc(double amt) {
            this.value.add(amt);
        }

        public void dec() {
            this.dec(1.0D);
        }

        public void dec(double amt) {
            this.value.add(-amt);
        }

        public void set(double val) {
            this.value.set(val);
        }

        public void setToCurrentTime() {
            this.set((double)timeProvider.currentTimeMillis() / 1000.0D);
        }

        public Timer startTimer() {
            return new Timer(this);
        }

        public double setToTime(Runnable timeable) {
            Timer timer = this.startTimer();

            double elapsed;
            try {
                timeable.run();
            } finally {
                elapsed = timer.setDuration();
            }

            return elapsed;
        }

        public <Object> Object setToTime(Callable<Object> timeable) {//Changed E for Object and <E> E too
           Timer timer = this.startTimer();

            Object var3;
            try {
                var3 = timeable.call();
            } catch (Exception var7) {
                throw new RuntimeException(var7);
            } finally {
                timer.setDuration();
            }

            return var3;
        }

        public double get() {
            return this.value.sum();
        }
    }

    public static class Timer implements Closeable {
        private final Child child;
        private final long start;

        private Timer(Child child) {
            this.child = child;
            this.start = Child.timeProvider.nanoTime();
        }

        public double setDuration() {
            double elapsed = (double)(Child.timeProvider.nanoTime() - this.start) / 1.0E9D;
            this.child.set(elapsed);
            return elapsed;
        }

        public void close() {
            this.setDuration();
        }
    }

    public static class Builder extends SimpleCollector.Builder<Builder,Gauge> {
        public Builder() {
        }

        public Gauge create() {
            return new Gauge(this);
        }
    }
}
