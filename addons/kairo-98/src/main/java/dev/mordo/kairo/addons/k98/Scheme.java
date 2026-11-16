package dev.mordo.kairo.addons.k98;

import dev.mordo.kairo.core.settings.EnumSetting;

/**
 * The colour schemes of the time, by their own colours (not by anyone's names): the face of every window and
 * button, its light and dark edges, the title bar's two ends, the desktop and the text.
 */
enum Scheme implements EnumSetting.Labeled {
	CLASSIC("Classic", 0xC0C0C0, 0xFFFFFF, 0xDFDFDF, 0x808080, 0x000000, 0x000080, 0x1084D0, 0x008080, 0x000000),
	SAND("Sand", 0xD5CCBB, 0xFFFFFF, 0xE8E2D6, 0xA28D68, 0x000000, 0x008080, 0x56A6A1, 0xA28D68, 0x000000),
	PLUM("Plum", 0xC8B4B4, 0xFFFFFF, 0xDCCDCD, 0x8C6464, 0x000000, 0x400040, 0x8C5A80, 0x402040, 0x000000),
	OCEAN("Ocean", 0xB8D0D0, 0xFFFFFF, 0xD4E4E4, 0x5A8C8C, 0x000000, 0x003C64, 0x2E7DA6, 0x00304A, 0x000000),
	SLATE("Slate", 0xB0BCC8, 0xFFFFFF, 0xCED6DE, 0x6A7A8A, 0x000000, 0x3A4F66, 0x7A92AA, 0x3C4650, 0x000000),
	SPRUCE("Spruce", 0xA8C8A8, 0xFFFFFF, 0xC8DEC8, 0x5A8A5A, 0x000000, 0x1E4E1E, 0x4E8C4E, 0x224822, 0x000000),
	HIGH_CONTRAST("High contrast", 0x000000, 0xFFFFFF, 0x808080, 0x808080, 0xFFFFFF, 0x800080, 0x800080, 0x000000, 0xFFFFFF);

	private final String label;
	/** Window and button face. */
	final int face;
	/** The bright outer edge (top and left of something raised). */
	final int light;
	/** The softer inner bright edge. */
	final int light2;
	/** The inner dark edge (bottom and right). */
	final int shadow;
	/** The outer dark edge. */
	final int dark;
	/** The title bar, left end and right end. */
	final int title1;
	final int title2;
	final int desktop;
	final int text;

	Scheme(String label, int face, int light, int light2, int shadow, int dark, int title1, int title2, int desktop, int text) {
		this.label = label;
		this.face = 0xFF000000 | face;
		this.light = 0xFF000000 | light;
		this.light2 = 0xFF000000 | light2;
		this.shadow = 0xFF000000 | shadow;
		this.dark = 0xFF000000 | dark;
		this.title1 = 0xFF000000 | title1;
		this.title2 = 0xFF000000 | title2;
		this.desktop = 0xFF000000 | desktop;
		this.text = 0xFF000000 | text;
	}

	@Override
	public String label() {
		return label;
	}

	/** What sunken fields and slots are filled with: the face, a little lighter. */
	int field() {
		return this == HIGH_CONTRAST ? 0xFF000000 : lerp(face, 0xFFFFFFFF, 0.35f);
	}

	/** Text on a title bar or a picked item. */
	int titleText() {
		return 0xFFFFFFFF;
	}

	static int lerp(int a, int b, float t) {
		int ar = a >> 16 & 0xFF;
		int ag = a >> 8 & 0xFF;
		int ab = a & 0xFF;
		int br = b >> 16 & 0xFF;
		int bg = b >> 8 & 0xFF;
		int bb = b & 0xFF;
		return 0xFF000000 | Math.round(ar + (br - ar) * t) << 16 | Math.round(ag + (bg - ag) * t) << 8 | Math.round(ab + (bb - ab) * t);
	}
}
