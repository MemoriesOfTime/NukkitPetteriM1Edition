package cn.nukkit.utils.spawners;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.entity.BaseEntity;
import cn.nukkit.entity.passive.EntityPolarBear;
import cn.nukkit.level.Level;
import cn.nukkit.level.Position;
import cn.nukkit.utils.AbstractEntitySpawner;
import cn.nukkit.utils.SpawnerTask;
import cn.nukkit.utils.Utils;

public class PolarBearSpawner extends AbstractEntitySpawner {

    public PolarBearSpawner(SpawnerTask spawnTask) {
        super(spawnTask);
    }

    @Override
    public void spawn(Player player, Position pos, Level level) {
        if (Utils.rand(1, 3) != 1) {
            return;
        }
        if (level.getBiomeId((int) pos.x, (int) pos.z) == 12) {
            if (level.isAnimalSpawningAllowedByTime()) {
                int blockId = level.getBlockIdAt((int) pos.x, (int) pos.y, (int) pos.z);
                if (blockId == Block.GRASS || blockId == Block.SNOW_LAYER) {
                    for (int i = 0; i < Utils.rand(1, 2); i++) {
                        BaseEntity entity = this.spawnTask.createEntity("PolarBear", pos.add(0.5, 1, 0.5));
                        if (entity == null) return;
                        if (Utils.rand(1, 20) == 1) {
                            entity.setBaby(true);
                        }
                    }
                }
            }
        }
    }

    @Override
    public final int getEntityNetworkId() {
        return EntityPolarBear.NETWORK_ID;
    }
}
