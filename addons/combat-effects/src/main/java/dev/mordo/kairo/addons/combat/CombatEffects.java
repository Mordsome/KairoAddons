package dev.mordo.kairo.addons.combat;

import dev.mordo.kairo.api.AddonRegistrar;
import dev.mordo.kairo.api.KairoAddon;

/**
 * Kairo addon "Combat Effects": plain Kairo keeps this part switched off and hidden unless this addon is installed (since
 * Kairo 0.4.0). The code itself is part of Kairo; this addon unlocks it.
 */
public final class CombatEffects implements KairoAddon {
	@Override
	public void register(AddonRegistrar kairo) {
		kairo.unlock("combat-effects");
	}
}
