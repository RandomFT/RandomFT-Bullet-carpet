package carpet.prometheus.metrics;

import carpet.prometheus.PrometheusExtension;
import net.minecraft.world.WorldServer;

public class LoadedChunksMetrics extends AbstractMetric {
	
	public LoadedChunksMetrics(String name, String help) {
        super(name, help, "world");
    }

    @Override
    public void update(PrometheusExtension extension) {
    	for (WorldServer world : extension.getServer().worlds) {
            this.getGauge().labels(world.provider.getDimensionType().getName()).set(world.getChunkProvider().getLoadedChunkCount());
        }
    }

}