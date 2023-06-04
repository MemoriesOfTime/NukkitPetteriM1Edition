package cn.nukkit.inventory;

import cn.nukkit.Server;
import cn.nukkit.item.Item;
import cn.nukkit.item.ItemFirework;
import cn.nukkit.item.ItemID;
import cn.nukkit.network.protocol.BatchPacket;
import cn.nukkit.network.protocol.CraftingDataPacket;
import cn.nukkit.network.protocol.DataPacket;
import cn.nukkit.network.protocol.ProtocolInfo;
import cn.nukkit.utils.*;
import io.netty.util.collection.CharObjectHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.util.*;
import java.util.zip.Deflater;

/**
 * @author MagicDroidX
 * Nukkit Project
 */
public class CraftingManager {

    private final Collection<Recipe> recipes313 = new ArrayDeque<>();
    private final Collection<Recipe> recipes332 = new ArrayDeque<>();
    private final Collection<Recipe> recipes354 = new ArrayDeque<>();
    private final Collection<Recipe> recipes419 = new ArrayDeque<>();
    public final Collection<Recipe> recipes = new ArrayDeque<>();

    public static BatchPacket packet313;
    public static BatchPacket packet340;
    public static BatchPacket packet361;
    public static BatchPacket packet354;
    public static BatchPacket packet388;
    public static DataPacket packet407;
    public static DataPacket packet419;
    public static DataPacket packet431;
    public static DataPacket packet440;
    public static DataPacket packet448;
    public static DataPacket packet465;
    public static DataPacket packet471;
    public static DataPacket packet486;
    public static DataPacket packet503;
    public static DataPacket packet527;
    public static DataPacket packet544;
    public static DataPacket packet554;
    public static DataPacket packet560;
    public static DataPacket packet567;
    public static DataPacket packet575;
    public static DataPacket packet582;

    private final Map<Integer, Map<UUID, ShapedRecipe>> shapedRecipes313 = new Int2ObjectOpenHashMap<>();
    private final Map<Integer, Map<UUID, ShapedRecipe>> shapedRecipes332 = new Int2ObjectOpenHashMap<>();
    private final Map<Integer, Map<UUID, ShapedRecipe>> shapedRecipes388 = new Int2ObjectOpenHashMap<>();
    private final Map<Integer, Map<UUID, ShapedRecipe>> shapedRecipes419 = new Int2ObjectOpenHashMap<>();
    protected final Map<Integer, Map<UUID, ShapedRecipe>> shapedRecipes = new Int2ObjectOpenHashMap<>();

    private final Map<Integer, Map<UUID, ShapelessRecipe>> shapelessRecipes313 = new Int2ObjectOpenHashMap<>();
    private final Map<Integer, Map<UUID, ShapelessRecipe>> shapelessRecipes332 = new Int2ObjectOpenHashMap<>();
    private final Map<Integer, Map<UUID, ShapelessRecipe>> shapelessRecipes388 = new Int2ObjectOpenHashMap<>();
    private final Map<Integer, Map<UUID, ShapelessRecipe>> shapelessRecipes419 = new Int2ObjectOpenHashMap<>();
    protected final Map<Integer, Map<UUID, ShapelessRecipe>> shapelessRecipes = new Int2ObjectOpenHashMap<>();

    public final Map<UUID, MultiRecipe> multiRecipes = new HashMap<>();
    public final Map<Integer, FurnaceRecipe> furnaceRecipes = new Int2ObjectOpenHashMap<>();
    private final Map<Integer, FurnaceRecipe> furnaceRecipesOld = new Int2ObjectOpenHashMap<>();
    public final Map<Integer, BrewingRecipe> brewingRecipes = new Int2ObjectOpenHashMap<>();
    private final Map<Integer, BrewingRecipe> brewingRecipesOld = new Int2ObjectOpenHashMap<>();
    public final Map<Integer, ContainerRecipe> containerRecipes = new Int2ObjectOpenHashMap<>();
    private final Map<Integer, ContainerRecipe> containerRecipesOld = new Int2ObjectOpenHashMap<>();
    public final Map<Integer, CampfireRecipe> campfireRecipes = new Int2ObjectOpenHashMap<>();
    private final Map<Integer, SmithingRecipe> smithingRecipeMap = new Int2ObjectOpenHashMap<>();

    private static int RECIPE_COUNT = 0;
    static int NEXT_NETWORK_ID = 0;

    public static final Comparator<Item> recipeComparator = (i1, i2) -> {
        if (i1.getId() > i2.getId()) {
            return 1;
        } else if (i1.getId() < i2.getId()) {
            return -1;
        } else if (i1.getDamage() > i2.getDamage()) {
            return 1;
        } else if (i1.getDamage() < i2.getDamage()) {
            return -1;
        } else return Integer.compare(i1.getCount(), i2.getCount());
    };

