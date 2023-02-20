package cn.nukkit.entity;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.block.Block;
import cn.nukkit.entity.mob.EntityEnderDragon;
import cn.nukkit.entity.mob.EntityFlyingMob;
import cn.nukkit.entity.mob.EntityMob;
import cn.nukkit.entity.mob.EntityRavager;
import cn.nukkit.entity.projectile.EntityProjectile;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.item.Item;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.level.particle.HeartParticle;
import cn.nukkit.math.AxisAlignedBB;
import cn.nukkit.math.Vector3;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.network.protocol.EntityEventPacket;
import cn.nukkit.utils.Utils;
import co.aikar.timings.Timings;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.apache.commons.math3.util.FastMath;

import java.util.concurrent.ThreadLocalRandom;

/**
 * The base class of all entities that have an AI
 */
public abstract class BaseEntity extends EntityCreature implements EntityAgeable {

    public int stayTime = 0;
    protected int moveTime = 0;

    protected float moveMultiplier = 1.0f;

    protected Vector3 target = null;
    protected Entity followTarget = null;
    protected int attackDelay = 0;
    private short inLoveTicks = 0;
    private short inLoveCooldown = 0;

    private boolean baby = false;
    private boolean movement = true;
    private boolean friendly = false;

    public Item[] armor;

    private static final Int2ObjectMap<Float> ARMOR_POINTS = new Int2ObjectOpenHashMap<Float>() {
        {
            put(Item.LEATHER_CAP, new Float(1));
            put(Item.LEATHER_TUNIC, new Float(3));
            put(Item.LEATHER_PANTS, new Float(2));
            put(Item.LEATHER_BOOTS, new Float(1));
            put(Item.CHAIN_HELMET, new Float(2));
            put(Item.CHAIN_CHESTPLATE, new Float(5));
            put(Item.CHAIN_LEGGINGS, new Float(4));
            put(Item.CHAIN_BOOTS, new Float(1));
            put(Item.GOLD_HELMET, new Float(2));
            put(Item.GOLD_CHESTPLATE, new Float(5));
            put(Item.GOLD_LEGGINGS, new Float(3));
            put(Item.GOLD_BOOTS, new Float(1));
            put(Item.IRON_HELMET, new Float(2));
            put(Item.IRON_CHESTPLATE, new Float(6));
            put(Item.IRON_LEGGINGS, new Float(5));
            put(Item.IRON_BOOTS, new Float(2));
            put(Item.DIAMOND_HELMET, new Float(3));
            put(Item.DIAMOND_CHESTPLATE, new Float(8));
            put(Item.DIAMOND_LEGGINGS, new Float(6));
            put(Item.DIAMOND_BOOTS, new Float(3));
            put(Item.NETHERITE_HELMET, new Float(3));
            put(Item.NETHERITE_CHESTPLATE, new Float(8));
            put(Item.NETHERITE_LEGGINGS, new Float(6));
            put(Item.NETHERITE_BOOTS, new Float(3));
            put(Item.TURTLE_SHELL, new Float(2));
        }
    };

    public BaseEntity(FullChunk chunk, CompoundTag nbt) {
        super(chunk, nbt);
        this.setHealth(this.getMaxHealth());
        this.setAirTicks(400);
    }

    public abstract Vector3 updateMove(int tickDiff);

    public abstract int getKillExperience();

    public boolean isFriendly() {
        return this.friendly;
    }

    public boolean isMovement() {
        return this.getServer().getMobAiEnabled() && this.movement;
    }

    public boolean isKnockback() {
        return this.attackTime > 0;
    }

    public void setFriendly(boolean bool) {
        this.friendly = bool;
    }

    public void setMovement(boolean value) {
        this.movement = value;
    }

    public double getSpeed() {
        if (this.baby) {
            return 1.2;
        }
        return 1;
    }

    public int getAge() {
        return this.age;
    }

    public Entity getTarget() {
        return this.followTarget != null ? this.followTarget : (this.target instanceof Entity ? (Entity) this.target : null);
    }

    public void setTarget(Entity target) {
        this.followTarget = target;
        this.moveTime = 0;
        this.stayTime = 0;
        this.target = null;
    }

    @Override
    public boolean isBaby() {
        return this.baby;
    }

    @Override
    public void setBaby(boolean baby) {
        this.baby = baby;
        this.setDataFlag(DATA_FLAGS, DATA_FLAG_BABY, baby);
        if (baby) {
            this.setScale(0.5f);
        } else {
            this.setScale(1.0f);
        }
    }

