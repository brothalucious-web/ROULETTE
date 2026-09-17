package com.roulette;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

public class RouletteMenu extends BetMenu {
	public static final int DATA_BALL = 0;
	public static final int DATA_STATE = 1;
	public static final int DATA_PICK = 2;
	public static final int DATA_OUTCOME = 3;
	public static final int DATA_COUNT = 4;

	public static final int OUTCOME_NONE = 0;
	public static final int OUTCOME_LOSE = 1;
	public static final int OUTCOME_WIN = 2;
	public static final int OUTCOME_JACKPOT = 3;

	/** Actions 0-2 pick red/black/green, 3 spins. */
	public static final int ACTION_SPIN = 3;

	public static final int BET_SLOT_X = 80;
	public static final int BET_SLOT_Y = 56;

	// Server-only spin progress.
	private int subStepsLeft;
	private int waitTicks;

	/** Client-side constructor. */
	public RouletteMenu(int containerId, Inventory playerInventory) {
		this(containerId, playerInventory, new SimpleContainer(1), new SimpleContainerData(DATA_COUNT));
	}

	public RouletteMenu(int containerId, Inventory playerInventory, Container bet, ContainerData data) {
		super(RouletteMod.ROULETTE_MENU, containerId, playerInventory, bet, data, DATA_COUNT, BET_SLOT_X, BET_SLOT_Y);
	}

	public boolean isSpinning() {
		return this.data.get(DATA_STATE) == 1;
	}

	@Override
	public boolean isBusy() {
		return this.isSpinning();
	}

	public int ballPosition() {
		return this.data.get(DATA_BALL);
	}

	public int pick() {
		return this.data.get(DATA_PICK);
	}

	public int outcome() {
		return this.data.get(DATA_OUTCOME);
	}

	@Override
	public void handleAction(ServerPlayer player, int action) {
		if (this.isSpinning()) {
			return;
		}
		if (action >= RouletteRules.RED && action <= RouletteRules.GREEN) {
			this.data.set(DATA_PICK, action);
			this.data.set(DATA_OUTCOME, OUTCOME_NONE);
		} else if (action == ACTION_SPIN && !this.bet.getItem(0).isEmpty()) {
			int pockets = 30 + player.getRandom().nextInt(RouletteRules.POCKETS);
			this.subStepsLeft = pockets * RouletteRules.SUBSTEPS - Math.floorMod(this.ballPosition(), RouletteRules.SUBSTEPS);
			this.waitTicks = 0;
			this.data.set(DATA_OUTCOME, OUTCOME_NONE);
			this.data.set(DATA_STATE, 1);
		}
	}

	@Override
	public void serverTick(ServerPlayer player) {
		if (!this.isSpinning()) {
			return;
		}
		if (this.waitTicks > 0) {
			this.waitTicks--;
			return;
		}

		// Fast at first, then the ball slows down and clicks into a pocket.
		int advance;
		int wait;
		if (this.subStepsLeft > 60) {
			advance = 3;
			wait = 0;
		} else if (this.subStepsLeft > 28) {
			advance = 2;
			wait = 0;
		} else if (this.subStepsLeft > 12) {
			advance = 1;
			wait = 0;
		} else if (this.subStepsLeft > 4) {
			advance = 1;
			wait = 1;
		} else {
			advance = 1;
			wait = 3;
		}
		advance = Math.min(advance, this.subStepsLeft);

		this.data.set(DATA_BALL, Math.floorMod(this.ballPosition() + advance, RouletteRules.BALL_POSITIONS));
		this.subStepsLeft -= advance;
		this.waitTicks = wait;

		if (this.subStepsLeft <= 0) {
			this.finish(player);
		}
	}

	@Override
	protected void settleNow(ServerPlayer player) {
		this.data.set(DATA_BALL, Math.floorMod(this.ballPosition() + this.subStepsLeft, RouletteRules.BALL_POSITIONS));
		this.subStepsLeft = 0;
		this.finish(player);
	}

	private void finish(ServerPlayer player) {
		this.data.set(DATA_STATE, 0);
		ItemStack stake = this.bet.getItem(0);
		if (stake.isEmpty()) {
			return;
		}

		int color = RouletteRules.pocketColor(this.ballPosition());
		MutableComponent header = Prizes.prefix()
				.append(Component.literal("Roulette landed on ").withStyle(ChatFormatting.GRAY))
				.append(Component.literal(RouletteRules.COLOR_NAMES[color]).withStyle(RouletteRules.COLOR_FORMATS[color], ChatFormatting.BOLD));

		if (color != this.pick()) {
			this.bet.setItem(0, ItemStack.EMPTY);
			this.data.set(DATA_OUTCOME, OUTCOME_LOSE);
			player.sendSystemMessage(header.append(Component.literal(" - you lost your bet.").withStyle(ChatFormatting.RED)));
			return;
		}

		int tier = RouletteRules.prizeTier(RouletteRules.betTier(stake), color);
		this.data.set(DATA_OUTCOME, tier == 4 ? OUTCOME_JACKPOT : OUTCOME_WIN);
		Prizes.award(player, header.append(Component.literal("!").withStyle(ChatFormatting.GRAY)), tier, RouletteRules.prizeRolls(stake.getCount()));
	}
}
