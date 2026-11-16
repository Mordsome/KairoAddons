package dev.mordo.kairo.addons.themes;

import dev.mordo.kairo.api.HudLook;
import dev.mordo.kairo.api.MenuLook;
import dev.mordo.kairo.core.settings.EnumSetting;
import dev.mordo.kairo.core.settings.Setting;
import dev.mordo.kairo.render.Colors;
import dev.mordo.kairo.render.Shapes;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.List;

/**
 * Square, pixel-sharp frames in the spirit of Minecraft's own: a tooltip's dark box with a glowing border (purple,
 * or your accent colour), or stone slots with a light top-left and a dark bottom-right that sink in when pressed.
 * Corners are always square; frame lines are whole pixels.
 */
final class RetroTheme implements MenuLook, HudLook {
	private static final String ID = "retro";

	enum Frame implements EnumSetting.Labeled {
		TOOLTIP("Tooltip"),
		STONE("Stone");

		private final String label;

		Frame(String label) {
			this.label = label;
		}

		@Override
		public String label() {
			return label;
		}
	}

	/** The tooltip frame's colour: Minecraft's own blue-purple, your accent colour, or a preset. */
	enum Border implements EnumSetting.Labeled {
		CLASSIC("Classic purple", 0xFF5000FF),
		ACCENT("Your colour", 0),
		GOLD("Gold", 0xFFFFC23A),
		GREEN("Green", 0xFF4CDB5A),
		RED("Red", 0xFFFF4848),
		AQUA("Aqua", 0xFF3FE0E8);

		private final String label;
		final int color;

		Border(String label, int color) {
			this.label = label;
			this.color = color;
		}

		@Override
		public String label() {
			return label;
		}
	}

	private final EnumSetting<Frame> frame = new EnumSetting<>("themes_retro_frame", "Frame", Frame.TOOLTIP)
		.description("Tooltip: dark boxes with a glowing border. Stone: grey raised slots that sink in when picked or pressed")
		.visibleWhen(Themes.inUse(ID));
	private final EnumSetting<Border> border = new EnumSetting<>("themes_retro_border", "Frame colour", Border.CLASSIC)
		.description("The colour of the tooltip frames: Minecraft's own blue-purple, your accent colour, or a preset")
		.visibleWhen(() -> Themes.inUse(ID).getAsBoolean() && frame.get() == Frame.TOOLTIP);

	@Override
	public String id() {
		return ID;
	}

	@Override
	public String name() {
		return "Retro";
	}

	@Override
	public List<Setting<?>> settings() {
		return List.of(frame, border);
	}

	/** The frame's colour at the top (bright); it fades to {@link #bottom} at the bottom, like Minecraft's tooltips. */
	private int top(int accent) {
		int colour = border.get() == Border.ACCENT ? accent | 0xFF000000 : border.get().color;
		return Colors.withAlpha(colour, border.get() == Border.CLASSIC ? 0.75f : 0.85f);
	}

	private int bottom(int accent) {
		int colour = border.get() == Border.ACCENT ? accent | 0xFF000000 : border.get().color;
		return Colors.withAlpha(Colors.lerp(colour, 0xFF000000, 0.5f), border.get() == Border.CLASSIC ? 0.75f : 0.85f);
	}

	// ---- menus ----

	@Override
	public void panel(GuiGraphicsExtractor g, float x, float y, float w, float h, float radius, int tint, int stroke, float strokeWidth, float opacity) {
		boolean coloured = Themes.coloured(tint) || Themes.coloured(stroke);
		int accent = Themes.coloured(stroke) ? stroke : Themes.coloured(tint) ? tint : dev.mordo.kairo.gui.Theme.accent();
		// How much the surface is "on" (hovered or picked): Kairo gives those a stronger tint.
		float on = Math.clamp((Themes.strength(tint) - 0.04f) * 4, 0f, 1f) * (coloured ? 1 : 0.5f);
		float px = pixel(w, h);
		if (frame.get() == Frame.STONE) {
			stone(g, x, y, w, h, px, 0.95f * opacity, accent, on, coloured && Themes.strength(tint) > 0.3f);
		} else {
			tooltip(g, x, y, w, h, px, 0.94f * opacity, top(accent), bottom(accent), accent, on, opacity);
		}
	}

	/** One frame line: one GUI pixel on small controls, two on big panels so the frame still reads. */
	private static float pixel(float w, float h) {
		return w > 140 && h > 90 ? 2 : 1;
	}