    @Override
    protected void initEntity() {
        super.initEntity();

        if (this.namedTag.contains("Movement")) {
            this.setMovement(this.namedTag.getBoolean("Movement"));
        }

        if (this.namedTag.contains("Age")) {
            this.age = this.namedTag.getShort("Age");
        }

        if (this.namedTag.getBoolean("Baby")) {
            this.setBaby(true);
        }
    }

    @Override
    public void saveNBT() {
        super.saveNBT();

        this.namedTag.putBoolean("Baby", this.baby);
        this.namedTag.putBoolean("Movement", this.isMovement());
        this.namedTag.putShort("Age", this.age);

        if (this.isInLove()) {
            this.namedTag.putShort("inLoveTicks", this.inLoveTicks);
        }
        if (this.isInLoveCooldown()) {
            this.namedTag.putShort("inLoveCooldown", this.inLoveCooldown);
        }

    }

    public boolean targetOption(EntityCreature creature, double distance) {
        if (this instanceof EntityMob) {
            if (creature instanceof Player) {
                Player player = (Player) creature;
                return !player.closed && player.spawned && player.isAlive() && (player.isSurvival() || player.isAdventure()) && distance <= 100;
            }
            return creature.isAlive() && !creature.closed && distance <= 100;
        }
        return false;
    }

    @Override
    public boolean entityBaseTick(int tickDiff) {
        if (Timings.entityBaseTickTimer != null) {
            Timings.entityBaseTickTimer.startTiming();
        }

        if (this.canDespawn() && this.age > Server.getInstance().mobDespawnTicks && !this.hasCustomName() && !(this instanceof EntityBoss)) {
            this.close();
            if (Timings.entityBaseTickTimer != null) {
                Timings.entityBaseTickTimer.stopTiming();
            }
            return true;
        }

        if (this instanceof EntityMob && this.attackDelay < 200) {
            this.attackDelay++;
        }

        boolean hasUpdate = super.entityBaseTick(tickDiff);

        if (this.moveTime > 0) {
            this.moveTime -= tickDiff;
        }

        if (this.isInLove()) {
            this.inLoveTicks -= tickDiff;
            if (this.age % 20 == 0) {
                for (int i = 0; i < 3; i++) {
                    this.level.addParticle(new HeartParticle(this.add(Utils.rand(-1.0, 1.0), this.getMountedYOffset() + Utils.rand(-1.0, 1.0), Utils.rand(-1.0, 1.0))));
                }
                Entity[] collidingEntities = this.level.getCollidingEntities(this.boundingBox.grow(0.5d, 0.5d, 0.5d));
                for (Entity entity : collidingEntities) {
                    if (this.checkSpawnBaby(entity)) {
                        break;
                    }
                }
            }
        } else if (isInLoveCooldown()) {
            this.inLoveCooldown -= tickDiff;
        }

        if (Timings.entityBaseTickTimer != null) {
            Timings.entityBaseTickTimer.stopTiming();
        }

        return hasUpdate;
    }

    protected boolean checkSpawnBaby(Entity entity) {
        if (!(entity instanceof BaseEntity) || entity.getNetworkId() != this.getNetworkId()) {
            return false;
        }
        BaseEntity baseEntity = (BaseEntity) entity;
        if (!baseEntity.isInLove() || baseEntity.isBaby() || baseEntity.age <= 0) {
            return false;
        }

        this.setInLove(false);
        baseEntity.setInLove(false);

        this.setInLoveCooldown((short) 1200);
        baseEntity.setInLoveCooldown((short) 1200);

        this.stayTime = 60;
        baseEntity.stayTime = 60;

        int i = 0;
        for (Entity entity2 : this.chunk.getEntities().values()) {
            if (entity2.getNetworkId() == getNetworkId()) {
                i++;
                if (i > 10) {
                    return true;
                }
            }
        }

        BaseEntity newEntity = (BaseEntity) Entity.createEntity(getNetworkId(), this, new Object[0]);
        newEntity.setBaby(true);
        newEntity.spawnToAll();
        return true;
    }


    @Override
    public boolean attack(EntityDamageEvent source) {
        if (this.isKnockback() && source instanceof EntityDamageByEntityEvent && ((EntityDamageByEntityEvent) source).getDamager() instanceof Player) {
            return false;
        }

        if (this.fireProof && (source.getCause() == EntityDamageEvent.DamageCause.FIRE || source.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK || source.getCause() == EntityDamageEvent.DamageCause.LAVA || source.getCause() == EntityDamageEvent.DamageCause.MAGMA)) {
            return false;
        }

        if (source instanceof EntityDamageByEntityEvent) {
            if (this instanceof EntityRavager && Utils.rand()) {
                ((EntityDamageByEntityEvent) source).setKnockBack(0f);
            } else {
                ((EntityDamageByEntityEvent) source).setKnockBack(0.25f);
            }
        }

        super.attack(source);

        if (!source.isCancelled()) {
            this.target = null;
            this.stayTime = 0;
        }

        return true;
    }

