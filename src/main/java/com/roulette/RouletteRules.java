package com.roulette;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Wheel layout, odds and prize rules shared by the server and the client screen. */
public final class RouletteRules {
	public static final int RED = 0;
	public static final int BLACK = 1;
	public static final int GREEN = 2;

	public static final int POCKETS = 12;
	/** The ball moves in quarter-pocket steps so the animation looks smooth. */
	public static final int SUBSTEPS = 4;
	public static final int BALL_POSITIONS = POCKETS * SUBSTEPS;

	/** Pocket colors clockwise, starting at the top of the wheel. */
	public static final int[] POCKET_COLORS = {GREEN, RED, BLACK, RED, BLACK, RED, GREEN, BLACK, RED, BLACK, RED, BLACK};

	public static final String[] COLOR_NAMES = {"RED", "BLACK", "GREEN"};
	public static final ChatFormatting[] COLOR_FORMATS = {ChatFormatting.RED, ChatFormatting.DARK_GRAY, ChatFormatting.GREEN};

	public static final String[] TIER_NAMES = {"", "Uncommon", "Rare", "Epic", "Legendary"};
	public static final ChatFormatting[] TIER_FORMATS = {ChatFormatting.WHITE, ChatFormatting.GREEN, ChatFormatting.AQUA, ChatFormatting.LIGHT_PURPLE, ChatFormatting.GOLD};

	private static final List<TagKey<Item>> BET_TIER_TAGS = List.of(
			TagKey.create(Registries.ITEM, RouletteMod.id("bet_tier1")),
			TagKey.create(Registries.ITEM, RouletteMod.id("bet_tier2")),
			TagKey.create(Registries.ITEM, RouletteMod.id("bet_tier3")),
			TagKey.create(Registries.ITEM, RouletteMod.id("bet_tier4")));

	private RouletteRules() {
	}

	public static int pocketColor(int ballPosition) {
		return POCKET_COLORS[Math.floorMod(ballPosition, BALL_POSITIONS) / SUBSTEPS];
	}

	/** 0 = anything not listed in a tag, up to 4 = legendary items. */
	public static int betTier(ItemStack stack) {
		int tier = 0;
		for (int i = 0; i < BET_TIER_TAGS.size(); i++) {
			if (stack.is(BET_TIER_TAGS.get(i))) {
				tier = i + 1;
			}
		}
		return tier;
	}

	/** Red/black wins give prizes one tier above the bet, green gives two tiers above. */
	public static int prizeTier(int betTier, int color) {
		return Math.min(4, betTier + (color == GREEN ? 2 : 1));
	}

	/** One prize per 16 items bet, minimum 1, maximum 5. */
	public static int prizeRolls(int betCount) {
		return Math.min(5, 1 + betCount / 16);
	}
}
