package com.diskree.achievetodo.certification;

import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.server.level.ServerPlayer;

/** 26.3: the teleport acknowledgement carries the client's position; a GameTest client echoes the server's. */
public final class TeleportAck {
    private TeleportAck() {}

    public static void accept(int id, ServerPlayer player) {
        new ServerboundAcceptTeleportationPacket(id, player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot())
            .handle(player.connection);
    }
}
