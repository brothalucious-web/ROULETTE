package com.roulette.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import com.roulette.RouletteMenu;
import com.roulette.RouletteMod;
import com.roulette.RouletteRules;
import com.roulette.network.RouletteActionPayload;

public class RouletteScreen extends AbstractContainerScreen<RouletteMenu> {
	private static final Identifier TEXTURE = RouletteMod.id("textures/gui/roulette.png");
	private static final int WHEEL_CENTER_X = 88;
	private static final int WHEEL_CENTER_Y = 64;
	private static final int BALL_ORBIT = 34;

	private static final String[] PICK_LABELS = {"Red", "Black", "Green"};
	private static final int[] PICK_MARKER_COLORS = {0xFFD02020, 0xFF202020, 0xFF20A030};
	private static final int[] TIER_TEXT_COLORS = {0xFF404040, 0xFF2E7D32, 0xFF1B7F8C, 0xFF8E24AA, 0xFFB26A00};

	private final Button[] pickButtons = new Button[3];
	private Button spinButton;
	private Button inventoryTab;
	private Button rouletteTab;
	private final ItemStack inventoryIcon = new ItemStack(Items.CHEST);
	private final ItemStack rouletteIcon = new ItemStack(Items.CLOCK);

	private int lastPocket = -1;
	private int lastOutcome = RouletteMenu.OUTCOME_NONE;
	private long outcomeShownUntil;

