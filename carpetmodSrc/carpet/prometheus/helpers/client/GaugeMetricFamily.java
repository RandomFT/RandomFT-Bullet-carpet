package carpet.prometheus.helpers.client;

import carpet.prometheus.helpers.client.Collector.MetricFamilySamples;
import carpet.prometheus.helpers.client.Collector.Type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GaugeMetricFamily extends MetricFamilySamples {
    private final List<String> labelNames;

    public GaugeMetricFamily(String name, String help, double value) {
        super(name, Type.GAUGE, help, new ArrayList());
        this.labelNames = Collections.emptyList();
        this.samples.add(new Sample(name, this.labelNames, Collections.emptyList(), value));
    }

    public GaugeMetricFamily(String name, String help, List<String> labelNames) {
        super(name, Type.GAUGE, help, new ArrayList());
        this.labelNames = labelNames;
    }

    public GaugeMetricFamily addMetric(List<String> labelValues, double value) {
        if (labelValues.size() != this.labelNames.size()) {
            throw new IllegalArgumentException("Incorrect number of labels.");
        }
        else {
            this.samples.add(new Sample(this.name, this.labelNames, labelValues, value));
            return this;
        }
    }
}
