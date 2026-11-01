package com.morerealisticgeneediting.block.entity;
import com.morerealisticgeneediting.MoreRealisticGeneEditing;
import com.morerealisticgeneediting.block.*;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.*;
import net.minecraft.block.entity.*;
import net.minecraft.registry.*;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import java.util.IdentityHashMap;
import java.util.Map;
public class ModBlockEntities {
 private static final Map<Block,BlockEntityType<? extends LabEquipmentBlockEntity>> LAB_TYPES=new IdentityHashMap<>();
 public static BlockEntityType<CellProcessorBlockEntity> CELL_PROCESSOR_BLOCK_ENTITY;
 public static BlockEntityType<SequencerBlockEntity> SEQUENCER_BLOCK_ENTITY;
 public static BlockEntityType<PcrMachineBlockEntity> PCR_MACHINE;
 public static BlockEntityType<IncubatorBlockEntity> INCUBATOR;
 public static BlockEntityType<ElectrophoresisTankBlockEntity> ELECTROPHORESIS_TANK;
 public static BlockEntityType<EthicsCaseBlockEntity> ETHICS_CASE_BLOCK_ENTITY;
 public static BlockEntityType<CentrifugeBlockEntity> CENTRIFUGE;
 public static BlockEntityType<ThermalCyclerBlockEntity> THERMAL_CYCLER;
 public static BlockEntityType<ElectrophoresisBlockEntity> ELECTROPHORESIS;
 public static BlockEntityType<BenchtopSequencerBlockEntity> BENCHTOP_SEQUENCER;
 public static BlockEntityType<LiquidHandlerBlockEntity> LIQUID_HANDLER;
 public static BlockEntityType<BioreactorBlockEntity> BIOREACTOR;
 public static BlockEntityType<BscBlockEntity> BIOSAFETY;
 public static BlockEntityType<SpectrophotometerBlockEntity> SPECTROPHOTOMETER;
 public static BlockEntityType<ElectroporatorBlockEntity> ELECTROPORATOR;
 public static void registerBlockEntities(){
  CELL_PROCESSOR_BLOCK_ENTITY=register("cell_processor",FabricBlockEntityTypeBuilder.create(CellProcessorBlockEntity::new,ModBlocks.CELL_PROCESSOR).build());
  SEQUENCER_BLOCK_ENTITY=register("sequencer",FabricBlockEntityTypeBuilder.create(SequencerBlockEntity::new,ModBlocks.SEQUENCER).build());
  PCR_MACHINE=register("pcr_machine",FabricBlockEntityTypeBuilder.create(PcrMachineBlockEntity::new,ModBlocks.PCR_MACHINE).build());
  INCUBATOR=register("incubator",FabricBlockEntityTypeBuilder.create(IncubatorBlockEntity::new,ModBlocks.INCUBATOR).build());
  ELECTROPHORESIS_TANK=register("electrophoresis_tank",FabricBlockEntityTypeBuilder.create(ElectrophoresisTankBlockEntity::new,ModBlocks.ELECTROPHORESIS_TANK).build());
  ETHICS_CASE_BLOCK_ENTITY=register("ethics_case_block",FabricBlockEntityTypeBuilder.create(EthicsCaseBlockEntity::new,ModBlocks.ETHICS_CASE_BLOCK).build());
  CENTRIFUGE=registerLab("centrifuge",(pos,state)->new CentrifugeBlockEntity(CENTRIFUGE,pos,state),LabEquipmentBlocks.PALM_CENTRIFUGE,LabEquipmentBlocks.REFRIGERATED_CENTRIFUGE,LabEquipmentBlocks.ULTRACENTRIFUGE);
  THERMAL_CYCLER=registerLab("thermal_cycler",(pos,state)->new ThermalCyclerBlockEntity(THERMAL_CYCLER,pos,state),LabEquipmentBlocks.OPEN_PCR,LabEquipmentBlocks.THERMAL_CYCLER,LabEquipmentBlocks.QPCR_SYSTEM);
  ELECTROPHORESIS=registerLab("electrophoresis",(pos,state)->new ElectrophoresisBlockEntity(ELECTROPHORESIS,pos,state),LabEquipmentBlocks.PORTABLE_ELECTROPHORESIS,LabEquipmentBlocks.VERTICAL_ELECTROPHORESIS,LabEquipmentBlocks.AUTO_ELECTROPHORESIS);
  BENCHTOP_SEQUENCER=registerLab("benchtop_sequencer",(pos,state)->new BenchtopSequencerBlockEntity(BENCHTOP_SEQUENCER,pos,state),LabEquipmentBlocks.BENCHTOP_SEQUENCER);
  LIQUID_HANDLER=registerLab("liquid_handler",(pos,state)->new LiquidHandlerBlockEntity(LIQUID_HANDLER,pos,state),LabEquipmentBlocks.AUTO_PIPETTING_STATION,LabEquipmentBlocks.PIPETTING_ROBOT);
  BIOREACTOR=registerLab("bioreactor",(pos,state)->new BioreactorBlockEntity(BIOREACTOR,pos,state),LabEquipmentBlocks.DIY_INCUBATOR,LabEquipmentBlocks.SHAKING_INCUBATOR,LabEquipmentBlocks.MODULAR_BIOREACTOR);
  BIOSAFETY=registerLab("biosafety",(pos,state)->new BscBlockEntity(BIOSAFETY,pos,state),LabEquipmentBlocks.LAMINAR_FLOW_HOOD,LabEquipmentBlocks.BIOSAFETY_CABINET,LabEquipmentBlocks.ISOLATOR_WORKSTATION);
  SPECTROPHOTOMETER=registerLab("spectrophotometer",(pos,state)->new SpectrophotometerBlockEntity(SPECTROPHOTOMETER,pos,state),LabEquipmentBlocks.VISIBLE_SPECTROPHOTOMETER,LabEquipmentBlocks.NANODROP,LabEquipmentBlocks.HT_SPECTROPHOTOMETER);
  ELECTROPORATOR=registerLab("electroporator",(pos,state)->new ElectroporatorBlockEntity(ELECTROPORATOR,pos,state),LabEquipmentBlocks.ELECTROPORATOR);
 }
 private static <T extends BlockEntity> BlockEntityType<T> register(String id,BlockEntityType<T> type){return Registry.register(Registries.BLOCK_ENTITY_TYPE,Identifier.of(MoreRealisticGeneEditing.MOD_ID,id),type);}
 private static <T extends LabEquipmentBlockEntity> BlockEntityType<T> registerLab(String id,FabricBlockEntityTypeBuilder.Factory<T> factory,Block... blocks){
  BlockEntityType<T> type=register(id,FabricBlockEntityTypeBuilder.create(factory,blocks).build());
  for(Block block:blocks)LAB_TYPES.put(block,type);return type;
 }
 public static BlockEntityType<? extends LabEquipmentBlockEntity> getLabType(Block block){return LAB_TYPES.get(block);}
 public static LabEquipmentBlockEntity createLabEntity(BlockPos pos,BlockState state){
  var type=getLabType(state.getBlock());if(type==null)throw new IllegalStateException("Unregistered laboratory block: "+state.getBlock());
  return type.instantiate(pos,state);
 }
}
