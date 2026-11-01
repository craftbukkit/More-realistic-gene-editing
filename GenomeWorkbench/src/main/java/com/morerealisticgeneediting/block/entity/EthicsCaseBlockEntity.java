package com.morerealisticgeneediting.block.entity;
import com.morerealisticgeneediting.ethics.EthicsCase;
import com.morerealisticgeneediting.ethics.EthicsCasebook;
import com.morerealisticgeneediting.inventory.ImplementedInventory;
import com.morerealisticgeneediting.screen.*;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.*;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import java.util.UUID;
public class EthicsCaseBlockEntity extends BlockEntity implements ExtendedScreenHandlerFactory<EthicsCaseOpenData>,ImplementedInventory {
    private final DefaultedList<ItemStack> inventory=DefaultedList.ofSize(0,ItemStack.EMPTY);
    private EthicsCase ethicsCase;
    private UUID assignedPlayerId;
    private boolean resolved;
    public EthicsCaseBlockEntity(BlockPos pos,BlockState state) { super(ModBlockEntities.ETHICS_CASE_BLOCK_ENTITY,pos,state); }
    @Override protected void writeNbt(NbtCompound nbt,RegistryWrapper.WrapperLookup lookup) {
        super.writeNbt(nbt,lookup);
        if(ethicsCase!=null)nbt.putString("ethicsCase",ethicsCase.id());
        if(assignedPlayerId!=null)nbt.putUuid("assignedPlayer",assignedPlayerId);
        nbt.putBoolean("Resolved",resolved);
    }
    @Override protected void readNbt(NbtCompound nbt,RegistryWrapper.WrapperLookup lookup) {
        super.readNbt(nbt,lookup);
        ethicsCase=EthicsCasebook.getCase(nbt.getString("ethicsCase")).orElse(null);
        assignedPlayerId=nbt.containsUuid("assignedPlayer")?nbt.getUuid("assignedPlayer"):null;resolved=nbt.getBoolean("Resolved");
    }
    public EthicsCase getEthicsCase() { return ethicsCase; }
    public void setEthicsCase(EthicsCase value,PlayerEntity player) { ethicsCase=value;assignedPlayerId=player.getUuid();resolved=false;markDirty(); }
    public boolean makeChoice(PlayerEntity player,int index) {
        if(world==null || world.isClient || !canPlayerUse(player) || resolved || ethicsCase==null || !player.getUuid().equals(assignedPlayerId) || index<0 || index>=ethicsCase.options().size())return false;
        player.sendMessage(Text.literal(ethicsCase.options().get(index).consequence()),false);
        resolved=true;markDirty();return true;
    }
    @Override public DefaultedList<ItemStack> getItems() { return inventory; }
    @Override public Text getDisplayName() { return Text.translatable("block.morerealisticgeneediting.ethics_case_block"); }
    private void ensureCase(PlayerEntity player){
        if(ethicsCase==null && world!=null && !world.isClient && canPlayerUse(player))
            EthicsCasebook.getAllCases().values().stream().findFirst().ifPresent(value->setEthicsCase(value,player));
    }
    @Override public EthicsCaseOpenData getScreenOpeningData(ServerPlayerEntity player) { ensureCase(player);return new EthicsCaseOpenData(pos,ethicsCase); }
    @Override public ScreenHandler createMenu(int syncId,PlayerInventory inventory,PlayerEntity player) { ensureCase(player);return new EthicsCaseScreenHandler(syncId,inventory,this,this); }
}
