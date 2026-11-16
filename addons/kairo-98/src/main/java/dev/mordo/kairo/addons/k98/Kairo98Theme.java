package dev.mordo.kairo.addons.k98;

import dev.mordo.kairo.api.HudLook;
import dev.mordo.kairo.api.MenuLook;
import dev.mordo.kairo.core.ClientSettings;
import dev.mordo.kairo.core.settings.BooleanSetting;
import dev.mordo.kairo.core.settings.EnumSetting;
import dev.mordo.kairo.core.settings.Setting;
import dev.mordo.kairo.render.Colors;
import dev.mordo.kairo.render.Shapes;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.List;

/**
 * Kairo 98's look: every surface a grey bevelled window or button of the late 90s, whole menus with a blue title bar,
 * black text, a flat-coloured desktop behind the main menu, HUD boxes like little title bars, and Minecraft's own
 * menus repainted to match (see {@link Repaint}).
 */
final class Kairo98Theme implements MenuLook, HudLook {
	static final String ID = "kairo-98";

	/** How HUD mods' boxes look. */
	enum HudBoxes implements EnumSetting.Labeled {
		TITLE_BAR("Title bar"),
		SIMPLE("Simple frame");

		private final String label;

		HudBoxes(String label) {
			this.label = label;
		}

		@Override
		public String label() {
			return label;
		}
	}

	final EnumSetting<Scheme> scheme = new EnumSetting<>("kairo98_scheme", "Colour scheme", Scheme.CLASSIC)
		.description("The colours of windows, title bars and the desktop. Minecraft's menus change with it");
	final BooleanSetting sixteen = new BooleanSetting("kairo98_sixteen_colours", "16-colour HUD icons", true)
		.description("Hearts, hunger, armour and the other HUD icons in the 16 colours of the time");
	final EnumSetting<HudBoxes> hudBoxes = new EnumSetting<>("kairo98_hud_boxes", "HUD mod boxes", HudBoxes.TITLE_BAR)
		.description("Title bar: a blue title-bar gradient behind every HUD mod. Simple frame: just a thin raised edge over a see-through dark box");
	final BooleanSetting retroAnimations = new BooleanSetting("kairo98_retro_animations", "Retro animations", false)
		.description("Hitting, mining and switching items move in a few hard steps, like an old game");
	final dev.mordo.kairo.core.settings.SliderSetting animationFrames = new dev.mordo.kairo.core.settings.SliderSetting(
		"kairo98_animation_frames", "Animation steps", 4, 2, 8, 1)
		.description("How many steps a swing takes: fewer is choppier")
		.visibleWhen(retroAnimations::isOn);
	final BooleanSetting bootScreen = new BooleanSetting("kairo98_boot_screen", "Start-up screen", true)
		.description("The loading screen in Kairo 98's style: a sky, the logo and a moving colour band");

	/** Where this frame's window buttons were drawn (close, maximise, minimise) and when. */
	private final float[][] windowButtons = new float[3][];
	private long windowButtonsAt;

	static boolean inUse() {
		return ClientSettings.INSTANCE.menuLook.get().equals(ID);
	}

	Scheme s() {
		return scheme.get();
	}

	@Override
	public String id() {
		return ID;
	}

	@Override
	public String name() {
		return "Kairo 98";
	}

	/** Everything is in Kairo 98's own Visuals tab (registered there, not with the theme). */
	@Override
	public List<Setting<?>> settings() {
		return List.of();
	}

	List<Setting<?>> tabSettings() {
		return List.of(scheme, hudBoxes, sixteen, retroAnimations, animationFrames, bootScreen);
	}

	@Override
	public String windowButton(float mx, float my) {
		if (System.nanoTime() - windowButtonsAt > 250_000_000L) {
			return null; // nothing with window buttons on screen right now
		}
		String[] names = {"close", "maximize", "minimize"};
		for (int i = 0; i < 3; i++) {
			float[] b = windowButtons[i];
			if (b != null && mx >= b[0] && mx <= b[0] + b[2] && my >= b[1] && my <= b[1] + b[3]) {
				return names[i];
			}
		}
		return null;
	}

	@Override
	public int textColor() {
		return s().text;
	}

	// ---- menus ----

	@Override
	public void panel(GuiGraphicsExtractor g, float x, float y, float w, float h, float radius, int tint, int stroke, float strokeWidth, float opacity) {
		Scheme s = s();
		float strength = Colors.alpha(tint) / 255f;
		boolean coloured = coloured(tint) || coloured(stroke);
		if (w > 400 && h > 240) {
			// A whole menu: a window, with its title bar sitting just above it.
			float bar = 24;
			Win.raised(g, s, x - 3, y - bar - 3, w + 6, h + bar + 6, 1.5f, s.face, opacity);
			Win.titleBar(g, s, x, y - bar, w, bar - 2, "Kairo", opacity, windowButtons);
			windowButtonsAt = System.nanoTime();
			Shapes.rect(g, x, y, w, h, 0, Win.a(s.face, opacity));
			return;
		}
		float px = Math.min(w, h) > 30 ? 1.5f : 1;
		boolean dark = !coloured && strength > 0.5f && luminance(tint) < 0.25f;
		if (dark) {
			// Kairo's dark popups and sunk-in fields: a white field, like the lists and boxes of the time.
			Win.sunken(g, s, x, y, w, h, px, s.field(), opacity);
			return;
		}
		if (coloured && strength > 0.3f) {
			// Picked (or strongly highlighted): pushed in, with the dotted focus line.
			Win.pressed(g, s, x, y, w, h, px, Scheme.lerp(s.face, s.light2, 0.35f), opacity);
			if (w > 12 && h > 10) {
				Win.focus(g, x + 4, y + 4, w - 8, h - 8, s.text, opacity * 0.8f);
			}
			return;
		}
		// Plain controls, buttons and cards: raised. Hovered ones a touch lighter.
		float hover = Math.clamp((strength - 0.05f) * 6, 0f, 1f);
		Win.raised(g, s, x, y, w, h, px, Scheme.lerp(s.face, s.light2, 0.5f * hover), opacity);
	}

