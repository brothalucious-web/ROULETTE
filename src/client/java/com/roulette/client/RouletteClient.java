package com.roulette.client;

import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.MenuScreens;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import com.roulette.RouletteMod;
import com.roulette.network.ExpandDomainPayload;

public class RouletteClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		MenuScreens.register(RouletteMod.ROULETTE_MENU, RouletteScreen::new);
		MenuScreens.register(RouletteMod.PLINKO_MENU, PlinkoScreen::new);
		MenuScreens.register(RouletteMod.SLOT_MACHINE_MENU, SlotMachineScreen::new);
		MenuScreens.register(RouletteMod.BLACKJACK_MENU, BlackjackScreen::new);
		InventoryTabs.register();

		KeyMapping domainKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.roulette.domain_expansion", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, KeyMapping.Category.MISC));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (domainKey.consumeClick()) {
				if (client.player != null) {
					ClientPlayNetworking.send(ExpandDomainPayload.INSTANCE);
				}
			}
		});
	}
}
