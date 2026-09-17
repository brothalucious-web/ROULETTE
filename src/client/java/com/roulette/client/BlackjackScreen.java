package com.roulette.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import com.roulette.BlackjackMenu;
import com.roulette.RouletteMod;
import com.roulette.RouletteRules;
import com.roulette.network.RouletteActionPayload;

public class BlackjackScreen extends AbstractContainerScreen<BlackjackMenu> {
	private static final Identifier TEXTURE = RouletteMod.id("textures/gui/blackjack.png");

	private static final int CARDS_LEFT = 30;
	private static final int CARDS_RIGHT = 169;
	private static final int DEALER_ROW_Y = 17;
	private static final int PLAYER_ROW_Y = 56;
	private static final int CARD_WIDTH = 18;
	private static final int CARD_HEIGHT = 26;

	private static final String[] SUITS = {"\u2660", "\u2665", "\u2666", "\u2663"};

	private Button dealButton;
	private Button hitButton;
	private Button standButton;
	private Button[] tabs;

	private int lastCardTotal = -1;
	private int lastOutcome = BlackjackMenu.OUTCOME_NONE;

	public BlackjackScreen(BlackjackMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, 176, 222);
	}

	@Override
	protected void init() {
		super.init();
		this.dealButton = this.addRenderableWidget(new RouletteButton(this.leftPos + 6, this.topPos + 105, 52, 18,
				Component.literal("DEAL").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
				button -> send(BlackjackMenu.ACTION_DEAL)));
		this.hitButton = this.addRenderableWidget(new RouletteButton(this.leftPos + 62, this.topPos + 105, 52, 18,
				Component.literal("HIT").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD),
				button -> send(BlackjackMenu.ACTION_HIT)));
		this.standButton = this.addRenderableWidget(new RouletteButton(this.leftPos + 118, this.topPos + 105, 52, 18,
				Component.literal("STAND").withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
				button -> send(BlackjackMenu.ACTION_STAND)));

		this.tabs = GameTabs.create(GameTabs.BLACKJACK);
		GameTabs.placeAbove(this.tabs, this.leftPos, this.topPos);
		for (Button tab : this.tabs) {
			this.addRenderableWidget(tab);
		}
		this.lastOutcome = this.menu.outcome();
		this.lastCardTotal = this.menu.playerCount() + this.menu.dealerCount();
	}

	private static void send(int action) {
		ClientPlayNetworking.send(new RouletteActionPayload(action));
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		int state = this.menu.state();
		this.dealButton.active = state == BlackjackMenu.STATE_BETTING && !this.menu.betStack().isEmpty();
		this.hitButton.active = state == BlackjackMenu.STATE_PLAYER;
		this.standButton.active = state == BlackjackMenu.STATE_PLAYER;

		int cardTotal = this.menu.playerCount() + this.menu.dealerCount();
		if (cardTotal > this.lastCardTotal && this.lastCardTotal >= 0) {
			ClientSounds.play("item.book.page_turn", 1.2F, 1.0F);
		}
		this.lastCardTotal = cardTotal;

		int outcome = this.menu.outcome();
		if (outcome != this.lastOutcome) {
			switch (outcome) {
				case BlackjackMenu.OUTCOME_LOSE -> ClientSounds.play("block.note_block.bass", 0.6F, 1.0F);
				case BlackjackMenu.OUTCOME_PUSH -> ClientSounds.play("block.note_block.chime", 1.0F, 1.0F);
				case BlackjackMenu.OUTCOME_WIN -> ClientSounds.play("entity.player.levelup", 1.0F, 1.0F);
				case BlackjackMenu.OUTCOME_BLACKJACK -> ClientSounds.play("ui.toast.challenge_complete", 1.0F, 1.0F);
				default -> { }
			}
			this.lastOutcome = outcome;
		}

		super.extractRenderState(graphics, mouseX, mouseY, a);
		GameTabs.drawIcons(graphics, this.tabs);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractBackground(graphics, mouseX, mouseY, a);
		graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.leftPos, this.topPos, 0, 0, 176, 222, 256, 256);

		int dealerCount = this.menu.dealerCount();
		for (int i = 0; i < dealerCount; i++) {
			boolean hidden = i == 1 && !this.menu.dealerRevealed();
			this.drawCard(graphics, cardX(i, dealerCount), this.topPos + DEALER_ROW_Y, this.menu.dealerCard(i), hidden);
		}
		int playerCount = this.menu.playerCount();
		for (int i = 0; i < playerCount; i++) {
			this.drawCard(graphics, cardX(i, playerCount), this.topPos + PLAYER_ROW_Y, this.menu.playerCard(i), false);
		}
	}

	private int cardX(int index, int count) {
		int step = count <= 1 ? 0 : Math.min(21, (CARDS_RIGHT - CARDS_LEFT - CARD_WIDTH) / (count - 1));
		return this.leftPos + CARDS_LEFT + index * step;
	}

	private void drawCard(GuiGraphicsExtractor graphics, int x, int y, int card, boolean hidden) {
		graphics.fill(x, y, x + CARD_WIDTH, y + CARD_HEIGHT, 0xFF202020);
		if (hidden || card < 0) {
			graphics.fill(x + 1, y + 1, x + CARD_WIDTH - 1, y + CARD_HEIGHT - 1, 0xFF1E3F9A);
			graphics.fill(x + 3, y + 3, x + CARD_WIDTH - 3, y + CARD_HEIGHT - 3, 0xFF2F5BD0);
			for (int yy = y + 5; yy < y + CARD_HEIGHT - 4; yy += 4) {
				graphics.fill(x + 5, yy, x + CARD_WIDTH - 5, yy + 1, 0xFF9FB6F0);
			}
			return;
		}
		graphics.fill(x + 1, y + 1, x + CARD_WIDTH - 1, y + CARD_HEIGHT - 1, 0xFFF8F6EE);
		int suit = card % 4;
		int color = (suit == 1 || suit == 2) ? 0xFFC62828 : 0xFF151515;
		graphics.text(this.font, BlackjackMenu.RANK_NAMES[card / 4], x + 2, y + 2, color, false);
		graphics.text(this.font, SUITS[suit], x + CARD_WIDTH - 8, y + CARD_HEIGHT - 10, color, false);
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		graphics.text(this.font, this.title.getString(), 8, 6, 0xFF404040, false);
		graphics.text(this.font, "Bet", 8, 56, 0xFFF0E6C0, true);
		graphics.text(this.font, "Inventory", 8, 128, 0xFF404040, false);

		int state = this.menu.state();
		if (this.menu.dealerCount() > 0) {
			String dealer = "Dealer: " + this.menu.visibleDealerValue() + (this.menu.dealerRevealed() ? "" : " + ?");
			graphics.text(this.font, dealer, CARDS_LEFT, DEALER_ROW_Y + CARD_HEIGHT + 2, 0xFFFFFFFF, true);
		}
		if (this.menu.playerCount() > 0) {
			graphics.text(this.font, "You: " + this.menu.playerValue(), CARDS_LEFT, PLAYER_ROW_Y + CARD_HEIGHT + 2, 0xFFFFFFFF, true);
		}

		String status;
		int color = 0xFFFFFFFF;
		int outcome = this.menu.outcome();
		if (state == BlackjackMenu.STATE_PLAYER) {
			status = "Hit or Stand?";
			color = 0xFFFFE680;
		} else if (state == BlackjackMenu.STATE_DEALER) {
			status = "Dealer's turn...";
		} else if (outcome == BlackjackMenu.OUTCOME_BLACKJACK) {
			status = "BLACKJACK! Check chat";
			color = 0xFFFFC23D;
		} else if (outcome == BlackjackMenu.OUTCOME_WIN) {
			status = "You win! Check chat";
			color = 0xFF8CFF8C;
		} else if (outcome == BlackjackMenu.OUTCOME_PUSH) {
			status = "Push - bet returned";
			color = 0xFFFFE680;
		} else if (outcome == BlackjackMenu.OUTCOME_LOSE) {
			status = this.menu.playerValue() > 21 ? "Bust - bet lost" : "Dealer wins - bet lost";
			color = 0xFFFF8080;
		} else if (this.menu.betStack().isEmpty()) {
			status = "Put an item in the Bet slot";
		} else {
			int betTier = RouletteRules.betTier(this.menu.betStack());
			status = "Win " + RouletteRules.TIER_NAMES[RouletteRules.prizeTierForJump(betTier, 1)]
					+ " / BJ " + RouletteRules.TIER_NAMES[RouletteRules.prizeTierForJump(betTier, 2)];
		}
		graphics.text(this.font, status, (176 - this.font.width(status)) / 2, 94, color, true);
	}
}
