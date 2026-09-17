package com.roulette;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class ModBlocks {
	public static final SlotMachineBlock SLOT_MACHINE_BASE = register("slot_machine_base");
	public static final SlotMachineBlock SLOT_MACHINE_TOP = register("slot_machine_top");

	private ModBlocks() {
	}

	private static SlotMachineBlock register(String name) {
		ResourceKey<net.minecraft.world.level.block.Block> key = ResourceKey.create(Registries.BLOCK, RouletteMod.id(name));
		BlockBehaviour.Properties properties = BlockBehaviour.Properties.of()
				.setId(key)
				.noOcclusion()
				.noLootTable()
				.strength(-1.0F, 3600000.0F)
				.sound(SoundType.METAL);
		return Registry.register(BuiltInRegistries.BLOCK, RouletteMod.id(name), new SlotMachineBlock(properties));
	}

	/** Forces the blocks above to register during mod initialization. */
	public static void init() {
	}
}
