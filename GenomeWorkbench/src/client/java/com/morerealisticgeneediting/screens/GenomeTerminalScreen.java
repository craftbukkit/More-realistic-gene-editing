package com.morerealisticgeneediting.screens;
import com.morerealisticgeneediting.network.c2s.*;
import com.morerealisticgeneediting.network.s2c.*;
import com.morerealisticgeneediting.project.ClientProjectManager;
import com.morerealisticgeneediting.project.ProjectRegistry;
import com.morerealisticgeneediting.project.ResearchProject;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import java.util.*;

/** Displays only versioned server responses. Coordinates are zero-based in the edited sequence. */
public class GenomeTerminalScreen extends HandledScreen<GenomeTerminalScreenHandler> {
    private static final int BASES=40, ROWS=11, PAGE=BASES*ROWS;
    private String sequence="";
    private long start;
    private long selectedPam=-1;
    private final Set<Long> motifHits=new HashSet<>();
    private String searchSummary="";
    private boolean projectTab;
    private int projectPage;
    private TextFieldWidget motif;
    private ButtonWidget editButton;
    public GenomeTerminalScreen(GenomeTerminalScreenHandler handler,PlayerInventory inventory,Text title) {
        super(handler,inventory,title);backgroundWidth=320;backgroundHeight=226;
    }
    private ButtonWidget button(String key,int bx,int by,int width,Runnable action) {
        return addDrawableChild(ButtonWidget.builder(Text.translatable(key),b->action.run()).dimensions(x+bx,y+by,width,20).build());
    }
    @Override protected void init() {
        super.init();
        button("button.mrge.genome",0,-24,100,()->{projectTab=false;refreshWidgets();});
        button("button.mrge.projects",104,-24,100,()->{projectTab=true;refreshWidgets();});
        if(projectTab){initProjects();return;}
        motif=new TextFieldWidget(textRenderer,x+8,y+151,180,20,Text.translatable("screen.mrge.motif"));
        motif.setMaxLength(64);addDrawableChild(motif);
        button("button.mrge.search",192,151,120,()->{
            if(!motif.getText().isBlank())ClientPlayNetworking.send(new C2SRequestMotifSearchPacket(handler.genomeId,handler.syncId,handler.revision,start,motif.getText()));
        });
        button("button.mrge.next_pam",8,176,148,this::nextPam);
        editButton=button("button.mrge.edit",164,176,148,()->{
            if(selectedPam>=0)ClientPlayNetworking.send(new C2SOpenGeneInsertionScreenPacket(handler.genomeId,handler.syncId,handler.revision,selectedPam));
        });
        editButton.active=selectedPam>=0;
        button("button.mrge.previous",8,201,90,()->requestSlice(Math.max(0,start-PAGE)));
        button("button.mrge.next",222,201,90,()->requestSlice(Math.min(Math.max(0,handler.totalLength-1),start+PAGE)));
        requestSlice(start);
    }
    private void refreshWidgets(){clearChildren();init();}
    private void initProjects(){
        List<ResearchProject> projects=new ArrayList<>(ProjectRegistry.getAllProjects());
        int first=projectPage*4;
        for(int i=first;i<Math.min(first+4,projects.size());i++) {
            ResearchProject project=projects.get(i);int yLine=35+(i-first)*39;
            ButtonWidget begin=button(project.isReferenceOnly()?"button.mrge.reference":"button.mrge.start",8,yLine,90,()->ClientPlayNetworking.send(new C2SStartProjectPacket(project.id())));
            begin.active=!project.isReferenceOnly();
            begin.setTooltip(Tooltip.of(Text.literal(project.description()+"\n"+String.join("\n",project.objectives()))));
            ButtonWidget complete=button("button.mrge.complete",110,yLine,90,()->ClientPlayNetworking.send(new C2SCompleteProjectPacket(project.id())));
            complete.active=!project.isReferenceOnly();
        }
        button("button.mrge.previous",8,201,90,()->{projectPage=Math.max(0,projectPage-1);refreshWidgets();});
        button("button.mrge.next",222,201,90,()->{if((projectPage+1)*4<projects.size())projectPage++;refreshWidgets();});
    }
    private void requestSlice(long position){
        if(handler.totalLength<=0)return;
        long safe=Math.max(0,Math.min(position,handler.totalLength-1));
        ClientPlayNetworking.send(new C2SRequestGenomeSlicePacket(handler.genomeId,handler.syncId,handler.revision,safe,(int)Math.min(PAGE,handler.totalLength-safe)));
    }
    private void nextPam(){
        if(sequence.isEmpty())return;
        List<Long> sites=new ArrayList<>();
        for(int i=0;i<=sequence.length()-3;i++)if(start+i>=20&&sequence.charAt(i+1)=='G'&&sequence.charAt(i+2)=='G')sites.add(start+i);
        if(sites.isEmpty()){selectedPam=-1;editButton.active=false;return;}
        int selected=sites.indexOf(selectedPam);selectedPam=sites.get((selected+1)%sites.size());editButton.active=true;
    }
    public void acceptSlice(S2CSendGenomeSlicePacket p){
        if(!matches(p.genomeId(),p.syncId(),p.revision()))return;
        start=p.start();sequence=p.sequence();selectedPam=-1;motifHits.clear();searchSummary="";
        if(editButton!=null)editButton.active=false;
    }
    public void acceptMotifs(S2CSendMotifSearchResultsPacket p){
        if(!matches(p.genomeId(),p.syncId(),p.revision()))return;
        motifHits.clear();for(long hit:p.positions())motifHits.add(hit);
        searchSummary=Text.translatable("screen.mrge.search_result",p.positions().length,p.scanStart(),p.scanStart()+p.scannedLength(),p.truncated()?"+":"").getString();
    }
    private boolean matches(UUID id,int sync,long revision){return id.equals(handler.genomeId)&&sync==handler.syncId&&revision==handler.revision;}
    @Override public boolean keyPressed(int keyCode,int scanCode,int modifiers){
        if(!projectTab&&motif!=null&&motif.isFocused()&&client!=null&&client.options.inventoryKey.matchesKey(keyCode,scanCode))return true;
        return super.keyPressed(keyCode,scanCode,modifiers);
    }
    @Override protected void drawBackground(DrawContext context,float delta,int mouseX,int mouseY){
        context.fill(x,y,x+backgroundWidth,y+backgroundHeight,0xFFDFE6E9);
        context.fill(x,y,x+backgroundWidth,y+18,0xFF183E4E);
    }
    @Override protected void drawForeground(DrawContext context,int mouseX,int mouseY){
        context.drawText(textRenderer,title,8,5,0xFFFFFF,false);
        if(projectTab){
            List<ResearchProject> projects=new ArrayList<>(ProjectRegistry.getAllProjects());int first=projectPage*4;
            for(int i=first;i<Math.min(first+4,projects.size());i++){
                ResearchProject p=projects.get(i);String state=ClientProjectManager.isProjectCompleted(p.id())?" ✓":ClientProjectManager.isProjectActive(p.id())?" •":"";
                context.drawText(textRenderer,textRenderer.trimToWidth(p.title()+state,300),8,22+(i-first)*39,0x183E4E,false);
            }
            return;
        }
        context.enableScissor(x+8,y+22,x+312,y+145);
        try {
            for(int line=0;line<ROWS;line++){
                int offset=line*BASES;if(offset>=sequence.length())break;
                context.drawText(textRenderer,Long.toString(start+offset),8,23+line*10,0x455A64,false);
                for(int col=0;col<Math.min(BASES,sequence.length()-offset);col++){
                    int index=offset+col;long pos=start+index;int cx=65+col*6,cy=23+line*10;
                    if(motifHits.contains(pos))context.fill(cx,cy,cx+6,cy+9,0xFFFFD76B);
                    if(selectedPam>=0&&pos>=selectedPam-20&&pos<selectedPam+3)context.fill(cx,cy,cx+6,cy+9,0xFF9AD2E1);
                    context.drawText(textRenderer,String.valueOf(sequence.charAt(index)),cx,cy,0x102C38,false);
                }
            }
        }finally{context.disableScissor();}
        if(!searchSummary.isEmpty())context.drawText(textRenderer,textRenderer.trimToWidth(searchSummary,300),8,140,0x183E4E,false);
        String footer=selectedPam>=0?"SpCas9 (+) PAM: "+selectedPam:"r"+handler.revision+" · "+start+" / "+handler.totalLength;
        context.drawText(textRenderer,textRenderer.trimToWidth(footer,118),101,207,0x183E4E,false);
    }
    @Override public void render(DrawContext context,int mouseX,int mouseY,float delta){renderBackground(context,mouseX,mouseY,delta);super.render(context,mouseX,mouseY,delta);}
}
