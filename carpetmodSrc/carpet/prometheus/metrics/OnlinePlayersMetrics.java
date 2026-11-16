package carpet.prometheus.metrics;

import carpet.prometheus.PrometheusExtension;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.WorldServer;

public class OnlinePlayersMetrics extends AbstractMetric {
	
	public OnlinePlayersMetrics(String name, String help) {
        super(name, help, "world", "name", "uuid");
    }

    @Override
    public void update(PrometheusExtension extension) {
        for (WorldServer world : extension.getServer().worlds) {
        	for (EntityPlayer player : world.playerEntities) {
                this.getGauge().labels(world.provider.getDimensionType().getName(), player.getName(), player.getUniqueID().toString()).set(1);
            }
        }
    }

}