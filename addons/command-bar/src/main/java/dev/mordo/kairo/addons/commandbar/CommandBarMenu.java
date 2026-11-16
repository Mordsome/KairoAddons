package dev.mordo.kairo.addons.commandbar;

import com.mojang.blaze3d.platform.InputConstants;
import dev.mordo.kairo.api.HomeMenu;
import dev.mordo.kairo.core.ClientSettings;
import dev.mordo.kairo.core.settings.BooleanSetting;
import dev.mordo.kairo.core.settings.Setting;
import dev.mordo.kairo.core.settings.SliderSetting;
import dev.mordo.kairo.gui.Icon;
import dev.mordo.kairo.gui.IconRenderer;
import dev.mordo.kairo.gui.Theme;
import dev.mordo.kairo.render.Animation;
import dev.mordo.kairo.render.Colors;
import dev.mordo.kairo.render.Shapes;
import dev.mordo.kairo.render.font.Fonts;
import dev.mordo.kairo.render.font.Typeface;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A search bar in the middle of the screen. With nothing typed, the player's ring sections sit under it as big
 * numbered tiles (number keys open them). Typing searches pages, mods and their settings as you type; arrows and
 * Enter open a result, Tab (or a right-click) switches a mod on or off without leaving. Esc clears the search, then
 * closes. Everything is drawn with Kairo's themed surfaces, so it wears the player's theme.
 */
final class CommandBarMenu implements HomeMenu {
	private static final String ID = "command-bar";
	private static final int MAX_QUERY = 40;
	private static final int RESULTS = 30;
	private static final int VISIBLE_ROWS = 7;

	private final BooleanSetting hints = new BooleanSetting("command_bar_hints", "Key hints", true)
		.description("A line under the bar with the keys you can use")
		.visibleWhen(() -> ClientSettings.INSTANCE.homeMenu.get().equals(ID));
	private final SliderSetting position = new SliderSetting("command_bar_height", "Height on screen", 0.22, 0.08, 0.4, 0.01).percent()
		.description("How far down the screen the bar sits")
		.visibleWhen(() -> ClientSettings.INSTANCE.homeMenu.get().equals(ID));

	private String query = "";
	private List<Item> results = List.of();
	/** The highlighted tile or result (keyboard and mouse). */
	private int selected;
	private int scroll;
	private long caretFrom = System.nanoTime();
	/** Where tiles and rows were drawn last frame, for the mouse. */
	private final List<float[]> boxes = new ArrayList<>();
	private final Map<Integer, Float> glow = new HashMap<>();
	private long lastFrame;
	private float lastMx = Float.NaN;
	private float lastMy = Float.NaN;

	@Override
	public String id() {
		return ID;
	}

	@Override
	public String name() {
		return "Command bar";
	}

	@Override
	public List<Setting<?>> settings() {
		return List.of(hints, position);
	}

	@Override
	public boolean takesText() {
		return true;
	}

	@Override
	public void opened(Context menu) {
		query = "";
		results = List.of();
		selected = 0;
		scroll = 0;
		glow.clear();
		caretFrom = System.nanoTime();
	}

	private boolean searching() {
		return !query.isEmpty();
	}

	private void search(Context menu) {
		results = searching() ? menu.search(query, RESULTS) : List.of();
		selected = 0;
		scroll = 0;
		caretFrom = System.nanoTime();
	}

	// ---- drawing ----

