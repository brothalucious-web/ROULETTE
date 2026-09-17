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

import com.roulette.PlinkoMenu;
import com.roulette.RouletteMod;
import com.roulette.RouletteRules;
import com.roulette.network.RouletteActionPayload;

public class PlinkoScreen extends AbstractContainerScreen<PlinkoMenu> {
	private static final Identifier TEXTURE = RouletteMod.id("textures/gui/plinko.png");

	/** Board geometry, must match the texture. */
	static final int BOARD_CENTER_X = 107;
	static final int BOUNCE_X = 7;
	static final int FIRST_PEG_Y = 22;
	static final int PEG_SPACING_Y = 10;
	static final int BUCKET_LEFT = 44;
	static final int BUCKET_WIDTH = 14;
	static final int BUCKET_TOP = 98;
	private static final int BALL_START_Y = 12;
	private static final int BALL_REST_Y = 102;

	private static final String[] BUCKET_LABELS = {"+3", "+2", "+1", "=", "X", "=", "+1", "+2", "+3"};

	private Button dropButton;
	private Button[] tabs;

	private int lastSegment = -1;
	private int lastProgress = -1;
	private long progressChangedAt;
	private int lastOutcome = PlinkoMenu.OUTCOME_NONE;
	private long outcomeShownUntil;

