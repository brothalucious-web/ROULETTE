package com.roulette.client;

import net.minecraft.client.gui.screens.MenuScreens;

import net.fabricmc.api.ClientModInitializer;

import com.roulette.RouletteMod;

public class RouletteClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		MenuScreens.register(RouletteMod.ROULETTE_MENU, RouletteScreen::new);
		MenuScreens.register(RouletteMod.PLINKO_MENU, PlinkoScreen::new);
		InventoryTabs.register();
	}
}
