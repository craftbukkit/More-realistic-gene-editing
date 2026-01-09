package com.morerealisticgeneediting.network.s2c;

import com.morerealisticgeneediting.genome.GenomeSlice;
import com.morerealisticgeneediting.network.PacketIdentifiers;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * S2C Packet to send a genome slice from the server to a client.
 */
public class S2CSendGenomeSlicePacket {

    /**
     * Sends the packet from the server to the specified player.
     * @param player The player to send the packet to.
     * @param slice The GenomeSlice to send.
     */
    public static void send(ServerPlayerEntity player, GenomeSlice slice) {
        PacketByteBuf buf = PacketByteBufs.create();

        // We need to send the total length of the genome for the scrollbar to work correctly.
        buf.writeLong(slice.getGenome().getTotalLength());
        buf.writeLong(slice.getStart());
        buf.writeInt(slice.getLength());
        buf.writeByteArray(slice.getPackedBases());

        ServerPlayNetworking.send(player, PacketIdentifiers.SEND_GENOME_SLICE, buf);
    }
}
