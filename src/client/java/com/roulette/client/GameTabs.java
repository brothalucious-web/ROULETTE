package com.roulette.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import com.roulette.network.OpenBlackjackPayload;
import com.roulette.network.OpenPlinkoPayload;
import com.roulette.network.OpenRoulettePayload;

/** The row of tabs above the inventory: Inventory, Roulette, Plinko, Blackjack. */
public final class GameTabs {
	public static final int INVENTORY = 0;
	public static final int ROULETTE = 1;
	public static final int PLINKO = 2;
	public static final int BLACKJACK = 3;
	public static final int COUNT = 4;

	public static final int TAB_WIDTH = 26;
	public static final int TAB_HEIGHT = 22;

	private static final int[] TAB_X = {4, 32, 60, 88};
	private static final String[] NAMES = {"Inventory", "Roulette", "Plinko", "Blackjack"};
	private static ItemStack[] icons;

	private GameTabs() {
	}

	/** Creates all tabs; the one for the current screen is greyed out. */
	public static Button[] create(int current) {
		Button[] tabs = new Button[COUNT];
		for (int i = 0; i < COUNT; i++) {
			final int tab = i;
			tabs[i] = new RouletteButton(0, 0, TAB_WIDTH, TAB_HEIGHT, Component.empty(), button -> open(tab));
			tabs[i].setTooltip(Tooltip.create(Component.literal(NAMES[i])));
			tabs[i].active = i != current;
		}
		return tabs;
	}

	/** Places the tabs in a row along the top edge of a panel. */
	public static void placeAbove(Button[] tabs, int left, int top) {
		for (int i = 0; i < tabs.length; i++) {
			tabs[i].setX(left + TAB_X[i]);
			tabs[i].setY(top - TAB_HEIGHT);
		}
	}

	public static void drawIcons(GuiGraphicsExtractor graphics, Button[] tabs) {
		if (icons == null) {
			icons = new ItemStack[] {new ItemStack(Items.CHEST), new ItemStack(Items.CLOCK), new ItemStack(Items.SNOWBALL),
					new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace("paper")))};
		}
		for (int i = 0; i < tabs.length; i++) {
			if (tabs[i] != null && tabs[i].visible) {
				graphics.item(icons[i], tabs[i].getX() + 5, tabs[i].getY() + 3);
			}
		}
	}

	private static void open(int tab) {
		Minecraft mc = Minecraft.getInstance();
		switch (tab) {
			case ROULETTE -> ClientPlayNetworking.send(OpenRoulettePayload.INSTANCE);
			case PLINKO -> ClientPlayNetworking.send(OpenPlinkoPayload.INSTANCE);
			case BLACKJACK -> ClientPlayNetworking.send(OpenBlackjackPayload.INSTANCE);
			default -> {
				if (mc.player != null) {
					mc.player.closeContainer();
					mc.gui.setScreen(new InventoryScreen(mc.player));
				}
			}
		}
	}
}
