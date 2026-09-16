package com.roulette;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

/** Rolls the prize loot tables and hands the items to the player. */
public final class Prizes {
	private Prizes() {
	}

	public static MutableComponent prefix() {
		return Component.literal("[Casino] ").withStyle(ChatFormatting.GOLD);
	}

	/** Announces the win in chat, then gives {@code rolls} prizes from the given tier. */
	public static void award(ServerPlayer player, MutableComponent header, int tier, int rolls) {
		player.sendSystemMessage(header
				.append(Component.literal(" You keep your bet and win " + rolls + " ").withStyle(ChatFormatting.GREEN))
				.append(Component.literal(RouletteRules.TIER_NAMES[tier]).withStyle(RouletteRules.TIER_FORMATS[tier], ChatFormatting.BOLD))
				.append(Component.literal(rolls == 1 ? " prize:" : " prizes:").withStyle(ChatFormatting.GREEN)));

		ServerLevel level = (ServerLevel) player.level();
		LootTable table = level.getServer().reloadableRegistries()
				.getLootTable(ResourceKey.create(Registries.LOOT_TABLE, RouletteMod.id("prize/tier" + tier)));
		LootParams params = new LootParams.Builder(level)
				.withParameter(LootContextParams.ORIGIN, player.position())
				.create(LootContextParamSets.CHEST);

		for (int i = 0; i < rolls; i++) {
			List<ItemStack> prizes = table.getRandomItems(params);
			for (ItemStack prize : prizes) {
				player.sendSystemMessage(Component.literal("  + " + prize.getCount() + "x ").withStyle(ChatFormatting.GRAY)
						.append(prize.getHoverName()));
				player.getInventory().add(prize);
				if (!prize.isEmpty()) {
					player.drop(prize, false);
				}
			}
		}
	}
}