	@Override
	public void render(GuiGraphicsExtractor g, Context menu, float mx, float my, float width, float height, float appear) {
		long now = System.nanoTime();
		float dt = lastFrame == 0 ? 0 : Math.min(0.1f, (now - lastFrame) / 1e9f);
		lastFrame = now;
		boolean mouseMoved = mx != lastMx || my != lastMy;
		lastMx = mx;
		lastMy = my;
		float fade = Math.clamp(appear, 0f, 1f);
		if (fade <= 0.01f) {
			return;
		}
		int accent = Theme.accent();
		float w = Math.min(920, width * 0.64f);
		float x = (width - w) / 2;
		float rise = Animation.reduceMotion() ? 0 : (1 - fade) * 18;
		float y = height * position.floatValue() + rise;
		boxes.clear();

		// The bar.
		float barH = 64;
		Shapes.shadow(g, x, y + 6, w, barH, 16, 22, Colors.argb(0.35f * fade, 0));
		Shapes.glassRect(g, x, y, w, barH, 16, Theme.glassTint(), Colors.withAlpha(accent, 0.55f), 1.5f, fade);
		IconRenderer.draw(g, Icon.SEARCH, x + 32, y + barH / 2, 22, Theme.text(0.8f, fade), 2.2f);
		float textX = x + 60;
		if (searching()) {
			Fonts.draw(g, Typeface.OUTFIT_MEDIUM, query, textX, y + barH / 2, 21, Theme.text(1, fade));
			float caretX = textX + Fonts.width(Typeface.OUTFIT_MEDIUM, query, 21) + 2;
			boolean on = Animation.reduceMotion() || ((now - caretFrom) / 530_000_000L) % 2 == 0;
			if (on) {
				Shapes.rect(g, caretX, y + barH / 2 - 12, 1.8f, 24, 0.9f, Colors.withAlpha(accent, fade));
			}
		} else {
			Fonts.draw(g, Typeface.OUTFIT_REGULAR, "Search pages, mods and settings...", textX, y + barH / 2, 19, Theme.text(0.45f, fade));
		}
		pill(g, "ESC", x + w - 22, y + barH / 2, fade, true);

		float top = y + barH + 18;
		if (searching()) {
			drawResults(g, menu, mx, my, mouseMoved, x, top, w, fade, dt, accent);
		} else {
			top = drawTiles(g, menu, mx, my, mouseMoved, x, top, w, fade, dt, accent);
		}
		if (hints.isOn()) {
			String hint = searching()
				? "↑ ↓  move     Enter  open     Tab  switch a mod on or off     Esc  clear"
				: "Type to search     1-" + Math.max(1, menu.sections().size()) + "  open     Arrows + Enter  move and open     Esc  close";
			float hy = searching() ? top + Math.min(VISIBLE_ROWS, Math.max(1, results.size())) * 58 + 24 : top + 10;
			Fonts.drawCentered(g, Typeface.MONO_MEDIUM, hint, width / 2, hy, 11, Theme.text(0.45f, fade));
		}
	}

	/** The ring sections as numbered tiles; returns the y under them. */
	private float drawTiles(GuiGraphicsExtractor g, Context menu, float mx, float my, boolean mouseMoved, float x, float top, float w,
							float fade, float dt, int accent) {
		List<Item> sections = menu.sections();
		int n = sections.size();
		int cols = n <= 4 ? n : (n + 1) / 2;
		float gap = 12;
		float tw = (w - gap * (cols - 1)) / cols;
		float th = 124;
		selected = Math.clamp(selected, 0, Math.max(0, n - 1));
		for (int i = 0; i < n; i++) {
			float tx = x + (i % cols) * (tw + gap);
			float ty = top + (i / cols) * (th + gap);
			boolean over = mx >= tx && mx <= tx + tw && my >= ty && my <= ty + th;
			if (over && mouseMoved) {
				selected = i;
			}
			float hl = glow(i, i == selected, dt);
			Item item = sections.get(i);
			drawTile(g, item, i + 1, tx, ty - hl * (Animation.reduceMotion() ? 0 : 2), tw, th, hl, fade, accent);
			boxes.add(new float[]{tx, ty, tw, th});
		}
		int rows = (n + cols - 1) / cols;
		return top + rows * (th + gap) + 6;
	}

	private void drawTile(GuiGraphicsExtractor g, Item item, int number, float x, float y, float w, float h, float hl, float fade, int accent) {
		boolean on = item.on() != null && item.on().getAsBoolean();
		int fill = Colors.lerp(Theme.SURFACE, Colors.withAlpha(accent, 0.22f), hl);
		int stroke = Colors.lerp(Theme.BORDER, Colors.withAlpha(accent, 0.85f), hl);
		Shapes.surface(g, x, y, w, h, 14, fill, stroke, 1 + hl * 0.5f, fade);
		IconRenderer.draw(g, item.icon(), x + 30, y + 32, 24, Colors.multiplyAlpha(Colors.lerp(Theme.text(0.85f), accent | 0xFF000000, hl), fade), 2f);
		Fonts.draw(g, Typeface.OUTFIT_SEMIBOLD, Fonts.ellipsize(Typeface.OUTFIT_SEMIBOLD, item.title(), 17.5f, w - 60), x + 16, y + 72, 17.5f,
			Theme.text(1, fade));
		String detail = item.detail() == null ? "" : item.detail();
		Fonts.draw(g, Typeface.OUTFIT_REGULAR, Fonts.ellipsize(Typeface.OUTFIT_REGULAR, detail, 11.5f, w - (item.on() != null ? 76 : 30)), x + 16, y + 98, 11.5f,
			Theme.text(0.5f, fade));
		pill(g, String.valueOf(number), x + w - 22, y + 24, fade, false);
		if (item.on() != null) {
			state(g, on, x + w - 14, y + h - 26, fade, accent);
		}
	}

