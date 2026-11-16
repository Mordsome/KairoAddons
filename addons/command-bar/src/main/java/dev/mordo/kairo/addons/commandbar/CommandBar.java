package dev.mordo.kairo.addons.commandbar;

import dev.mordo.kairo.api.AddonRegistrar;
import dev.mordo.kairo.api.KairoAddon;

/** Kairo addon "Command Bar": one home menu, {@link CommandBarMenu}, that Right Shift can open instead of the ring. */
public final class CommandBar implements KairoAddon {
	@Override
	public void register(AddonRegistrar kairo) {
		kairo.homeMenu(new CommandBarMenu());
	}
}
