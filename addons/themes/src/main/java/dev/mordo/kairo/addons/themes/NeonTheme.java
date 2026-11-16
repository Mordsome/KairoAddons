package dev.mordo.kairo.addons.themes;

import dev.mordo.kairo.api.HudLook;
import dev.mordo.kairo.api.MenuLook;
import dev.mordo.kairo.core.settings.BooleanSetting;
import dev.mordo.kairo.core.settings.EnumSetting;
import dev.mordo.kairo.core.settings.Setting;
import dev.mordo.kairo.core.settings.SliderSetting;
import dev.mordo.kairo.render.Animation;
import dev.mordo.kairo.render.Colors;
import dev.mordo.kairo.render.Shapes;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.List;

/**
 * Dark surfaces with bright tubes for edges and a glow around them. Big panels keep a dim tube; hovered and picked
 * controls light up. The glow can breathe slowly (never with Reduce motion, and it never flickers or flashes).
 */
final class NeonTheme implements MenuLook, HudLook {
	private static final String ID = "neon";
	private static final int DARK = 0xFF0B0A12;

	enum Tube implements EnumSetting.Labeled {
		ACCENT("Your colour", 0),
		PINK("Pink", 0xFFFF3BD0),
		CYAN("Cyan", 0xFF2EF2FF),
		LIME("Lime", 0xFF9DFF3A),
		ORANGE("Orange", 0xFFFF9A2E);

		private final String label;
		final int color;

		Tube(String label, int color) {
			this.label = label;
			this.color = color;
		}

		@Override
		public String label() {
			return label;
		}
	}

	private final EnumSetting<Tube> tube = new EnumSetting<>("themes_neon_colour", "Neon colour", Tube.ACCENT)
		.description("The colour of the glowing edges")
		.visibleWhen(Themes.inUse(ID));
	private final SliderSetting glow = new SliderSetting("themes_neon_glow", "Glow", 0.6, 0.0, 1.0, 0.05).percent()
		.description("How far and how strongly the edges glow")
		.visibleWhen(Themes.inUse(ID));
	private final BooleanSetting breathe = new BooleanSetting("themes_neon_breathe", "Breathe", true)
		.description("The glow slowly brightens and dims (off with Reduce motion)")
		.visibleWhen(Themes.inUse(ID));

	@Override
	public String id() {
		return ID;
	}

	@Override
	public String name() {
		return "Neon";
	}

	@Override
	public List<Setting<?>> settings() {
		return List.of(tube, glow, breathe);
	}

	private int colour(int accent) {
		return (tube.get() == Tube.ACCENT ? accent : tube.get().color) | 0xFF000000;
	}

	private float pulse(float seed) {
		if (!breathe.isOn() || Animation.reduceMotion()) {
			return 1;
		}
		return 0.8f + 0.2f * (float) Math.sin(System.nanoTime() / 1e9 * 1.6 + seed * 2.1);
	}

	// ---- menus ----

	@Override
	public void panel(GuiGraphicsExtractor g, float x, float y, float w, float h, float radius, int tint, int stroke, float strokeWidth, float opacity) {
		int accent = Themes.coloured(stroke) ? stroke : Themes.coloured(tint) ? tint : dev.mordo.kairo.gui.Theme.accent();
		int neon = colour(accent);
		boolean big = w > 140 && h > 90;
		// How lit the surface is: hovered and picked controls carry a stronger (or coloured) tint.
		float lit = Math.clamp((Themes.strength(tint) - 0.04f) * 3 + (Themes.coloured(tint) || Themes.coloured(stroke) ? 0.5f : 0), 0f, 1f);
		float g0 = glow.floatValue();
		float p = pulse(x * 0.01f + y * 0.013f);
		if (g0 > 0 && (lit > 0.05f || big)) {
			float soft = big ? 6 + 10 * g0 : 2 + 5 * g0;
			float strength = big ? 0.18f + 0.2f * g0 : (0.15f + 0.4f * g0) * lit;
			Shapes.shadow(g, x, y, w, h, radius, soft, Colors.withAlpha(neon, Math.min(1, strength * p) * opacity));
		}
		int body = Colors.lerp(Themes.body(DARK, tint, 0.25f), neon, 0.18f * lit);
		float alpha = big ? 0.9f : Math.clamp(0.75f + Themes.strength(tint), 0.75f, 0.96f);
		Shapes.rect(g, x, y, w, h, radius, Colors.withAlpha(body, alpha * opacity));
		float tubeAlpha = big ? 0.55f : 0.3f + 0.65f * lit;
		Shapes.outline(g, x, y, w, h, radius, Colors.withAlpha(neon, tubeAlpha * opacity), big ? 1.4f : 1.1f);
		if (lit > 0.05f && w > 4 && h > 4) {
			Shapes.outline(g, x + 1, y + 1, w - 2, h - 2, Math.max(0, radius - 1), Colors.withAlpha(Colors.lerp(neon, 0xFFFFFFFF, 0.65f), 0.4f * lit * opacity), 0.5f);
		}
	}

