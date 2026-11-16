package dev.mordo.kairo.addons.plastic;

import dev.mordo.kairo.api.MenuLook;
import dev.mordo.kairo.core.settings.BooleanSetting;
import dev.mordo.kairo.core.settings.EnumSetting;
import dev.mordo.kairo.core.settings.Setting;
import dev.mordo.kairo.core.settings.SliderSetting;
import dev.mordo.kairo.render.Colors;
import dev.mordo.kairo.render.Shapes;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.List;

/**
 * Menus as moulded plastic: every panel, button and ring segment is a solid slab, raised a little off the screen. A
 * slab has a darker side showing underneath (its thickness), a body lit from above (lighter at the top), a soft
 * gloss across its upper half and a thin dark rim. Nothing is see-through and nothing is blurred.
 *
 * <p>Kairo hands every surface a tint: faint for plain panels, strong and in your accent colour for hovered and
 * selected ones. The plastic takes that colour in, so highlights still show.
 */
public final class PlasticLook implements MenuLook {
	public enum Tone implements EnumSetting.Labeled {
		GRAPHITE("Graphite", 0xFF2C2F37),
		STORM("Storm blue", 0xFF263142),
		PLUM("Plum", 0xFF35293F),
		MOSS("Moss", 0xFF27352E),
		CLAY("Clay", 0xFF3B2F2B);

		private final String label;
		final int color;

		Tone(String label, int color) {
			this.label = label;
			this.color = color;
		}

		@Override
		public String label() {
			return label;
		}
	}

	private final EnumSetting<Tone> tone = new EnumSetting<>("plastic_tone", "Plastic colour", Tone.GRAPHITE)
		.description("The colour of the plastic itself. Highlights still use your accent colour");
	private final SliderSetting depth = new SliderSetting("plastic_depth", "Depth", 0.6, 0.0, 1.0, 0.05).percent()
		.description("How thick the slabs look: how much of their side shows underneath");
	private final SliderSetting gloss = new SliderSetting("plastic_gloss", "Gloss", 0.5, 0.0, 1.0, 0.05).percent()
		.description("0% is matte, 100% is shiny");
	private final BooleanSetting rim = new BooleanSetting("plastic_rim", "Dark rim", true)
		.description("A thin dark line round every slab");

	@Override
	public String id() {
		return "plastic";
	}

	@Override
	public String name() {
		return "Plastic";
	}

	@Override
	public List<Setting<?>> settings() {
		return List.of(tone, depth, gloss, rim);
	}

	/** The plastic's colour for a surface with this tint: the tone, pulled towards the tint by how strong the tint is. */
	private int body(int tint) {
		float strength = Colors.alpha(tint) / 255f;
		return Colors.lerp(tone.get().color, 0xFF000000 | (tint & 0xFFFFFF), Math.clamp(strength * 0.78f, 0f, 0.85f));
	}

	/** A coloured edge (your accent on a selected tile) is kept; Kairo's white glass edges are not. */
	private static boolean coloured(int stroke) {
		int r = stroke >> 16 & 0xFF;
		int g = stroke >> 8 & 0xFF;
		int b = stroke & 0xFF;
		return Colors.alpha(stroke) > 40 && Math.max(r, Math.max(g, b)) - Math.min(r, Math.min(g, b)) >= 24;
	}

