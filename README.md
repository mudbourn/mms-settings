# mms-settings

Client-side Fabric mod for Minecraft 1.21.11. One "Modpack Settings" keybind
(unbound by default) replaces the separate config keybinds of the pack's mods.

## Hub

A grid of shortcuts. Each tile appears only when its mod is loaded and opens
with the hub as the parent screen. A screen is taken from the mod's Mod Menu
config factory first, then from the mod's own screen class by reflection.

- Open Parties and Claims: grouped, searchable view of the player configs
- Simple Voice Chat settings
- Xaero's Minimap settings and the waypoints menu (waypoints need a world)
- Better Combat settings
- Iris shader pack menu (Shaders)
- Distant Horizons settings
- MMS Economy, when it exposes a Mod Menu config screen
- Controls, Music and Sounds, Video Settings

"All mods" lists every Mod Menu config screen and the files in `config/`, with
search. The hub is also the Mod Menu config screen of this mod.

## Speedometer

`/speedometer` toggles a blocks-per-second readout beside the offhand slot,
on the side away from the main hand. It averages the last half second of
movement, vertical included.

## Open Parties and Claims view

- Configs: my, party, default, server, wilderness, expired, plus sub-configs.
- Options sit in folders that follow the option id, opened one page at a time
  with a Back button and a path line. Labels use OPAC's own option names.
- Settings with Players, Mobs and Other versions sit on their own "By Players /
  Mobs / Other" page in each folder. One switch at the top picks the category
  and every row shows and edits that category's value.
- Search lists every matching option on one page, by name or id.
- Comments show as tooltips; options the player cannot change are greyed out.
- "Edit in OPAC" opens OPAC's screen for the current config (player groups,
  new sub-configs). "OPAC main menu" opens OPAC's main menu, which holds the
  claiming mode (claim as yourself or as the server).
- Changes go through OPAC's own client sync path (`setCastValue` then
  `PlayerConfigClientSynchronizer.syncToServer`), resolved by reflection in
  `OpacWriter`. If that fails the failure is logged once, a notice is shown and
  "Edit in OPAC" stays available.
- An "MMS view" button is added to OPAC's main config menu through an optional
  mixin that is skipped when OPAC is absent.

Pinned to Open Parties and Claims 0.33.x (`breaks` outside that range).

## Build

    ./gradlew build

The jar is written to `build/libs/mms-settings-<version>.jar`. The OPAC jar for
compiling lives in `libs/` (not committed).

## Phase 2

TODO: Xaero's Minimap and World Map options as grouped screens like the OPAC
view. Their settings are an internal `ModOptions` enum shown by `GuiSettings`,
read and written through `ModSettings`; pin to Minimap 26.6.2 and World Map
1.47.2. Until then the hub opens Xaero's own screens.