	private void drawResults(GuiGraphicsExtractor g, Context menu, float mx, float my, boolean mouseMoved, float x, float top, float w,
							 float fade, float dt, int accent) {
		if (results.isEmpty()) {
			Shapes.surface(g, x, top, w, 56, 12, Theme.SURFACE, Theme.BORDER, 1, fade);
			Fonts.draw(g, Typeface.OUTFIT_REGULAR, "Nothing called \"" + query + "\". Try part of a name, like \"keys\" or \"sky\".", x + 20, top + 28,
				14, Theme.text(0.55f, fade));
			return;
		}
		selected = Math.clamp(selected, 0, results.size() - 1);
		if (selected < scroll) {
			scroll = selected;
		} else if (selected >= scroll + VISIBLE_ROWS) {
			scroll = selected - VISIBLE_ROWS + 1;
		}
		float rowH = 52;
		float gap = 6;
		int shown = Math.min(VISIBLE_ROWS, results.size() - scroll);
		for (int r = 0; r < shown; r++) {
			int i = scroll + r;
			float ry = top + r * (rowH + gap);
			boolean over = mx >= x && mx <= x + w && my >= ry && my <= ry + rowH;
			if (over && mouseMoved) {
				selected = i;
			}
			float hl = glow(1000 + i, i == selected, dt);
			Item item = results.get(i);
			int fill = Colors.lerp(Theme.SURFACE, Colors.withAlpha(accent, 0.2f), hl);
			int stroke = Colors.lerp(Theme.BORDER, Colors.withAlpha(accent, 0.8f), hl);
			Shapes.surface(g, x, ry, w, rowH, 11, fill, stroke, 1, fade);
			IconRenderer.draw(g, item.icon(), x + 26, ry + rowH / 2, 19, Colors.multiplyAlpha(Colors.lerp(Theme.text(0.8f), accent | 0xFF000000, hl), fade), 2f);
			float tx = x + 52;
			float right = x + w - 16;
			if (item.on() != null) {
				state(g, item.on().getAsBoolean(), right - 6, ry + rowH / 2, fade, accent);
				right -= 46;
			}
			float kindW = Fonts.width(Typeface.MONO_BOLD, item.kind().toUpperCase(java.util.Locale.ROOT), 8.5f) + 14;
			pill(g, item.kind().toUpperCase(java.util.Locale.ROOT), right - kindW / 2, ry + rowH / 2, fade, false);
			right -= kindW + 10;
			Fonts.draw(g, Typeface.OUTFIT_SEMIBOLD, Fonts.ellipsize(Typeface.OUTFIT_SEMIBOLD, item.title(), 15.5f, right - tx), tx, ry + 18, 15.5f,
				Theme.text(1, fade));
			String detail = item.detail() == null ? "" : item.detail();
			Fonts.draw(g, Typeface.OUTFIT_REGULAR, Fonts.ellipsize(Typeface.OUTFIT_REGULAR, detail, 11.5f, right - tx), tx, ry + 36, 11.5f,
				Theme.text(0.5f, fade));
			boxes.add(new float[]{x, ry, w, rowH, i});
		}
		if (results.size() > VISIBLE_ROWS) {
			// Where in the list we are.
			float trackH = VISIBLE_ROWS * (rowH + gap) - gap;
			float barH = Math.max(18, trackH * VISIBLE_ROWS / results.size());
			float barY = top + (trackH - barH) * scroll / Math.max(1, results.size() - VISIBLE_ROWS);
			Shapes.rect(g, x + w + 8, barY, 3, barH, 1.5f, Theme.text(0.25f, fade));
		}
	}

	/** A small rounded label (a number, a key, a kind). */
	private static void pill(GuiGraphicsExtractor g, String text, float cx, float cy, float fade, boolean key) {
		float tw = Fonts.width(Typeface.MONO_BOLD, text, 8.5f) + 12;
		Shapes.rect(g, cx - tw / 2, cy - 9, tw, 18, 6, Colors.argb((key ? 0.10f : 0.07f) * fade, 0xFFFFFF), Colors.argb(0.14f * fade, 0xFFFFFF), 1);
		Fonts.drawCentered(g, Typeface.MONO_BOLD, text, cx, cy, 8.5f, Theme.text(0.75f, fade));
	}

