package com.morerealisticgeneediting.network;
import com.morerealisticgeneediting.network.s2c.*;
import com.morerealisticgeneediting.project.ClientProjectManager;
import com.morerealisticgeneediting.project.PlayerProjectState;
import com.morerealisticgeneediting.screens.GenomeTerminalScreen;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
public final class ClientPacketHandler {
    public static void register(){
        ClientPlayNetworking.registerGlobalReceiver(S2CSendGenomeSlicePacket.ID,(packet,context)->context.client().execute(()->{if(context.client().currentScreen instanceof GenomeTerminalScreen screen)screen.acceptSlice(packet);}));
        ClientPlayNetworking.registerGlobalReceiver(S2CSendMotifSearchResultsPacket.ID,(packet,context)->context.client().execute(()->{if(context.client().currentScreen instanceof GenomeTerminalScreen screen)screen.acceptMotifs(packet);}));
        ClientPlayNetworking.registerGlobalReceiver(S2CPlayerProjectStatePacket.ID,(packet,context)->context.client().execute(()->ClientProjectManager.setState(packet.state())));
        ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->ClientProjectManager.setState(PlayerProjectState.EMPTY));
    }
}