    @SuppressWarnings("unchecked")
    public CraftingManager() {
        MainLogger.getLogger().debug("Loading recipes...");
        ConfigSection recipes_419_config = new Config(Config.YAML).loadFromStream(Server.class.getClassLoader().getResourceAsStream("recipes419.json")).getRootSection();
        List<Map> recipes_388 = new Config(Config.YAML).loadFromStream(Server.class.getClassLoader().getResourceAsStream("recipes388.json")).getRootSection().getMapList("recipes");
        List<Map> recipes_332 = new Config(Config.YAML).loadFromStream(Server.class.getClassLoader().getResourceAsStream("recipes332.json")).getMapList("recipes");
        List<Map> recipes_313 = new Config(Config.YAML).loadFromStream(Server.class.getClassLoader().getResourceAsStream("recipes313.json")).getMapList("recipes");

        //TODO
        ConfigSection recipes_smithing_config = new Config(Config.YAML).loadFromStream(Server.class.getClassLoader().getResourceAsStream("recipes_smithing_test.json")).getRootSection();
        for (Map<String, Object> recipe : (List<Map<String, Object>>)recipes_smithing_config.get((Object)"smithing")) {
            List<Map> outputs = ((List<Map>) recipe.get("output"));
            if (outputs.size() > 1) {
                continue;
            }

            String recipeId = (String) recipe.get("id");
            int priority = Math.max(Utils.toInt(recipe.get("priority")) - 1, 0);

            Map<String, Object> first = outputs.get(0);
            Item item = Item.fromJson(first);

            List<Item> sorted = new ArrayList<>();
            for (Map<String, Object> ingredient : ((List<Map>) recipe.get("input"))) {
                sorted.add(Item.fromJson(ingredient));
            }

            new SmithingRecipe(recipeId, priority, sorted, item).registerToCraftingManager(this);
        }


        for (Map<String, Object> recipe : (List<Map<String, Object>>)recipes_419_config.get((Object)"shaped")) {
            if (!"crafting_table".equals(recipe.get("block"))) {
                // Ignore other recipes than crafting table ones
                continue;
            }
            List<Map> outputs = ((List<Map>) recipe.get("output"));
            Map<String, Object> first = outputs.remove(0);
            String[] shape = ((List<String>) recipe.get("shape")).toArray(new String[0]);
            Map<Character, Item> ingredients = new CharObjectHashMap();
            List<Item> extraResults = new ArrayList();
            Map<String, Map<String, Object>> input = (Map) recipe.get("input");
            for (Map.Entry<String, Map<String, Object>> ingredientEntry : input.entrySet()) {
                char ingredientChar = ingredientEntry.getKey().charAt(0);
                Item ingredient = Item.fromJson(ingredientEntry.getValue());

                ingredients.put(ingredientChar, ingredient);
            }

            for (Map<String, Object> data : outputs) {
                extraResults.add(Item.fromJson(data));
            }

            this.registerRecipe(419, new ShapedRecipe((String)recipe.get("id"), Utils.toInt(recipe.get("priority")), Item.fromJson(first), shape, ingredients, extraResults));
            this.registerRecipe(527, new ShapedRecipe((String)recipe.get("id"), Utils.toInt(recipe.get("priority")), Item.fromJson(first), shape, ingredients, extraResults));
        }

        for (Map<String, Object> recipe : (List<Map<String, Object>>) recipes_419_config.get((Object)"shapeless")) {
            if (!"crafting_table".equals((String) recipe.get("block"))) {
                // Ignore other recipes than crafting table ones
                continue;
            }
            // TODO: handle multiple result items
            List<Map> outputs = ((List<Map>) recipe.get("output"));
            if (outputs.size() > 1) {
                continue;
            }

            String recipeId = (String) recipe.get("id");
            int priority = Math.max(Utils.toInt(recipe.get("priority")) - 1, 0);

            Map<String, Object> first = outputs.get(0);
            Item item = Item.fromJson(first);
            if (item.getId() == 401) {
                Item itemFirework = item.clone();
                List<Item> sorted = new ArrayList();
                if (itemFirework instanceof ItemFirework) {
                    boolean hasResult = false;
                    for (Map<String, Object> ingredient : ((List<Map>) recipe.get("input"))) {
                        Item ingredientItem = Item.fromJson(ingredient);
                        sorted.add(ingredientItem);
                        if (ingredientItem.getId() != 289) {
                            continue;
                        }
                        sorted.add(ingredientItem.clone());
                        hasResult = true;
                    }
                    if (!hasResult) {
                        throw new RuntimeException("Missing result item for " + recipe);
                    }
                } else {
                    throw new RuntimeException("Unexpected result item: " + itemFirework.toString());
                }
                sorted.sort(recipeComparator);
                ((ItemFirework)itemFirework).setFlight(2);
                this.registerRecipe(419, new ShapelessRecipe(recipeId, priority, item, sorted));
                this.registerRecipe(527, new ShapelessRecipe(recipeId, priority, item, sorted));

                itemFirework = item.clone();
                if (itemFirework instanceof ItemFirework) {
                    sorted = new ArrayList();
                    boolean hasResult = false;
                    for (Map<String, Object> ingredient : ((List<Map>) recipe.get("input"))) {
                        Item ingredientItem = Item.fromJson(ingredient);
                        sorted.add(ingredientItem);
                        if (ingredientItem.getId() != 289) {
                            continue;
                        }
                        sorted.add(ingredientItem.clone());
                        sorted.add(ingredientItem.clone());
                        hasResult = true;
                    }
                    if (!hasResult) {
                        throw new RuntimeException("Missing result item for " + recipe);
                    }
                    sorted.sort(recipeComparator);
                    ((ItemFirework)itemFirework).setFlight(3);
                    this.registerRecipe(419, new ShapelessRecipe(recipeId, priority, itemFirework, sorted));
                    this.registerRecipe(527, new ShapelessRecipe(recipeId, priority, itemFirework, sorted));
                } else {
                    throw new RuntimeException("Unexpected result item: " + itemFirework.toString());
                }
            }

            List<Item> sorted = new ArrayList<>();
            for (Map<String, Object> ingredient : ((List<Map>) recipe.get("input"))) {
                sorted.add(Item.fromJson(ingredient));
            }
            // Bake sorted list
            sorted.sort(recipeComparator);

            this.registerRecipe(419, new ShapelessRecipe(recipeId, priority, item, sorted));
            this.registerRecipe(527, new ShapelessRecipe(recipeId, priority, item, sorted));
        }

        for (Map<String, Object> recipe : (List<Map<String, Object>>) recipes_419_config.get((Object)"smelting")) {
            String craftingBlock = (String)recipe.get("block");
            if (!"furnace".equals(craftingBlock) && !"campfire".equals(craftingBlock)) {
                continue;
            }

            Map<String, Object> resultMap = (Map) recipe.get("output");
            Item resultItem = Item.fromJson(resultMap);
            Item inputItem;
            try {
                inputItem = Item.fromJson(resultMap);
            } catch (Exception exception) {
                inputItem = Item.get(Utils.toInt(recipe.get("inputId")), recipe.containsKey("inputDamage") ? Utils.toInt(recipe.get("inputDamage")) : -1, 1);
            }

            switch (craftingBlock) {
                case "furnace": {
                    this.registerRecipe(419, new FurnaceRecipe(resultItem, inputItem));
                    break;
                }
                case "campfire": {
                    this.registerRecipe(419, new CampfireRecipe(resultItem, inputItem));
                }
            }
        }

        for (Map<String, Object> recipe : recipes_388) {
            try {
                switch (Utils.toInt(recipe.get("type"))) {
                    case 0:
                        String craftingBlock = (String) recipe.get("block");
                        if (!"crafting_table".equals(craftingBlock)) {
                            // Ignore other recipes than crafting table ones
                            continue;
                        }
                        List<Map> outputs = ((List<Map>) recipe.get("output"));
                        if (outputs.size() > 1) {
                            continue;
                        }
                        Map<String, Object> first = outputs.get(0);
                        List<Item> sorted = new ArrayList<>();
                        for (Map<String, Object> ingredient : ((List<Map>) recipe.get("input"))) {
                            sorted.add(Item.fromJson(ingredient));
                        }
                        sorted.sort(recipeComparator);

                        String recipeId = (String) recipe.get("id");
                        int priority = Utils.toInt(recipe.get("priority"));

                        this.registerRecipe(388, new ShapelessRecipe(recipeId, priority, Item.fromJson(first), sorted));
                        break;
                    case 1:
                        craftingBlock = (String) recipe.get("block");
                        if (!"crafting_table".equals(craftingBlock)) {
                            // Ignore other recipes than crafting table ones
                            continue;
                        }
                        outputs = (List<Map>) recipe.get("output");

                        first = outputs.remove(0);
                        String[] shape = ((List<String>) recipe.get("shape")).toArray(new String[0]);
                        Map<Character, Item> ingredients = new CharObjectHashMap<>();
                        List<Item> extraResults = new ArrayList<>();

                        Map<String, Map<String, Object>> input = (Map) recipe.get("input");
                        for (Map.Entry<String, Map<String, Object>> ingredientEntry : input.entrySet()) {
                            char ingredientChar = ingredientEntry.getKey().charAt(0);
                            Item ingredient = Item.fromJson(ingredientEntry.getValue());

                            ingredients.put(ingredientChar, ingredient);
                        }

                        for (Map<String, Object> data : outputs) {
                            extraResults.add(Item.fromJson(data));
                        }

                        recipeId = (String) recipe.get("id");
                        priority = Utils.toInt(recipe.get("priority"));

                        this.registerRecipe(388, new ShapedRecipe(recipeId, priority, Item.fromJson(first), shape, ingredients, extraResults));
                        break;
                    case 2:
                    case 3:
                        craftingBlock = (String) recipe.get("block");
                        if (!"furnace".equals(craftingBlock) && !"campfire".equals(craftingBlock)) {
                            // Ignore other recipes than furnaces
                            continue;
                        }
                        Map<String, Object> resultMap = (Map) recipe.get("output");
                        Item resultItem = Item.fromJson(resultMap);
                        Item inputItem;
                        try {
                            Map<String, Object> inputMap = (Map) recipe.get("input");
                            inputItem = Item.fromJson(inputMap);
                        } catch (Exception old) {
                            inputItem = Item.get(Utils.toInt(recipe.get("inputId")), recipe.containsKey("inputDamage") ? Utils.toInt(recipe.get("inputDamage")) : -1, 1);
                        }

                        switch (craftingBlock){
                            case "furnace":
                                this.registerRecipe(388, new FurnaceRecipe(resultItem, inputItem));
                                break;
                            case "campfire":
                                this.registerRecipe(388, new CampfireRecipe(resultItem, inputItem));
                                break;
                        }
                        break;
                    /*case 4:
                        this.registerRecipe(new MultiRecipe(UUID.fromString((String) recipe.get("uuid"))));
                        break;*/
                    default:
                        break;
                }
            } catch (Exception e) {
                MainLogger.getLogger().error("Exception during registering (protocol 388) recipe", e);
            }
        }

        for (Map<String, Object> recipe : recipes_313) {
            try {
                switch (Utils.toInt(recipe.get("type"))) {
                    case 0:
                        Map<String, Object> first = ((List<Map>) recipe.get("output")).get(0);
                        List<Item> sorted = new ArrayList<>();
                        for (Map<String, Object> ingredient : ((List<Map>) recipe.get("input"))) {
                            sorted.add(Item.fromJsonOld(ingredient));
                        }
                        sorted.sort(recipeComparator);
                        this.registerRecipe(313, new ShapelessRecipe(Item.fromJsonOld(first), sorted));
                        break;
                    case 1:
                        List<Map> output = (List<Map>) recipe.get("output");
                        first = output.remove(0);
                        String[] shape = ((List<String>) recipe.get("shape")).toArray(new String[0]);
                        Map<Character, Item> ingredients = new CharObjectHashMap<>();
                        List<Item> extraResults = new ArrayList<>();
                        Map<String, Map<String, Object>> input = (Map) recipe.get("input");
                        for (Map.Entry<String, Map<String, Object>> ingredientEntry : input.entrySet()) {
                            char ingredientChar = ingredientEntry.getKey().charAt(0);
                            Item ingredient = Item.fromJsonOld(ingredientEntry.getValue());
                            ingredients.put(ingredientChar, ingredient);
                        }
                        for (Map<String, Object> data : output) {
                            extraResults.add(Item.fromJsonOld(data));
                        }
                        this.registerRecipe(313, new ShapedRecipe(Item.fromJsonOld(first), shape, ingredients, extraResults));
                        break;
                    case 2:
                    case 3:
                        Map<String, Object> resultMap = (Map) recipe.get("output");
                        Item resultItem = Item.fromJsonOld(resultMap);
                        Item inputItem;
                        try {
                            Map<String, Object> inputMap = (Map) recipe.get("input");
                            inputItem = Item.fromJsonOld(inputMap);
                        } catch (Exception old) {
                            inputItem = Item.get(Utils.toInt(recipe.get("inputId")), recipe.containsKey("inputDamage") ? Utils.toInt(recipe.get("inputDamage")) : -1, 1);
                        }
                        this.furnaceRecipesOld.put(getItemHash(inputItem), new FurnaceRecipe(resultItem, inputItem));
                        break;
                    default:
                        break;
                }
            } catch (Exception e) {
                MainLogger.getLogger().error("Exception during registering (protocol 313) recipe", e);
            }
        }

        for (Map<String, Object> recipe : recipes_332) {
            try {
                switch (Utils.toInt(recipe.get("type"))) {
                    case 0:
                        Map<String, Object> first = ((List<Map>) recipe.get("output")).get(0);
                        List<Item> sorted = new ArrayList<>();
                        for (Map<String, Object> ingredient : ((List<Map>) recipe.get("input"))) {
                            sorted.add(Item.fromJsonOld(ingredient));
                        }
                        sorted.sort(recipeComparator);
                        this.registerRecipe(332, new ShapelessRecipe(Item.fromJsonOld(first), sorted));
                        break;
                    case 1:
                        List<Map> output = (List<Map>) recipe.get("output");
                        first = output.remove(0);
                        String[] shape = ((List<String>) recipe.get("shape")).toArray(new String[0]);
                        Map<Character, Item> ingredients = new CharObjectHashMap<>();
                        List<Item> extraResults = new ArrayList<>();
                        Map<String, Map<String, Object>> input = (Map) recipe.get("input");
                        for (Map.Entry<String, Map<String, Object>> ingredientEntry : input.entrySet()) {
                            char ingredientChar = ingredientEntry.getKey().charAt(0);
                            Item ingredient = Item.fromJsonOld(ingredientEntry.getValue());
                            ingredients.put(ingredientChar, ingredient);
                        }
                        for (Map<String, Object> data : output) {
                            extraResults.add(Item.fromJsonOld(data));
                        }
                        this.registerRecipe(332, new ShapedRecipe(Item.fromJsonOld(first), shape, ingredients, extraResults));
                        break;
                    default:
                        break;
                }
            } catch (Exception e) {
                MainLogger.getLogger().error("Exception during registering (protocol 332) recipe", e);
            }
        }

        Config extras = new Config(Config.YAML).loadFromStream(Server.class.getClassLoader().getResourceAsStream("recipes388.json"));
        List<Map> potionMixes = extras.getMapList("potionMixes");
        for (Map potionMix : potionMixes) {
            int fromPotionId = ((Number) potionMix.get("fromPotionId")).intValue();
            int ingredient = ((Number) potionMix.get("ingredient")).intValue();
            int toPotionId = ((Number) potionMix.get("toPotionId")).intValue();
            registerBrewingRecipeOld(new BrewingRecipe(Item.get(ItemID.POTION, fromPotionId), Item.get(ingredient), Item.get(ItemID.POTION, toPotionId)));
        }

        List<Map> containerMixes = extras.getMapList("containerMixes");
        for (Map containerMix : containerMixes) {
            int fromItemId = ((Number) containerMix.get("fromItemId")).intValue();
            int ingredient = ((Number) containerMix.get("ingredient")).intValue();
            int toItemId = ((Number) containerMix.get("toItemId")).intValue();
            registerContainerRecipeOld(new ContainerRecipe(Item.get(fromItemId), Item.get(ingredient), Item.get(toItemId)));
        }

        Config extras407 = new Config(Config.YAML).loadFromStream(Server.class.getClassLoader().getResourceAsStream("extras_407.json"));
        List<Map> potionMixes407 = extras407.getMapList("potionMixes");
        for (Map potionMix : potionMixes407) {
            int fromPotionId = ((Number) potionMix.get("inputId")).intValue();
            int fromPotionMeta = ((Number) potionMix.get("inputMeta")).intValue();
            int ingredient = ((Number) potionMix.get("reagentId")).intValue();
            int ingredientMeta = ((Number) potionMix.get("reagentMeta")).intValue();
            int toPotionId = ((Number) potionMix.get("outputId")).intValue();
            int toPotionMeta = ((Number) potionMix.get("outputMeta")).intValue();
            registerBrewingRecipe(new BrewingRecipe(Item.get(fromPotionId, fromPotionMeta), Item.get(ingredient, ingredientMeta), Item.get(toPotionId, toPotionMeta)));
        }

        List<Map> containerMixes407 = extras407.getMapList("containerMixes");
        for (Map containerMix : containerMixes407) {
            int fromItemId = ((Number) containerMix.get("inputId")).intValue();
            int ingredient = ((Number) containerMix.get("reagentId")).intValue();
            int toItemId = ((Number) containerMix.get("outputId")).intValue();
            registerContainerRecipe(new ContainerRecipe(Item.get(fromItemId), Item.get(ingredient), Item.get(toItemId)));
        }

        this.rebuildPacket();
        MainLogger.getLogger().debug("Loaded " + this.recipes.size() + " recipes");
    }

