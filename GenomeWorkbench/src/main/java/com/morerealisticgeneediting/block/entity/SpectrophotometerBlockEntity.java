package com.morerealisticgeneediting.block.entity;

import com.morerealisticgeneediting.block.LabEquipmentBlockEntity;
import com.morerealisticgeneediting.equipment.*;
import com.morerealisticgeneediting.item.*;
import com.morerealisticgeneediting.util.StackData;
import com.morerealisticgeneediting.screen.equipment.SpectrophotometerScreenHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.*;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.*;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

/** Non-destructive observations of stored virtual aliquots, restricted to instrument wavelengths. */
public class SpectrophotometerBlockEntity extends LabEquipmentBlockEntity {
    public static final int SAMPLE_SLOT=0, BLANK_SLOT=1, CUVETTE_SLOT=2;
    private boolean blanked, hasResults;
    private NbtCompound measuredSample = new NbtCompound();
    private NbtCompound measuredBlank = new NbtCompound();
    private float absorbance260=Float.NaN, absorbance280=Float.NaN, absorbance230=Float.NaN;
    private float concentration=Float.NaN, purityRatio260_280=Float.NaN, purityRatio260_230=Float.NaN;
    private float visibleAbsorbance=Float.NaN;
    private String sampleType="unknown";
    private EquipmentSpecs.SpectrophotometerSpec spec;
    private final PropertyDelegate measurements = new PropertyDelegate() {
        public int get(int index) { return switch(index) {
            case 0 -> scaled(tier==EquipmentTier.BASIC ? visibleAbsorbance : absorbance260, 1000);
            case 1 -> scaled(concentration,100);
            case 2 -> tier==EquipmentTier.BASIC ? 600 : 260;
            case 3 -> tier.getLevel();
            case 4 -> hasResults() ? 1 : 0;
            default -> 0;
        }; }
        public void set(int index,int value) { }
        public int size() { return 5; }
    };
    private static int scaled(float value,int factor) { return Float.isFinite(value) ? Math.min(32767,Math.round(value*factor)) : -1; }
    public SpectrophotometerBlockEntity(BlockEntityType<?> type,BlockPos pos,BlockState state) {
        super(type,pos,state,3); updateSpec();
    }
    private void updateSpec() { spec=switch(tier) {
        case BASIC -> EquipmentSpecs.VISIBLE_721;
        case ADVANCED -> EquipmentSpecs.NANODROP_ONE;
        case ELITE -> EquipmentSpecs.LUNATIC;
    }; }
    @Override public void setTier(EquipmentTier tier) { super.setTier(tier); updateSpec(); clearResults(); }
    @Override protected boolean isInputSlot(int slot) { return slot>=0&&slot<3; }
    @Override protected boolean isOutputSlot(int slot) { return false; }
    @Override public boolean isValid(int slot,ItemStack stack) { return switch(slot) {
        case SAMPLE_SLOT -> stack.isOf(ModItems.GENOME_SAMPLE)||stack.isOf(ModItems.DNA_SAMPLE)||stack.isOf(ModItems.AMPLIFIED_GENE_FRAGMENT);
        case BLANK_SLOT -> stack.isOf(LabEquipmentItems.PBS);
        case CUVETTE_SLOT -> stack.isOf(ModItems.OPTICAL_CUVETTE);
        default -> false;
    }; }
    @Override protected boolean canProcess() {
        return blanked&&isValid(SAMPLE_SLOT,getStack(SAMPLE_SLOT))&&isValid(BLANK_SLOT,getStack(BLANK_SLOT))
            && measuredBlank.equals(StackData.read(getStack(BLANK_SLOT)))
            && (tier!=EquipmentTier.BASIC||isValid(CUVETTE_SLOT,getStack(CUVETTE_SLOT)));
    }
    @Override protected boolean canProcess(int ignored) { return canProcess(); }
    @Override protected void completeProcess() { }
    @Override public void tick() { /* Only explicit player requests trigger measurements. */ }
    public void performBlank() {
        if(world==null||world.isClient||!isValid(BLANK_SLOT,getStack(BLANK_SLOT))) return;
        measuredBlank=StackData.read(getStack(BLANK_SLOT)); blanked=true; clearResults();
    }
    public void measure() {
        if(world==null||world.isClient||!canProcess())return;
        measuredSample=StackData.read(getStack(SAMPLE_SLOT));
        sampleType=measuredSample.contains("SampleType")?measuredSample.getString("SampleType"):"unknown";
        MeasurementModels.OpticalReading reading=MeasurementModels.optical(sampleType,
            field(measuredSample,"Absorbance260"),field(measuredSample,"Absorbance280"),field(measuredSample,"Absorbance230"),
            blank("Absorbance260"),blank("Absorbance280"),blank("Absorbance230"),spec.minWavelength(),spec.maxWavelength());
        absorbance260=reading.a260();absorbance280=reading.a280();absorbance230=reading.a230();
        concentration=reading.concentration();purityRatio260_280=reading.ratio260280();purityRatio260_230=reading.ratio260230();
        visibleAbsorbance=field(measuredSample,"VisibleAbsorbance");
        if(Float.isFinite(visibleAbsorbance))visibleAbsorbance=Math.max(0,visibleAbsorbance-blank("VisibleAbsorbance"));
        hasResults=true;markDirty();
    }
    private float blank(String key) { return measuredBlank.contains(key)?field(measuredBlank,key):0; }
    private static float field(NbtCompound data,String key) {
        float value=data.contains(key)?data.getFloat(key):Float.NaN;
        return Float.isFinite(value)&&value>=0?value:Float.NaN;
    }
    public void clearResults() {
        hasResults=false;absorbance260=absorbance280=absorbance230=concentration=Float.NaN;
        purityRatio260_280=purityRatio260_230=visibleAbsorbance=Float.NaN;markDirty();
    }
    public String getQualityAssessment() {
        if(!hasResults())return "No current measurement";
        if(tier==EquipmentTier.BASIC)return "Visible absorbance only; UV concentration and purity unavailable";
        return "Absorbance ratios are screening observations; identity, contamination and editing require independent evidence";
    }
    public boolean isBlanked(){return blanked;}
    public boolean hasResults(){return hasResults&&measuredSample.equals(StackData.read(getStack(SAMPLE_SLOT)))&&isValid(SAMPLE_SLOT,getStack(SAMPLE_SLOT))&&canProcess();}
    public float getAbsorbance260(){return hasResults()?absorbance260:Float.NaN;}
    public float getAbsorbance280(){return hasResults()?absorbance280:Float.NaN;}
    public float getAbsorbance230(){return hasResults()?absorbance230:Float.NaN;}
    public float getConcentration(){return hasResults()?concentration:Float.NaN;}
    public float getPurityRatio260_280(){return hasResults()?purityRatio260_280:Float.NaN;}
    public float getPurityRatio260_230(){return hasResults()?purityRatio260_230:Float.NaN;}
    public String getSampleType(){return sampleType;}
    /** No unmeasured synthetic full spectrum is presented as observed data. */
    public float[] getSpectrumData(){return new float[0];}
    public int getSpectrumMinWl(){return spec.minWavelength();}
    public int getSpectrumMaxWl(){return spec.maxWavelength();}
    @Override protected void writeNbt(NbtCompound nbt,RegistryWrapper.WrapperLookup registries){
        super.writeNbt(nbt,registries);nbt.putBoolean("Blanked",blanked);nbt.putBoolean("HasResults",hasResults);
        nbt.put("MeasuredSample",measuredSample.copy());nbt.put("MeasuredBlank",measuredBlank.copy());
        nbt.putFloat("A260",absorbance260);nbt.putFloat("A280",absorbance280);nbt.putFloat("A230",absorbance230);
        nbt.putFloat("Concentration",concentration);nbt.putFloat("Ratio260_280",purityRatio260_280);nbt.putFloat("Ratio260_230",purityRatio260_230);
        nbt.putFloat("VisibleAbsorbance",visibleAbsorbance);nbt.putString("SampleType",sampleType);
    }
    @Override protected void readNbt(NbtCompound nbt,RegistryWrapper.WrapperLookup registries){
        super.readNbt(nbt,registries);updateSpec();blanked=nbt.getBoolean("Blanked");hasResults=nbt.getBoolean("HasResults");
        measuredSample=nbt.getCompound("MeasuredSample").copy();measuredBlank=nbt.getCompound("MeasuredBlank").copy();
        absorbance260=nbt.getFloat("A260");absorbance280=nbt.getFloat("A280");absorbance230=nbt.getFloat("A230");
        concentration=nbt.getFloat("Concentration");purityRatio260_280=nbt.getFloat("Ratio260_280");purityRatio260_230=nbt.getFloat("Ratio260_230");
        visibleAbsorbance=nbt.getFloat("VisibleAbsorbance");sampleType=nbt.getString("SampleType");
        if(!MeasurementModels.supportsWavelength(spec.minWavelength(),spec.maxWavelength(),260))
            absorbance260=absorbance280=absorbance230=concentration=purityRatio260_280=purityRatio260_230=Float.NaN;
    }
    @Override public Text getDisplayName(){return Text.translatable("block.morerealisticgeneediting."+switch(tier){case BASIC->"visible_spectrophotometer";case ADVANCED->"nanodrop";case ELITE->"ht_spectrophotometer";});}
    @Override public ScreenHandler createMenu(int syncId,PlayerInventory inventory,PlayerEntity player){return new SpectrophotometerScreenHandler(syncId,inventory,this,measurements);}
}