	@Override
	public void sector(GuiGraphicsExtractor g, float cx, float cy, float innerR, float outerR, float midAngle, float halfAngle, float gap,
					   float cornerRadius, int tint, int stroke, float strokeWidth, float opacity) {
		int accent = Themes.coloured(stroke) ? stroke : Themes.coloured(tint) ? tint : dev.mordo.kairo.gui.Theme.accent();
		int neon = colour(accent);
		float lit = Math.clamp((Themes.strength(tint) - 0.04f) * 3 + (Themes.coloured(tint) || Themes.coloured(stroke) ? 0.5f : 0), 0f, 1f);
		float p = pulse(midAngle);
		float g0 = glow.floatValue();
		if (g0 > 0) {
			// The glow: wide, faint copies of the tube.
			for (int i = 3; i >= 1; i--) {
				Shapes.sector(g, cx, cy, innerR, outerR, midAngle, halfAngle, gap, cornerRadius, 0,
					Colors.withAlpha(neon, (0.05f + 0.1f * lit) * g0 * p * opacity), 1.5f + i * 2.2f * g0);
			}
		}
		int body = Colors.lerp(Themes.body(DARK, tint, 0.25f), neon, 0.2f * lit);
		Shapes.sector(g, cx, cy, innerR, outerR, midAngle, halfAngle, gap, cornerRadius, Colors.withAlpha(body, 0.9f * opacity),
			Colors.withAlpha(neon, (0.45f + 0.5f * lit) * opacity), 1.2f);
	}

	@Override
	public void backdrop(GuiGraphicsExtractor g, float opacity, float dim, float vignette) {
		int w = g.guiWidth();
		int h = g.guiHeight();
		g.fill(0, 0, w, h, Colors.withAlpha(0xFF06050B, Math.clamp(opacity * (0.78f + dim * 0.3f), 0f, 0.94f)));
		// A faint glow of the neon colour rising from the bottom edge.
		int neon = colour(dev.mordo.kairo.gui.Theme.accent());
		Shapes.gradientRect(g, 0, h * 0.7f, w, h * 0.3f, 0, Colors.withAlpha(neon, 0), Colors.withAlpha(neon, 0.12f * glow.floatValue() * opacity));
	}

	// ---- HUD ----

	@Override
	public void box(GuiGraphicsExtractor g, float x, float y, float w, float h, float radius, int fill, int accent, float seed, float pressed) {
		int neon = colour(accent);
		float strength = 0.7f + 0.3f * Colors.alpha(fill) / 255f;
		float p = pulse(seed);
		float g0 = glow.floatValue();
		if (g0 > 0) {
			float soft = 2 + 6 * g0;
			Shapes.shadow(g, x, y, w, h, radius, soft, Colors.withAlpha(neon, Math.min(1, (0.25f + 0.4f * g0) * p + 0.35f * pressed)));
		}
		int body = Colors.withAlpha(Colors.lerp(Colors.lerp(fill | 0xFF000000, 0xFF000000, 0.72f), neon, 0.35f * pressed), 0.92f * strength);
		Shapes.rect(g, x, y, w, h, radius, body);
		Shapes.outline(g, x, y, w, h, radius, Colors.withAlpha(neon, 0.95f), 1.1f);
		if (w > 3 && h > 3) {
			Shapes.outline(g, x + 0.9f, y + 0.9f, w - 1.8f, h - 1.8f, Math.max(0, radius - 0.9f),
				Colors.withAlpha(Colors.lerp(neon, 0xFFFFFFFF, 0.65f), 0.35f + 0.3f * pressed), 0.45f);
		}
	}
}
