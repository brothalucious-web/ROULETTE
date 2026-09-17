package com.roulette.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import com.roulette.RouletteMod;

/** A button press inside a game. Roulette: 0-2 picks a color, 3 spins. Plinko: 0 drops. */
public record RouletteActionPayload(int action) implements CustomPacketPayload {
	public static final Type<RouletteActionPayload> TYPE = new Type<>(RouletteMod.id("action"));
	public static final StreamCodec<FriendlyByteBuf, RouletteActionPayload> CODEC =
			ByteBufCodecs.VAR_INT.map(RouletteActionPayload::new, RouletteActionPayload::action).cast();

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