    private CraftingDataPacket packetFor(int protocol) {
        CraftingDataPacket pk = new CraftingDataPacket();
        pk.protocol = protocol;
        for (Recipe recipe : this.getRecipes(protocol)) {
            if (recipe instanceof ShapedRecipe) {
                pk.addShapedRecipe((ShapedRecipe) recipe);
            } else if (recipe instanceof ShapelessRecipe) {
                pk.addShapelessRecipe((ShapelessRecipe) recipe);
            }
        }
        for (FurnaceRecipe recipe : this.getFurnaceRecipes(protocol).values()) {
            pk.addFurnaceRecipe(recipe);
        }
        if (protocol >= ProtocolInfo.v1_13_0) {
            for (BrewingRecipe recipe : this.getBrewingRecipes(protocol).values()) {
                pk.addBrewingRecipe(recipe);
            }
            for (ContainerRecipe recipe : this.getContainerRecipes(protocol).values()) {
                pk.addContainerRecipe(recipe);
            }
            if (protocol >= ProtocolInfo.v1_16_0) {
                for (MultiRecipe recipe : this.getMultiRecipes(protocol).values()) {
                    pk.addMultiRecipe(recipe);
                }
            }
        }

        //TODO 整理并确定版本
        if (protocol >= ProtocolInfo.v1_19_80) {
            for (SmithingRecipe recipe : this.getSmithingRecipeMap().values()) {
                pk.addShapelessRecipe(recipe);
            }
        }

        pk.tryEncode();
        return pk;
    }

