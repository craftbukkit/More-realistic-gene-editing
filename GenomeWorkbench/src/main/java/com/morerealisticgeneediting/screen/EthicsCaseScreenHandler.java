package com.morerealisticgeneediting.screen;
import com.morerealisticgeneediting.block.entity.EthicsCaseBlockEntity;
import com.morerealisticgeneediting.ethics.EthicsCase;
import net.minecraft.entity.player.*;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
public class EthicsCaseScreenHandler extends ScreenHandler {
    public final EthicsCase ethicsCase;
    private final EthicsCaseBlockEntity blockEntity;
    private final Inventory inventory;
    public EthicsCaseScreenHandler(int syncId,PlayerInventory playerInventory,EthicsCaseOpenData data) {
        super(ModScreenHandlers.ETHICS_CASE_SCREEN_HANDLER,syncId);this.ethicsCase=data.ethicsCase();this.blockEntity=null;this.inventory=new SimpleInventory(0);
    }
    public EthicsCaseScreenHandler(int syncId,PlayerInventory playerInventory,Inventory inventory,EthicsCaseBlockEntity entity) {
        super(ModScreenHandlers.ETHICS_CASE_SCREEN_HANDLER,syncId);this.inventory=inventory;this.blockEntity=entity;this.ethicsCase=entity.getEthicsCase();
    }
    @Override public boolean onButtonClick(PlayerEntity player,int id) { return blockEntity!=null && canUse(player) && blockEntity.makeChoice(player,id); }
    @Override public boolean canUse(PlayerEntity player) { return blockEntity==null?player.getWorld().isClient:inventory.canPlayerUse(player); }
    @Override public ItemStack quickMove(PlayerEntity player,int slot) { return ItemStack.EMPTY; }
}
