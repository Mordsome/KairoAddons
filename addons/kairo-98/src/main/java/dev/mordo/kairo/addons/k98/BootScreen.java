package dev.mordo.kairo.addons.k98;

import dev.mordo.kairo.render.Animation;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Kairo 98's loading screen, in the spirit of a late-90s start-up: a sky with blocky clouds, Kairo's four-tile mark
 * and its name in a little block font, a progress bar of blocks for the real progress, and a band of colour moving
 * along the bottom edge. With Reduce motion the clouds and the band stand still; nothing flashes.
 *
 * <p>Drawn with Minecraft's plainest drawing only (filled rectangles): the very first loading screen comes before
 * any of Kairo's shapes, shaders or fonts are loaded.
 */
final class BootScreen {
	private static final long START = System.nanoTime();

	/** A 5 x 7 block font for the few letters the screen needs. */
	private static final String[] GLYPHS = {
		"K", "10001100101010011000101001001010001",
		"A", "01110100011000111111100011000110001",
		"I", "11111001000010000100001000010011111",
		"R", "11110100011000111110101001001010001",
		"O", "01110100011000110001100011000101110",
		"9", "01110100011000101111000010001001100",
		"8", "01110100011000101110100011000101110",
		"S", "01111100001000001110000010000111110",
		"T", "11111001000010000100001000010000100",
		"N", "10001110011010110011100011000110001",
		"G", "01110100011000010111100011000101111",
		".", "00000000000000000000000000011000110",
		" ", "00000000000000000000000000000000000",
	};

	private BootScreen() {
	}

	private static int a(int rgb, float alpha) {
		return Math.round(Math.clamp(alpha, 0f, 1f) * 255) << 24 | (rgb & 0xFFFFFF);
	}

