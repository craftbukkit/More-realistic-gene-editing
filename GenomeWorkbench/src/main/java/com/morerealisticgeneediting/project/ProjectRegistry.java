package com.morerealisticgeneediting.project;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.morerealisticgeneediting.MoreRealisticGeneEditing;
import net.fabricmc.fabric.api.resource.*;
import net.minecraft.resource.*;
import net.minecraft.util.Identifier;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
public final class ProjectRegistry {
    private static final Identifier FILE=Identifier.of(MoreRealisticGeneEditing.MOD_ID,"projects.json");
    private static Map<String,ResearchProject> projects=Map.of();
    public static void initialize() {
        ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            public Identifier getFabricId(){return Identifier.of(MoreRealisticGeneEditing.MOD_ID,"projects");}
            public void reload(ResourceManager manager){
                Map<String,ResearchProject> next=new LinkedHashMap<>();
                try {
                    var resource=manager.getResource(FILE);
                    if(resource.isPresent())try(var reader=new InputStreamReader(resource.get().getInputStream(),StandardCharsets.UTF_8)) {
                        List<ResearchProject> parsed=new Gson().fromJson(reader,new TypeToken<List<ResearchProject>>(){}.getType());
                        if(parsed==null)throw new IllegalArgumentException("Empty projects");
                        for(ResearchProject p:parsed)if(next.putIfAbsent(p.id(),p)!=null)throw new IllegalArgumentException("Duplicate project: "+p.id());
                        for(ResearchProject p:parsed)for(String prerequisite:p.prerequisites())if(!next.containsKey(prerequisite))throw new IllegalArgumentException("Missing prerequisite: "+prerequisite);
                    }
                    projects=Collections.unmodifiableMap(next);
                } catch(Exception e) {projects=Map.of();MoreRealisticGeneEditing.LOGGER.error("Cannot load research projects",e);}
            }
        });
    }
    public static ResearchProject getProject(String id){return projects.get(id);}
    public static List<ResearchProject> getAllProjects(){return List.copyOf(projects.values());}
}
