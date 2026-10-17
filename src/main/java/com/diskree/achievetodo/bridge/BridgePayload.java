package com.diskree.achievetodo.bridge;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** One body per native custom payload. No inner channel, VarInt or byte-array prefix. */
public record BridgePayload(byte[] bytes) implements CustomPacketPayload {
    public static final Type<BridgePayload> ID = new Type<>(Identifier.parse(BridgeWire.CHANNEL));
    public static final StreamCodec<FriendlyByteBuf, BridgePayload> CODEC = new StreamCodec<>() {
        @Override public BridgePayload decode(FriendlyByteBuf buffer) {
            int size=buffer.readableBytes();
            if (size>BridgeWire.MAX_FRAME) throw new IllegalArgumentException("ATD frame size");
            byte[] bytes=new byte[size]; buffer.readBytes(bytes);
            return new BridgePayload(bytes);
        }
        @Override public void encode(FriendlyByteBuf buffer,BridgePayload payload) { buffer.writeBytes(payload.bytes); }
    };
    public BridgePayload {
        if (bytes.length>BridgeWire.MAX_FRAME) throw new IllegalArgumentException("ATD frame size");
        bytes=bytes.clone();
    }
    @Override public byte[] bytes() { return bytes.clone(); }
    @Override public Type<? extends CustomPacketPayload> type() { return ID; }
    public static void register() {
        PayloadTypeRegistry.clientboundConfiguration().register(ID,CODEC);
        PayloadTypeRegistry.serverboundConfiguration().register(ID,CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ID,CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ID,CODEC);
    }
}
