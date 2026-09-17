package com.roulette;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

/** A three-reel slot machine found inside the Jackpot Palace domain. */
public class SlotMachineMenu extends BetMenu {
	public static final int DATA_STATE = 0;
	public static final int DATA_REEL = 1; // reels use slots 1, 2, 3
	public static final int DATA_STOPPED = 4;
	public static final int DATA_OUTCOME = 5;
	public static final int DATA_COUNT = 6;

	public static final int OUTCOME_NONE = 0;
	public static final int OUTCOME_LOSE = 1;
	public static final int OUTCOME_KEEP = 2;
	public static final int OUTCOME_WIN = 3;
	public static final int OUTCOME_JACKPOT = 4;

	public static final int ACTION_PULL = 0;

	/** Reel symbols, lowest to highest. Three stars is the jackpot. */
	public static final String[] SYMBOL_ITEMS = {"coal", "iron_ingot", "gold_ingot", "emerald", "diamond", "netherite_ingot", "nether_star"};
	public static final String[] SYMBOL_NAMES = {"Coal", "Iron", "Gold", "Emerald", "Diamond", "Netherite", "Star"};
	public static final int SYMBOLS = SYMBOL_ITEMS.length;
	public static final int STAR = 6;

	public static final int BET_SLOT_X = 15;
	public static final int BET_SLOT_Y = 48;

	/** Server only: the machine block this menu belongs to (null on the client). */
	private final BlockPos machinePos;
	private final int[] targets = new int[3];
	private final int[] stopAt = new int[3];
	private int spinTicks;

	/** Client-side constructor. */
	public SlotMachineMenu(int containerId, Inventory playerInventory) {
		this(containerId, playerInventory, new SimpleContainer(1), new SimpleContainerData(DATA_COUNT), null);
	}

	public SlotMachineMenu(int containerId, Inventory playerInventory, Container bet, ContainerData data, BlockPos machinePos) {
		super(RouletteMod.SLOT_MACHINE_MENU, containerId, playerInventory, bet, data, DATA_COUNT, BET_SLOT_X, BET_SLOT_Y);
		this.machinePos = machinePos;
	}

	public boolean isSpinning() {
		return this.data.get(DATA_STATE) == 1;
	}

	@Override
	public boolean isBusy() {
		return this.isSpinning();
	}

	public int reel(int index) {
		return Math.floorMod(this.data.get(DATA_REEL + index), SYMBOLS);
	}

	public int stoppedReels() {
		return this.data.get(DATA_STOPPED);
	}

	public int outcome() {
		return this.data.get(DATA_OUTCOME);
	}

	@Override
	public boolean stillValid(Player player) {
		// Closes automatically when the domain collapses.
		return this.machinePos == null || DomainManager.isMachine(player.level(), this.machinePos);
	}

	@Override
	public void handleAction(ServerPlayer player, int action) {
		if (action != ACTION_PULL || this.isSpinning() || this.bet.getItem(0).isEmpty()) {
			return;
		}
		var random = player.getRandom();
		int roll = random.nextInt(100);
		if (roll < 4) {
			this.setTargets(STAR, STAR, STAR);                  // 4%  jackpot
		} else if (roll < 12) {
			int s = 4 + random.nextInt(2);                      // 8%  diamond / netherite triple
			this.setTargets(s, s, s);
		} else if (roll < 26) {
			int s = 2 + random.nextInt(2);                      // 14% gold / emerald triple
			this.setTargets(s, s, s);
		} else if (roll < 44) {
			int s = random.nextInt(2);                          // 18% coal / iron triple
			this.setTargets(s, s, s);
		} else if (roll < 72) {
			int s = random.nextInt(SYMBOLS);                    // 28% a pair: keep your bet
			int other = (s + 1 + random.nextInt(SYMBOLS - 1)) % SYMBOLS;
			int odd = random.nextInt(3);
			this.setTargets(odd == 0 ? other : s, odd == 1 ? other : s, odd == 2 ? other : s);
		} else {
			int a = random.nextInt(SYMBOLS);                    // 28% no match: bet lost
			int b = (a + 1 + random.nextInt(SYMBOLS - 1)) % SYMBOLS;
			int c = random.nextInt(SYMBOLS);
			while (c == a || c == b) {
				c = random.nextInt(SYMBOLS);
			}
			this.setTargets(a, b, c);
		}

		this.stopAt[0] = 20;
		this.stopAt[1] = 35;
		// "Reach": if the first two match, the last reel keeps you waiting.
		this.stopAt[2] = this.targets[0] == this.targets[1] ? 85 : 50;
		this.spinTicks = 0;
		this.data.set(DATA_STOPPED, 0);
		this.data.set(DATA_OUTCOME, OUTCOME_NONE);
		this.data.set(DATA_STATE, 1);
	}

