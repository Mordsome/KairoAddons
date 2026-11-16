package dev.mordo.kairo.addons.themes;

import dev.mordo.kairo.api.HudLook;
import dev.mordo.kairo.api.MenuLook;
import dev.mordo.kairo.core.settings.EnumSetting;
import dev.mordo.kairo.core.settings.Setting;
import dev.mordo.kairo.core.settings.SliderSetting;
import dev.mordo.kairo.render.Colors;
import dev.mordo.kairo.render.Shapes;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.List;

/**
 * Almost nothing: flat dark panels with small corners and a hairline edge, no blur, and a thin line in your accent
 * colour on whatever is hovered or picked (and on every HUD box). Quiet and sharp.
 */
final class MinimalTheme implements MenuLook, HudLook {
	private static final String ID = "minimal";
	private static final int BASE = 0xFF121418;

	enum Line implements EnumSetting.Labeled {
		LEFT("Left"),
		UNDER("Under"),
		NONE("None");

		private final String label;

		Line(String label) {
			this.label = label;
		}

		@Override
		public String label() {
			return label;
		}
	}

	private final EnumSetting<Line> line = new EnumSetting<>("themes_minimal_line", "Accent line", Line.LEFT)
		.description("Where the line in your accent colour goes, on picked controls and HUD boxes")
		.visibleWhen(Themes.inUse(ID));
	private final SliderSetting faint = new SliderSetting("themes_minimal_fill", "HUD box", 0.45, 0.0, 1.0, 0.05).percent()
		.description("How much of your HUD background colour shows: 0% leaves only the line")
		.visibleWhen(Themes.inUse(ID));

	@Override
	public String id() {
		return ID;
	}

	@Override
	public String name() {
		return "Minimal";
	}

	@Override
	public List<Setting<?>> settings() {
		return List.of(line, faint);
	}

	// ---- menus ----

	@Override
	public void panel(GuiGraphicsExtractor g, float x, float y, float w, float h, float radius, int tint, int stroke, float strokeWidth, float opacity) {
		float r = Math.min(radius, 4);
		boolean big = w > 140 && h > 90;
		int body = Themes.body(BASE, tint, 0.55f);
		float alpha = big ? 0.86f : Math.clamp(0.55f + Themes.strength(tint) * 1.5f, 0.55f, 0.95f);
		int edge = Themes.coloured(stroke) ? Colors.withAlpha(stroke, 0.6f) : Colors.withAlpha(0xFFFFFFFF, 0.07f);
		Shapes.rect(g, x, y, w, h, r, Colors.withAlpha(body, alpha * opacity), Colors.multiplyAlpha(edge, opacity), 1);
		if (!big && (Themes.coloured(tint) || Themes.coloured(stroke))) {
			int accent = Themes.coloured(stroke) ? stroke : tint;
			accentLine(g, x, y, w, h, Colors.withAlpha(accent | 0xFF000000, opacity));
		}
	}

	@Override
	public void sector(GuiGraphicsExtractor g, float cx, float cy, float innerR, float outerR, float midAngle, float halfAngle, float gap,
					   float cornerRadius, int tint, int stroke, float strokeWidth, float opacity) {
		int body = Themes.body(BASE, tint, 0.55f);
		float alpha = Math.clamp(0.7f + Themes.strength(tint), 0.7f, 0.95f);
		Shapes.sector(g, cx, cy, innerR, outerR, midAngle, halfAngle, gap, Math.min(cornerRadius, 3), Colors.withAlpha(body, alpha * opacity),
			Colors.withAlpha(0xFFFFFFFF, 0.07f * opacity), 1);
		if (Themes.coloured(tint) || Themes.coloured(stroke)) {
			int accent = (Themes.coloured(stroke) ? stroke : tint) | 0xFF000000;
			// The accent line, on the outer edge of a picked segment.
			Shapes.sector(g, cx, cy, outerR - 2, outerR, midAngle, halfAngle, gap, 0, Colors.withAlpha(accent, opacity), 0, 0);
		}
	}

	@Override
	public void backdrop(GuiGraphicsExtractor g, float opacity, float dim, float vignette) {
		// No blur: a flat dark sheet over the world.
		g.fill(0, 0, g.guiWidth(), g.guiHeight(), Colors.withAlpha(0xFF0A0B0D, Math.clamp(opacity * (0.74f + dim * 0.4f), 0f, 0.94f)));
	}

	private void accentLine(GuiGraphicsExtractor g, float x, float y, float w, float h, int colour) {
		switch (line.get()) {
			case LEFT -> {
				float inset = Math.min(3, h * 0.2f);
				Shapes.rect(g, x, y + inset, 2, h - inset * 2, 1, colour);
			}
			case UNDER -> {
				float inset = Math.min(4, w * 0.15f);
				Shapes.rect(g, x + inset, y + h - 1.5f, w - inset * 2, 1.5f, 0.75f, colour);
			}
			case NONE -> {
			}
		}
	}

	// ---- HUD ----

	@Override
	public void box(GuiGraphicsExtractor g, float x, float y, float w, float h, float radius, int fill, int accent, float seed, float pressed) {
		float r = Math.min(radius, 2);
		int body = Colors.multiplyAlpha(fill, faint.floatValue() + (1 - faint.floatValue()) * pressed);
		if (Colors.alpha(body) > 0) {
			Shapes.rect(g, x, y, w, h, r, body);
		}
		if (pressed > 0) {
			Shapes.rect(g, x, y, w, h, r, Colors.withAlpha(accent, 0.18f * pressed));
		}
		int colour = Colors.withAlpha(accent, 0.75f + 0.25f * pressed);
		switch (line.get()) {
			case LEFT -> {
				float inset = Math.min(2, h * 0.2f);
				Shapes.rect(g, x, y + inset, 1.5f, h - inset * 2, 0.75f, colour);
			}
			case UNDER -> {
				float inset = Math.min(2, w * 0.15f);
				Shapes.rect(g, x + inset, y + h - 1.2f, w - inset * 2, 1.2f, 0.6f, colour);
			}
			case NONE -> {
			}
		}
	}
}