	@Override
	public void sector(GuiGraphicsExtractor g, float cx, float cy, float innerR, float outerR, float midAngle, float halfAngle, float gap,
					   float cornerRadius, int tint, int stroke, float strokeWidth, float opacity) {
		boolean coloured = Themes.coloured(tint) || Themes.coloured(stroke);
		int accent = Themes.coloured(stroke) ? stroke : Themes.coloured(tint) ? tint : dev.mordo.kairo.gui.Theme.accent();
		float on = Math.clamp((Themes.strength(tint) - 0.04f) * 4, 0f, 1f) * (coloured ? 1 : 0.5f);
		if (frame.get() == Frame.STONE) {
			int body = Colors.lerp(0xFF3A3A3A, accent | 0xFF000000, 0.35f * on);
			Shapes.sector(g, cx, cy, innerR, outerR, midAngle, halfAngle, gap, 0, Colors.withAlpha(body, 0.95f * opacity),
				Colors.withAlpha(0xFF151515, opacity), 2);
			Shapes.sector(g, cx, cy, outerR - 2, outerR, midAngle, halfAngle, gap, 0, Colors.withAlpha(0xFF6E6E6E, opacity), 0, 0);
		} else {
			int back = Colors.lerp(0xFF100010, accent | 0xFF000000, 0.35f * on);
			Shapes.sector(g, cx, cy, innerR, outerR, midAngle, halfAngle, gap, 0, Colors.withAlpha(back, 0.94f * opacity),
				Colors.multiplyAlpha(Colors.lerp(top(accent), 0xFFFFFFFF, 0.4f * on), opacity), 2);
		}
	}

	@Override
	public void backdrop(GuiGraphicsExtractor g, float opacity, float dim, float vignette) {
		// Like Minecraft's own menus: the world darkened, no blur.
		int top = Colors.withAlpha(0xFF101010, Math.clamp(opacity * (0.74f + dim * 0.35f), 0f, 0.92f));
		int bottom = Colors.withAlpha(0xFF101010, Math.clamp(opacity * (0.84f + dim * 0.3f), 0f, 0.95f));
		Shapes.gradientRect(g, 0, 0, g.guiWidth(), g.guiHeight(), 0, top, bottom);
	}

	// ---- HUD ----

	@Override
	public void box(GuiGraphicsExtractor g, float x, float y, float w, float h, float radius, int fill, int accent, float seed, float pressed) {
		// Retro boxes are nearly solid, like the tooltips and slots they copy; your background opacity still counts a little.
		float strength = 0.78f + 0.22f * Colors.alpha(fill) / 255f;
		if (frame.get() == Frame.STONE) {
			stone(g, x, y, w, h, 1, strength, accent, pressed, pressed > 0.5f);
		} else {
			tooltip(g, x, y, w, h, 1, 0.94f * strength, top(accent), bottom(accent), accent, pressed, 1);
		}
	}

	/** Minecraft's tooltip shape: a box with its corner pixels cut, and a frame line just inside it. */
	private static void tooltip(GuiGraphicsExtractor g, float x, float y, float w, float h, float px, float alpha, int top, int bottom, int accent,
								float on, float opacity) {
		int back = Colors.withAlpha(Colors.lerp(0xFF100010, accent | 0xFF000000, 0.4f * on), alpha);
		Shapes.rect(g, x + px, y, w - 2 * px, h, 0, back);
		Shapes.rect(g, x, y + px, px, h - 2 * px, 0, back);
		Shapes.rect(g, x + w - px, y + px, px, h - 2 * px, 0, back);
		int lit = Colors.multiplyAlpha(Colors.lerp(top, Colors.withAlpha(0xFFFFFFFF, Colors.alpha(top) / 255f), 0.5f * on), opacity);
		int low = Colors.multiplyAlpha(bottom, opacity);
		Shapes.rect(g, x + px, y + px, w - 2 * px, px, 0, lit);
		Shapes.rect(g, x + px, y + h - 2 * px, w - 2 * px, px, 0, low);
		Shapes.gradientRect(g, x + px, y + 2 * px, px, h - 4 * px, 0, lit, low);
		Shapes.gradientRect(g, x + w - 2 * px, y + 2 * px, px, h - 4 * px, 0, lit, low);
	}

	/** A stone slot: raised (light top-left, dark bottom-right), sunk in while {@code down}. */
	private static void stone(GuiGraphicsExtractor g, float x, float y, float w, float h, float px, float alpha, int accent, float on, boolean down) {
		int body = Colors.withAlpha(Colors.lerp(0xFF3A3A3A, accent | 0xFF000000, 0.3f * on), alpha);
		int light = Colors.withAlpha(0xFF6E6E6E, alpha);
		int dark = Colors.withAlpha(0xFF151515, alpha);
		Shapes.rect(g, x, y, w, h, 0, body);
		int topLeft = down ? dark : light;
		int bottomRight = down ? light : dark;
		Shapes.rect(g, x, y, w - px, px, 0, topLeft);
		Shapes.rect(g, x, y + px, px, h - 2 * px, 0, topLeft);
		Shapes.rect(g, x + px, y + h - px, w - px, px, 0, bottomRight);
		Shapes.rect(g, x + w - px, y + px, px, h - 2 * px, 0, bottomRight);
	}
}
