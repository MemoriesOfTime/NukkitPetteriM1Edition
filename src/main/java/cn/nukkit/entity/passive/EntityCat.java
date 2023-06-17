package cn.nukkit.entity.passive;

import cn.nukkit.Player;
import cn.nukkit.entity.EntityCreature;
import cn.nukkit.item.Item;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.utils.Utils;

public class EntityCat extends EntityWalkingAnimal {

    public static final int NETWORK_ID = 75;

    public EntityCat(FullChunk chunk, CompoundTag nbt) {
        super(chunk, nbt);
    }

    @Override
    public int getNetworkId() {
        return NETWORK_ID;
    }

    @Override
    public float getWidth() {
        if (this.isBaby()) {
            return 0.3f;
        }
        return 0.6f;
    }

    @Override
    public float getHeight() {
        if (this.isBaby()) {
            return 0.35f;
        }
        return 0.7f;
    }

    @Override
    public void initEntity() {
        this.setMaxHealth(10);

        super.initEntity();

        this.noFallDamage = true;
    }

    @Override
    public Item[] getDrops() {
        if (!this.isBaby()) {
            int c = Utils.rand(0, 2);
            if (c > 0) return new Item[]{Item.get(Item.STRING, 0, c)};
        }

        return Item.EMPTY_ARRAY;
    }

    @Override
    public int getKillExperience() {
        return this.isBaby() ? 0 : Utils.rand(1, 3);
    }

    @Override
    public boolean targetOption(EntityCreature creature, double distance) {
        if (!this.isInLove() && creature instanceof Player) {
            Player player = (Player) creature;
            int id = player.getInventory().getItemInHandFast().getId();
            return player.spawned && player.isAlive() && !player.closed && (id == Item.RAW_FISH || id == Item.RAW_SALMON) && distance <= 49;
        }
        return false;
    }
}
