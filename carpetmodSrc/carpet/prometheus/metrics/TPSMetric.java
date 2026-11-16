package carpet.prometheus.metrics;

import carpet.helpers.TickSpeed;
import carpet.prometheus.PrometheusExtension;

public class TPSMetric extends AbstractMetric {
	
	public TPSMetric(String name, String help) {
        super(name, help);
    }

    @Override
    public void update(PrometheusExtension extension) {
        this.getGauge().set(TickSpeed.getTPS());
    }

}