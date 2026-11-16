package dev.mordo.kairo.addons.k98;

import dev.mordo.kairo.api.AddonRegistrar;
import dev.mordo.kairo.api.KairoAddon;
import dev.mordo.kairo.gui.Icon;

/**
 * Kairo addon "Kairo 98": a late-90s desktop look for all of Kairo and Minecraft's menus ({@link Kairo98Theme}, which
 * is both the menu look and the HUD look, so it shows as one theme), a taskbar with a Start menu in place of the
 * ring ({@link StartMenu}), retro animations, and a tab of its own in Visuals with every option.
 */
public final class Kairo98 implements KairoAddon {
	private static Kairo98Theme theme;

	@Override
	public void register(AddonRegistrar kairo) {
		theme = new Kairo98Theme();
		kairo.menuLook(theme);
		kairo.hudLook(theme);
		kairo.homeMenu(new StartMenu(theme));
		kairo.visualsTab("Kairo 98", Icon.GRID, theme.tabSettings());
	}

	/**
	 * A swing or switch progress (0 to 1) in a few hard steps while Kairo 98's retro animations are on; unchanged
	 * otherwise. Used by the hand renderer.
	 */
	public static float choppy(float progress) {
		Kairo98Theme t = theme;
		if (t == null || !t.retroAnimations.isOn() || !Kairo98Theme.inUse() || progress <= 0 || progress >= 1) {
			return progress;
		}
		int frames = t.animationFrames.get().intValue();
		return (float) Math.floor(progress * frames) / frames;
	}
}