	public RouletteScreen(RouletteMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, 176, 222);
	}

	@Override
	protected void init() {
		super.init();

		for (int i = 0; i < 3; i++) {
			final int action = i;
			this.pickButtons[i] = this.addRenderableWidget(new RouletteButton(
					this.leftPos + 6, this.topPos + 26 + i * 24, 32, 20,
					Component.literal(PICK_LABELS[i]).withStyle(RouletteRules.COLOR_FORMATS[i]),
					button -> sendAction(action)));
		}
		this.pickButtons[RouletteRules.GREEN].setTooltip(Tooltip.create(Component.literal("Rarer, but prizes jump 2 tiers")));

		this.spinButton = this.addRenderableWidget(new RouletteButton(
				this.leftPos + 139, this.topPos + 54, 32, 20,
				Component.literal("SPIN").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD),
				button -> sendAction(RouletteMenu.ACTION_SPIN)));

		this.inventoryTab = this.addRenderableWidget(new RouletteButton(
				this.leftPos + InventoryTabs.INVENTORY_TAB_X, this.topPos - InventoryTabs.TAB_HEIGHT,
				InventoryTabs.TAB_WIDTH, InventoryTabs.TAB_HEIGHT, Component.empty(),
				button -> backToInventory()));
		this.inventoryTab.setTooltip(Tooltip.create(Component.literal("Inventory")));

		this.rouletteTab = this.addRenderableWidget(new RouletteButton(
				this.leftPos + InventoryTabs.ROULETTE_TAB_X, this.topPos - InventoryTabs.TAB_HEIGHT,
				InventoryTabs.TAB_WIDTH, InventoryTabs.TAB_HEIGHT, Component.empty(),
				button -> { }));
		this.rouletteTab.active = false;

		this.lastOutcome = this.menu.outcome();
	}

	private static void sendAction(int action) {
		ClientPlayNetworking.send(new RouletteActionPayload(action));
	}

	private void backToInventory() {
		Minecraft mc = this.minecraft;
		if (mc.player == null) {
			return;
		}
		mc.player.closeContainer();
		mc.gui.setScreen(new InventoryScreen(mc.player));
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		boolean spinning = this.menu.isSpinning();
		for (Button button : this.pickButtons) {
			button.active = !spinning;
		}
		this.spinButton.active = !spinning && !this.menu.betStack().isEmpty();

		int pocket = this.menu.ballPosition() / RouletteRules.SUBSTEPS;
		if (spinning && this.lastPocket != -1 && pocket != this.lastPocket) {
			playSound("block.note_block.hat", 1.6F, 0.5F);
		}
		this.lastPocket = pocket;

		int outcome = this.menu.outcome();
		if (outcome != this.lastOutcome) {
			switch (outcome) {
				case RouletteMenu.OUTCOME_LOSE -> playSound("block.note_block.bass", 0.6F, 1.0F);
				case RouletteMenu.OUTCOME_WIN -> playSound("entity.player.levelup", 1.0F, 1.0F);
				case RouletteMenu.OUTCOME_JACKPOT -> playSound("ui.toast.challenge_complete", 1.0F, 1.0F);
				default -> { }
			}
			if (outcome != RouletteMenu.OUTCOME_NONE) {
				this.outcomeShownUntil = System.currentTimeMillis() + 3000L;
			}
			this.lastOutcome = outcome;
		}

		super.extractRenderState(graphics, mouseX, mouseY, a);

		graphics.item(this.inventoryIcon, this.inventoryTab.getX() + 5, this.inventoryTab.getY() + 3);
		graphics.item(this.rouletteIcon, this.rouletteTab.getX() + 5, this.rouletteTab.getY() + 3);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractBackground(graphics, mouseX, mouseY, a);
		graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.leftPos, this.topPos, 0, 0, 176, 222, 256, 256);

		// Marker next to the chosen color.
		int pick = Math.max(0, Math.min(2, this.menu.pick()));
		int markerY = this.topPos + 26 + pick * 24;
		graphics.fill(this.leftPos + 2, markerY + 3, this.leftPos + 4, markerY + 17, PICK_MARKER_COLORS[pick]);

		// The ball.
		double angle = Math.toRadians(-90.0 + this.menu.ballPosition() * (360.0 / RouletteRules.BALL_POSITIONS));
		int bx = this.leftPos + WHEEL_CENTER_X + (int) Math.round(Math.cos(angle) * BALL_ORBIT);
		int by = this.topPos + WHEEL_CENTER_Y + (int) Math.round(Math.sin(angle) * BALL_ORBIT);
		graphics.fill(bx - 3, by - 2, bx + 3, by + 2, 0xFF151515);
		graphics.fill(bx - 2, by - 3, bx + 2, by + 3, 0xFF151515);
		graphics.fill(bx - 2, by - 2, bx + 2, by + 2, 0xFFF2F2F2);
		graphics.fill(bx - 2, by - 2, bx - 1, by - 1, 0xFFFFFFFF);
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		graphics.text(this.font, this.title.getString(), 8, 6, 0xFF404040, false);
		graphics.text(this.font, "Inventory", 8, 128, 0xFF404040, false);

		String status;
		int color = 0xFF404040;
		int outcome = this.menu.outcome();
		if (this.menu.isSpinning()) {
			status = "Spinning...";
		} else if (outcome != RouletteMenu.OUTCOME_NONE && System.currentTimeMillis() < this.outcomeShownUntil) {
			int landed = RouletteRules.pocketColor(this.menu.ballPosition());
			if (outcome == RouletteMenu.OUTCOME_LOSE) {
				status = RouletteRules.COLOR_NAMES[landed] + " - no luck!";
				color = 0xFFA02020;
			} else if (outcome == RouletteMenu.OUTCOME_JACKPOT) {
				status = "JACKPOT! Check chat";
				color = TIER_TEXT_COLORS[4];
			} else {
				status = RouletteRules.COLOR_NAMES[landed] + " - you won!";
				color = 0xFF2E7D32;
			}
		} else if (this.menu.betStack().isEmpty()) {
			status = "Put an item in the middle";
		} else {
			int tier = RouletteRules.prizeTier(RouletteRules.betTier(this.menu.betStack()), this.menu.pick());
			status = "Win: " + RouletteRules.TIER_NAMES[tier] + " prizes";
			color = TIER_TEXT_COLORS[tier];
		}
		graphics.text(this.font, status, (176 - this.font.width(status)) / 2, 114, color, false);
	}

	private static void playSound(String id, float pitch, float volume) {
		SoundEvent event = SoundEvent.createVariableRangeEvent(Identifier.withDefaultNamespace(id));
		Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(event, pitch, volume));
	}
}
