package carpet.prometheus.metrics;

import carpet.prometheus.PrometheusExtension;
import carpet.prometheus.helpers.client.Gauge;

public abstract class AbstractMetric {
	
	private final Gauge gauge;
    private final String name;

    public AbstractMetric(String name, String help, String... labels) {
        this.name = name;
        this.gauge = new Gauge.Builder().
                name(String.format("minecraft_%s", name)).
                help(help).
                labelNames(labels).
                create();
    }

    public abstract void update(PrometheusExtension extension);

    public Gauge getGauge() {
        return this.gauge;
    }

    public String getName() {
        return this.name;
    }

}