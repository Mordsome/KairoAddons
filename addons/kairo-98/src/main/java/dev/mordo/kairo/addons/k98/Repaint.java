package dev.mordo.kairo.addons.k98;

import java.util.HashMap;
import java.util.Map;

/**
 * Repaints Minecraft's own GUI images (Kairo reads them from the game and offers each one; nothing of Minecraft's
 * is shipped here). Container backgrounds and other grey GUI pieces swap Minecraft's greys for the scheme's;
 * the hotbar, its selection, the experience bar and the buttons are drawn anew in the period's style; the other HUD
 * icons (hearts, hunger, armour...) are brought down to the 16 colours of the time.
 */
final class Repaint {
	/** The 16 colours of the period's screens. */
	private static final int[] SIXTEEN = {
		0x000000, 0x800000, 0x008000, 0x808000, 0x000080, 0x800080, 0x008080, 0xC0C0C0,
		0x808080, 0xFF0000, 0x00FF00, 0xFFFF00, 0x0000FF, 0xFF00FF, 0x00FFFF, 0xFFFFFF};

	private Repaint() {
	}

	static boolean repaint(Scheme s, boolean sixteen, String path, int[] px, int w, int h) {
		String sprite = path.startsWith("textures/gui/sprites/") ? path.substring("textures/gui/sprites/".length()) : null;
		if (sprite != null) {
			switch (sprite) {
				case "hud/hotbar.png" -> {
					hotbar(s, px, w, h);
					return true;
				}
				case "hud/hotbar_selection.png" -> {
					selection(s, px, w, h);
					return true;
				}
				case "hud/hotbar_offhand_left.png", "hud/hotbar_offhand_right.png" -> {
					offhand(s, px, w, h);
					return true;
				}
				case "hud/experience_bar_background.png", "hud/locator_bar_background.png" -> {
					track(s, px, w, h);
					return true;
				}
				case "hud/experience_bar_progress.png" -> {
					blocks(s, px, w, h);
					return true;
				}
				case "widget/button.png", "widget/button_highlighted.png", "widget/button_disabled.png" -> {
					button(s, px, w, h, sprite.endsWith("highlighted.png"), sprite.endsWith("disabled.png"));
					return true;
				}
				default -> {
				}
			}
			if (sprite.startsWith("hud/") && sixteen) {
				return quantize(px);
			}
			if (sprite.startsWith("toast/") || sprite.startsWith("hud/")) {
				return false;
			}
		}
		return greys(s, px);
	}

	// ---- Minecraft's greys -> the scheme's ----

	/**
	 * Only Minecraft's mid greys change (white highlights and black outlines stay): that way every colour scheme
	 * repaints exactly the same images, which lets Kairo switch schemes on the GPU without reloading.
	 */
	private static boolean greys(Scheme s, int[] px) {
		Map<Integer, Integer> map = new HashMap<>();
		map.put(0xC6C6C6, s.face);
		map.put(0xDBDBDB, s.light2);
		map.put(0x555555, s.shadow);
		map.put(0x8B8B8B, s.field());
		map.put(0x373737, s.shadow);
		boolean changed = false;
		for (int i = 0; i < px.length; i++) {
			int a = px[i] >>> 24;
			if (a < 250) {
				continue;
			}
			Integer to = map.get(px[i] & 0xFFFFFF);
			if (to != null) {
				px[i] = 0xFF000000 | to;
				changed = true; // counted even when a scheme happens to keep the colour, so the set never changes
			}
		}
		return changed;
	}

	/** Every visible pixel to the nearest of the 16 colours; see-through stays see-through. */
	private static boolean quantize(int[] px) {
		boolean changed = false;
		for (int i = 0; i < px.length; i++) {
			int a = px[i] >>> 24;
			if (a == 0) {
				continue;
			}
			int out = a < 110 ? 0 : 0xFF000000 | nearest(px[i]);
			if (out != px[i]) {
				px[i] = out;
				changed = true;
			}
		}
		return changed;
	}

