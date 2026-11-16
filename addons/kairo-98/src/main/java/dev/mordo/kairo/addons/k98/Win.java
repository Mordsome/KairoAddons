package dev.mordo.kairo.addons.k98;

import dev.mordo.kairo.render.Colors;
import dev.mordo.kairo.render.Shapes;
import dev.mordo.kairo.render.font.Fonts;
import dev.mordo.kairo.render.font.Typeface;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Drawing the period's pieces: raised and sunken bevels, title bars, caption buttons. {@code px} is one edge line. */
final class Win {
	private Win() {
	}

	static int a(int argb, float opacity) {
		return Colors.multiplyAlpha(argb, opacity);
	}

	/** Two-tone edge: {@code tl} along the top and left, {@code br} along the bottom and right. */
	static void edge(GuiGraphicsExtractor g, float x, float y, float w, float h, float px, int tl, int br, float opacity) {
		Shapes.rect(g, x, y, w - px, px, 0, a(tl, opacity));
		Shapes.rect(g, x, y + px, px, h - 2 * px, 0, a(tl, opacity));
		Shapes.rect(g, x, y + h - px, w, px, 0, a(br, opacity));
		Shapes.rect(g, x + w - px, y, px, h - px, 0, a(br, opacity));
	}

	/** Something raised: a window, a button. */
	static void raised(GuiGraphicsExtractor g, Scheme s, float x, float y, float w, float h, float px, int face, float opacity) {
		Shapes.rect(g, x, y, w, h, 0, a(face, opacity));
		edge(g, x, y, w, h, px, s.light, s.dark, opacity);
		if (w > 4 * px && h > 4 * px) {
			edge(g, x + px, y + px, w - 2 * px, h - 2 * px, px, s.light2, s.shadow, opacity);
		}
	}

	/** Something pushed in: a pressed button, a field, a slot. */
	static void sunken(GuiGraphicsExtractor g, Scheme s, float x, float y, float w, float h, float px, int fill, float opacity) {
		Shapes.rect(g, x, y, w, h, 0, a(fill, opacity));
		edge(g, x, y, w, h, px, s.shadow, s.light, opacity);
		if (w > 4 * px && h > 4 * px) {
			edge(g, x + px, y + px, w - 2 * px, h - 2 * px, px, s.dark, s.light2, opacity);
		}
	}

	/** A pressed button: dark outer line, shadow inside, the face moved a little. */
	static void pressed(GuiGraphicsExtractor g, Scheme s, float x, float y, float w, float h, float px, int face, float opacity) {
		Shapes.rect(g, x, y, w, h, 0, a(face, opacity));
		edge(g, x, y, w, h, px, s.dark, s.light, opacity);
		if (w > 4 * px && h > 4 * px) {
			edge(g, x + px, y + px, w - 2 * px, h - 2 * px, px, s.shadow, s.face, opacity);
		}
	}

	/** The dotted rectangle round whatever has the keyboard. */
	static void focus(GuiGraphicsExtractor g, float x, float y, float w, float h, int colour, float opacity) {
		float step = 2;
		for (float i = 0; i < w; i += step * 2) {
			Shapes.rect(g, x + i, y, Math.min(step, w - i), 1, 0, a(colour, opacity));
			Shapes.rect(g, x + i, y + h - 1, Math.min(step, w - i), 1, 0, a(colour, opacity));
		}
		for (float i = 0; i < h; i += step * 2) {
			Shapes.rect(g, x, y + i, 1, Math.min(step, h - i), 0, a(colour, opacity));
			Shapes.rect(g, x + w - 1, y + i, 1, Math.min(step, h - i), 0, a(colour, opacity));
		}
	}

	/**
	 * A title bar with its caption and the three buttons on the right. Where each button went is written to
	 * {@code buttons} (close, maximise, minimise; x, y, w, h each), if given.
	 */
	static void titleBar(GuiGraphicsExtractor g, Scheme s, float x, float y, float w, float h, String caption, float opacity, float[][] buttons) {
		Shapes.gradientRectH(g, x, y, w, h, 0, a(s.title1, opacity), a(s.title2, opacity));
		float size = h * 0.5f;
		Fonts.draw(g, Typeface.OUTFIT_BOLD, caption, x + 8, y + h / 2, size, a(s.titleText(), opacity));
		float bw = h * 0.9f;
		float bh = h * 0.72f;
		float by = y + (h - bh) / 2;
		float bx = x + w - 4 - bw;
		for (int i = 0; i < 3; i++) {
			raised(g, s, bx, by, bw, bh, 1, s.face, opacity);
			if (buttons != null) {
				buttons[i] = new float[]{bx, by, bw, bh};
			}
			float cx = bx + bw / 2;
			float cy = by + bh / 2;
			int ink = a(s.text, opacity);
			switch (i) {
				case 0 -> { // close
					Shapes.line(g, cx - bh * 0.22f, cy - bh * 0.22f, cx + bh * 0.22f, cy + bh * 0.22f, 1.6f, ink);
					Shapes.line(g, cx - bh * 0.22f, cy + bh * 0.22f, cx + bh * 0.22f, cy - bh * 0.22f, 1.6f, ink);
				}
				case 1 -> { // maximise
					Shapes.rect(g, cx - bh * 0.25f, cy - bh * 0.22f, bh * 0.5f, bh * 0.44f, 0, 0, ink, 1);
					Shapes.rect(g, cx - bh * 0.25f, cy - bh * 0.22f, bh * 0.5f, 1.6f, 0, ink);
				}
				default -> // minimise
					Shapes.rect(g, cx - bh * 0.22f, cy + bh * 0.12f, bh * 0.36f, 1.6f, 0, ink);
			}
			bx -= bw + (i == 0 ? 3 : 0);
		}
	}
}