    public void rebuildPacket() {
        packet582 = packetFor(582);
        packet575 = packetFor(575);
        packet567 = packetFor(567);
        packet560 = packetFor(560);
        packet554 = packetFor(554);
        packet544 = packetFor(544);
        packet527 = packetFor(527);
        packet503 = packetFor(503);
        packet486 = packetFor(486);
        packet471 = packetFor(471);
        packet465 = packetFor(465);
        packet448 = packetFor(448);
        packet440 = packetFor(440);
        packet431 = packetFor(431);
        packet419 = packetFor(419);
        packet407 = packetFor(407).compress(Deflater.BEST_COMPRESSION);
        packet388 = packetFor(388).compress(Deflater.BEST_COMPRESSION);
        packet361 = packetFor(361).compress(Deflater.BEST_COMPRESSION);
        packet354 = packetFor(354).compress(Deflater.BEST_COMPRESSION);
        packet340 = packetFor(340).compress(Deflater.BEST_COMPRESSION);
        packet313 = packetFor(313).compress(Deflater.BEST_COMPRESSION);
    }

    public Map<Integer, SmithingRecipe> getSmithingRecipeMap() {
        return smithingRecipeMap;
    }

    public Collection<Recipe> getRecipes() {
        Server.mvw("CraftingManager#getRecipes()");
        return this.getRecipes(ProtocolInfo.CURRENT_PROTOCOL);
    }

