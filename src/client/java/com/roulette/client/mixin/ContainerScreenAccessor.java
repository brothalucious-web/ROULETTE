package com.roulette.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

/** Lets the inventory tabs follow the panel when the recipe book moves it. */
@Mixin(AbstractContainerScreen.class)
public interface ContainerScreenAccessor {
	@Accessor("leftPos")
	int roulette$getLeftPos();

	@Accessor("topPos")
	int roulette$getTopPos();

	@Accessor("imageWidth")
	int roulette$getImageWidth();
}