	static void draw(GuiGraphicsExtractor g, Scheme s, int width, int height, float progress, float alpha) {
		float time = Animation.reduceMotion() ? 0 : (System.nanoTime() - START) / 1e9f;
		int skyTop = Scheme.lerp(s.title1, 0xFF000000, 0.15f);
		int skyBottom = Scheme.lerp(s.title2, 0xFFFFFFFF, 0.35f);
		g.fillGradient(0, 0, width, height, a(skyTop, alpha), a(skyBottom, alpha));

		// Blocky clouds, drifting slowly to the right.
		int u = Math.max(2, height / 90);
		int[][] clouds = {{8, 12, 14}, {52, 22, 10}, {90, 8, 12}, {30, 46, 16}, {74, 58, 11}, {118, 38, 9}};
		for (int[] c : clouds) {
			int cw = c[2] * u;
			int cx = (int) ((c[0] * u + time * u * 1.5f) % (width + cw * 2)) - cw;
			int cy = c[1] * height / 100;
			int white = a(0xFFFFFF, 0.85f * alpha);
			g.fill(cx, cy + u, cx + cw, cy + 3 * u, white);
			g.fill(cx + cw / 5, cy, cx + cw * 13 / 20, cy + u, white);
			g.fill(cx + cw / 10, cy + 3 * u, cx + cw * 8 / 10, cy + 4 * u, white);
		}

		// The mark and the name, in the middle.
		int cx = width / 2;
		int cy = height * 42 / 100;
		int tile = Math.max(6, height * 7 / 100);
		int gap = Math.max(1, tile / 8);
		int[] colours = {0xE04030, 0x40A040, 0x3060D0, 0xE0B020};
		int px = Math.max(2, height / 70);
		int textW = text("KAIRO", px) + px * 3 + text("98", px * 6 / 10);
		int total = tile * 2 + gap + tile + textW;
		int markX = cx - total / 2;
		for (int i = 0; i < 4; i++) {
			int x = markX + (i % 2) * (tile + gap);
			int y = cy - tile - gap / 2 + (i / 2) * (tile + gap);
			int shadow = Math.max(1, tile / 8);
			g.fill(x + shadow, y + shadow, x + tile + shadow, y + tile + shadow, a(0x000000, 0.25f * alpha));
			g.fill(x, y, x + tile, y + tile, a(colours[i], alpha));
		}
		int textX = markX + tile * 2 + gap + tile;
		int textY = cy - px * 7 / 2;
		drawText(g, "KAIRO", textX + px / 2, textY + px / 2, px, a(0x000000, 0.3f * alpha));
		int afterKairo = drawText(g, "KAIRO", textX, textY, px, a(0xFFFFFF, alpha));
		drawText(g, "98", afterKairo + px * 3, textY - px, Math.max(1, px * 6 / 10), a(0xFFE070, alpha));
		int small = Math.max(1, px / 2);
		int line = "STARTING KAIRO 98...".length() * 6 * small;
		drawText(g, "STARTING KAIRO 98...", cx - line / 2, cy + tile * 2, small, a(0xFFFFFF, 0.9f * alpha));

		// The real progress: a sunken bar filling with blocks.
		int bw = Math.min(width * 2 / 5, height);
		int bh = Math.max(8, height * 35 / 1000);
		int bx = cx - bw / 2;
		int by = cy + tile * 3;
		g.fill(bx, by, bx + bw, by + bh, a(s.face, alpha));
		g.fill(bx, by, bx + bw, by + 1, a(s.shadow, alpha));
		g.fill(bx, by, bx + 1, by + bh, a(s.shadow, alpha));
		g.fill(bx, by + bh - 1, bx + bw, by + bh, a(s.light, alpha));
		g.fill(bx + bw - 1, by, bx + bw, by + bh, a(s.light, alpha));
		int block = Math.max(3, bh * 55 / 100);
		int inner = bw - 6;
		int blocks = inner / (block + 2);
		int lit = Math.round(blocks * Math.clamp(progress, 0f, 1f));
		for (int i = 0; i < lit; i++) {
			int x = bx + 3 + i * (block + 2);
			g.fill(x, by + bh / 5, x + block, by + bh - bh / 5, a(s.title1, alpha));
		}

		// The band of colour along the bottom, moving to the right.
		int bandH = Math.max(4, height * 25 / 1000);
		int bandY = height - bandH;
		int[] band = {s.title1, s.title2, 0x7FD8FF, 0xFFFFFF, 0x7FD8FF, s.title2, s.title1};
		int span = Math.max(1, width * 4 / 5);
		int offset = (int) ((time * width * 0.25f) % span);
		int strip = Math.max(2, width / 320);
		for (int x = 0; x < width; x += strip) {
			float t = (float) Math.floorMod(x - offset, span) / span * (band.length - 1);
			int i = Math.min(band.length - 2, (int) t);
			int colour = Scheme.lerp(0xFF000000 | band[i], 0xFF000000 | band[i + 1], t - i);
			g.fill(x, bandY, x + strip, height, a(colour, alpha));
		}
	}

	/** Width of {@code text} in the block font at {@code px} per block. */
	private static int text(String text, int px) {
		return text.length() * 6 * px;
	}

	/** Draws {@code text} in the block font; returns the x after it. */
	private static int drawText(GuiGraphicsExtractor g, String text, int x, int y, int px, int colour) {
		for (char ch : text.toCharArray()) {
			String bits = null;
			for (int i = 0; i < GLYPHS.length; i += 2) {
				if (GLYPHS[i].charAt(0) == ch) {
					bits = GLYPHS[i + 1];
				}
			}
			if (bits != null) {
				for (int row = 0; row < 7; row++) {
					for (int col = 0; col < 5; col++) {
						if (bits.charAt(row * 5 + col) == '1') {
							g.fill(x + col * px, y + row * px, x + (col + 1) * px, y + (row + 1) * px, colour);
						}
					}
				}
			}
			x += 6 * px;
		}
		return x - px;
	}
}
