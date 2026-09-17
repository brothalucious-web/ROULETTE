package com.roulette;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import com.roulette.network.ExpandDomainPayload;
import com.roulette.network.OpenPlinkoPayload;
import com.roulette.network.OpenRoulettePayload;
import com.roulette.network.RouletteActionPayload;

public class RouletteMod implements ModInitializer {
	public static final String MOD_ID = "roulette";

	public static final MenuType<RouletteMenu> ROULETTE_MENU = new MenuType<>(RouletteMenu::new, FeatureFlags.VANILLA_SET);
	public static final MenuType<PlinkoMenu> PLINKO_MENU = new MenuType<>(PlinkoMenu::new, FeatureFlags.VANILLA_SET);
	public static final MenuType<SlotMachineMenu> SLOT_MACHINE_MENU = new MenuType<>(SlotMachineMenu::new, FeatureFlags.VANILLA_SET);

	@Override
	public void onInitialize() {
		ModBlocks.init();
		Registry.register(BuiltInRegistries.MENU, id("roulette"), ROULETTE_MENU);
		Registry.register(BuiltInRegistries.MENU, id("plinko"), PLINKO_MENU);
		Registry.register(BuiltInRegistries.MENU, id("slot_machine"), SLOT_MACHINE_MENU);

		PayloadTypeRegistry.serverboundPlay().register(OpenRoulettePayload.TYPE, OpenRoulettePayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(OpenPlinkoPayload.TYPE, OpenPlinkoPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(RouletteActionPayload.TYPE, RouletteActionPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(ExpandDomainPayload.TYPE, ExpandDomainPayload.CODEC);

		// Tabs ask the server to open a game.
		ServerPlayNetworking.registerGlobalReceiver(OpenRoulettePayload.TYPE, (payload, context) -> openRoulette(context.player()));
		ServerPlayNetworking.registerGlobalReceiver(OpenPlinkoPayload.TYPE, (payload, context) -> openPlinko(context.player()));

		// Domain Expansion key.
		ServerPlayNetworking.registerGlobalReceiver(ExpandDomainPayload.TYPE, (payload, context) -> DomainManager.expand(context.player()));

		// Right-clicking a slot machine inside a domain.
		UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
			if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer
					&& DomainManager.isMachine(level, hitResult.getBlockPos())) {
				openSlotMachine(serverPlayer, hitResult.getBlockPos().immutable());
				return InteractionResult.SUCCESS;
			}
			return InteractionResult.PASS;
		});

		// Domain walls and machines can't be broken.
		PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> !DomainManager.isDomainBlock(level, pos));

		// Put the world back if the server shuts down mid-domain.
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> DomainManager.collapseAll());

		// Button presses inside a game.
		ServerPlayNetworking.registerGlobalReceiver(RouletteActionPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			if (player.containerMenu instanceof GameMenu menu) {
				menu.handleAction(player, payload.action());
			}
		});

		// Drive the animations for everyone who has a game open.
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			DomainManager.tick();
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				if (player.containerMenu instanceof GameMenu menu) {
					menu.serverTick(player);
				}
			}
		});
	}

	public static void openRoulette(ServerPlayer player) {
		if (player.isSpectator() || player.containerMenu instanceof RouletteMenu) {
			return;
		}
		player.openMenu(new SimpleMenuProvider(
				(containerId, inventory, p) -> new RouletteMenu(containerId, inventory, new SimpleContainer(1), new SimpleContainerData(RouletteMenu.DATA_COUNT)),
				Component.literal("Roulette")));
	}

	public static void openPlinko(ServerPlayer player) {
		if (player.isSpectator() || player.containerMenu instanceof PlinkoMenu) {
			return;
		}
		player.openMenu(new SimpleMenuProvider(
				(containerId, inventory, p) -> new PlinkoMenu(containerId, inventory, new SimpleContainer(1), new SimpleContainerData(PlinkoMenu.DATA_COUNT)),
				Component.literal("Plinko")));
	}

	public static void openSlotMachine(ServerPlayer player, BlockPos machinePos) {
		if (player.isSpectator() || player.containerMenu instanceof SlotMachineMenu) {
			return;
		}
		player.openMenu(new SimpleMenuProvider(
				(containerId, inventory, p) -> new SlotMachineMenu(containerId, inventory, new SimpleContainer(1),
						new SimpleContainerData(SlotMachineMenu.DATA_COUNT), machinePos),
				Component.literal("Slot Machine")));
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
