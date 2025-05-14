package com.eskol.bopcompatibility;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber(modid = BopCompatibility.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

        // Enable/disable main features
    private static final ForgeConfigSpec.BooleanValue ENABLE_AUTO_REGISTRATION = BUILDER
                .comment("Enable automatic registration of BOP biomes")
                .define("enableAutoRegistration", true);

    private static final ForgeConfigSpec.BooleanValue ENABLE_LOGGING = BUILDER
                .comment("Enable detailed logging")
                .define("enableDetailedLogging", true);

    // List of biomes to ignore
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> IGNORED_BIOMES = BUILDER
                .comment("List of BOP biomes that will be ignored by automatic mapping")
                .defineListAllowEmpty("ignoredBiomes", List.of(), Config::validateBiomeName);

    // Custom mappings (user-defined)
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> CUSTOM_MAPPINGS = BUILDER
                .comment("Custom mappings in the format 'biomesoplenty:biome_name=TYPE1,TYPE2,TYPE3'")
                .defineListAllowEmpty("customMappings", List.of(
                        "biomesoplenty:jade_cliffs=MOUNTAIN,FOREST,COLD",
                        "biomesoplenty:lavender_field=PLAINS,LUSH,RARE"
                ), Config::validateCustomMapping);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    // Variables accesibles desde el código
    public static boolean enableAutoRegistration;
    public static boolean enableDetailedLogging;
    public static List<String> ignoredBiomes;
    public static Map<String, List<String>> customBiomeMappings;

    private static boolean validateBiomeName(final Object obj) {
        return obj instanceof String biomeName && biomeName.startsWith("biomesoplenty:");
    }

    private static boolean validateCustomMapping(final Object obj) {
        if (!(obj instanceof String mapping)) {
            return false;
        }
        
        String[] parts = mapping.split("=");
        if (parts.length != 2) {
            return false;
        }
        
        // Verificar que el bioma comienza con biomesoplenty:
        if (!parts[0].startsWith("biomesoplenty:")) {
            return false;
        }
        
        // Verificar que hay al menos un tipo de bioma definido
        String[] types = parts[1].split(",");
        return types.length > 0;
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        enableAutoRegistration = ENABLE_AUTO_REGISTRATION.get();
        enableDetailedLogging = ENABLE_LOGGING.get();
        ignoredBiomes = (List<String>) IGNORED_BIOMES.get();
        
        // Convertir las cadenas de mapeo personalizado a un mapa
        customBiomeMappings = new HashMap<>();
        for (String mapping : CUSTOM_MAPPINGS.get()) {
            String[] parts = mapping.split("=");
            String biomeName = parts[0];
            List<String> types = Arrays.asList(parts[1].split(","));
            customBiomeMappings.put(biomeName, types);
        }
    }
}