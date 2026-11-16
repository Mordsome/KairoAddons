package dev.mordo.kairo.addons.themes;

import dev.mordo.kairo.api.HudLook;
import dev.mordo.kairo.api.MenuLook;
import dev.mordo.kairo.core.settings.BooleanSetting;
import dev.mordo.kairo.core.settings.Setting;
import dev.mordo.kairo.core.settings.SliderSetting;
import dev.mordo.kairo.render.Colors;
import dev.mordo.kairo.render.Shapes;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.List;

/**
 * Bright frosted glass: Kairo's own blurred glass, frosted whiter, with a thin bright edge, a line of light along the
 * top of every surface and a soft shadow under it. The HUD gets the same frost without the blur (HUD boxes are drawn
 * over the game every frame).
 */
final class GlassTheme implements MenuLook, HudLook {
	private static final String ID = "glass";

	private final SliderSetting frost = new SliderSetting("themes_glass_frost", "Frost", 0.6, 0.0, 1.0, 0.05).percent()
		.description("How white and bright the glass is: its tint, edge and highlight")
		.visibleWhen(Themes.inUse(ID));
	private final BooleanSetting shadow = new BooleanSetting("themes_glass_shadow", "Shadow", true)
		.description("A soft shadow under panels and HUD boxes")
		.visibleWhen(Themes.inUse(ID));

	@Override
	public String id() {
		return ID;
	}

	@Override
	public String name() {
		return "Glass";
	}

	@Override
	public List<Setting<?>> settings() {
		return List.of(frost, shadow);
	}

	// ---- menus ----

	@Override
	public void panel(GuiGraphicsExtractor g, float x, float y, float w, float h, float radius, int tint, int stroke, float strokeWidth, float opacity) {
		float f = frost.floatValue();
		if (shadow.isOn() && w > 140 && h > 90) {
			Shapes.shadow(g, x, y + 4, w, h, radius, 18, Colors.withAlpha(0xFF000000, 0.3f * opacity));
		}
		int edge = Themes.coloured(stroke) ? stroke : Colors.withAlpha(0xFFFFFFFF, 0.16f + 0.24f * f);
		if (w > 400 && h > 240) {
			// The big panels (a whole menu): Kairo's blurred glass, frosted brighter.
			boolean coloured = Themes.coloured(tint);
			float a = Math.min(1, Themes.strength(tint) * (1 + 0.5f * f) + (coloured ? 0.05f : 0.04f * f));
			int frosted = Colors.withAlpha(coloured ? tint : Colors.lerp(tint | 0xFF000000, 0xFFFFFFFF, 0.22f), a);
			Shapes.kairoGlass(g, x, y, w, h, radius, frosted, edge, Math.max(0.8f, strokeWidth), opacity);
		} else {
			// Cards and controls: a frosted pane, no blur (blur would take on the colours of whatever is behind, like grass).
			float s = Themes.strength(tint);
			boolean accent = Themes.coloured(tint) && s > 0.25f;
			int base = accent ? tint | 0xFF000000 : 0xFFFFFFFF;
			float a = accent ? Math.min(1, s + 0.1f) : Math.min(0.6f, 0.08f + 0.08f * f + s * 1.6f);
			Shapes.gradientRect(g, x, y, w, h, radius, Colors.withAlpha(Colors.lerp(base, 0xFFFFFFFF, 0.2f), a * opacity),
				Colors.withAlpha(base, a * 0.6f * opacity));
			Shapes.outline(g, x, y, w, h, radius, Colors.multiplyAlpha(edge, opacity), Math.max(0.8f, strokeWidth));
		}
		highlight(g, x, y, w, h, radius, f * opacity);
	}

	@Override
	public void sector(GuiGraphicsExtractor g, float cx, float cy, float innerR, float outerR, float midAngle, float halfAngle, float gap,
					   float cornerRadius, int tint, int stroke, float strokeWidth, float opacity) {
		float f = frost.floatValue();
		boolean coloured = Themes.coloured(tint);
		float a = Math.min(1, Themes.strength(tint) * (1 + 0.5f * f) + (coloured ? 0.05f : 0.04f * f));
		int frosted = Colors.withAlpha(coloured ? tint : Colors.lerp(tint | 0xFF000000, 0xFFFFFFFF, 0.22f), a);
		int edge = Themes.coloured(stroke) ? stroke : Colors.withAlpha(0xFFFFFFFF, 0.16f + 0.24f * f);
		Shapes.kairoGlassSector(g, cx, cy, innerR, outerR, midAngle, halfAngle, gap, cornerRadius, frosted, edge, Math.max(0.8f, strokeWidth), opacity);
		// Light caught along the outer rim.
		Shapes.sector(g, cx, cy, outerR - 1.5f, outerR, midAngle, halfAngle, gap, cornerRadius, Colors.withAlpha(0xFFFFFFFF, 0.3f * f * opacity), 0, 0);
	}

	@Override
	public void backdrop(GuiGraphicsExtractor g, float opacity, float dim, float vignette) {
		// Kairo's own blurred world, a touch less dim so the glass reads brighter.
		Shapes.kairoBackdrop(g, opacity, dim * (1 - 0.3f * frost.floatValue()), vignette);
	}

	private static void highlight(GuiGraphicsExtractor g, float x, float y, float w, float h, float radius, float strength) {
		float inset = Math.max(2, radius * 0.8f);
		if (h > 6 && w > inset * 2 + 2 && strength > 0.01f) {
			Shapes.rect(g, x + inset, y + 0.6f, w - inset * 2, 0.8f, 0.4f, Colors.withAlpha(0xFFFFFFFF, 0.45f * strength));
		}
	}

	// ---- HUD ----

	@Override
	public void box(GuiGraphicsExtractor g, float x, float y, float w, float h, float radius, int fill, int accent, float seed, float pressed) {
		float strength = Colors.alpha(fill) / 255f;
		float f = frost.floatValue();
		if (shadow.isOn()) {
			Shapes.shadow(g, x, y + 1, w, h, radius, 4, Colors.argb(0.28f * Math.max(0.35f, strength), 0));
		}
		int opaque = fill | 0xFF000000;
		int top = Colors.withAlpha(Colors.lerp(opaque, 0xFFFFFFFF, 0.22f * f), Math.min(1, strength * 0.9f + 0.08f));
		int bottom = Colors.withAlpha(Colors.lerp(opaque, 0xFF000000, 0.15f), strength * 0.85f);
		Shapes.gradientRect(g, x, y, w, h, radius, top, bottom);
		if (pressed > 0) {
			Shapes.rect(g, x, y, w, h, radius, Colors.withAlpha(accent, 0.3f * pressed));
		}
		Shapes.outline(g, x, y, w, h, radius, Colors.withAlpha(0xFFFFFFFF, 0.1f + 0.22f * f + 0.3f * pressed), 0.6f);
		float inset = Math.max(1.5f, radius * 0.7f);
		if (w > inset * 2 + 1) {
			Shapes.rect(g, x + inset, y + 0.5f, w - inset * 2, 0.6f, 0.3f, Colors.withAlpha(0xFFFFFFFF, 0.4f * f));
		}
	}
}
