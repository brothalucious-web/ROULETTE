package com.roulette;

import java.util.ArrayList;
import java.util.List;

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

/** Blackjack against the dealer, one deck per hand. Dealer stands on 17. */
public class BlackjackMenu extends BetMenu {
	public static final int MAX_CARDS = 10;

	public static final int DATA_STATE = 0;
	public static final int DATA_OUTCOME = 1;
	public static final int DATA_REVEALED = 2;
	public static final int DATA_PLAYER_COUNT = 3;
	public static final int DATA_DEALER_COUNT = 4;
	public static final int DATA_PLAYER_CARDS = 5;
	public static final int DATA_DEALER_CARDS = DATA_PLAYER_CARDS + MAX_CARDS;
	public static final int DATA_COUNT = DATA_DEALER_CARDS + MAX_CARDS;

	public static final int STATE_BETTING = 0;
	public static final int STATE_PLAYER = 1;
	public static final int STATE_DEALER = 2;

	public static final int OUTCOME_NONE = 0;
	public static final int OUTCOME_LOSE = 1;
	public static final int OUTCOME_PUSH = 2;
	public static final int OUTCOME_WIN = 3;
	public static final int OUTCOME_BLACKJACK = 4;

	public static final int ACTION_DEAL = 0;
	public static final int ACTION_HIT = 1;
	public static final int ACTION_STAND = 2;

	public static final int BET_SLOT_X = 8;
	public static final int BET_SLOT_Y = 66;

	/** Cards are 0-51: rank * 4 + suit. Rank 0 = Ace, 12 = King. */
	public static final String[] RANK_NAMES = {"A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K"};

	private final List<Integer> deck = new ArrayList<>();
	private int dealerTimer;

	/** Client-side constructor. */
	public BlackjackMenu(int containerId, Inventory playerInventory) {
		this(containerId, playerInventory, new SimpleContainer(1), new SimpleContainerData(DATA_COUNT));
	}

	public BlackjackMenu(int containerId, Inventory playerInventory, Container bet, ContainerData data) {
		super(RouletteMod.BLACKJACK_MENU, containerId, playerInventory, bet, data, DATA_COUNT, BET_SLOT_X, BET_SLOT_Y);
	}

	public int state() {
		return this.data.get(DATA_STATE);
	}

	@Override
	public boolean isBusy() {
		return this.state() != STATE_BETTING;
	}

	public int outcome() {
		return this.data.get(DATA_OUTCOME);
	}

	public boolean dealerRevealed() {
		return this.data.get(DATA_REVEALED) == 1;
	}

	public int playerCount() {
		return this.data.get(DATA_PLAYER_COUNT);
	}

	public int dealerCount() {
		return this.data.get(DATA_DEALER_COUNT);
	}

	/** Returns the card (0-51) or -1 for none. */
	public int playerCard(int index) {
		return this.data.get(DATA_PLAYER_CARDS + index) - 1;
	}

	public int dealerCard(int index) {
		return this.data.get(DATA_DEALER_CARDS + index) - 1;
	}

	public int playerValue() {
		return this.handValue(DATA_PLAYER_CARDS, this.playerCount());
	}

	public int dealerValue() {
		return this.handValue(DATA_DEALER_CARDS, this.dealerCount());
	}

	/** What the player is allowed to see of the dealer's hand. */
	public int visibleDealerValue() {
		return this.handValue(DATA_DEALER_CARDS, this.dealerRevealed() ? this.dealerCount() : Math.min(1, this.dealerCount()));
	}

	private int handValue(int start, int count) {
		int total = 0;
		boolean hasAce = false;
		for (int i = 0; i < count; i++) {
			int card = this.data.get(start + i) - 1;
			if (card < 0) {
				continue;
			}
			int rank = card / 4;
			if (rank == 0) {
				hasAce = true;
			}
			total += Math.min(10, rank + 1);
		}
		return hasAce && total + 10 <= 21 ? total + 10 : total;
	}

	@Override
	public void handleAction(ServerPlayer player, int action) {
		switch (action) {
			case ACTION_DEAL -> this.deal(player);
			case ACTION_HIT -> this.hit(player);
			case ACTION_STAND -> this.stand();
			default -> { }
		}
	}

	private void deal(ServerPlayer player) {
		if (this.state() != STATE_BETTING || this.bet.getItem(0).isEmpty()) {
			return;
		}
		// Fresh shuffled deck every hand.
		this.deck.clear();
		for (int i = 0; i < 52; i++) {
			this.deck.add(i);
		}
		var random = player.getRandom();
		for (int i = this.deck.size() - 1; i > 0; i--) {
			int j = random.nextInt(i + 1);
			int tmp = this.deck.get(i);
			this.deck.set(i, this.deck.get(j));
			this.deck.set(j, tmp);
		}

		for (int i = 0; i < MAX_CARDS; i++) {
			this.data.set(DATA_PLAYER_CARDS + i, 0);
			this.data.set(DATA_DEALER_CARDS + i, 0);
		}
		this.data.set(DATA_PLAYER_COUNT, 0);
		this.data.set(DATA_DEALER_COUNT, 0);
		this.data.set(DATA_REVEALED, 0);
		this.data.set(DATA_OUTCOME, OUTCOME_NONE);

		this.giveCard(DATA_PLAYER_CARDS, DATA_PLAYER_COUNT);
		this.giveCard(DATA_DEALER_CARDS, DATA_DEALER_COUNT);
		this.giveCard(DATA_PLAYER_CARDS, DATA_PLAYER_COUNT);
		this.giveCard(DATA_DEALER_CARDS, DATA_DEALER_COUNT);
		this.data.set(DATA_STATE, STATE_PLAYER);

		if (this.playerValue() == 21 || this.dealerValue() == 21) {
			this.data.set(DATA_REVEALED, 1);
			this.resolve(player);
		}
	}

