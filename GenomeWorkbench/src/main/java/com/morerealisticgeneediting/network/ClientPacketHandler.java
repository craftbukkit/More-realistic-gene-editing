package com.morerealisticgeneediting.network;

import com.morerealisticgeneediting.MoreRealisticGeneEditing;
import com.morerealisticgeneediting.genome.GenomeSlice;
import com.morerealisticgeneediting.network.s2c.S2CPlayerProjectStatePacket;
import com.morerealisticgeneediting.network.s2c.S2CSendGenomeSlicePacket;
import com.morerealisticgeneediting.network.s2c.S2CSendMotifSearchResultsPacket;
import com.morerealisticgeneediting.project.ClientProjectManager;
import com.morerealisticgeneediting.project.PlayerProjectState;
import com.morerealisticgeneediting.screens.GenomeTerminalScreen;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.network.PacketByteBuf;

public class ClientPacketHandler {

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(S2CSendGenomeSlicePacket.ID, (client, handler, buf, responseSender) -> {
            S2CSendGenomeSlicePacket.receive(client, handler, buf, responseSender);
        });

        ClientPlayNetworking.registerGlobalReceiver(S2CSendMotifSearchResultsPacket.ID, (client, handler, buf, responseSender) -> {
            S2CSendMotifSearchResultsPacket.receive(client, handler, buf, responseSender);
        });

        ClientPlayNetworking.registerGlobalReceiver(S2CPlayerProjectStatePacket.ID, (client, handler, buf, responseSender) -> {
            PlayerProjectState state = S2CPlayerProjectStatePacket.readState(buf);
            client.execute(() -> {
                ClientProjectManager.setState(state);
            });
        });
    }
}
