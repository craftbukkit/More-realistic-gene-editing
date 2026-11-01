package com.morerealisticgeneediting.ethics;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.morerealisticgeneediting.MoreRealisticGeneEditing;
import net.fabricmc.fabric.api.resource.*;
import net.minecraft.resource.*;
import net.minecraft.util.Identifier;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
public final class EthicsCasebook {
    private static Map<Identifier,EthicsCase> cases=Map.of();
    public static void initialize() {
        ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(new SimpleSynchronousResourceReloadListener(){
            public Identifier getFabricId(){return Identifier.of(MoreRealisticGeneEditing.MOD_ID,"ethics_casebook");}
            public void reload(ResourceManager manager){
                Map<Identifier,EthicsCase> next=new LinkedHashMap<>();
                var resource=manager.getResource(Identifier.of(MoreRealisticGeneEditing.MOD_ID,"ethics/cases.json"));
                try {
                    if(resource.isPresent())try(var reader=new InputStreamReader(resource.get().getInputStream(),StandardCharsets.UTF_8)) {
                        Map<String,EthicsCase> parsed=new Gson().fromJson(reader,new TypeToken<Map<String,EthicsCase>>(){}.getType());
                        if(parsed==null)throw new IllegalArgumentException("Empty ethics cases");
                        parsed.forEach((key,value)->{
                            if(!key.equals(value.id()))throw new IllegalArgumentException("Ethics id mismatch");
                            next.put(Identifier.of(MoreRealisticGeneEditing.MOD_ID,"ethics/"+key),value);
                        });
                    }
                    cases=Collections.unmodifiableMap(next);
                } catch(Exception e){cases=Map.of();MoreRealisticGeneEditing.LOGGER.error("Cannot load ethics cases",e);}
            }
        });
    }
    public static Optional<EthicsCase> getCase(Identifier id){return Optional.ofNullable(cases.get(id));}
    public static Optional<EthicsCase> getCase(String id){
        if(id==null || id.isBlank())return Optional.empty();
        Identifier parsed=Identifier.tryParse(id.contains(":")?id:MoreRealisticGeneEditing.MOD_ID+":ethics/"+id);
        return parsed==null?Optional.empty():getCase(parsed);
    }
    public static Map<Identifier,EthicsCase> getAllCases(){return cases;}
}
