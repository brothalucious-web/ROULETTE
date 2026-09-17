package com.roulette;

import net.minecraft.server.level.ServerPlayer;

/** A casino game screen that reacts to button presses and ticks on the server. */
public interface GameMenu {
	void handleAction(ServerPlayer player, int action);

	void serverTick(ServerPlayer player);
}