    public Collection<Recipe> getRecipes(int protocol) {
        if (protocol >= 524) {
            return this.recipes;
        }
        if (protocol >= 419) {
            return this.recipes419;
        }
        if (protocol >= 354) {
            return this.recipes354;
        }
        if (protocol >= 340) {
            return this.recipes332;
        }
        return this.recipes313;
    }

    private Collection<Recipe> getRegisterRecipes(int protocol) {
        if (protocol == 527) {
            return this.recipes;
        }
        if (protocol == 419) {
            return this.recipes419;
        }
        if (protocol == 388) {
            return this.recipes354;
        }
        if (protocol == 332) {
            return this.recipes332;
        }
        if (protocol == 313) {
            return this.recipes313;
        }
        throw new IllegalArgumentException("Invalid protocol: " + protocol + " Supported: 419, 388, 332, 313");
    }

    public Map<Integer, FurnaceRecipe> getFurnaceRecipes() {
        Server.mvw("CraftingManager#getFurnaceRecipes()");
        return this.getFurnaceRecipes(ProtocolInfo.CURRENT_PROTOCOL);
    }

    public Map<Integer, FurnaceRecipe> getFurnaceRecipes(int protocol) {
        if (protocol >= ProtocolInfo.v1_10_0) {
            return this.furnaceRecipes;
        }
        return this.furnaceRecipesOld;
    }

