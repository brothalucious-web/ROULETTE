package com.roulette.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

final class ClientSounds {
	private ClientSounds() {
	}

	static void play(String id, float pitch, float volume) {
		SoundEvent event = SoundEvent.createVariableRangeEvent(Identifier.withDefaultNamespace(id));
		Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(event, pitch, volume));
	}
}