    @Override
    public boolean move(double dx, double dy, double dz) {
        if (dy < -10 || dy > 10) {
            if (!(this instanceof EntityFlyingMob)) {
                this.kill();
            }
            return false;
        }

        if (dx == 0 && dz == 0 && dy == 0) {
            return false;
        }

        if (Timings.entityMoveTimer != null) {
            Timings.entityMoveTimer.startTiming();
        }

        this.blocksAround = null;

        double movX = dx * moveMultiplier;
        double movY = dy;
        double movZ = dz * moveMultiplier;

        AxisAlignedBB[] list = this.level.getCollisionCubes(this, this.boundingBox.addCoord(dx, dy, dz), false);

        for (AxisAlignedBB bb : list) {
            dx = bb.calculateXOffset(this.boundingBox, dx);
        }
        this.boundingBox.offset(dx, 0, 0);

        for (AxisAlignedBB bb : list) {
            dz = bb.calculateZOffset(this.boundingBox, dz);
        }
        this.boundingBox.offset(0, 0, dz);

        for (AxisAlignedBB bb : list) {
            dy = bb.calculateYOffset(this.boundingBox, dy);
        }
        this.boundingBox.offset(0, dy, 0);

        this.setComponents(this.x + dx, this.y + dy, this.z + dz);
        this.checkChunks();

        this.checkGroundState(movX, movY, movZ, dx, dy, dz);
        this.updateFallState(this.onGround);

        if (Timings.entityMoveTimer != null) {
            Timings.entityMoveTimer.stopTiming();
        }
        return true;
    }

    @Override
    public boolean onInteract(Player player, Item item, Vector3 clickedPos) {
        if (item.getId() == Item.NAME_TAG && !player.isAdventure()) {
            if (item.hasCustomName() && !(this instanceof EntityEnderDragon)) {
                this.setNameTag(item.getCustomName());
                this.setNameTagVisible(true);
                //player.getInventory().decreaseCount(player.getInventory().getHeldItemIndex());
                return true;
            }
        }

        return false;
    }

    public void setInLove() {
        this.setInLove(true);
    }

    public void setInLove(boolean inLove) {
        if (inLove && !this.isBaby()) {
            this.inLoveTicks = 600;
            this.setDataFlag(DATA_FLAGS, DATA_FLAG_INLOVE, true);
        } else {
            this.inLoveTicks = 0;
            this.setDataFlag(DATA_FLAGS, DATA_FLAG_INLOVE, false);
        }
    }

    public boolean isInLove() {
        return inLoveTicks > 0;
    }

    public void setInLoveCooldown(short inLoveCooldown) {
        this.inLoveCooldown = inLoveCooldown;
    }

    public boolean isInLoveCooldown() {
        return this.inLoveCooldown > 0;
    }