    public Map<Integer, ContainerRecipe> getContainerRecipes(int protocol) {
        if (protocol >= ProtocolInfo.v1_16_0) {
            return this.containerRecipes;
        }
        return this.containerRecipesOld;
    }

    public Map<Integer, BrewingRecipe> getBrewingRecipes(int protocol) {
        if (protocol >= ProtocolInfo.v1_16_0) {
            return this.brewingRecipes;
        }
        return this.brewingRecipesOld;
    }

    public Map<UUID, MultiRecipe> getMultiRecipes(int protocol) {
        if (protocol >= ProtocolInfo.v1_16_0) {
            return this.multiRecipes;
        }
        throw new IllegalArgumentException("Multi recipes are not supported for protocol " + protocol + " (< 407)");
    }

    public FurnaceRecipe matchFurnaceRecipe(Item input) {
        FurnaceRecipe recipe = this.furnaceRecipes.get(getItemHash(input));
        if (recipe == null) recipe = this.furnaceRecipes.get(getItemHash(input.getId(), 0));
        return recipe;
    }

    private static UUID getMultiItemHash(Collection<Item> items) {
        BinaryStream stream = new BinaryStream();
        for (Item item : items) {
            stream.putVarInt(getFullItemHash(item));
        }
        return UUID.nameUUIDFromBytes(stream.getBuffer());
    }

    private static int getFullItemHash(Item item) {
        //return 31 * getItemHash(item) + item.getCount();
        return (getItemHash(item) << 6) | (item.getCount() & 0x3f);
    }

    public void registerFurnaceRecipe(FurnaceRecipe recipe) {
        this.furnaceRecipes.put(getItemHash(recipe.getInput()), recipe);
    }

    public void registerCampfireRecipe(CampfireRecipe recipe) {
        Item input = recipe.getInput();
        this.campfireRecipes.put(getItemHash(input), recipe);
    }

    private static int getItemHash(Item item) {
        return getItemHash(item.getId(), item.getDamage());
    }

    private static int getItemHash(int id, int meta) {
        //return (id << 4) | (meta & 0xf);
        //return (id << Block.DATA_BITS) | (meta & Block.DATA_MASK);
        return (id << 12) | (meta & 0xfff);
    }

    public Map<Integer, Map<UUID, ShapedRecipe>> getShapedRecipes(int n) {
        if (n >= 524) {
            return this.shapedRecipes;
        }
        if (n >= 419) {
            return this.shapedRecipes419;
        }
        if (n >= 354) {
            return this.shapedRecipes388;
        }
        if (n >= 340) {
            return this.shapedRecipes332;
        }
        return this.shapedRecipes313;
    }

    public void registerShapedRecipe(ShapedRecipe recipe) {
        Server.mvw("CraftingManager#registerShapedRecipe(ShapedRecipe)");
        this.registerShapedRecipe(313, recipe);
        this.registerShapedRecipe(332, recipe);
        this.registerShapedRecipe(388, recipe);
        this.registerShapedRecipe(419, recipe);
        this.registerShapedRecipe(527, recipe);
    }

