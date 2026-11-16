package dev.mordo.kairo.addons.k98;

import com.mojang.blaze3d.platform.InputConstants;
import dev.mordo.kairo.api.HomeMenu;
import dev.mordo.kairo.gui.Icon;
import dev.mordo.kairo.gui.IconRenderer;
import dev.mordo.kairo.render.Animation;
import dev.mordo.kairo.render.Shapes;
import dev.mordo.kairo.render.font.Fonts;
import dev.mordo.kairo.render.font.Typeface;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * What Right Shift opens with Kairo 98: a taskbar along the bottom with a Start button and a clock, and the Start
 * menu open above it with your ring sections (a blue band down its side). Hover or arrow keys pick, a click or Enter
 * opens, 1-8 open directly. Clicking Start or Esc folds the menu away; Esc again (or the menu key) leaves.
 */
final class StartMenu implements HomeMenu {
	static final String ID = "kairo-98-start";
	private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm");

	private final Kairo98Theme theme;
	private boolean open = true;
	private int selected = -1;
	private float slide;
	private long lastFrame;
	private float lastMx = Float.NaN;
	private float lastMy = Float.NaN;
	/** Where things were drawn last frame, for the mouse: the Start button, the menu, and each item. */
	private final float[] start = new float[4];
	private final float[] menuBox = new float[4];
	private final List<float[]> rows = new ArrayList<>();

	StartMenu(Kairo98Theme theme) {
		this.theme = theme;
	}

	@Override
	public String id() {
		return ID;
	}

	@Override
	public String name() {
		return "Start menu (Kairo 98)";
	}

	@Override
	public void opened(Context menu) {
		open = true;
		selected = -1;
		slide = 0;
	}

	@Override
	public void render(GuiGraphicsExtractor g, Context menu, float mx, float my, float width, float height, float appear) {
		Scheme s = theme.s();
		long now = System.nanoTime();
		float dt = lastFrame == 0 ? 0 : Math.min(0.1f, (now - lastFrame) / 1e9f);
		lastFrame = now;
		boolean moved = mx != lastMx || my != lastMy;
		lastMx = mx;
		lastMy = my;
		float fade = Math.clamp(appear, 0f, 1f);
		if (fade <= 0.01f) {
			return;
		}
		slide = Animation.reduceMotion() ? (open ? 1 : 0) : Math.clamp(slide + (open ? 1 : -1) * dt * 7, 0f, 1f);

		// The taskbar.
		float barH = 40;
		float barY = height - barH * Math.min(1, fade * 1.4f);
		Shapes.rect(g, 0, barY, width, barH, 0, Win.a(s.face, fade));
		Shapes.rect(g, 0, barY, width, 1.5f, 0, Win.a(s.light2, fade));
		Shapes.rect(g, 0, barY + 1.5f, width, 1.5f, 0, Win.a(s.light, fade));
		float bx = 5;
		float by = barY + 6;
		float bw = 100;
		float bh = barH - 10;
		boolean overStart = in(mx, my, bx, by, bw, bh);
		if (open) {
			Win.pressed(g, s, bx, by, bw, bh, 1.5f, s.face, fade);
		} else {
			Win.raised(g, s, bx, by, bw, bh, 1.5f, s.face, fade);
		}
		logo(g, s, bx + 18, by + bh / 2 + (open ? 1 : 0), 15, fade);
		Fonts.draw(g, Typeface.OUTFIT_BOLD, "Start", bx + 34, by + bh / 2 + (open ? 1 : 0), 15, Win.a(s.text, fade));
		start[0] = bx;
		start[1] = by;
		start[2] = bw;
		start[3] = bh;
		// The tray with the clock.
		float tw = 92;
		Win.sunken(g, s, width - tw - 5, by, tw, bh, 1.5f, s.face, fade);
		Fonts.drawCentered(g, Typeface.OUTFIT_MEDIUM, LocalTime.now().format(CLOCK), width - tw / 2 - 5, by + bh / 2, 13.5f, Win.a(s.text, fade));
		// A task button for the menu itself.
		float taskX = bx + bw + 8;
		Win.pressed(g, s, taskX, by, 180, bh, 1.5f, Scheme.lerp(s.face, s.light2, 0.4f), fade);
		IconRenderer.draw(g, Icon.GRID, taskX + 16, by + bh / 2, 14, Win.a(s.text, fade), 2f);
		Fonts.draw(g, Typeface.OUTFIT_SEMIBOLD, "Kairo", taskX + 30, by + bh / 2, 13.5f, Win.a(s.text, fade));

		// The Start menu, sliding up out of the taskbar.
		rows.clear();
		menuBox[2] = 0;
		if (slide <= 0.01f) {
			return;
		}
		List<Item> items = menu.sections();
		float rowH = 40;
		float band = 30;
		float mw = 300;
		float mh = 8 + items.size() * rowH + 12 + rowH + 8;
		float mxPos = 3;
		float myPos = barY - mh * slide - 1;
		float a = fade * Math.min(1, slide * 1.5f);
		Win.raised(g, s, mxPos, myPos, mw, mh, 1.5f, s.face, a);
		// The blue band down the left, with the name running up it.
		Shapes.gradientRect(g, mxPos + 3, myPos + 3, band, mh - 6, 0, Win.a(s.title2, a), Win.a(s.title1, a));
		g.pose().pushMatrix();
		g.pose().translate(mxPos + 3 + band / 2, myPos + mh - 12);
		g.pose().rotate((float) (-Math.PI / 2));
		Fonts.draw(g, Typeface.OUTFIT_BOLD, "Kairo", 0, 0, 19, Win.a(0xFFFFFFFF, a));
		float kw = Fonts.width(Typeface.OUTFIT_BOLD, "Kairo", 19);
		Fonts.draw(g, Typeface.OUTFIT_REGULAR, "98", kw + 5, 0, 19, Win.a(0xFFDDDDDD, a));
		g.pose().popMatrix();
		menuBox[0] = mxPos;
		menuBox[1] = myPos;
		menuBox[2] = mw;
		menuBox[3] = mh;

		float ix = mxPos + band + 6;
		float iw = mw - band - 12;
		float iy = myPos + 8;
		for (int i = 0; i <= items.size(); i++) {
			boolean last = i == items.size();
			if (last) {
				// A line, then a way out.
				Win.edge(g, ix, iy + 4, iw, 3, 1.5f, s.shadow, s.light, a);
				iy += 12;
			}
			boolean over = in(mx, my, ix, iy, iw, rowH);
			if (over && moved) {
				selected = i;
			}
			boolean picked = i == selected;
			if (picked) {
				Shapes.rect(g, ix, iy, iw, rowH, 0, Win.a(s.title1, a));
			}
			int ink = picked ? s.titleText() : s.text;
			Icon icon = last ? Icon.ARROW_LEFT : items.get(i).icon();
			String label = last ? "Back to the game" : items.get(i).title();
			IconRenderer.draw(g, icon, ix + 22, iy + rowH / 2, 20, Win.a(ink, a), 2f);
			Fonts.draw(g, Typeface.OUTFIT_MEDIUM, label, ix + 44, iy + rowH / 2, 15, Win.a(ink, a));
			if (!last) {
				Item item = items.get(i);
				if (item.on() != null) {
					String state = item.on().getAsBoolean() ? "On" : "Off";
					Fonts.draw(g, Typeface.OUTFIT_REGULAR, state, ix + iw - 12, iy + rowH / 2, 12.5f, Win.a(ink, a * 0.85f), Fonts.Align.RIGHT, 0, false);
				} else if (i < 9) {
					Fonts.draw(g, Typeface.MONO_MEDIUM, String.valueOf(i + 1), ix + iw - 12, iy + rowH / 2, 11, Win.a(ink, a * 0.6f), Fonts.Align.RIGHT, 0, false);
				}
			}
			rows.add(new float[]{ix, iy, iw, rowH, i});
			iy += rowH;
		}
	}

