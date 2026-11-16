package dev.mordo.kairo.addons.themes;

import dev.mordo.kairo.api.AddonRegistrar;
import dev.mordo.kairo.api.KairoAddon;
import dev.mordo.kairo.core.ClientSettings;
import dev.mordo.kairo.render.Colors;

import java.util.function.BooleanSupplier;

/**
 * Kairo addon "Themes": four themes, each one class that is both a menu look and a HUD look with the same id, so
 * Kairo shows it as one theme (menus, ring, controls and HUD together).
 */
public final class Themes implements KairoAddon {
	@Override
	public void register(AddonRegistrar kairo) {
		for (var theme : new Object[]{new GlassTheme(), new MinimalTheme(), new RetroTheme(), new NeonTheme()}) {
			kairo.menuLook((dev.mordo.kairo.api.MenuLook) theme);
			kairo.hudLook((dev.mordo.kairo.api.HudLook) theme);
		}
	}

	/** A theme's options only show while it is in use, on the menus or on the HUD. */
	static BooleanSupplier inUse(String id) {
		return () -> ClientSettings.INSTANCE.menuLook.get().equals(id) || ClientSettings.INSTANCE.hudLook.get().equals(id);
	}

	/** How strongly a surface's tint should show, 0 to 1 (Kairo: faint for plain panels, strong for picked ones). */
	static float strength(int tint) {
		return Colors.alpha(tint) / 255f;
	}

	/** True for a tint or edge in a real colour (your accent on a hovered or picked control), not Kairo's white or grey. */
	static boolean coloured(int argb) {
		int r = argb >> 16 & 0xFF;
		int g = argb >> 8 & 0xFF;
		int b = argb & 0xFF;
		return Colors.alpha(argb) > 30 && Math.max(r, Math.max(g, b)) - Math.min(r, Math.min(g, b)) >= 24;
	}

	/** {@code base}, pulled towards the tint's own colour as far as the tint is strong (a coloured tint pulls more). */
	static int body(int base, int tint, float pull) {
		float s = strength(tint) * (coloured(tint) ? 1.2f : 0.6f);
		return Colors.lerp(base | 0xFF000000, tint | 0xFF000000, Math.clamp(s * pull, 0f, 0.9f));
	}
}