	@Override
	public void panel(GuiGraphicsExtractor g, float x, float y, float w, float h, float radius, int tint, int stroke, float strokeWidth, float opacity) {
		int body = body(tint);
		int light = Colors.lerp(body, 0xFFFFFFFF, 0.13f + 0.07f * gloss.floatValue());
		int side = Colors.lerp(body, 0xFF000000, 0.55f);
		float small = Math.min(w, h);
		// The slab's thickness: big panels are thick, small buttons and switches thin.
		float drop = depth.floatValue() * Math.clamp(small * 0.085f, 0.8f, 4.5f);
		if (w > 140 && h > 90) {
			// Big panels cast a soft shadow on whatever is behind them.
			Shapes.shadow(g, x, y + drop + 3, w, h, radius, 16, Colors.withAlpha(0xFF000000, 0.38f * opacity));
		}
		if (drop > 0.3f) {
			Shapes.rect(g, x, y + drop, w, h, radius, Colors.multiplyAlpha(side, opacity));
		}
		Shapes.gradientRect(g, x, y, w, h, radius, Colors.multiplyAlpha(light, opacity), Colors.multiplyAlpha(body, opacity));
		if (gloss.floatValue() > 0.02f && small > 7) {
			// The gloss: light caught on the upper half, fading out downwards.
			float sheen = Math.min(h * 0.5f, 28);
			Shapes.gradientRect(g, x + 1, y + 1, w - 2, sheen, Math.max(0, radius - 1), Colors.withAlpha(0xFFFFFFFF, 0.17f * gloss.floatValue() * opacity),
				Colors.withAlpha(0xFFFFFFFF, 0));
		}
		if (coloured(stroke)) {
			Shapes.outline(g, x, y, w, h, radius, Colors.multiplyAlpha(stroke, opacity), Math.max(1, strokeWidth));
		} else if (rim.isOn()) {
			Shapes.outline(g, x, y, w, h, radius, Colors.withAlpha(0xFF000000, 0.42f * opacity), 1);
		}
	}

	@Override
	public void sector(GuiGraphicsExtractor g, float cx, float cy, float innerR, float outerR, float midAngle, float halfAngle, float gap,
					   float cornerRadius, int tint, int stroke, float strokeWidth, float opacity) {
		int body = body(tint);
		int top = Colors.lerp(body, 0xFFFFFFFF, 0.07f + 0.05f * gloss.floatValue());
		int side = Colors.lerp(body, 0xFF000000, 0.55f);
		float drop = depth.floatValue() * 4;
		if (drop > 0.3f) {
			Shapes.sector(g, cx, cy + drop, innerR, outerR, midAngle, halfAngle, gap, cornerRadius, Colors.multiplyAlpha(side, opacity), 0, 0);
		}
		int edge = coloured(stroke) ? Colors.multiplyAlpha(stroke, opacity) : rim.isOn() ? Colors.withAlpha(0xFF000000, 0.42f * opacity) : 0;
		Shapes.sector(g, cx, cy, innerR, outerR, midAngle, halfAngle, gap, cornerRadius, Colors.multiplyAlpha(top, opacity), edge,
			edge == 0 ? 0 : Math.max(1, strokeWidth));
		if (gloss.floatValue() > 0.02f) {
			// The gloss: light caught on the outer edge, fading inwards in thin steps so it reads as a soft gradient
			// (the same shape as the segment, so it has no ends of its own).
			float band = (outerR - innerR) * 0.2f;
			int steps = 5;
			for (int i = 0; i < steps; i++) {
				float from = outerR - band * (i + 1) / steps;
				float to = outerR - band * i / steps;
				float strength = 1 - i / (float) steps;
				Shapes.sector(g, cx, cy, from, to, midAngle, halfAngle, gap, i == 0 ? cornerRadius : 0,
					Colors.withAlpha(0xFFFFFFFF, 0.055f * strength * strength * gloss.floatValue() * opacity), 0, 0);
			}
		}
	}

	@Override
	public void backdrop(GuiGraphicsExtractor g, float opacity, float dim, float vignette) {
		// No blur: the world dims behind a sheet of the same plastic, darker.
		int sheet = Colors.lerp(tone.get().color, 0xFF000000, 0.62f);
		float alpha = Math.clamp(opacity * (0.6f + dim * 0.32f), 0f, 0.95f);
		g.fill(0, 0, g.guiWidth(), g.guiHeight(), Colors.withAlpha(sheet, alpha));
	}
}
