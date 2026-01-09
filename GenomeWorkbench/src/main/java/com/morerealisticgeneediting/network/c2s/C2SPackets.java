package com.morerealisticgeneediting.network.c2s;

public class C2SPackets {
    public static void register() {
        C2SStartProjectPacket.register();
        C2SCompleteProjectPacket.register();
    }
}
