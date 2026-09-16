package com.roulette;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import com.roulette.network.OpenRoulettePayload;
import com.roulette.network.RouletteActionPayload;

public class RouletteMod implements ModInitializer {
	public static final String MOD_ID = "roulette";

	public static final MenuType<RouletteMenu> ROULETTE_MENU = new MenuType<>(RouletteMenu::new, FeatureFlags.VANILLA_SET);

	@Override
	public void onInitialize() {
		Registry.register(BuiltInRegistries.MENU, id("roulette"), ROULETTE_MENU);

		PayloadTypeRegistry.serverboundPlay().register(OpenRoulettePayload.TYPE, OpenRoulettePayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(RouletteActionPayload.TYPE, RouletteActionPayload.CODEC);

		// The inventory tab asks the server to open the roulette menu.
		ServerPlayNetworking.registerGlobalReceiver(OpenRoulettePayload.TYPE, (payload, context) -> openRoulette(context.player()));

		// Color picks and the SPIN button.
		ServerPlayNetworking.registerGlobalReceiver(RouletteActionPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			if (player.containerMenu instanceof RouletteMenu menu) {
				menu.handleAction(player, payload.action());
			}
		});

		// Drive the spin animation for everyone who has the wheel open.
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				if (player.containerMenu instanceof RouletteMenu menu) {
					menu.serverTick(player);
				}
			}
		});
	}

	public static void openRoulette(ServerPlayer player) {
		if (player.isSpectator() || player.containerMenu instanceof RouletteMenu) {
			return;
		}
		player.openMenu(new SimpleMenuProvider(
				(containerId, inventory, p) -> new RouletteMenu(containerId, inventory, new SimpleContainer(1), new SimpleContainerData(RouletteMenu.DATA_COUNT)),
				Component.literal("Roulette")));
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
