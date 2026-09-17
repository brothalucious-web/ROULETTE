package com.roulette.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import com.roulette.RouletteMod;
import com.roulette.SlotMachineMenu;
import com.roulette.network.RouletteActionPayload;

public class SlotMachineScreen extends AbstractContainerScreen<SlotMachineMenu> {
	private static final Identifier TEXTURE = RouletteMod.id("textures/gui/slots.png");
	static final int REEL_LEFT = 51;
	static final int REEL_SPACING = 26;
	static final int REEL_TOP = 26;
	static final int ROW_HEIGHT = 18;

	private final ItemStack[] symbols = new ItemStack[SlotMachineMenu.SYMBOLS];
	private Button pullButton;

	private int lastStopped;
	private int lastReel0 = -1;
	private int lastOutcome = SlotMachineMenu.OUTCOME_NONE;
	private long outcomeShownUntil;

	public SlotMachineScreen(SlotMachineMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, 176, 222);
		for (int i = 0; i < SlotMachineMenu.SYMBOLS; i++) {
			this.symbols[i] = new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(SlotMachineMenu.SYMBOL_ITEMS[i])));
		}
	}

	@Override
	protected void init() {
		super.init();
		this.pullButton = this.addRenderableWidget(new RouletteButton(
				this.leftPos + 139, this.topPos + 48, 32, 20,
				Component.literal("PULL").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
				button -> ClientPlayNetworking.send(new RouletteActionPayload(SlotMachineMenu.ACTION_PULL))));
		this.lastOutcome = this.menu.outcome();
		this.lastStopped = this.menu.stoppedReels();
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		boolean spinning = this.menu.isSpinning();
		this.pullButton.active = !spinning && !this.menu.betStack().isEmpty();

		if (spinning) {
			int reel0 = this.menu.reel(0);
			if (this.menu.stoppedReels() == 0 && reel0 != this.lastReel0 && reel0 % 2 == 0) {
				ClientSounds.play("block.note_block.hat", 1.8F, 0.35F);
			}
			this.lastReel0 = reel0;
		}
		int stopped = this.menu.stoppedReels();
		if (spinning && stopped > this.lastStopped) {
			boolean reach = stopped == 2 && this.menu.reel(0) == this.menu.reel(1);
			ClientSounds.play(reach ? "block.note_block.bell" : "block.note_block.pling", reach ? 1.5F : 1.0F, 0.8F);
		}
		this.lastStopped = stopped;

		int outcome = this.menu.outcome();
		if (outcome != this.lastOutcome) {
			switch (outcome) {
				case SlotMachineMenu.OUTCOME_LOSE -> ClientSounds.play("block.note_block.bass", 0.6F, 1.0F);
				case SlotMachineMenu.OUTCOME_KEEP -> ClientSounds.play("block.note_block.chime", 1.0F, 1.0F);
				case SlotMachineMenu.OUTCOME_WIN -> ClientSounds.play("entity.player.levelup", 1.0F, 1.0F);
				case SlotMachineMenu.OUTCOME_JACKPOT -> ClientSounds.play("ui.toast.challenge_complete", 1.0F, 1.0F);
				default -> { }
			}
			if (outcome != SlotMachineMenu.OUTCOME_NONE) {
				this.outcomeShownUntil = System.currentTimeMillis() + 3000L;
			}
			this.lastOutcome = outcome;
		}

		super.extractRenderState(graphics, mouseX, mouseY, a);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractBackground(graphics, mouseX, mouseY, a);
		graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.leftPos, this.topPos, 0, 0, 176, 222, 256, 256);

		boolean flash = !this.menu.isSpinning()
				&& (this.menu.outcome() == SlotMachineMenu.OUTCOME_WIN || this.menu.outcome() == SlotMachineMenu.OUTCOME_JACKPOT)
				&& System.currentTimeMillis() < this.outcomeShownUntil
				&& (System.currentTimeMillis() / 250L) % 2L == 0L;
		if (flash) {
			int y = this.topPos + REEL_TOP + ROW_HEIGHT;
			graphics.fill(this.leftPos + REEL_LEFT, y, this.leftPos + REEL_LEFT + REEL_SPACING * 2 + 22, y + ROW_HEIGHT, 0x80FFD700);
		}

		for (int i = 0; i < 3; i++) {
			int face = this.menu.reel(i);
			int x = this.leftPos + REEL_LEFT + i * REEL_SPACING + 3;
			for (int row = 0; row < 3; row++) {
				int symbol = Math.floorMod(face + (row - 1), SlotMachineMenu.SYMBOLS);
				graphics.item(this.symbols[symbol], x, this.topPos + REEL_TOP + 1 + row * ROW_HEIGHT);
			}
		}
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		graphics.text(this.font, this.title.getString(), 8, 6, 0xFF404040, false);
		graphics.text(this.font, "Bet", 14, 38, 0xFF404040, false);
		graphics.text(this.font, "Inventory", 8, 128, 0xFF404040, false);

		String status = "";
		int color = 0xFF404040;
		int outcome = this.menu.outcome();
		if (this.menu.isSpinning()) {
			boolean reach = this.menu.stoppedReels() == 2 && this.menu.reel(0) == this.menu.reel(1);
			status = reach ? "REACH!" : "Spinning...";
			color = reach ? 0xFFB26A00 : 0xFF404040;
		} else if (outcome != SlotMachineMenu.OUTCOME_NONE && System.currentTimeMillis() < this.outcomeShownUntil) {
			switch (outcome) {
				case SlotMachineMenu.OUTCOME_LOSE -> {
					status = "No match - bet lost";
					color = 0xFFA02020;
				}
				case SlotMachineMenu.OUTCOME_KEEP -> status = "A pair - you keep your bet";
				case SlotMachineMenu.OUTCOME_JACKPOT -> {
					status = "JACKPOT! Check chat";
					color = RouletteScreen.TIER_TEXT_COLORS[4];
				}
				default -> {
					status = "Triple! Check chat";
					color = 0xFF2E7D32;
				}
			}
		} else if (this.menu.betStack().isEmpty()) {
			status = "Put an item in the Bet slot";
		} else {
			status = "3 Stars = JACKPOT";
			color = RouletteScreen.TIER_TEXT_COLORS[4];
		}
		graphics.text(this.font, status, (176 - this.font.width(status)) / 2, 114, color, false);
	}
}
