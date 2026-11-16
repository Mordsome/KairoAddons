package dev.mordo.kairo.addons.animations;

import dev.mordo.kairo.api.AddonRegistrar;
import dev.mordo.kairo.api.KairoAddon;

/**
 * Kairo addon "Animations": plain Kairo keeps this part switched off and hidden unless this addon is installed (since
 * Kairo 0.4.0). The code itself is part of Kairo; this addon unlocks it.
 */
public final class Animations implements KairoAddon {
	@Override
	public void register(AddonRegistrar kairo) {
		kairo.unlock("animations");
	}
}