    public Item[] getRandomArmor() {
        Item[] slots = new Item[4];
        Item helmet = Item.get(0);
        Item chestplate = Item.get(0);
        Item leggings = Item.get(0);
        Item boots = Item.get(0);

        switch (Utils.rand(1, 5)) {
            case 1:
                if (Utils.rand(1, 100) < 39) {
                    if (Utils.rand(0, 1) == 0) {
                        helmet = Item.get(Item.LEATHER_HELMET, 0, 1);
                        this.addHealth(1);
                    }
                }
                break;
            case 2:
                if (Utils.rand(1, 100) < 50) {
                    if (Utils.rand(0, 1) == 0) {
                        helmet = Item.get(Item.GOLD_HELMET, 0, 1);
                        this.addHealth(1);
                    }
                }
                break;
            case 3:
                if (Utils.rand(1, 100) < 14) {
                    if (Utils.rand(0, 1) == 0) {
                        helmet = Item.get(Item.CHAIN_HELMET, 0, 1);
                        this.addHealth(1);
                    }
                }
                break;
            case 4:
                if (Utils.rand(1, 100) < 3) {
                    if (Utils.rand(0, 1) == 0) {
                        helmet = Item.get(Item.IRON_HELMET, 0, 1);
                        this.addHealth(1);
                    }
                }
                break;
            case 5:
                if (Utils.rand(1, 100) == 100) {
                    if (Utils.rand(0, 1) == 0) {
                        helmet = Item.get(Item.DIAMOND_HELMET, 0, 1);
                        this.addHealth(2);
                    }
                }
                break;
            default:
                break;
        }

        slots[0] = helmet;

        if (Utils.rand(1, 4) != 1) {
            switch (Utils.rand(1, 5)) {
                case 1:
                    if (Utils.rand(1, 100) < 39) {
                        if (Utils.rand(0, 1) == 0) {
                            chestplate = Item.get(Item.LEATHER_CHESTPLATE, 0, 1);
                            this.addHealth(1);
                        }
                    }
                    break;
                case 2:
                    if (Utils.rand(1, 100) < 50) {
                        if (Utils.rand(0, 1) == 0) {
                            chestplate = Item.get(Item.GOLD_CHESTPLATE, 0, 1);
                            this.addHealth(1);
                        }
                    }
                    break;
                case 3:
                    if (Utils.rand(1, 100) < 14) {
                        if (Utils.rand(0, 1) == 0) {
                            chestplate = Item.get(Item.CHAIN_CHESTPLATE, 0, 1);
                            this.addHealth(1);
                        }
                    }
                    break;
                case 4:
                    if (Utils.rand(1, 100) < 3) {
                        if (Utils.rand(0, 1) == 0) {
                            chestplate = Item.get(Item.IRON_CHESTPLATE, 0, 1);
                            this.addHealth(2);
                        }
                    }
                    break;
                case 5:
                    if (Utils.rand(1, 100) == 100) {
                        if (Utils.rand(0, 1) == 0) {
                            chestplate = Item.get(Item.DIAMOND_CHESTPLATE, 0, 1);
                            this.addHealth(3);
                        }
                    }
                    break;
                default:
                    break;
            }
        }

        slots[1] = chestplate;

        if (Utils.rand(1, 2) == 2) {
            switch (Utils.rand(1, 5)) {
                case 1:
                    if (Utils.rand(1, 100) < 39) {
                        if (Utils.rand(0, 1) == 0) {
                            leggings = Item.get(Item.LEATHER_LEGGINGS, 0, 1);
                            this.addHealth(1);
                        }
                    }
                    break;
                case 2:
                    if (Utils.rand(1, 100) < 50) {
                        if (Utils.rand(0, 1) == 0) {
                            leggings = Item.get(Item.GOLD_LEGGINGS, 0, 1);
                            this.addHealth(1);
                        }
                    }
                    break;
                case 3:
                    if (Utils.rand(1, 100) < 14) {
                        if (Utils.rand(0, 1) == 0) {
                            leggings = Item.get(Item.CHAIN_LEGGINGS, 0, 1);
                            this.addHealth(1);
                        }
                    }
                    break;
                case 4:
                    if (Utils.rand(1, 100) < 3) {
                        if (Utils.rand(0, 1) == 0) {
                            leggings = Item.get(Item.IRON_LEGGINGS, 0, 1);
                            this.addHealth(1);
                        }
                    }
                    break;
                case 5:
                    if (Utils.rand(1, 100) == 100) {
                        if (Utils.rand(0, 1) == 0) {
                            leggings = Item.get(Item.DIAMOND_LEGGINGS, 0, 1);
                            this.addHealth(2);
                        }
                    }
                    break;
                default:
                    break;
            }
        }

        slots[2] = leggings;

        if (Utils.rand(1, 5) < 3) {
            switch (Utils.rand(1, 5)) {
                case 1:
                    if (Utils.rand(1, 100) < 39) {
                        if (Utils.rand(0, 1) == 0) {
                            boots = Item.get(Item.LEATHER_BOOTS, 0, 1);
                            this.addHealth(1);
                        }
                    }
                    break;
                case 2:
                    if (Utils.rand(1, 100) < 50) {
                        if (Utils.rand(0, 1) == 0) {
                            boots = Item.get(Item.GOLD_BOOTS, 0, 1);
                            this.addHealth(1);
                        }
                    }
                    break;
                case 3:
                    if (Utils.rand(1, 100) < 14) {
                        if (Utils.rand(0, 1) == 0) {
                            boots = Item.get(Item.CHAIN_BOOTS, 0, 1);
                            this.addHealth(1);
                        }
                    }
                    break;
                case 4:
                    if (Utils.rand(1, 100) < 3) {
                        if (Utils.rand(0, 1) == 0) {
                            boots = Item.get(Item.IRON_BOOTS, 0, 1);
                            this.addHealth(1);
                        }
                    }
                    break;
                case 5:
                    if (Utils.rand(1, 100) == 100) {
                        if (Utils.rand(0, 1) == 0) {
                            boots = Item.get(Item.DIAMOND_BOOTS, 0, 1);
                            this.addHealth(2);
                        }
                    }
                    break;
                default:
                    break;
            }
        }

        slots[3] = boots;

        return slots;
    }

