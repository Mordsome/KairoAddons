# Kairo Addons

Official addons for [Kairo and Kairo Void](https://github.com/Mordsome/Kairo): small extras you add on top of the
client, like a different look for the menus. Free, open source, by Mordsome.

Every addon works on both editions unless it says otherwise, and any set of addons works together.

## Addons

| Addon | What it does |
|---|---|
| **Plastic Menus** | A solid, moulded-plastic look for every Kairo menu: no glass, soft 3D edges. Pick the plastic's colour, depth and gloss. |
| **Animations** | Plain Kairo only (0.4.0+): attack, mining and inspect animations, the swing trail and held item looks. |
| **Themes** | Four themes for all of Kairo: Glass, Minimal, Retro and Neon, on the main menu, the pause menu, the ring, every menu and control, and the HUD. Picked from pictures. Retro's frames come in Minecraft's purple, your colour, gold, green, red or aqua. Kairo 0.4.0+. |
| **Kairo 98** | All of Kairo as a late-90s desktop: grey bevelled windows with blue title bars, a teal desktop, a taskbar with a Start menu instead of the ring, and Minecraft's own inventories, hotbar, hearts and experience bar to match. Several colour schemes, two HUD box styles, choppy retro hit and mining animations and its own start-up screen, all in a Kairo 98 tab in Visuals. Doesn't go together with Themes, Plastic Menus or Command Bar. Kairo 0.4.0+. |
| **Command Bar** | Right Shift opens a search bar instead of the ring: your ring sections as big numbered tiles, and type to find any page, mod or setting. Kairo 0.4.0+. |
| **Combat Effects** | Plain Kairo only (0.4.0+): hit particles, kill effects, damage numbers, the edge flash and sound packs. |

Since Kairo 0.4.0, plain Kairo keeps its animations and combat effects switched off unless these two addons are
installed, so the client stays light for people who don't use them. Your old choices are kept and come back with the
addon. Kairo Void has all of it built in.

## Get them

**With the Kairo Launcher** (the easy way): open Addons in the
[Kairo Launcher](https://github.com/Mordsome/KairoLauncher), install the ones you want, choose whether each is for
Kairo, Kairo Void or both, and press Play. Turn them off or uninstall them there too.

**By hand:** download the addon's jar from the [latest release](https://github.com/Mordsome/KairoAddons/releases/latest)
and put it in your `mods` folder next to Kairo (0.3.0 or newer). To remove it, take it out again.

A theme you have just installed is switched on for you once. Change it any time in the game, by picture: Right Shift →
Visuals → **Theme** (Kairo Void: Visuals → Interface → Themes). The theme reaches the HUD too.

Some addons don't go together (Kairo 98 with Themes, Plastic Menus and Command Bar); the launcher won't turn on two at
once and says why.

In the Kairo Launcher addons are listed in groups: **Themes** (how Kairo looks: Plastic Menus, Themes, Kairo 98), **Effects**
(Animations, Combat Effects), **Fun** and **Tools** (Command Bar).

The Command Bar is chosen in Visuals → **Ring** → Right Shift opens (Kairo Void: Visuals → Interface → Menu style);
switch back to the ring there any time.

## Fair play and privacy

Addons follow Kairo's rules: visual and client-side only, no cheats, no automation, nothing sent anywhere. They can only
add things through the sockets Kairo provides, never change Kairo itself.

## For developers

How addons, Kairo and the launcher fit together is written down once, in **[SPEC.md](SPEC.md)**.

```bash
# 1. Put Kairo's jar (kairo-<version>.jar, from Mordsome/Kairo's releases) in libs/
# 2. Build every addon and the release folder (jars, icons, addons.json, SHA256SUMS.txt)
./gradlew catalogue        # result: build/release/
```

On Windows use `gradlew.bat`. Needs a JDK (17 or newer) to start Gradle; the Java 25 toolchain downloads by itself.

Each addon is one folder in `addons/`:

```
addons/plastic-menus/
  addon.json                      its id, name, version, texts, editions and what it needs
  icon.png                        256 x 256, shown by the launcher
  src/main/java/...               the code: one class implementing dev.mordo.kairo.api.KairoAddon
  src/main/resources/fabric.mod.json   filled in from addon.json when it is built
```

To try an addon in a Kairo development run, from Kairo's own source folder:

```bash
./gradlew runClient -Paddons=/path/to/kairo-addon-plastic-menus-1.0.0.jar
```

Releasing: push a tag `addons-<number>`. GitHub Actions builds everything and makes a draft release; check it and
publish it by hand. The launcher reads the newest published release.

## Licence

GPL-3.0, like Kairo. Copyright © 2026 Mordsome. See [LICENSE](LICENSE).

Not an official Minecraft product. Not approved by or associated with Mojang or Microsoft.
