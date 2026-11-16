# Kairo Addons: the contract (version 1)

This is the one description of how Kairo addons work that **Kairo**, the **Kairo Launcher** and this repository all
follow. If two of them disagree, this file is right and the code is wrong.

An **addon** is a small separate mod that adds things to Kairo and Kairo Void (a menu look, mods, HUD elements) through
sockets Kairo provides. The launcher lists the official addons, installs them, turns them on and off per edition and
starts the game with the chosen ones. Addons can also be installed by hand.

Words used below: **MUST** is a rule; **SHOULD** is the recommended way, with room for a better one that gives the same
result.

## 1. Who does what

| Part | Its job |
|---|---|
| **Kairo / Kairo Void** (`Mordsome/Kairo`) | Provides the sockets (`dev.mordo.kairo.api`), loads every addon Fabric hands it, and stays working whatever an addon does. Knows nothing about the launcher. |
| **This repository** (`Mordsome/KairoAddons`) | Holds the source of every official addon and publishes them as GitHub releases with a catalogue. |
| **Kairo Launcher** (`Mordsome/KairoLauncher`) | Reads the catalogue, downloads and verifies addons, remembers the player's choices, and starts the game with the right ones. Never runs or reads addon code. |

Only Mordsome publishes addons here. Other people can write addons with the same sockets and share the jar themselves
(it installs by hand, see 6), but nothing gets into the launcher's list except through this repository's releases.

## 2. What an addon is

A Fabric mod jar, built from this repository, that:

- has the mod id `kairo-addon-<id>`, where `<id>` is the addon's id: lower-case letters, digits and dashes, starting
  with a letter or digit, at most 40 characters (`plastic-menus`);
- is named `kairo-addon-<id>-<version>.jar`, with a `MAJOR.MINOR.PATCH` version;
- depends on `kairo` (both editions have that mod id) and on the Minecraft version it was built for;
- names its entry class under the `kairo:addon` entrypoint, and says which sockets version it needs and which editions
  it is for:

```json
"entrypoints": { "kairo:addon": ["dev.mordo.kairo.addons.plastic.PlasticMenus"] },
"depends": { "fabricloader": ">=0.20.1", "minecraft": "~26.3", "kairo": ">=0.3.0" },
"custom": { "kairo:addon": { "api": 1, "editions": ["kairo", "void"], "conflicts": [] } }
```

`conflicts` (optional, since Kairo 0.4.0) lists the ids of addons this one doesn't go together with (Kairo 98 lists
Themes, Plastic Menus and Command Bar: they would fight over the same look and menu). A clash counts both ways. If two
clashing addons end up in the game anyway, **the one that lists the other wins** and the other isn't loaded (a line in
the log says why).

Rules for every addon (MUST):

1. **Only add.** An addon registers new things through the sockets. It never replaces, patches or reaches into Kairo's
   or another addon's code (no mixins into `dev.mordo.kairo`, no reflection on it). A purely visual mixin into
   Minecraft's own rendering is allowed (Kairo 98's choppy hand animation is one); it must chain with others (no
   `@Overwrite`, no `@Redirect` on calls Kairo might change too).
2. **Unique names.** Everything it registers (looks, mods, settings) has an id starting with its own addon id, so two
   addons can never collide.
3. **Kairo's rules.** Visual and client-side only: no cheats, no automation, no packets, nothing sent anywhere, no
   downloads. Respect Fewer flashes and Reduce motion.
4. **No bundled mods**, and nothing an addon needs except Kairo and Minecraft.

These rules are what makes any set of addons work together: each one only adds entries to lists, and where only one
entry can be in use at a time (the menu look), Kairo shows a chooser.

## 3. The sockets (inside Kairo)

Package `dev.mordo.kairo.api`, version **1** (`Addons.API_VERSION`), in Kairo 0.3.0 and later:

| Socket | What it adds |
|---|---|
| `AddonRegistrar.menuLook(MenuLook)` | A look for the menus: how panels, ring segments and the backdrop are drawn, and since Kairo 0.4.0 every control too (buttons, cards, fields, tabs, popups, toasts, Minecraft's own buttons). The player picks one in Visuals; a look seen for the first time is picked once. A look can have its own options. Since Kairo 0.4.0 a look may also ask for a text colour (`textColor`, for light surfaces), draw a desktop behind the main menu (`desktop`), and repaint Minecraft's own GUI images (`repaints`, `repaint`): Kairo reads each image from the game, the look changes its pixels, and Kairo keeps the result in a resource pack it writes itself, so no Mojang image is ever shipped in an addon. Inventory glass and the Hotbar mod step aside while such a look is in use. A look can also have window buttons (`windowButton`: Kairo closes the menu or makes it fill the screen), and its own loading screen (`loadingBackground`, `loading`; drawn with plain filled rectangles only, since the first loading screen comes before any shaders or fonts). A change of the look's colours that repaints the same images is written straight to the GPU, without a reload. |
| `AddonRegistrar.hudLook(HudLook)` | Kairo 0.4.0 and later. A look for the HUD: how the box behind each HUD mod and each key of Keystrokes is drawn (only the box; text and icons stay the mod's own). A menu look and a HUD look with the **same id** are one **theme**: Visuals → Theme shows it as one picture (drawn live by the look itself) and picking it sets both. One object may implement both interfaces; its options are then saved and listed once. A look seen for the first time is picked once. |
| `AddonRegistrar.homeMenu(HomeMenu)` | Kairo 0.4.0 and later. A different home for the menu: what Right Shift opens instead of the ring, and where "Back" in every page leads. Kairo gives it the player's ring sections and a search over pages, mods and mod settings (`HomeMenu.Context`), and runs what it picks; the menu only draws and handles input. The player picks it in Visuals → Ring (Right Shift opens); one seen for the first time is picked once. If it throws, the ring takes over until the game restarts. |
| `AddonRegistrar.visualsTab(String, Icon, List<Setting>)` | Kairo 0.4.0 and later. A tab of the addon's own in Visuals (a card under Interface in Kairo Void) with its options; Kairo saves them. |
| `AddonRegistrar.mod(Module)` | A mod in the Mods list (a `Module`, or a `HudElement` for the HUD), with its own settings. |
| `AddonRegistrar.unlock(String)` | Kairo 0.4.0 and later. Turns on a part of plain Kairo that only comes with an addon: `animations` (attack, mining and inspect animations, swing trail, held item looks) or `combat-effects` (hit particles, kill effects, damage numbers, edge flash, sound packs). Without its addon that part's settings are locked to "off" and hidden; the player's own choices are kept. Kairo Void always has every part. |

Kairo calls each addon's `KairoAddon.register(AddonRegistrar)` once at start-up, after its own mods are registered and
before settings are loaded. Addons draw with Kairo's own helpers (`render.Shapes`, `render.Colors`, `gui.Theme`,
`core.settings.*`); these count as part of the sockets. A look may build on Kairo's own glass with
`Shapes.kairoGlass`, `Shapes.kairoGlassSector` and `Shapes.kairoBackdrop` (0.4.0 and later), which never call a look.

How Kairo protects itself (MUST, already built):

- An addon whose `api` is higher than Kairo's is not loaded (a line in the log says why). The same for an addon whose
  `editions` leaves out the edition that is running.
- An addon that throws while registering is skipped; a menu or HUD look (or a home menu) that throws is switched off
  until the game restarts, and Kairo's own look (or the ring) takes over.
- A saved choice whose addon is gone behaves like the default. Removing an addon never breaks the settings.

**The sockets version** goes up only when an addon made for the old version could break. New sockets that old addons
don't need do not raise it (`unlock`, `hudLook` and `homeMenu` came in Kairo 0.4.0 with the sockets still at version 1: an addon that uses it
says `"kairo": ">=0.4.0"`, which keeps it off older Kairo). More sockets (ring styles, main menu styles, Visuals cards) will be added as addons need
them.

## 4. Releases of this repository: the catalogue

Every release of `Mordsome/KairoAddons` holds **every** current addon, so the newest release is always the whole
catalogue. `./gradlew catalogue` builds its files into `build/release/`:

| File | What it is |
|---|---|
| `addons.json` | The catalogue: the list below. |
| `kairo-addon-<id>-<version>.jar` | One per addon. |
| `<id>.png` | The addon's icon, 256 x 256 (optional). |
| `SHA256SUMS.txt` | `sha256sum` lines for every other file in the release, `addons.json` included. |
| `SHA256SUMS.txt.sig` | Optional: Mordsome's signature over `SHA256SUMS.txt`, the same scheme Kairo's releases use. |

`addons.json`:

```json
{
  "schema": 1,
  "addons": [
    {
      "id": "plastic-menus",
      "name": "Plastic Menus",
      "summary": "One line for the list.",
      "description": "A few sentences for the addon's own page.",
      "version": "1.0.0",
      "file": "kairo-addon-plastic-menus-1.0.0.jar",
      "sha256": "<64 hex characters>",
      "size": 30732,
      "icon": "plastic-menus.png",
      "editions": ["kairo", "void"],
      "api": 1,
      "kairo": ">=0.3.0",
      "minecraft": "~26.3",
      "group": "themes",
      "tags": ["menus", "look"]
    }
  ]
}
```

- `editions`: the editions the addon works on (`kairo`, `void` or both).
- `api`: the sockets version it needs. `kairo` and `minecraft`: version ranges in Fabric's notation, the same ones as in
  the jar's `fabric.mod.json`.
- `group`: which group the addon is listed under: `themes` (how Kairo looks), `effects` (animations and effects),
  `fun` (playful extras) or `tools` (useful helpers). A reader that doesn't know a group lists it under "Other"; a
  missing group means `other`. Groups are shown in this order: Themes, Effects, Fun, Tools, Other.
- `conflicts`: ids of addons this one doesn't go together with (see 2). Counts both ways. May be missing (none).
- `icon`, `group`, `conflicts` and `tags` may be missing. Unknown fields MUST be ignored, so the catalogue can grow.
- A catalogue with a `schema` the reader doesn't know MUST be treated as "no catalogue" (keep what is installed, offer
  nothing new).

Release tags are `addons-<number>` (`addons-1`, `addons-2`, ...). Drafts and pre-releases are not catalogues.

## 5. What the launcher does

### Reading the catalogue (MUST)

1. Take the newest release of `Mordsome/KairoAddons` that is not a draft or pre-release and has `addons.json` and
   `SHA256SUMS.txt`, both downloaded only from `https://github.com/Mordsome/KairoAddons/releases/download/`.
2. If the release has `SHA256SUMS.txt.sig`, it must verify against Mordsome's release key (and if the launcher is set
   to require signatures, a release without one is skipped), exactly as for Kairo's releases.
3. `addons.json` must match its line in `SHA256SUMS.txt`. Every addon's `file` must be in `SHA256SUMS.txt` with the
   same hash as its `sha256` field; an addon that isn't is left out of the list.
4. Treat the catalogue as untrusted input: check every field's type and length, accept only ids and file names of the
   shapes in 2, and never use a name from it as a path without that check.
5. When GitHub can't be reached, use the last catalogue that verified (the launcher already does this for Kairo).

### Installing, updating, removing (MUST)

- **Install** downloads the jar from the same release, checks its SHA-256 and that it is a real jar, and keeps it in
  the launcher's own data folder (SHOULD: `addons/kairo-addon-<id>-<version>.jar`), not inside any install.
- **Update**: when the catalogue has a newer version of an installed addon, download it the same way and delete the old
  jar once the new one has verified. SHOULD happen together with "Keep Kairo updated".
- **Uninstall** deletes the jar and the launcher's record of it. Options the player set inside the game stay in that
  install's Kairo settings and are simply unused; they come back if the addon is installed again.
- An installed addon that has left the catalogue stays installed and usable, marked as no longer listed.

### The player's choices (MUST)

Addons are chosen **once for the whole launcher**, not per install, with an edition choice for each:

| Choice for an installed addon | Loaded when playing |
|---|---|
| Kairo and Kairo Void | Any Kairo or Kairo Void install |
| Kairo only | Kairo installs |
| Kairo Void only | Kairo Void installs |
| Off | Nothing (the jar stays downloaded) |

A choice the addon's `editions` doesn't allow is not offered.