	/** Kairo's mark in the scheme's colours: four little tiles, like the ring's segments. */
	private static void logo(GuiGraphicsExtractor g, Scheme s, float cx, float cy, float size, float fade) {
		float half = size / 2;
		float gap = 1.5f;
		float t = (size - gap) / 2;
		int[] colours = {0xFFE04030, 0xFF40A040, 0xFF3060D0, 0xFFE0B020};
		for (int i = 0; i < 4; i++) {
			float x = cx - half + (i % 2) * (t + gap);
			float y = cy - half + (i / 2) * (t + gap);
			Shapes.rect(g, x, y, t, t, 0, Win.a(colours[i], fade));
		}
	}

	private static boolean in(float mx, float my, float x, float y, float w, float h) {
		return mx >= x && mx <= x + w && my >= y && my <= y + h;
	}

	private void run(Context menu, int index) {
		List<Item> items = menu.sections();
		if (index == items.size()) {
			menu.close();
		} else if (index >= 0 && index < items.size()) {
			items.get(index).open().run();
		}
	}

	// ---- input ----

	@Override
	public boolean mouseClicked(Context menu, float mx, float my, int button) {
		if (button != 0) {
			return false;
		}
		if (in(mx, my, start[0], start[1], start[2], start[3])) {
			open = !open;
			dev.mordo.kairo.sound.UiSounds.click();
			return true;
		}
		if (open) {
			for (float[] row : rows) {
				if (in(mx, my, row[0], row[1], row[2], row[3])) {
					run(menu, (int) row[4]);
					return true;
				}
			}
			if (!in(mx, my, menuBox[0], menuBox[1], menuBox[2], menuBox[3])) {
				open = false;
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean charTyped(Context menu, int codepoint) {
		if (codepoint >= '1' && codepoint <= '9') {
			int index = codepoint - '1';
			if (index < menu.sections().size()) {
				run(menu, index);
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean keyPressed(Context menu, int key, boolean shift, boolean ctrl) {
		int count = menu.sections().size() + 1;
		switch (key) {
			case InputConstants.KEY_UP -> {
				open = true;
				selected = selected <= 0 ? count - 1 : selected - 1;
				return true;
			}
			case InputConstants.KEY_DOWN, InputConstants.KEY_TAB -> {
				open = true;
				selected = selected < 0 || selected >= count - 1 ? 0 : selected + 1;
				return true;
			}
			case InputConstants.KEY_RETURN, InputConstants.KEY_NUMPADENTER, InputConstants.KEY_SPACE -> {
				if (open && selected >= 0) {
					run(menu, selected);
				} else {
					open = true;
				}
				return true;
			}
			default -> {
				return false;
			}
		}
	}

	@Override
	public boolean escape(Context menu) {
		if (open) {
			open = false;
			return true;
		}
		return false;
	}
}