	private static int nearest(int argb) {
		int r = argb >> 16 & 0xFF;
		int g = argb >> 8 & 0xFF;
		int b = argb & 0xFF;
		int best = 0;
		long bestD = Long.MAX_VALUE;
		for (int c : SIXTEEN) {
			int dr = r - (c >> 16 & 0xFF);
			int dg = g - (c >> 8 & 0xFF);
			int db = b - (c & 0xFF);
			long d = 3L * dr * dr + 4L * dg * dg + 2L * db * db;
			if (d < bestD) {
				bestD = d;
				best = c;
			}
		}
		return best;
	}

	// ---- drawn anew ----

	private static void fill(int[] px, int w, int x, int y, int fw, int fh, int colour) {
		for (int j = Math.max(0, y); j < y + fh; j++) {
			for (int i = Math.max(0, x); i < x + fw; i++) {
				if (i < w && j * w + i < px.length) {
					px[j * w + i] = colour;
				}
			}
		}
	}

	private static void edge(int[] px, int w, int x, int y, int ew, int eh, int tl, int br) {
		fill(px, w, x, y, ew - 1, 1, tl);
		fill(px, w, x, y, 1, eh - 1, tl);
		fill(px, w, x, y + eh - 1, ew, 1, br);
		fill(px, w, x + ew - 1, y, 1, eh, br);
	}

	private static void raised(Scheme s, int[] px, int w, int x, int y, int rw, int rh) {
		fill(px, w, x, y, rw, rh, s.face);
		edge(px, w, x, y, rw, rh, s.light, s.dark);
		edge(px, w, x + 1, y + 1, rw - 2, rh - 2, s.light2, s.shadow);
	}

	private static void sunken(Scheme s, int[] px, int w, int x, int y, int sw, int sh, int fill) {
		fill(px, w, x, y, sw, sh, fill);
		edge(px, w, x, y, sw, sh, s.shadow, s.light);
		edge(px, w, x + 1, y + 1, sw - 2, sh - 2, s.dark, s.light2);
	}

	/** The hotbar: a raised bar with nine sunken slots. */
	private static void hotbar(Scheme s, int[] px, int w, int h) {
		raised(s, px, w, 0, 0, w, h);
		for (int i = 0; i < 9; i++) {
			sunken(s, px, w, 1 + i * 20, 1, 20, Math.min(20, h - 2), s.field());
		}
	}

	/** The picked slot: a thick frame in the title colour, see-through inside. */
	private static void selection(Scheme s, int[] px, int w, int h) {
		java.util.Arrays.fill(px, 0);
		edge(px, w, 0, 0, w, h, s.title1, s.title1);
		edge(px, w, 1, 1, w - 2, h - 2, s.title2, s.title2);
		edge(px, w, 2, 2, w - 4, h - 4, s.light, s.light);
	}

	private static void offhand(Scheme s, int[] px, int w, int h) {
		java.util.Arrays.fill(px, 0);
		raised(s, px, w, 0, 0, Math.min(w, 24), h);
		sunken(s, px, w, 2, 2, Math.min(w, 24) - 4, h - 4, s.field());
	}

	/** A sunken progress track. */
	private static void track(Scheme s, int[] px, int w, int h) {
		sunken(s, px, w, 0, 0, w, h, s.face);
	}

	/** Progress in separate blocks of the title colour, like the period's progress bars. */
	private static void blocks(Scheme s, int[] px, int w, int h) {
		java.util.Arrays.fill(px, 0);
		for (int x = 2; x + 3 <= w - 2; x += 5) {
			fill(px, w, x, 1, 4, Math.max(1, h - 2), s.title1);
		}
	}

	/** A push button (Minecraft stretches it to any size by its borders). */
	private static void button(Scheme s, int[] px, int w, int h, boolean highlighted, boolean disabled) {
		raised(s, px, w, 0, 0, w, h);
		if (highlighted) {
			// The default-button look: an extra dark outline.
			edge(px, w, 0, 0, w, h, s.dark, s.dark);
			edge(px, w, 1, 1, w - 2, h - 2, s.light, s.dark);
			edge(px, w, 2, 2, w - 4, h - 4, s.light2, s.shadow);
		}
		if (disabled) {
			fill(px, w, 2, 2, w - 4, h - 4, s.face);
		}
	}
}
