package com.roulette.client;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;

import com.roulette.client.mixin.ContainerScreenAccessor;

/** Adds the game tabs to the player's own inventory screen. */
public final class InventoryTabs {
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
		Button[] tabs = GameTabs.create(GameTabs.INVENTORY);

		if (creative) {
			// Creative already has tabs above and below, so the game tabs go down the right edge.
			tabs[GameTabs.INVENTORY].visible = false;
		}
		for (Button tab : tabs) {
			Screens.getWidgets(screen).add(tab);
		}

		// The panel moves when the recipe book opens, so re-position every frame.
		ScreenEvents.beforeExtract(screen).register((s, graphics, mouseX, mouseY, delta) -> {
			int left = panel.roulette$getLeftPos();
			int top = panel.roulette$getTopPos();
			if (creative) {
				int x = left + panel.roulette$getImageWidth() + 2;
				for (int i = GameTabs.ROULETTE; i < GameTabs.COUNT; i++) {
					tabs[i].setX(x);
					tabs[i].setY(top + 4 + (i - GameTabs.ROULETTE) * (GameTabs.TAB_HEIGHT + 2));
				}
			} else {
				GameTabs.placeAbove(tabs, left, top);
			}
		});

		ScreenEvents.afterExtract(screen).register((s, graphics, mouseX, mouseY, delta) -> GameTabs.drawIcons(graphics, tabs));
	}
}