	private void setTargets(int a, int b, int c) {
		this.targets[0] = a;
		this.targets[1] = b;
		this.targets[2] = c;
	}

	@Override
	public void serverTick(ServerPlayer player) {
		if (!this.isSpinning()) {
			return;
		}
		this.spinTicks++;
		int stopped = this.stoppedReels();
		for (int i = stopped; i < 3; i++) {
			if (i == stopped && this.spinTicks >= this.stopAt[i]) {
				this.data.set(DATA_REEL + i, this.targets[i]);
				stopped++;
			} else {
				this.data.set(DATA_REEL + i, (this.reel(i) + 1) % SYMBOLS);
			}
		}
		this.data.set(DATA_STOPPED, stopped);
		if (stopped >= 3) {
			this.finish(player);
		}
	}

	@Override
	protected void settleNow(ServerPlayer player) {
		for (int i = 0; i < 3; i++) {
			this.data.set(DATA_REEL + i, this.targets[i]);
		}
		this.data.set(DATA_STOPPED, 3);
		this.finish(player);
	}

	private void finish(ServerPlayer player) {
		this.data.set(DATA_STATE, 0);
		ItemStack stake = this.bet.getItem(0);
		if (stake.isEmpty()) {
			return;
		}

		int a = this.targets[0];
		int b = this.targets[1];
		int c = this.targets[2];
		MutableComponent header = Prizes.prefix()
				.append(Component.literal("Slots [" + SYMBOL_NAMES[a] + " | " + SYMBOL_NAMES[b] + " | " + SYMBOL_NAMES[c] + "]")
						.withStyle(ChatFormatting.WHITE));

		if (a == b && b == c) {
			if (a == STAR) {
				this.data.set(DATA_OUTCOME, OUTCOME_JACKPOT);
				player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, DomainManager.JACKPOT_BUFF_TICKS, 1));
				player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, DomainManager.JACKPOT_BUFF_TICKS, 1));
				player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, DomainManager.JACKPOT_BUFF_TICKS, 0));
				player.addEffect(new MobEffectInstance(MobEffects.LUCK, DomainManager.JACKPOT_BUFF_TICKS, 0));
				player.sendSystemMessage(Component.literal("JACKPOT! Your luck overflows for 4 minutes 11 seconds!")
						.withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
				Prizes.award(player, header, 4, RouletteRules.prizeRolls(stake.getCount()));
				return;
			}
			int jump = a < 2 ? 1 : (a < 4 ? 2 : 3);
			int tier = RouletteRules.prizeTierForJump(RouletteRules.betTier(stake), jump);
			this.data.set(DATA_OUTCOME, tier == 4 ? OUTCOME_JACKPOT : OUTCOME_WIN);
			Prizes.award(player, header.append(Component.literal(" Triple!").withStyle(ChatFormatting.AQUA)), tier,
					RouletteRules.prizeRolls(stake.getCount()));
			return;
		}

		if (a == b || b == c || a == c) {
			this.data.set(DATA_OUTCOME, OUTCOME_KEEP);
			player.sendSystemMessage(header.append(Component.literal(" A pair - you keep your bet.").withStyle(ChatFormatting.YELLOW)));
			return;
		}

		this.bet.setItem(0, ItemStack.EMPTY);
		this.data.set(DATA_OUTCOME, OUTCOME_LOSE);
		player.sendSystemMessage(header.append(Component.literal(" No match - you lost your bet.").withStyle(ChatFormatting.RED)));
	}
}
