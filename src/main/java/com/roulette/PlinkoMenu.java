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

public class PlinkoMenu extends BetMenu {
	public static final int DATA_STATE = 0;
	public static final int DATA_PROGRESS = 1;
	public static final int DATA_PATH = 2;
	public static final int DATA_OUTCOME = 3;
	public static final int DATA_COUNT = 4;

	public static final int OUTCOME_NONE = 0;
	public static final int OUTCOME_LOSE = 1;
	public static final int OUTCOME_KEEP = 2;
	public static final int OUTCOME_WIN = 3;
	public static final int OUTCOME_JACKPOT = 4;

	public static final int ACTION_DROP = 0;

	public static final int ROWS = 8;
	public static final int BUCKETS = ROWS + 1;
	/** Each fall between pegs takes this many ticks. The ball makes ROWS + 1 falls. */
	public static final int TICKS_PER_ROW = 5;
	public static final int TOTAL_TICKS = (ROWS + 1) * TICKS_PER_ROW;

	/**
	 * Prize tier jump for each bucket, left to right. -1 = bet lost, 0 = bet returned.
	 * Chances (out of 256): 1, 8, 28, 56, 70, 56, 28, 8, 1.
	 */
	public static final int[] BUCKET_JUMPS = {3, 2, 1, 0, -1, 0, 1, 2, 3};

	public static final int BET_SLOT_X = 15;
	public static final int BET_SLOT_Y = 30;

	/** Client-side constructor. */
	public PlinkoMenu(int containerId, Inventory playerInventory) {
		this(containerId, playerInventory, new SimpleContainer(1), new SimpleContainerData(DATA_COUNT));
	}

	public PlinkoMenu(int containerId, Inventory playerInventory, Container bet, ContainerData data) {
		super(RouletteMod.PLINKO_MENU, containerId, playerInventory, bet, data, DATA_COUNT, BET_SLOT_X, BET_SLOT_Y);
	}

	public boolean isDropping() {
		return this.data.get(DATA_STATE) == 1;
	}

	@Override
	public boolean isBusy() {
		return this.isDropping();
	}

	public int progress() {
		return this.data.get(DATA_PROGRESS);
	}

	/** Bit n set = the ball bounced right at peg row n. */
	public int path() {
		return this.data.get(DATA_PATH) & 0xFF;
	}

	public int outcome() {
		return this.data.get(DATA_OUTCOME);
	}

	public static int bucketFor(int path) {
		return Integer.bitCount(path & 0xFF);
	}

	@Override
	public void handleAction(ServerPlayer player, int action) {
		if (action != ACTION_DROP || this.isDropping() || this.bet.getItem(0).isEmpty()) {
			return;
		}
		this.data.set(DATA_PATH, player.getRandom().nextInt(256));
		this.data.set(DATA_PROGRESS, 0);
		this.data.set(DATA_OUTCOME, OUTCOME_NONE);
		this.data.set(DATA_STATE, 1);
	}

	@Override
	public void serverTick(ServerPlayer player) {
		if (!this.isDropping()) {
			return;
		}
		int next = this.progress() + 1;
		this.data.set(DATA_PROGRESS, next);
		if (next >= TOTAL_TICKS) {
			this.finish(player);
		}
	}

	@Override
	protected void settleNow(ServerPlayer player) {
		this.data.set(DATA_PROGRESS, TOTAL_TICKS);
		this.finish(player);
	}

	private void finish(ServerPlayer player) {
		this.data.set(DATA_STATE, 0);
		ItemStack stake = this.bet.getItem(0);
		if (stake.isEmpty()) {
			return;
		}

		int jump = BUCKET_JUMPS[bucketFor(this.path())];
		MutableComponent header = Prizes.prefix().append(Component.literal("Plinko: ").withStyle(ChatFormatting.GRAY));

		if (jump < 0) {
			this.bet.setItem(0, ItemStack.EMPTY);
			this.data.set(DATA_OUTCOME, OUTCOME_LOSE);
			player.sendSystemMessage(header.append(Component.literal("the middle bucket - you lost your bet.").withStyle(ChatFormatting.RED)));
			return;
		}
		if (jump == 0) {
			this.data.set(DATA_OUTCOME, OUTCOME_KEEP);
			player.sendSystemMessage(header.append(Component.literal("safe bucket - you keep your bet.").withStyle(ChatFormatting.YELLOW)));
			return;
		}

		int tier = RouletteRules.prizeTierForJump(RouletteRules.betTier(stake), jump);
		this.data.set(DATA_OUTCOME, tier == 4 ? OUTCOME_JACKPOT : OUTCOME_WIN);
		Prizes.award(player, header.append(Component.literal("+" + jump + " bucket!").withStyle(ChatFormatting.AQUA)), tier,
				RouletteRules.prizeRolls(stake.getCount()));
	}
}