	public PlinkoScreen(PlinkoMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, 176, 222);
	}

	@Override
	protected void init() {
		super.init();
		this.dropButton = this.addRenderableWidget(new RouletteButton(
				this.leftPos + 5, this.topPos + 54, 34, 20,
				Component.literal("DROP").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD),
				button -> ClientPlayNetworking.send(new RouletteActionPayload(PlinkoMenu.ACTION_DROP))));

		this.tabs = GameTabs.create(GameTabs.PLINKO);
		GameTabs.placeAbove(this.tabs, this.leftPos, this.topPos);
		for (Button tab : this.tabs) {
			this.addRenderableWidget(tab);
		}
		this.lastOutcome = this.menu.outcome();
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		boolean dropping = this.menu.isDropping();
		this.dropButton.active = !dropping && !this.menu.betStack().isEmpty();

		int progress = this.menu.progress();
		if (progress != this.lastProgress) {
			this.lastProgress = progress;
			this.progressChangedAt = System.currentTimeMillis();
		}
		int segment = Math.min(PlinkoMenu.ROWS, progress / PlinkoMenu.TICKS_PER_ROW);
		if (dropping && segment != this.lastSegment && segment > 0) {
			ClientSounds.play("block.note_block.hat", 0.9F + segment * 0.08F, 0.6F);
		}
		this.lastSegment = segment;

		int outcome = this.menu.outcome();
		if (outcome != this.lastOutcome) {
			switch (outcome) {
				case PlinkoMenu.OUTCOME_LOSE -> ClientSounds.play("block.note_block.bass", 0.6F, 1.0F);
				case PlinkoMenu.OUTCOME_KEEP -> ClientSounds.play("block.note_block.chime", 1.0F, 1.0F);
				case PlinkoMenu.OUTCOME_WIN -> ClientSounds.play("entity.player.levelup", 1.0F, 1.0F);
				case PlinkoMenu.OUTCOME_JACKPOT -> ClientSounds.play("ui.toast.challenge_complete", 1.0F, 1.0F);
				default -> { }
			}
			if (outcome != PlinkoMenu.OUTCOME_NONE) {
				this.outcomeShownUntil = System.currentTimeMillis() + 3000L;
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

		// Light up the bucket the ball landed in.
		if (!this.menu.isDropping() && this.menu.outcome() != PlinkoMenu.OUTCOME_NONE) {
			int bucket = PlinkoMenu.bucketFor(this.menu.path());
			int x = this.leftPos + BUCKET_LEFT + bucket * BUCKET_WIDTH;
			int y = this.topPos + BUCKET_TOP;
			graphics.fill(x + 1, y, x + BUCKET_WIDTH, y + 1, 0xFFFFFFFF);
			graphics.fill(x + 1, y + 14, x + BUCKET_WIDTH, y + 15, 0xFFFFFFFF);
		}

		if (this.menu.isDropping() || this.menu.outcome() != PlinkoMenu.OUTCOME_NONE) {
			float progress = this.menu.progress();
			if (this.menu.isDropping()) {
				// Smooth the motion between server ticks.
				progress += Math.min(1.0F, (System.currentTimeMillis() - this.progressChangedAt) / 50.0F);
			}
			progress = Math.min(progress, PlinkoMenu.TOTAL_TICKS);
			int path = this.menu.path();
			float fall = progress / PlinkoMenu.TICKS_PER_ROW;
			int k = Math.min((int) fall, PlinkoMenu.ROWS);
			float f = Math.min(1.0F, fall - k);
			if (k >= PlinkoMenu.ROWS + 1) {
				k = PlinkoMenu.ROWS;
				f = 1.0F;
			}

			float x0 = ballX(path, k);
			float y0 = ballY(k);
			float x1 = ballX(path, k + 1);
			float y1 = ballY(k + 1);
			float hop = k == 0 ? 0.0F : (float) Math.sin(Math.PI * f) * 4.0F;
			int bx = this.leftPos + Math.round(x0 + (x1 - x0) * f);
			int by = this.topPos + Math.round(y0 + (y1 - y0) * f - hop);
			RouletteScreen.drawBall(graphics, bx, by);
		}
	}

	/** Ball x after it has fallen past {@code k} positions (0 = start, 1 = on the first peg, ROWS + 1 = in a bucket). */
	private static float ballX(int path, int k) {
		if (k <= 1) {
			return BOARD_CENTER_X;
		}
		int bounces = Math.min(k - 1, PlinkoMenu.ROWS);
		int rights = Integer.bitCount(path & ((1 << bounces) - 1));
		return BOARD_CENTER_X + BOUNCE_X * (2 * rights - bounces);
	}

	private static float ballY(int k) {
		if (k == 0) {
			return BALL_START_Y;
		}
		if (k > PlinkoMenu.ROWS) {
			return BALL_REST_Y;
		}
		return FIRST_PEG_Y + PEG_SPACING_Y * (k - 1) - 4;
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		graphics.text(this.font, this.title.getString(), 8, 6, 0xFF404040, false);
		graphics.text(this.font, "Bet", 14, 20, 0xFF404040, false);
		graphics.text(this.font, "Inventory", 8, 128, 0xFF404040, false);

		for (int i = 0; i < PlinkoMenu.BUCKETS; i++) {
			String label = BUCKET_LABELS[i];
			int x = BUCKET_LEFT + i * BUCKET_WIDTH + (BUCKET_WIDTH - this.font.width(label)) / 2 + 1;
			graphics.text(this.font, label, x, BUCKET_TOP + 4, 0xFFFFFFFF, true);
		}

		String status = "";
		int color = 0xFF404040;
		int outcome = this.menu.outcome();
		if (this.menu.isDropping()) {
			status = "Dropping...";
		} else if (outcome != PlinkoMenu.OUTCOME_NONE && System.currentTimeMillis() < this.outcomeShownUntil) {
			switch (outcome) {
				case PlinkoMenu.OUTCOME_LOSE -> {
					status = "Middle bucket - bet lost";
					color = 0xFFA02020;
				}
				case PlinkoMenu.OUTCOME_KEEP -> status = "Safe - you keep your bet";
				case PlinkoMenu.OUTCOME_JACKPOT -> {
					status = "JACKPOT! Check chat";
					color = RouletteScreen.TIER_TEXT_COLORS[4];
				}
				default -> {
					status = "You won! Check chat";
					color = 0xFF2E7D32;
				}
			}
		} else if (this.menu.betStack().isEmpty()) {
			status = "Put an item in the Bet slot";
		} else {
			int tier = RouletteRules.prizeTierForJump(RouletteRules.betTier(this.menu.betStack()), 3);
			status = "Edges win " + RouletteRules.TIER_NAMES[tier] + " prizes";
			color = RouletteScreen.TIER_TEXT_COLORS[tier];
		}
		graphics.text(this.font, status, (176 - this.font.width(status)) / 2, 116, color, false);
	}
}
