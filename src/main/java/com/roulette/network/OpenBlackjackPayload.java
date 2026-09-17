package com.roulette.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import com.roulette.RouletteMod;

/** Sent when the player clicks the Blackjack tab. */
public record OpenBlackjackPayload() implements CustomPacketPayload {
	public static final OpenBlackjackPayload INSTANCE = new OpenBlackjackPayload();
	public static final Type<OpenBlackjackPayload> TYPE = new Type<>(RouletteMod.id("open_blackjack"));
	public static final StreamCodec<FriendlyByteBuf, OpenBlackjackPayload> CODEC = StreamCodec.unit(INSTANCE);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
