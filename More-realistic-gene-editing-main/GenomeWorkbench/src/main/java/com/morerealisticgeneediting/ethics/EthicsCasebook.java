package com.morerealisticgeneediting.ethics;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.morerealisticgeneediting.MoreRealisticGeneEditing;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;

import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * A repository for loading and accessing all ethical cases in the game.
 * Cases are loaded from JSON data files.
 */
public class EthicsCasebook {

    private static final Map<Identifier, EthicsCase> cases = new HashMap<>();
    private static final Gson GSON = new Gson();
    private static final Identifier CASE_PATH = new Identifier(MoreRealisticGeneEditing.MOD_ID, "ethics/cases.json");

    /**
     * Initializes the casebook and registers it with Minecraft's resource manager to be loaded on startup.
     */
    public static void initialize() {
        ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override
            public Identifier getFabricId() {
                return new Identifier(MoreRealisticGeneEditing.MOD_ID, "ethics_casebook");
            }

            @Override
            public void reload(ResourceManager manager) {
                loadCases(manager);
            }
        });
    }

    /**
     * Loads all ethics cases from the JSON file defined by CASE_PATH.
     * @param manager The resource manager to use for loading.
     */
    private static void loadCases(ResourceManager manager) {
        cases.clear();
        manager.findResources("ethics", path -> path.getPath().endsWith(".json")).forEach((identifier, resource) -> {
            try (InputStreamReader reader = new InputStreamReader(resource.getInputStream())) {
                Type caseMapType = new TypeToken<Map<String, EthicsCase>>() {}.getType();
                Map<String, EthicsCase> loadedCases = GSON.fromJson(reader, caseMapType);
                loadedCases.forEach((key, value) -> {
                    cases.put(new Identifier(MoreRealisticGeneEditing.MOD_ID, "ethics/" + key), value);
                });
                MoreRealisticGeneEditing.LOGGER.info("Successfully loaded {} ethics cases.", loadedCases.size());
            } catch (Exception e) {
                MoreRealisticGeneEditing.LOGGER.error("Failed to load ethics case file: " + identifier.toString(), e);
            }
        });
    }

    /**
     * Retrieves a specific ethics case by its identifier.
     * @param id The unique identifier of the case.
     * @return An Optional containing the case if found, otherwise empty.
     */
    public static Optional<EthicsCase> getCase(Identifier id) {
        return Optional.ofNullable(cases.get(id));
    }

    /**
     * Gets an unmodifiable view of all loaded cases.
     * @return A map of all cases.
     */
    public static Map<Identifier, EthicsCase> getAllCases() {
        return Collections.unmodifiableMap(cases);
    }
}
