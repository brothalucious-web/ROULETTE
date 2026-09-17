package com.roulette.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import com.roulette.RouletteMod;

/** Sent when the player presses the Domain Expansion key. */
public record ExpandDomainPayload() implements CustomPacketPayload {
	public static final ExpandDomainPayload INSTANCE = new ExpandDomainPayload();
	public static final Type<ExpandDomainPayload> TYPE = new Type<>(RouletteMod.id("expand_domain"));
	public static final StreamCodec<FriendlyByteBuf, ExpandDomainPayload> CODEC = StreamCodec.unit(INSTANCE);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
