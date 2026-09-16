package com.roulette;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

public class RouletteMenu extends AbstractContainerMenu {
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

	private static final int BET_SLOT = 0;
	private static final int INV_START = 1;
	private static final int HOTBAR_START = 28;
	private static final int INV_END = 37;

	private final Container bet;
	private final ContainerData data;

	// Server-only spin progress.
	private int subStepsLeft;
	private int waitTicks;

	/** Client-side constructor. */
	public RouletteMenu(int containerId, Inventory playerInventory) {
		this(containerId, playerInventory, new SimpleContainer(1), new SimpleContainerData(DATA_COUNT));
	}

	public RouletteMenu(int containerId, Inventory playerInventory, Container bet, ContainerData data) {
		super(RouletteMod.ROULETTE_MENU, containerId);
		checkContainerSize(bet, 1);
		checkContainerDataCount(data, DATA_COUNT);
		this.bet = bet;
		this.data = data;

		this.addSlot(new BetSlot(bet, 0, BET_SLOT_X, BET_SLOT_Y));
		this.addStandardInventorySlots(playerInventory, 8, 140);
		this.addDataSlots(data);
	}

	public boolean isSpinning() {
		return this.data.get(DATA_STATE) == 1;
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

	public ItemStack betStack() {
		return this.slots.get(BET_SLOT).getItem();
	}

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

	private void finish(ServerPlayer player) {
		this.data.set(DATA_STATE, 0);
		ItemStack stake = this.bet.getItem(0);
		if (stake.isEmpty()) {
			return;
		}

		int color = RouletteRules.pocketColor(this.ballPosition());
		MutableComponent header = Component.literal("[Roulette] ").withStyle(ChatFormatting.GOLD)
				.append(Component.literal("Landed on ").withStyle(ChatFormatting.GRAY))
				.append(Component.literal(RouletteRules.COLOR_NAMES[color]).withStyle(RouletteRules.COLOR_FORMATS[color], ChatFormatting.BOLD));

		if (color != this.pick()) {
			this.bet.setItem(0, ItemStack.EMPTY);
			this.data.set(DATA_OUTCOME, OUTCOME_LOSE);
			player.sendSystemMessage(header.append(Component.literal(" - you lost your bet.").withStyle(ChatFormatting.RED)));
			return;
		}

		int tier = RouletteRules.prizeTier(RouletteRules.betTier(stake), color);
		int rolls = RouletteRules.prizeRolls(stake.getCount());
		this.data.set(DATA_OUTCOME, tier == 4 ? OUTCOME_JACKPOT : OUTCOME_WIN);

		player.sendSystemMessage(header
				.append(Component.literal("! You keep your bet and win " + rolls + " ").withStyle(ChatFormatting.GREEN))
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

	@Override
	public void removed(Player player) {
		super.removed(player);
		if (player instanceof ServerPlayer serverPlayer) {
			// Closing mid-spin settles the bet immediately so it can't be dodged.
			if (this.isSpinning()) {
				this.data.set(DATA_BALL, Math.floorMod(this.ballPosition() + this.subStepsLeft, RouletteRules.BALL_POSITIONS));
				this.subStepsLeft = 0;
				this.finish(serverPlayer);
			}
			this.clearContainer(player, this.bet);
		}
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		Slot slot = this.slots.get(index);
		if (!slot.hasItem()) {
			return ItemStack.EMPTY;
		}
		ItemStack stack = slot.getItem();
		ItemStack original = stack.copy();

		if (index == BET_SLOT) {
			if (this.isSpinning() || !this.moveItemStackTo(stack, INV_START, INV_END, true)) {
				return ItemStack.EMPTY;
			}
		} else if (this.isSpinning() || !this.moveItemStackTo(stack, BET_SLOT, BET_SLOT + 1, false)) {
			if (index < HOTBAR_START) {
				if (!this.moveItemStackTo(stack, HOTBAR_START, INV_END, false)) {
					return ItemStack.EMPTY;
				}
			} else if (!this.moveItemStackTo(stack, INV_START, HOTBAR_START, false)) {
				return ItemStack.EMPTY;
			}
		}

		if (stack.isEmpty()) {
			slot.set(ItemStack.EMPTY);
		} else {
			slot.setChanged();
		}
		if (stack.getCount() == original.getCount()) {
			return ItemStack.EMPTY;
		}
		slot.onTake(player, stack);
		return original;
	}

	@Override
	public boolean canTakeItemForPickAll(ItemStack carried, Slot target) {
		return target.container != this.bet && super.canTakeItemForPickAll(carried, target);
	}

	@Override
	public boolean stillValid(Player player) {
		return true;
	}

	/** The middle of the wheel. Locked while the ball is rolling. */
	private class BetSlot extends Slot {
		BetSlot(Container container, int index, int x, int y) {
			super(container, index, x, y);
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return !RouletteMenu.this.isSpinning();
		}

		@Override
		public boolean mayPickup(Player player) {
			return !RouletteMenu.this.isSpinning();
		}
	}
}