**Clashing addons (MUST):** two addons where either lists the other in `conflicts` are never both loaded into the same
install. When the player turns one on (or widens its editions) while a clashing one is on for an edition they share,
the launcher says which addons clash and why, and lets the player keep one: the other is turned off for those
editions (SHOULD: one dialog with both names and a "Keep X" / "Keep Y" choice). The Addons place SHOULD show a clash
on each addon's page ("Doesn't go together with ..."). SHOULD: the launcher's state is one file in its data
folder, `addons.json`:

```json
{ "schema": 1, "addons": { "plastic-menus": { "version": "1.0.0", "file": "kairo-addon-plastic-menus-1.0.0.jar",
  "sha256": "<hex>", "kairo": true, "void": false } } }
```

### Starting the game (MUST)

When a **Kairo or Kairo Void install** is played, the game must load exactly these addon jars and no others: installed,
switched on for that install's edition, and **compatible** with it. Compatible means all of:

- the addon's `editions` includes the install's edition;
- the install's Kairo version satisfies the addon's `kairo` range;
- the install's Minecraft version satisfies its `minecraft` range;
- the addon's `api` is not higher than the sockets version of that Kairo (Kairo 0.3.x has 1; when in doubt, the `kairo`
  range decides).

An addon that is on but not compatible is not loaded, and the launcher says why in plain words ("needs Kairo 0.4 or
newer").

SHOULD: load them with Fabric Loader's `fabric.addMods` system property, which takes extra mod jars from anywhere:

```
-Dfabric.addMods=<path to jar 1><separator><path to jar 2>...
```

The separator is the platform's path separator (`;` on Windows, `:` on Linux and macOS). This keeps addons out of the
installs' `mods` folders, so on/off never moves a file, every install shares one copy, and an install's folder stays
exactly what the player put there. Copying jars into `mods/` gives the same result and is allowed, as long as turning an
addon off or uninstalling it leaves nothing behind.

Also (MUST):

- If an addon with the same mod id is already in the install's own `mods` folder (the player put it there by hand), do
  not pass the launcher's copy as well: Fabric refuses two mods with one id.
- Addons are never loaded into installs that aren't Kairo or Kairo Void.
- The launcher never executes, unpacks or reads code from an addon jar. It may read `fabric.mod.json` from it, as it
  does for other mods.

### What the launcher shows (SHOULD)

An Addons place with every catalogue entry (icon, name, summary, version, which editions it is for), listed under its
`group` (a heading per group, in the order above; empty groups not shown), and for each:
Install, the edition choice above, Uninstall, and "Update available". Changes apply the next time the game starts, and
the launcher says so while the game is running.

## 6. Restarting the game (Kairo 0.4.0 and later)

Kairo's Settings → About has **Restart the game**. A game can't start itself again cleanly, so the launcher does it:

- When the launcher starts a **Kairo or Kairo Void install**, it SHOULD pass
  `-Dkairo.launcher.restart=<path>`: a file path inside the launcher's own data folder, unique to this launch, that
  doesn't exist yet. Kairo then shows "Restart the game"; without the property it shows "Close the game".
- To restart, Kairo saves its settings, writes the text `restart` into that file and closes the game normally. It
  never writes anything else, anywhere else.
- When that game process has exited, the launcher MUST check the file. If it exists: delete it, then start the **same
  install again** (same account, same addons, same options), exactly as if the player had pressed Play. If it doesn't
  exist, nothing happens. A crash is never treated as a restart.
- The launcher MUST delete the file before each launch, and restart at most once per request (the file is gone after
  the check), so a game can never restart in a loop. It MUST ignore the file if its content isn't `restart`.

## 7. Installing by hand

Without the launcher: download the addon's jar from this repository's
[releases](https://github.com/Mordsome/KairoAddons/releases) and put it in the `mods` folder next to Kairo. To switch it
off, take it out again.

## 8. Changing this contract

- Things that old readers can ignore (new optional fields, new sockets, new tags) need no version change.
- A change to `addons.json` that old launchers would misread raises its `schema`.
- A change to the sockets that could break old addons raises `api`, and Kairo keeps refusing addons that ask for more
  than it has.
