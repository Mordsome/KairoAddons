package dev.mordo.kairo.addons.plastic;

import dev.mordo.kairo.api.AddonRegistrar;
import dev.mordo.kairo.api.KairoAddon;

/** Kairo addon "Plastic Menus": adds one menu look, {@link PlasticLook}. */
public final class PlasticMenus implements KairoAddon {
	@Override
	public void register(AddonRegistrar kairo) {
		kairo.menuLook(new PlasticLook());
	}
}