	private void giveCard(int cardsStart, int countSlot) {
		int count = this.data.get(countSlot);
		if (count >= MAX_CARDS || this.deck.isEmpty()) {
			return;
		}
		int card = this.deck.remove(this.deck.size() - 1);
		this.data.set(cardsStart + count, card + 1);
		this.data.set(countSlot, count + 1);
	}

	private void hit(ServerPlayer player) {
		if (this.state() != STATE_PLAYER) {
			return;
		}
		this.giveCard(DATA_PLAYER_CARDS, DATA_PLAYER_COUNT);
		int value = this.playerValue();
		if (value > 21) {
			this.data.set(DATA_REVEALED, 1);
			this.resolve(player);
		} else if (value == 21 || this.playerCount() >= MAX_CARDS) {
			this.stand();
		}
	}

	private void stand() {
		if (this.state() != STATE_PLAYER) {
			return;
		}
		this.data.set(DATA_STATE, STATE_DEALER);
		this.data.set(DATA_REVEALED, 1);
		this.dealerTimer = 15;
	}

	@Override
	public void serverTick(ServerPlayer player) {
		if (this.state() != STATE_DEALER) {
			return;
		}
		if (--this.dealerTimer > 0) {
			return;
		}
		if (this.dealerValue() < 17 && this.dealerCount() < MAX_CARDS) {
			this.giveCard(DATA_DEALER_CARDS, DATA_DEALER_COUNT);
			this.dealerTimer = 12;
		} else {
			this.resolve(player);
		}
	}

	@Override
	protected void settleNow(ServerPlayer player) {
		if (this.state() == STATE_PLAYER) {
			this.stand();
		}
		while (this.dealerValue() < 17 && this.dealerCount() < MAX_CARDS && !this.deck.isEmpty()) {
			this.giveCard(DATA_DEALER_CARDS, DATA_DEALER_COUNT);
		}
		this.resolve(player);
	}

	private void resolve(ServerPlayer player) {
		this.data.set(DATA_STATE, STATE_BETTING);
		this.data.set(DATA_REVEALED, 1);
		ItemStack stake = this.bet.getItem(0);
		if (stake.isEmpty()) {
			return;
		}

		int you = this.playerValue();
		int dealer = this.dealerValue();
		boolean youBlackjack = you == 21 && this.playerCount() == 2;
		boolean dealerBlackjack = dealer == 21 && this.dealerCount() == 2;
		MutableComponent header = Prizes.prefix().append(Component.literal("Blackjack: you " + you + ", dealer " + dealer + ".")
				.withStyle(ChatFormatting.WHITE));

		if (you > 21) {
			this.lose(player, header, " Bust - you lost your bet.");
		} else if (youBlackjack && !dealerBlackjack) {
			this.win(player, stake, header.append(Component.literal(" BLACKJACK!").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)), 2);
		} else if (dealerBlackjack && !youBlackjack) {
			this.lose(player, header, " Dealer has blackjack - you lost your bet.");
		} else if (dealer > 21 || you > dealer) {
			this.win(player, stake, header.append(Component.literal(dealer > 21 ? " Dealer busts!" : " You beat the dealer!")
					.withStyle(ChatFormatting.GREEN)), 1);
		} else if (you == dealer) {
			this.data.set(DATA_OUTCOME, OUTCOME_PUSH);
			player.sendSystemMessage(header.append(Component.literal(" Push - you keep your bet.").withStyle(ChatFormatting.YELLOW)));
		} else {
			this.lose(player, header, " Dealer wins - you lost your bet.");
		}
	}

	private void win(ServerPlayer player, ItemStack stake, MutableComponent header, int jump) {
		int tier = RouletteRules.prizeTierForJump(RouletteRules.betTier(stake), jump);
		this.data.set(DATA_OUTCOME, jump == 2 ? OUTCOME_BLACKJACK : OUTCOME_WIN);
		Prizes.award(player, header, tier, RouletteRules.prizeRolls(stake.getCount()));
	}

	private void lose(ServerPlayer player, MutableComponent header, String message) {
		this.bet.setItem(0, ItemStack.EMPTY);
		this.data.set(DATA_OUTCOME, OUTCOME_LOSE);
		player.sendSystemMessage(header.append(Component.literal(message).withStyle(ChatFormatting.RED)));
	}
}
