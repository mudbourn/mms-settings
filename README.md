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
- MMS Economy, when it exposes a Mod Menu config screen
- Controls, Music and Sounds, Video Settings

"All mods" lists every Mod Menu config screen and the files in `config/`, with
search. The hub is also the Mod Menu config screen of this mod.

## Open Parties and Claims view

- Configs: my, party, default, server, wilderness, expired, plus sub-configs.
- Options are grouped into collapsible sections by id prefix, or sorted A-Z.
- Option families named "X by Players / Mobs / Other" show as one row per
  setting with Players, Mobs and Other buttons. Each opens a page with that
  setting's options for the category and a Back button. Search shows them inline.
- Search matches the translated name and the option id.
- Comments show as tooltips; options the player cannot change are greyed out.
- Changes go through OPAC's own client sync path (`setCastValue` then
  `PlayerConfigClientSynchronizer.syncToServer`), resolved by reflection in
  `OpacWriter`. If that fails the failure is logged once, a notice is shown, and
  the "Open OPAC screen" button opens OPAC's original screen.
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