    public void registerShapedRecipe(int protocol, ShapedRecipe recipe) {
        int resultHash = getItemHash(recipe.getResult());
        Map<UUID, ShapedRecipe> map;
        switch (protocol) {
            case 313:
                map = shapedRecipes313.computeIfAbsent(resultHash, k -> new HashMap<>());
                break;
            case 332:
                map = shapedRecipes332.computeIfAbsent(resultHash, k -> new HashMap<>());
                break;
            case 388: {
                map = this.shapedRecipes388.computeIfAbsent(resultHash, n -> new HashMap());
                break;
            }
            case 419: {
                map = this.shapedRecipes419.computeIfAbsent(resultHash, n -> new HashMap());
                break;
            }
            case 527: {
                map = this.shapedRecipes.computeIfAbsent(resultHash, n -> new HashMap());
                break;
            }
            default:
                throw new IllegalArgumentException("Tried to register a shaped recipe for unsupported protocol version: " + protocol);
        }
        map.put(getMultiItemHash(new LinkedList<>(recipe.getIngredientsAggregate())), recipe);
    }

    public void registerRecipe(Recipe recipe) {
        Server.mvw("CraftingManager#registerRecipe(Recipe)");
        this.registerRecipe(527, recipe);
    }

    public void registerRecipe(int protocol, Recipe recipe) {
        if (recipe instanceof CraftingRecipe) {
            UUID id = Utils.dataToUUID(String.valueOf(++RECIPE_COUNT), String.valueOf(recipe.getResult().getId()), String.valueOf(recipe.getResult().getDamage()), String.valueOf(recipe.getResult().getCount()), Arrays.toString(recipe.getResult().getCompoundTag()));
            ((CraftingRecipe) recipe).setId(id);
            this.getRegisterRecipes(protocol).add(recipe);
            if (recipe instanceof ShapedRecipe) {
                this.registerShapedRecipe(protocol, (ShapedRecipe) recipe);
            }else if (recipe instanceof ShapelessRecipe) {
                this.registerShapelessRecipe(protocol, (ShapelessRecipe) recipe);
            }
        }
        recipe.registerToCraftingManager(this);
    }

    public Map<Integer, Map<UUID, ShapelessRecipe>> getShapelessRecipes(int protocol) {
        if (protocol >= 524) {
            return this.shapelessRecipes;
        }
        if (protocol >= 419) {
            return this.shapelessRecipes419;
        }
        if (protocol >= 354) {
            return this.shapelessRecipes388;
        }
        if (protocol >= 340) {
            return this.shapelessRecipes332;
        }
        return this.shapelessRecipes313;
    }

    public void registerShapelessRecipe(ShapelessRecipe recipe) {
        Server.mvw("CraftingManager#registerShapelessRecipe(ShapelessRecipe)");
        this.registerShapelessRecipe(313, recipe);
        this.registerShapelessRecipe(332, recipe);
        this.registerShapelessRecipe(388, recipe);
        this.registerShapelessRecipe(419, recipe);
        this.registerShapelessRecipe(527, recipe);
    }

    public void registerShapelessRecipe(int protocol, ShapelessRecipe recipe) {
        List<Item> list = recipe.getIngredientsAggregate();
        UUID hash = getMultiItemHash(list);
        int resultHash = getItemHash(recipe.getResult());
        Map<UUID, ShapelessRecipe> map;
        switch (protocol) {
            case 313:
                map = shapelessRecipes313.computeIfAbsent(resultHash, k -> new HashMap<>());
                break;
            case 332:
                map = shapelessRecipes332.computeIfAbsent(resultHash, k -> new HashMap<>());
                break;
            case 388:
                map = shapelessRecipes388.computeIfAbsent(resultHash, k -> new HashMap<>());
                break;
            case 419:
                map = shapelessRecipes419.computeIfAbsent(resultHash, k -> new HashMap<>());
                break;
            case 527:
                map = shapelessRecipes.computeIfAbsent(resultHash, k -> new HashMap<>());
                break;
            default:
                throw new IllegalArgumentException("Tried to register a shapeless recipe for unsupported protocol version: " + protocol);
        }
        map.put(hash, recipe);
    }

    private static int getPotionHash(Item ingredient, Item potion) {
        int ingredientHash = ((ingredient.getId() & 0x3FF) << 6) | (ingredient.getDamage() & 0x3F);
        int potionHash = ((potion.getId() & 0x3FF) << 6) | (potion.getDamage() & 0x3F);
        return ingredientHash << 16 | potionHash;
    }

    private static int getPotionHashOld(int ingredientId, int potionType) {
        //return (ingredientId << 6) | potionType;
        return (ingredientId << 15) | potionType;
    }

    private static int getContainerHash(int ingredientId, int containerId) {
        //return (ingredientId << 9) | containerId;
        return (ingredientId << 15) | containerId;
    }

    public void registerSmithingRecipe(SmithingRecipe recipe) {
        Item input = recipe.getIngredient();
        Item potion = recipe.getEquipment();
        this.smithingRecipeMap.put(getContainerHash(input.getId(), potion.getId()), recipe);
    }

    public void registerBrewingRecipe(BrewingRecipe recipe) {
        Item input = recipe.getIngredient();
        Item potion = recipe.getInput();
        int potionHash = getPotionHash(input, potion);
        this.brewingRecipes.put(potionHash, recipe);
    }

