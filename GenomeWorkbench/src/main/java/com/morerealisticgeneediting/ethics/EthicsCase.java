package com.morerealisticgeneediting.ethics;
import net.minecraft.network.PacketByteBuf;
import java.util.ArrayList;
import java.util.List;
/** Bounded discussion content. Choices report reasoning, not medical outcomes. */
public record EthicsCase(String id,String title,String description,List<EthicsOption> options) {
    public EthicsCase {
        if(id==null || !id.matches("[a-z0-9_]{1,80}") || title==null || title.length()>256
            || description==null || description.length()>8192 || options==null || options.isEmpty() || options.size()>16)
            throw new IllegalArgumentException("Invalid ethics case");
        options=List.copyOf(options);
    }
    public void write(PacketByteBuf buf){
        buf.writeString(id,80);buf.writeString(title,256);buf.writeString(description,8192);
        buf.writeVarInt(options.size());options.forEach(o->o.write(buf));
    }
    public static EthicsCase from(PacketByteBuf buf){
        String id=buf.readString(80),title=buf.readString(256),description=buf.readString(8192);
        int count=buf.readVarInt();if(count<1 || count>16)throw new IllegalArgumentException("Invalid option count");
        List<EthicsOption> options=new ArrayList<>(count);for(int i=0;i<count;i++)options.add(EthicsOption.from(buf));
        return new EthicsCase(id,title,description,options);
    }
    public record EthicsOption(String id,String text,String consequence){
        public EthicsOption {
            if(id==null || !id.matches("[a-z0-9_]{1,80}") || text==null || text.length()>2048 || consequence==null || consequence.length()>8192)
                throw new IllegalArgumentException("Invalid ethics option");
        }
        public void write(PacketByteBuf buf){buf.writeString(id,80);buf.writeString(text,2048);buf.writeString(consequence,8192);}
        public static EthicsOption from(PacketByteBuf buf){return new EthicsOption(buf.readString(80),buf.readString(2048),buf.readString(8192));}
    }
}
