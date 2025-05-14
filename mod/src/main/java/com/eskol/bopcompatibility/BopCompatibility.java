package com.eskol.bopcompatibility;

import com.mojang.logging.LogUtils;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

import java.util.*;
import java.util.Locale;
import java.util.Set;

@Mod(BopCompatibility.MODID)
public class BopCompatibility {
    public static final String MODID = "bopcompatibility";
    private static final Logger LOGGER = LogUtils.getLogger();
    
    private final Set<String> processedBiomes = new HashSet<>();

    public BopCompatibility() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::onCommonSetup);
        
        // Registrar config
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        
        MinecraftForge.EVENT_BUS.register(this);
        
        LOGGER.info("BOP Compatibility mod initialized");
    }

    private void onCommonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            if (ModList.get().isLoaded("biomesoplenty")) {
                LOGGER.info("Biomes O' Plenty detected, registering biome compatibility...");
                if (Config.enableAutoRegistration) {
                    registerBopBiomes();
                } else {
                    LOGGER.info("Auto-registration is disabled in config, skipping");
                }
            } else {
                LOGGER.info("Biomes O' Plenty not found, skipping biome registration");
            }
        });
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        if (ModList.get().isLoaded("biomesoplenty") && Config.enableAutoRegistration) {
            LOGGER.info("Server starting, checking for any missed biomes...");
            
            // Buscar biomas que pudieran haberse registrado tardíamente
            Registry<Biome> registry = getBiomeRegistry();
            if (registry != null) {
                for (ResourceKey<Biome> key : registry.keySet()) {
                    if (key.location().getNamespace().equals("biomesoplenty")) {
                        String biomeId = key.location().toString();
                        if (!processedBiomes.contains(biomeId)) {
                            registerBiomeCompat(biomeId);
                        }
                    }
                }
            }
            
            // Generar reporte
            if (Config.enableDetailedLogging) {
                logBiomeMappings();
            }
        }
    }

    private void registerBopBiomes() {
        // Primero procesar los biomas conocidos
        for (String biomeId : getKnownBopBiomeIds()) {
            registerBiomeCompat(biomeId);
        }
        
        // Luego intentar descubrir otros automáticamente
        Registry<Biome> registry = getBiomeRegistry();
        if (registry != null) {
            for (ResourceKey<Biome> key : registry.keySet()) {
                if (key.location().getNamespace().equals("biomesoplenty")) {
                    String biomeId = key.location().toString();
                    if (!processedBiomes.contains(biomeId)) {
                        registerBiomeCompat(biomeId);
                    }
                }
            }
        }
    }

    private void registerBiomeCompat(String biomeId) {
        // Evitar procesamiento duplicado
        if (processedBiomes.contains(biomeId)) {
            return;
        }
        
        // Ignorar biomas en la lista de ignorados
        if (Config.ignoredBiomes.contains(biomeId)) {
            if (Config.enableDetailedLogging) {
                LOGGER.info("Biome {} is in the ignore list, skipping", biomeId);
            }
            processedBiomes.add(biomeId);
            return;
        }
        
        try {
            ResourceLocation location = new ResourceLocation(biomeId);
            ResourceKey<Biome> biomeKey = ResourceKey.create(net.minecraft.core.registries.Registries.BIOME, location);
            
            // Verificar si hay un mapeo personalizado
            if (Config.customBiomeMappings.containsKey(biomeId)) {
                List<String> typeNames = Config.customBiomeMappings.get(biomeId);
                List<BiomeDictionary.Type> types = new ArrayList<>();
                
                for (String typeName : typeNames) {
                    BiomeDictionary.Type type = getTypeFromString(typeName);
                    if (type != null) {
                        types.add(type);
                    }
                }
                
                if (!types.isEmpty()) {
                    BiomeDictionary.addTypes(biomeKey, types.toArray(new BiomeDictionary.Type[0]));
                    if (Config.enableDetailedLogging) {
                        LOGGER.info("Custom mapped {} to types: {}", biomeId, types);
                    }
                    processedBiomes.add(biomeId);
                    return;
                }
            }
            
            // Si no hay mapeo personalizado o es inválido, usar inferencia automática
            BiomeDictionary.Type[] types = inferBiomeTypesFromName(location.getPath());
            
            if (types.length > 0) {
                BiomeDictionary.addTypes(biomeKey, types);
                if (Config.enableDetailedLogging) {
                    LOGGER.info("Auto-mapped {} to types: {}", biomeId, Arrays.toString(types));
                }
            } else {
                LOGGER.warn("No types could be inferred for biome: {}", biomeId);
            }
            
            processedBiomes.add(biomeId);
        } catch (Exception e) {
            LOGGER.error("Error registering biome compatibility for {}: {}", biomeId, e.getMessage());
            if (Config.enableDetailedLogging) {
                e.printStackTrace();
            }
        }
    }

    private Registry<Biome> getBiomeRegistry() {
        try {
            if (net.minecraft.server.MinecraftServer.getServer() != null) {
                return net.minecraft.server.MinecraftServer.getServer().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BIOME);
            } else {
                LOGGER.warn("Cannot access biome registry - server not available");
                return null;
            }
        } catch (Exception e) {
            LOGGER.error("Error accessing biome registry: {}", e.getMessage());
            return null;
        }
    }

    private BiomeDictionary.Type getTypeFromString(String typeName) {
        try {
            return BiomeDictionary.Type.getType(typeName);
        } catch (Exception e) {
            LOGGER.error("Invalid biome type name: {}", typeName);
            return null;
        }
    }

    private void logBiomeMappings() {
        Registry<Biome> registry = getBiomeRegistry();
        if (registry == null) return;
        
        LOGGER.info("--- Biome mapping report ---");
        for (ResourceKey<Biome> key : registry.keySet()) {
            if (key.location().getNamespace().equals("biomesoplenty")) {
                Set<BiomeDictionary.Type> types = BiomeDictionary.getTypes(key);
                LOGGER.info("{}: {}", key.location(), types);
            }
        }
        LOGGER.info("---------------------------");
    }

    private List<String> getKnownBopBiomeIds() {
        return List.of(
            "biomesoplenty:jade_cliffs",
            "biomesoplenty:lavender_field",
            "biomesoplenty:crag",
            "biomesoplenty:volcano",
            "biomesoplenty:bayou",
            "biomesoplenty:cherry_blossom_grove",
            "biomesoplenty:coniferous_forest",
            "biomesoplenty:dead_forest",
            "biomesoplenty:fir_clearing",
            "biomesoplenty:field",
            "biomesoplenty:marsh",
            "biomesoplenty:meadow",
            "biomesoplenty:old_growth_dead_forest",
            "biomesoplenty:rainforest",
            "biomesoplenty:rocky_shrubland",
            "biomesoplenty:scrubland",
            "biomesoplenty:seasonal_forest",
            "biomesoplenty:snowy_coniferous_forest",
            "biomesoplenty:wetland",
            "biomesoplenty:wasteland",
            "biomesoplenty:dryland",
            "biomesoplenty:dune_beach",
            "biomesoplenty:lush_desert"
        );
    }

    private BiomeDictionary.Type[] inferBiomeTypesFromName(String name) {
        name = name.toLowerCase(Locale.ROOT);
        List<BiomeDictionary.Type> types = new ArrayList<>();

        // Terrain types
        if (name.contains("forest") || name.contains("grove") || name.contains("woodland")) types.add(BiomeDictionary.Type.FOREST);
        if (name.contains("field") || name.contains("plain") || name.contains("meadow")) types.add(BiomeDictionary.Type.PLAINS);
        if (name.contains("mountain") || name.contains("cliff") || name.contains("crag") || name.contains("highland")) types.add(BiomeDictionary.Type.MOUNTAIN);
        if (name.contains("desert") || name.contains("dune") || name.contains("wasteland") || name.contains("dryland")) types.add(BiomeDictionary.Type.SANDY);
        if (name.contains("jungle") || name.contains("rainforest") || name.contains("tropical")) types.add(BiomeDictionary.Type.JUNGLE);
        if (name.contains("marsh") || name.contains("swamp") || name.contains("bog") || name.contains("wetland") || name.contains("bayou")) types.add(BiomeDictionary.Type.SWAMP);
        if (name.contains("beach") || name.contains("shore")) types.add(BiomeDictionary.Type.BEACH);
        if (name.contains("ocean") || name.contains("sea")) types.add(BiomeDictionary.Type.OCEAN);
        if (name.contains("river")) types.add(BiomeDictionary.Type.RIVER);
        if (name.contains("lake")) types.add(BiomeDictionary.Type.WATER);
        if (name.contains("end")) types.add(BiomeDictionary.Type.END);
        if (name.contains("nether") || name.contains("hell")) types.add(BiomeDictionary.Type.NETHER);
        if (name.contains("cave") || name.contains("grotto")) types.add(BiomeDictionary.Type.CAVE);
        if (name.contains("mesa") || name.contains("badland")) types.add(BiomeDictionary.Type.MESA);
        if (name.contains("savanna")) types.add(BiomeDictionary.Type.SAVANNA);

        // Climate and condition types
        if (name.contains("volcano") || name.contains("lava") || name.contains("hot") || name.contains("desert")) types.add(BiomeDictionary.Type.HOT);
        if (name.contains("cold") || name.contains("snow") || name.contains("frozen") || name.contains("ice")) types.add(BiomeDictionary.Type.COLD);
        if (name.contains("wet") || name.contains("marsh") || name.contains("swamp") || name.contains("rain")) types.add(BiomeDictionary.Type.WET);
        if (name.contains("dry") || name.contains("arid") || name.contains("waste")) types.add(BiomeDictionary.Type.DRY);
        if (name.contains("dead") || name.contains("waste") || name.contains("corrupt")) types.add(BiomeDictionary.Type.DEAD);
        if (name.contains("lush") || name.contains("fertile") || name.contains("garden")) types.add(BiomeDictionary.Type.LUSH);
        if (name.contains("magic") || name.contains("enchant") || name.contains("mystic")) types.add(BiomeDictionary.Type.MAGICAL);
        if (name.contains("rare") || name.contains("unusual")) types.add(BiomeDictionary.Type.RARE);
        if (name.contains("spooky") || name.contains("haunted") || name.contains("ominous")) types.add(BiomeDictionary.Type.SPOOKY);

        // Biome-specific types
        if (name.contains("mushroom")) types.add(BiomeDictionary.Type.MUSHROOM);
        if (name.contains("hills")) types.add(BiomeDictionary.Type.HILLS);
        if (name.contains("island")) types.add(BiomeDictionary.Type.ISLAND);
        if (name.contains("highland")) types.add(BiomeDictionary.Type.PLATEAU);

        return types.toArray(new BiomeDictionary.Type[0]);
    }
}