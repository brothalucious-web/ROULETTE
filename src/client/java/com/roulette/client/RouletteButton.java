package com.roulette.client;

import java.util.function.Supplier;

import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** Button.Plain's constructor is protected in 26.2, so this subclass exposes it. */
public class RouletteButton extends Button.Plain {
	public RouletteButton(int x, int y, int width, int height, Component message, Button.OnPress onPress) {
		super(x, y, width, height, message, onPress, Supplier::get);
	}
}