	@Override
	public void sector(GuiGraphicsExtractor g, float cx, float cy, float innerR, float outerR, float midAngle, float halfAngle, float gap,
					   float cornerRadius, int tint, int stroke, float strokeWidth, float opacity) {
		Scheme s = s();
		boolean picked = (coloured(tint) || coloured(stroke)) && Colors.alpha(tint) / 255f > 0.2f;
		int face = picked ? s.title1 : s.face;
		Shapes.sector(g, cx, cy, innerR, outerR, midAngle, halfAngle, gap, 0, Win.a(s.dark, opacity), 0, 0);
		Shapes.sector(g, cx, cy, innerR + 1.5f, outerR - 1.5f, midAngle, halfAngle, gap + 3, 0, Win.a(s.light, opacity), 0, 0);
		Shapes.sector(g, cx, cy, innerR + 3, outerR - 3, midAngle, halfAngle, gap + 6, 0, Win.a(face, opacity), 0, 0);
	}

	@Override
	public void backdrop(GuiGraphicsExtractor g, float opacity, float dim, float vignette) {
		// In a world: the desktop colour over the game, half see-through.
		g.fill(0, 0, g.guiWidth(), g.guiHeight(), Colors.withAlpha(s().desktop, Math.clamp(opacity * (0.55f + dim * 0.4f), 0f, 0.9f)));
	}

	@Override
	public int loadingBackground() {
		return bootScreen.isOn() ? Scheme.lerp(s().title1, 0xFF000000, 0.15f) & 0xFFFFFF : 0;
	}

	@Override
	public boolean loading(GuiGraphicsExtractor g, int width, int height, float progress, float alpha) {
		if (!bootScreen.isOn()) {
			return false;
		}
		BootScreen.draw(g, s(), width, height, progress, alpha);
		return true;
	}

	@Override
	public boolean desktop(GuiGraphicsExtractor g, int width, int height) {
		g.fill(0, 0, width, height, s().desktop);
		return true;
	}

	// ---- Minecraft's own images ----

	@Override
	public boolean repaints() {
		return true;
	}

	@Override
	public String repaintKey() {
		return s().name() + (sixteen.isOn() ? ":16" : ":full");
	}

	@Override
	public boolean repaint(String path, int[] argb, int width, int height) {
		return Repaint.repaint(s(), sixteen.isOn(), path, argb, width, height);
	}

	// ---- HUD ----

	@Override
	public void box(GuiGraphicsExtractor g, float x, float y, float w, float h, float radius, int fill, int accent, float seed, float pressed) {
		Scheme s = s();
		if (hudBoxes.get() == HudBoxes.SIMPLE) {
			// Just a thin raised edge over the player's own (see-through) box colour; a pressed key fills with the title colour.
			int body = Colors.lerp(Colors.withAlpha(0xFF000000, Math.max(0.2f, Colors.alpha(fill) / 255f * 0.8f)),
				Colors.withAlpha(s.title2, 0.75f), pressed);
			Shapes.rect(g, x, y, w, h, 0, body);
			Win.edge(g, x, y, w, h, 1, Colors.withAlpha(s.light, 0.75f), Colors.withAlpha(s.dark, 0.75f), 1);
			return;
		}
		// A little title bar: HUD text is light, so the box is the dark blue of a title bar (brighter when pressed).
		int left = Scheme.lerp(s.title1, s.title2, 0.5f * pressed);
		int right = Scheme.lerp(s.title2, 0xFFFFFFFF, 0.25f * pressed);
		Shapes.gradientRectH(g, x, y, w, h, 0, left, right);
		Win.edge(g, x, y, w, h, 1, s.light, s.dark, 1);
	}

	// ---- helpers ----

	static boolean coloured(int argb) {
		int r = argb >> 16 & 0xFF;
		int g = argb >> 8 & 0xFF;
		int b = argb & 0xFF;
		return Colors.alpha(argb) > 30 && Math.max(r, Math.max(g, b)) - Math.min(r, Math.min(g, b)) >= 24;
	}

	static float luminance(int argb) {
		return ((argb >> 16 & 0xFF) * 0.3f + (argb >> 8 & 0xFF) * 0.59f + (argb & 0xFF) * 0.11f) / 255f;
	}
}
