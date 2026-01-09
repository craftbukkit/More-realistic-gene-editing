package com.morerealisticgeneediting.project;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.morerealisticgeneediting.MoreRealisticGeneEditing;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ProjectRegistry {

    private static final Identifier PROJECTS_FILE = new Identifier(MoreRealisticGeneEditing.MOD_ID, "projects.json");
    private static final Gson GSON = new Gson();
    private static Map<String, ResearchProject> projects = Collections.emptyMap();

    public static void initialize() {
        ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override
            public Identifier getFabricId() {
                return new Identifier(MoreRealisticGeneEditing.MOD_ID, "projects");
            }

            @Override
            public void reload(ResourceManager manager) {
                // We only expect one file, but the API returns a map. We'll just take the first.
                manager.findResources("projects", path -> path.equals(PROJECTS_FILE)).forEach((id, resource) -> {
                     try (InputStream stream = resource.getInputStream();
                         InputStreamReader reader = new InputStreamReader(stream)) {

                        Type listType = new TypeToken<List<ResearchProject>>() {}.getType();
                        List<ResearchProject> projectList = GSON.fromJson(reader, listType);

                        if (projectList != null) {
                            projects = projectList.stream().collect(Collectors.toMap(ResearchProject::id, Function.identity()));
                            MoreRealisticGeneEditing.LOGGER.info("Successfully loaded {} research projects.", projects.size());
                        } else {
                            MoreRealisticGeneEditing.LOGGER.warn("projects.json is empty or malformed. No projects loaded.");
                            projects = Collections.emptyMap();
                        }

                    } catch (JsonSyntaxException e) {
                        MoreRealisticGeneEditing.LOGGER.error("Failed to parse projects.json. Please check for syntax errors.", e);
                        projects = Collections.emptyMap();
                    } catch (Exception e) {
                        MoreRealisticGeneEditing.LOGGER.error("An unexpected error occurred while loading research projects.", e);
                        projects = Collections.emptyMap();
                    }
                });
            }
        });
    }

    public static ResearchProject getProject(String id) {
        return projects.get(id);
    }

    public static List<ResearchProject> getAllProjects() {
        return List.copyOf(projects.values());
    }
}
