package com.roulette.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import com.roulette.RouletteMod;

/** Sent when the player clicks the Roulette tab in their inventory. */
public record OpenRoulettePayload() implements CustomPacketPayload {
	public static final OpenRoulettePayload INSTANCE = new OpenRoulettePayload();
	public static final Type<OpenRoulettePayload> TYPE = new Type<>(RouletteMod.id("open"));
	public static final StreamCodec<FriendlyByteBuf, OpenRoulettePayload> CODEC = StreamCodec.unit(INSTANCE);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
