package com.morerealisticgeneediting.network;

import com.morerealisticgeneediting.network.c2s.C2SPackets;

/**
 * A centralized place to register all server-side network packet handlers.
 */
public class ServerPacketHandler {

    /**
     * Registers all server-side packet receivers.
     */
    public static void register() {
        // Register all Client-to-Server (C2S) packets
        C2SPackets.register();

        // If we had Server-to-Client (S2C) packets that required server-side listeners
        // (e.g., for request-response patterns), we would register them here.
        // For now, our S2C packets are sent directly and don't need a listener on the server.
    }
}
