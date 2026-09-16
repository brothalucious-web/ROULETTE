package com.roulette.client;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;

import com.roulette.client.mixin.ContainerScreenAccessor;
import com.roulette.network.OpenRoulettePayload;

/** Adds the Inventory / Roulette tabs on top of the player's inventory screen. */
public final class InventoryTabs {
	public static final int TAB_WIDTH = 26;
	public static final int TAB_HEIGHT = 22;
	public static final int INVENTORY_TAB_X = 4;
	public static final int ROULETTE_TAB_X = 32;

	private InventoryTabs() {
	}

	public static void register() {
		ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
			if (screen instanceof InventoryScreen || screen instanceof CreativeModeInventoryScreen) {
				addTabs(screen);
			}
		});
	}

	private static void addTabs(Screen screen) {
		ContainerScreenAccessor panel = (ContainerScreenAccessor) (AbstractContainerScreen<?>) screen;
		boolean creative = screen instanceof CreativeModeInventoryScreen;
		ItemStack inventoryIcon = new ItemStack(Items.CHEST);
		ItemStack rouletteIcon = new ItemStack(Items.CLOCK);

		Button inventoryTab = new RouletteButton(0, 0, TAB_WIDTH, TAB_HEIGHT, Component.empty(), button -> { });
		inventoryTab.active = false;
		Button rouletteTab = new RouletteButton(0, 0, TAB_WIDTH, TAB_HEIGHT, Component.empty(),
				button -> ClientPlayNetworking.send(OpenRoulettePayload.INSTANCE));
		rouletteTab.setTooltip(Tooltip.create(Component.literal("Roulette")));

		if (!creative) {
			Screens.getWidgets(screen).add(inventoryTab);
		}
		Screens.getWidgets(screen).add(rouletteTab);

		// The panel moves when the recipe book opens, so re-position every frame.
		ScreenEvents.beforeExtract(screen).register((s, graphics, mouseX, mouseY, delta) -> {
			int left = panel.roulette$getLeftPos();
			int top = panel.roulette$getTopPos();
			if (creative) {
				// Creative already has tabs above and below, so sit on the right edge instead.
				rouletteTab.setX(left + panel.roulette$getImageWidth() + 2);
				rouletteTab.setY(top + 4);
			} else {
				inventoryTab.setX(left + INVENTORY_TAB_X);
				inventoryTab.setY(top - TAB_HEIGHT);
				rouletteTab.setX(left + ROULETTE_TAB_X);
				rouletteTab.setY(top - TAB_HEIGHT);
			}
		});

		ScreenEvents.afterExtract(screen).register((s, graphics, mouseX, mouseY, delta) -> {
			if (!creative) {
				graphics.item(inventoryIcon, inventoryTab.getX() + 5, inventoryTab.getY() + 3);
			}
			graphics.item(rouletteIcon, rouletteTab.getX() + 5, rouletteTab.getY() + 3);
		});
	}
}