    private void addHealth(int health) {
        this.maxHealth = this.maxHealth + health;
        this.setHealth(this.getHealth() + health);
    }

    public boolean canDespawn() {
        return Server.getInstance().despawnMobs;
    }

    public int nearbyDistanceMultiplier() {
        return 1;
    }

    @Override
    protected void checkGroundState(double movX, double movY, double movZ, double dx, double dy, double dz) {
        if (onGround && movX == 0 && movY == 0 && movZ == 0 && dx == 0 && dy == 0 && dz == 0) {
            return;
        }
        this.isCollidedVertically = movY != dy;
        this.isCollidedHorizontally = (movX != dx || movZ != dz);
        this.isCollided = (this.isCollidedHorizontally || this.isCollidedVertically);
        this.onGround = (movY != dy && movY < 0);
    }

    public static void setProjectileMotion(EntityProjectile projectile, double pitch, double yawR, double pitchR, double speed) {
        double verticalMultiplier = Math.cos(pitchR);
        double x = verticalMultiplier * Math.sin(-yawR);
        double z = verticalMultiplier * Math.cos(yawR);
        double y = Math.sin(-(FastMath.toRadians(pitch)));
        double magnitude = Math.sqrt(x * x + y * y + z * z);
        if (magnitude > 0) {
            x += (x * (speed - magnitude)) / magnitude;
            y += (y * (speed - magnitude)) / magnitude;
            z += (z * (speed - magnitude)) / magnitude;
        }
        ThreadLocalRandom rand = ThreadLocalRandom.current();
        x += rand.nextGaussian() * 0.007499999832361937 * 6;
        y += rand.nextGaussian() * 0.007499999832361937 * 6;
        z += rand.nextGaussian() * 0.007499999832361937 * 6;
        projectile.setMotion(new Vector3(x, y, z));
    }

    public boolean canTarget(Entity entity) {
        return entity instanceof Player && entity.canBeFollowed();
    }

    @Override
    protected void checkBlockCollision() {
        for (Block block : this.getCollisionBlocks()) {
            block.onEntityCollide(this);
        }

        // TODO: portals
    }

    /**
     * Get armor defense points for item
     *
     * @param item item id
     * @return defense points
     */
    protected float getArmorPoints(int item) {
        Float points = ARMOR_POINTS.get(item);
        if (points == null) {
            return 0;
        }
        return points;
    }

    /**
     * Play attack animation to viewers
     */
    protected void playAttack() {
        EntityEventPacket pk = new EntityEventPacket();
        pk.eid = this.getId();
        pk.event = EntityEventPacket.ARM_SWING;
        Server.broadcastPacket(this.getViewers().values(), pk);
    }

    /**
     * 满足攻击目标条件
     *
     * @return 是否满足
     */
    public boolean isMeetAttackConditions(Vector3 target) {
        return this.getServer().getMobAiEnabled() && target instanceof Entity;
    }

    /**
     * 获取攻击目标
     *
     * @param target 目标
     * @return 有可能为空指针
     */
    protected Entity getAttackTarget(Vector3 target) {
        if (isMeetAttackConditions(target)) {
            Entity entity = (Entity) target;
            if (!entity.isClosed() && target != this.followTarget) {
                return entity;
            }
        }
        return null;
    }

    protected void lookAt(Vector3 target) {
        double dx = this.x - target.x;
        double dy = this.y - target.y;
        double dz = this.z - target.z;
        double yaw = Math.asin(dx / Math.sqrt(dx * dx + dz * dz)) / Math.PI * 180.0d;
        double asin = Math.asin(dy / Math.sqrt(dx * dx + dz * dz + dy * dy)) / Math.PI * 180.0d;
        long pitch = Math.round(asin);
        if (dz > 0.0d) {
            yaw = -yaw + 180.0d;
        }
        this.setRotation(yaw, pitch);
    }

    protected EntityHuman getNearbyHuman() {
        AxisAlignedBB bb = this.boundingBox.clone().expand(2.5, 2.5, 2.5);
        EntityHuman human = null;
        for (Entity collidingEntity : this.level.getCollidingEntities(bb)) {
            if (collidingEntity instanceof EntityHuman) {
                human = (EntityHuman) collidingEntity;
                break;
            }
        }
        return human;
    }

}
