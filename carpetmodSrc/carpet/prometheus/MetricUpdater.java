package carpet.prometheus;

import carpet.prometheus.metrics.AbstractMetric;

import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;

public class MetricUpdater extends TimerTask {
	
	private final PrometheusExtension extension;
    private final List<AbstractMetric> metrics = new ArrayList<>();

    public MetricUpdater(PrometheusExtension prometheusExtension) {
        this.extension = prometheusExtension;
    }

    @Override
    public void run() {
        for (AbstractMetric metric : this.metrics) {
            metric.getGauge().clear();
            try {
                metric.update(this.extension);
            } catch (Exception exception) {
                //exception.printStackTrace();
            }
        }
    }

    public void addMetric(AbstractMetric metric) {
        this.metrics.add(metric);
    }

    public List<AbstractMetric> getMetrics() {
        return metrics;
    }

}