package com.roulette;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Shared logic for games with one bet slot above the player's inventory. */
public abstract class BetMenu extends AbstractContainerMenu implements GameMenu {
	protected static final int BET_SLOT = 0;
	private static final int INV_START = 1;
	private static final int HOTBAR_START = 28;
	private static final int INV_END = 37;

	protected final Container bet;
	protected final ContainerData data;

	protected BetMenu(MenuType<?> type, int containerId, Inventory playerInventory, Container bet, ContainerData data,
			int dataCount, int betX, int betY) {
		super(type, containerId);
		checkContainerSize(bet, 1);
		checkContainerDataCount(data, dataCount);
		this.bet = bet;
		this.data = data;

		this.addSlot(new BetSlot(bet, 0, betX, betY));
		this.addStandardInventorySlots(playerInventory, 8, 140);
		this.addDataSlots(data);
	}

	/** True while the bet is in play and must stay locked. */
	public abstract boolean isBusy();

	/** Finish the current round instantly (used when the screen is closed mid-game). */
	protected abstract void settleNow(ServerPlayer player);

	public ItemStack betStack() {
		return this.slots.get(BET_SLOT).getItem();
	}

	@Override
	public void removed(Player player) {
		super.removed(player);
		if (player instanceof ServerPlayer serverPlayer) {
			if (this.isBusy()) {
				this.settleNow(serverPlayer);
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
			if (this.isBusy() || !this.moveItemStackTo(stack, INV_START, INV_END, true)) {
				return ItemStack.EMPTY;
			}
		} else if (this.isBusy() || !this.moveItemStackTo(stack, BET_SLOT, BET_SLOT + 1, false)) {
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

	private class BetSlot extends Slot {
		BetSlot(Container container, int index, int x, int y) {
			super(container, index, x, y);
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return !BetMenu.this.isBusy();
		}

		@Override
		public boolean mayPickup(Player player) {
			return !BetMenu.this.isBusy();
		}
	}
}