    public void registerBrewingRecipeOld(BrewingRecipe recipe) {
        Item input = recipe.getIngredient();
        Item potion = recipe.getInput();
        this.brewingRecipesOld.put(getPotionHashOld(input.getId(), potion.getDamage()), recipe);
    }

    public void registerContainerRecipe(ContainerRecipe recipe) {
        Item input = recipe.getIngredient();
        Item potion = recipe.getInput();
        this.containerRecipes.put(getContainerHash(input.getId(), potion.getId()), recipe);
    }

    public void registerContainerRecipeOld(ContainerRecipe recipe) {
        Item input = recipe.getIngredient();
        Item potion = recipe.getInput();
        this.containerRecipesOld.put(getContainerHash(input.getId(), potion.getId()), recipe);
    }

    public BrewingRecipe matchBrewingRecipe(Item input, Item potion) {
        return this.brewingRecipes.get(getPotionHash(input, potion));
    }

    public CampfireRecipe matchCampfireRecipe(Item input) {
        CampfireRecipe recipe = this.campfireRecipes.get(getItemHash(input));
        if (recipe == null) recipe = this.campfireRecipes.get(getItemHash(input.getId(), 0));
        return recipe;
    }

    public ContainerRecipe matchContainerRecipe(Item input, Item potion) {
        return this.containerRecipes.get(getContainerHash(input.getId(), potion.getId()));
    }

    public CraftingRecipe matchRecipe(List<Item> inputList, Item primaryOutput, List<Item> extraOutputList) {
        Server.mvw("CraftingManager#matchRecipe(List<Item>, Item , List<Item>)");
        return this.matchRecipe(ProtocolInfo.CURRENT_PROTOCOL, inputList, primaryOutput, extraOutputList);
    }

    public CraftingRecipe matchRecipe(int protocol, List<Item> inputList, Item primaryOutput, List<Item> extraOutputList) {
        int outputHash = getItemHash(primaryOutput);
        if (this.getShapedRecipes(protocol).containsKey(outputHash)) {
            inputList.sort(recipeComparator);

            UUID inputHash = getMultiItemHash(inputList);

            Map<UUID, ShapedRecipe> recipeMap = this.getShapedRecipes(protocol).get(outputHash);

            if (recipeMap != null) {
                ShapedRecipe recipe = recipeMap.get(inputHash);

                if (recipe != null && (recipe.matchItems(inputList, extraOutputList) || matchItemsAccumulation(recipe, inputList, primaryOutput, extraOutputList))) {
                    return recipe;
                }

                for (ShapedRecipe shapedRecipe : recipeMap.values()) {
                    if (shapedRecipe.matchItems(inputList, extraOutputList) || matchItemsAccumulation(shapedRecipe, inputList, primaryOutput, extraOutputList)) {
                        return shapedRecipe;
                    }
                }
            }
        }

        if (this.getShapelessRecipes(protocol).containsKey(outputHash)) {
            inputList.sort(recipeComparator);

            Map<UUID, ShapelessRecipe> recipes = this.getShapelessRecipes(protocol).get(outputHash);

            if (recipes == null) {
                return null;
            }

            UUID inputHash = getMultiItemHash(inputList);
            ShapelessRecipe recipe = recipes.get(inputHash);

            if (recipe != null && (recipe.matchItems(inputList, extraOutputList) || matchItemsAccumulation(recipe, inputList, primaryOutput, extraOutputList))) {
                return recipe;
            }

            for (ShapelessRecipe shapelessRecipe : recipes.values()) {
                if (shapelessRecipe.matchItems(inputList, extraOutputList) || matchItemsAccumulation(shapelessRecipe, inputList, primaryOutput, extraOutputList)) {
                    return shapelessRecipe;
                }
            }
        }

        return null;
    }

    private static boolean matchItemsAccumulation(CraftingRecipe recipe, List<Item> inputList, Item primaryOutput, List<Item> extraOutputList) {
        Item recipeResult = recipe.getResult();
        if (primaryOutput.equals(recipeResult, recipeResult.hasMeta(), recipeResult.hasCompoundTag()) && primaryOutput.getCount() % recipeResult.getCount() == 0) {
            int multiplier = primaryOutput.getCount() / recipeResult.getCount();
            return recipe.matchItems(inputList, extraOutputList, multiplier);
        }
        return false;
    }

    public void registerMultiRecipe(MultiRecipe recipe) {
        this.multiRecipes.put(recipe.getId(), recipe);
    }

    public SmithingRecipe matchSmithingRecipe(Item equipment, Item ingredient) {
        return this.getSmithingRecipeMap().get(getContainerHash(ingredient.getId(), equipment.getId()));
    }

    public static class Entry {
        final int resultItemId;
        final int resultMeta;
        final int ingredientItemId;
        final int ingredientMeta;
        final String recipeShape;
        final int resultAmount;

        public Entry(int resultItemId, int resultMeta, int ingredientItemId, int ingredientMeta, String recipeShape, int resultAmount) {
            this.resultItemId = resultItemId;
            this.resultMeta = resultMeta;
            this.ingredientItemId = ingredientItemId;
            this.ingredientMeta = ingredientMeta;
            this.recipeShape = recipeShape;
            this.resultAmount = resultAmount;
        }
    }
}
