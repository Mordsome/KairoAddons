package dev.mordo.kairo.addons.k98.mixin;

import dev.mordo.kairo.addons.k98.Kairo98;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Kairo 98's retro animations: the first-person swing (hitting and mining) and the item coming up after a switch
 * move in a few hard steps, like an old game at a low frame rate. Only changes the numbers the hand is drawn with.
 */
@Mixin(FirstPersonHandsAndItemsRenderer.class)
abstract class ChoppySwingMixin {
	/** How far through its swing the arm is (0 to 1). */
	@ModifyVariable(method = "submitArmWithItem", at = @At("HEAD"), argsOnly = true, ordinal = 2)
	private float kairo98$swing(float swing) {
		return Kairo98.choppy(swing);
	}

	/** How far the held item is lowered while switching (0 to 1). */
	@ModifyVariable(method = "submitArmWithItem", at = @At("HEAD"), argsOnly = true, ordinal = 3)
	private float kairo98$equip(float equip) {
		return Kairo98.choppy(equip);
	}
}