	/** On / off, for PvP mode and mods. */
	private static void state(GuiGraphicsExtractor g, boolean on, float right, float cy, float fade, int accent) {
		float w = 34;
		float h = 18;
		float x = right - w;
		Shapes.rect(g, x, cy - h / 2, w, h, h / 2, on ? Colors.withAlpha(accent, 0.9f * fade) : Colors.argb(0.16f * fade, 0xFFFFFF));
		float knob = on ? x + w - h / 2 : x + h / 2;
		Shapes.circle(g, knob, cy, h / 2 - 3, Colors.argb(fade, on ? 0xFFFFFF : 0xC8C8C8));
	}

	/** Eases each tile's or row's highlight towards on or off. */
	private float glow(int key, boolean on, float dt) {
		float now = glow.getOrDefault(key, 0f);
		float target = on ? 1 : 0;
		float next = Animation.reduceMotion() ? target : now + (target - now) * Math.min(1, dt * 14);
		glow.put(key, next);
		return next;
	}

	// ---- input ----

	@Override
	public boolean charTyped(Context menu, int codepoint) {
		if (Character.isISOControl(codepoint)) {
			return false;
		}
		if (!searching() && codepoint >= '1' && codepoint <= '9') {
			// With nothing typed, a number opens that tile (like the ring's number keys).
			List<Item> sections = menu.sections();
			int index = codepoint - '1';
			if (index < sections.size()) {
				sections.get(index).open().run();
				return true;
			}
		}
		if (!searching() && Character.isWhitespace(codepoint)) {
			return true;
		}
		if (query.length() < MAX_QUERY) {
			query += new String(Character.toChars(codepoint));
			search(menu);
		}
		return true;
	}

	@Override
	public boolean keyPressed(Context menu, int key, boolean shift, boolean ctrl) {
		switch (key) {
			case InputConstants.KEY_BACKSPACE -> {
				if (searching()) {
					query = ctrl ? "" : query.substring(0, query.offsetByCodePoints(query.length(), -1));
					search(menu);
				}
				return true;
			}
			case InputConstants.KEY_RETURN, InputConstants.KEY_NUMPADENTER -> {
				Item item = current(menu);
				if (item != null) {
					if (shift && item.toggle() != null) {
						item.toggle().run();
					} else {
						item.open().run();
					}
				}
				return true;
			}
			case InputConstants.KEY_TAB -> {
				Item item = current(menu);
				if (item != null && item.toggle() != null) {
					item.toggle().run();
				} else if (!searching()) {
					move(menu, shift ? -1 : 1);
				}
				return true;
			}
			case InputConstants.KEY_DOWN -> {
				move(menu, searching() ? 1 : columns(menu));
				return true;
			}
			case InputConstants.KEY_UP -> {
				move(menu, searching() ? -1 : -columns(menu));
				return true;
			}
			case InputConstants.KEY_RIGHT -> {
				if (!searching()) {
					move(menu, 1);
				}
				return true;
			}
			case InputConstants.KEY_LEFT -> {
				if (!searching()) {
					move(menu, -1);
				}
				return true;
			}
			default -> {
				return false;
			}
		}
	}

	private static int columns(Context menu) {
		int n = menu.sections().size();
		return n <= 4 ? n : (n + 1) / 2;
	}

	private void move(Context menu, int by) {
		int n = searching() ? results.size() : menu.sections().size();
		if (n == 0) {
			return;
		}
		if (searching()) {
			selected = Math.clamp(selected + by, 0, n - 1);
		} else {
			selected = Math.floorMod(selected + by, n);
		}
		dev.mordo.kairo.sound.UiSounds.hover();
	}

	private Item current(Context menu) {
		List<Item> list = searching() ? results : menu.sections();
		return selected >= 0 && selected < list.size() ? list.get(selected) : null;
	}

	@Override
	public boolean mouseClicked(Context menu, float mx, float my, int button) {
		for (int i = 0; i < boxes.size(); i++) {
			float[] b = boxes.get(i);
			if (mx >= b[0] && mx <= b[0] + b[2] && my >= b[1] && my <= b[1] + b[3]) {
				int index = b.length > 4 ? (int) b[4] : i;
				Item item = (searching() ? results : menu.sections()).get(index);
				selected = index;
				if (button == 1 && item.toggle() != null) {
					item.toggle().run();
				} else if (button == 0) {
					item.open().run();
				}
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean mouseScrolled(Context menu, float mx, float my, float amount) {
		if (searching() && results.size() > VISIBLE_ROWS) {
			scroll = Math.clamp(scroll - (int) Math.signum(amount), 0, results.size() - VISIBLE_ROWS);
			selected = Math.clamp(selected, scroll, scroll + VISIBLE_ROWS - 1);
			return true;
		}
		return false;
	}

	@Override
	public boolean escape(Context menu) {
		if (searching()) {
			query = "";
			search(menu);
			return true;
		}
		return false;
	}
}
