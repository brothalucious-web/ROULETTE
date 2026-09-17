package com.roulette.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import com.roulette.RouletteMod;

/** Sent when the player clicks the Plinko tab. */
public record OpenPlinkoPayload() implements CustomPacketPayload {
	public static final OpenPlinkoPayload INSTANCE = new OpenPlinkoPayload();
	public static final Type<OpenPlinkoPayload> TYPE = new Type<>(RouletteMod.id("open_plinko"));
	public static final StreamCodec<FriendlyByteBuf, OpenPlinkoPayload> CODEC = StreamCodec.unit(INSTANCE);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
