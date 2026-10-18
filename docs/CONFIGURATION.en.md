# CavesLite 2.0 preview configuration

This guide describes `2.0.0-preview.1`. The sparse starter preset replaces the previous all-disabled distribution policy. Use the [Spanish guide](CONFIGURATION.es.md) for the full Spanish reference.

## Installation and migration

Requires Paper/Purpur 26.3 and Java 25. Vault and PlaceholderAPI are optional. No client mod/resource pack is required; Folia is not supported.

Stop the server, back up `plugins/CavesLite/`, replace the previous JAR and restart. Do not use Bukkit's global reload. Existing configuration/language files are not overwritten. The starter preset is automatic only for a fresh installation. For an existing installation, back up your old config and copy the exported `plugins/CavesLite/presets/basic.yml` over `plugins/CavesLite/config.yml`, adapt world names, then use `/clite reload`. Source presets also live under `src/main/resources/presets/`.

## Approved starter roster

| Creature ID | Enabled | Weight |
|---|---|---|
| `alpha-spider` | No | 1 |
| `cave-golem` | Yes | 2 |
| `crying-bat` | No | 1 |
| `dead-miner` | Yes | 3 |
| `hexed-armor` | No | 1 |
| `hungering-darkness` | Yes | 1 |
| `lava-creeper` | No | 1 |
| `magma-monster` | Yes | 1 |
| `mimic` | Yes | 1 |
| `smoke-demon` | Yes | 1 |
| `tnt-creeper` | Yes | 3 |
| `watcher` | No | 1 |
| `giant-zombie` | Yes | 1 |
| `chicken-jockey` | Yes | 1 |
| `killer-bunny` | Yes | 1 |
| `illusioner` | No | 1 |
| `spider-jockey` | Yes | 2 |
| `skeleton-horseman` | Yes | 2 |

Herobrine is enabled separately: randomized 15–25 minute natural encounter attempts per player, with at most one active encounter per player. It does not share the other creatures' weight pool.

Basic settings: `mobs.try-chance: 10`; ordinary creature cooldown 120 seconds and cap 2 loaded primary entities per type across worlds. Darkness, Mimic and Giant use 600 seconds/cap 1. Mimic also has a per-world chest cap. Nether is OFF per creature, including Herobrine. Natural encounters exclude the End. Starter world name is `world`; an empty world list allows Overworld-type environments, not all dimensions.

## Enable and customize

As OP or with `caveslite.command`, use `/clite`, choose a creature, enable natural spawning, set positive selection weight and Save. Saving writes the config and reloads it immediately. White names indicate enabled encounters; gray struck-through names indicate disabled ones. Enabled does not guarantee a spawn: conditions/cooldowns/caps still apply. Giant dialogs include chase, attack, reinforcement and loot switches and selected numeric settings; all advanced options are available in YAML.

```yaml
mobs:
  alpha-spider:
    enabled: true
    nether-enabled: false
    priority: 1
    cooldown-seconds: 120
    max-active: 2
```

Merge the example into the complete config; do not replace the entire file with it. `enabled: false` disables natural participation while retaining its weight. Weight 0 excludes an otherwise enabled creature from the pool. Manual summons remain available for testing and are not constrained by natural probability/cooldown/caps; Giant summons require physical space and Herobrine summons obey the per-player encounter cap.

## Chance, weight and actual frequency

The common engine processes `NATURAL` events only, for `mobs.replace-mobs` entity types. World/light/chance checks precede weighted selection; the selected creature must then pass its own context/height/space checks, cooldown and cap. A failed selection leaves the original vanilla spawn; there is no reroll to another creature.

Weight 3 has three times the selection weight of weight 1 inside that pool. It is not 3%, nor a promise of three times the observed spawn rate. Ten percent is an attempt ceiling on compatible natural events, not ten percent of every mob on the server. Spawners, eggs and other-plugin spawns are not replaced. Ore-triggered golems and Giant reinforcements are separate bounded triggers.

`mobs.y-min/y-max` and per-creature `spawn-y-min/spawn-y-max` affect Overworld contexts. Giant uses its own 80–320 range, night 13000–23000 and sky exposure. Cave creatures keep their underground context. Global `max-light-level: 16` disables the extra global light check; creature-specific darkness checks still apply.

Common caps concern registered loaded primary entities; they are not a census of unloaded chunks or a replacement for vanilla mob caps. Stale IDs are purged and tagged entities re-register on load. Cooldowns are shared per type across worlds and reset when the plugin restarts. Mounted passengers remain tagged and may appear separately in diagnostics.

## Vanilla-style and performance profiles

The basic preset is the recommended sparse vanilla-style starting point. It does not change Paper/Purpur settings.

`presets/performance.yml` keeps the roster/weights but lowers the attempt chance to 5%, ordinary cooldowns to 240 seconds/cap 1, and Herobrine attempts to every 30–45 minutes. The three rare exceptions remain at 600 seconds/cap 1. Back up the current config, copy that preset to `config.yml`, adapt world names and reload to use it.

Server tuning can reduce available natural spawn events and entity activity through caps, spawn intervals and simulation/activation distances. CavesLite does not compensate by creating unlimited entities. The performance preset reduces custom encounters; it is not a fixed TPS improvement guarantee. Measure comparable workloads. Official references: [Paper world settings](https://docs.papermc.io/paper/reference/world-configuration/) and [Bukkit intervals/caps](https://docs.papermc.io/paper/reference/bukkit-configuration/). No external optimization guide is automatically applied.

## Nether and End

To opt a creature into Nether, allow its world name in `mobs.worlds`, enable the creature with positive priority and set its `nether-enabled: true`. The same replacement list and engine apply; add Nether entity types to the list if desired. Overworld height/cave/night/sky context is skipped there, but creature-specific requirements remain.

Physical space is checked; generated fortress/bastion structure bounds exclude replacements. Ruined portals are allowed. `mobs.nether.ruined-portal-radius` is the 3D distance to generated portal bounds; `giant-zombie-ruined-portal-weight-multiplier` affects only the Giant's relative weight nearby. One is neutral; zero removes its local weight. Neither enables a disabled creature or changes a zero base weight. Portal lookup only examines loaded chunks.

Herobrine retains separate world/height/encounter rules and has its own Nether switch, default OFF. The End excludes natural encounters. Administrative summons are separate from natural rules.

## Herobrine mechanics and diagnosis

Encounter types: `observe`, `flee-on-sight`, `behind`, `false-chase`, `outside-observer`, `watcher`, `lurking`, `stalking`, `creeping`. Placement requires loaded safe positions and its local protected-structure checks. Failed placement retries later, so the interval schedules attempts, not guaranteed appearances.

Combat begins when attacked by a player; the attacker becomes its target subject to the per-player cap. It keeps facing the target and follows ground routes through accessible elevation changes. It does not climb solid vertical walls or move through blocks. A monotonic elapsed clock drives attack cooldowns, encounters and expiry independently of daylight/world clock changes.

Defaults: 2 base damage points (one heart before reductions), 24 ticks between attacks, up to 35 seconds combat followed by 4 seconds escape. Low health, distance or target loss can end the encounter earlier. A blocked escape route does not extend its deadline. The scheduler checks every four server ticks; stalled server ticks delay execution until the server resumes. Additional knockback is applied only after an actual health/absorption decrease. Armor, shields, absorption, creative mode and protection plugins retain normal effects.

`/clite debug herobrine [player]` shows combat/flee flags, elapsed seconds and requested/applied damage (`dealt` includes absorption). It does not override god mode or protections. The old `/clite herobrine debug` spelling remains an alias, while completion/help use the grouped command.

Outside combat, `Stop.` is attempted at 0.5% per check only while watched with line of sight. Continuous observation may cause disappearance after 8 seconds; stalking/watcher types retain a 45-second maximum idle lifetime. Redstone hints, sneaky strike and sound settings remain configurable. Third-party health bars may still call the entity Mannequin; that compatibility change has not been made.

## Creature-specific controls

| ID | Existing mechanics / relevant settings |
|---|---|
| `alpha-spider` | Web and minion chances: `cobweb-chance`, `minion-chance`. |
| `cave-golem` | Mineral helmet, combat effects, pickaxe/non-pickaxe damage modifiers and variants. Ore release now respects enable/cooldown/cap and is Overworld-only. |
| `crying-bat` | Scream chance and death-after-scream chance. |
| `dead-miner` | Mining equipment, damage-triggered drops and torches in darkness; target/torch/drop/head settings. |
| `hexed-armor` | Equipment swap chance and optional binding curse. |
| `hungering-darkness` | Total-darkness spawning and light-related targeting/damage. Existing default damage remains 200: rare is not synonymous with weak. |
| `lava-creeper` | Explosion/terrain/lava effects, initially disabled. |
| `magma-monster` | Burning zombie with fire/magma effects and target/extinguish options. |
| `mimic` | Chest disguise/opening encounter; chest spacing, count, lifetime and unload cleanup. |
| `smoke-demon` | Invisible cloud applying blindness/wither in darkness; light, radius and particle. |
| `tnt-creeper` | Additional TNT on explosion and damage-triggered blast chance. Existing TNT can affect terrain; the new no-terrain-destruction guarantee concerns Giant abilities. |
| `watcher` | Custom-headed creature, separate from Herobrine's watcher encounter type. |
| `giant-zombie` | [Complete Giant guide](GIANT.en.md). |
| `chicken-jockey` | Zombie riding a chicken with configurable speed amplifier. |
| `killer-bunny` | Hostile Killer Bunny variant with permanent Speed II, default health 12. |
| `illusioner` | Server Illusioner behavior. |
| `spider-jockey` | Skeleton riding a spider, based on normal entity behavior. |
| `skeleton-horseman` | Skeleton on a skeleton horse; configured health 30 applies to the horse. No vanilla horse trap or added special attacks. |

`name`/`show-name` configure entity labels. `extra-drops` adds item materials; `death-command` optionally runs a console command when a player killed the entity. Placeholders: `%player%`, `%mob%`, `%world%`, `%x%`, `%y%`, `%z%` (also `{player}`). Giant also has its own quantity/chance loot table. Compound encounters tag primary and passenger entities.

## Other systems

`caverns.ambient` controls weighted sounds/depth scaling; `footsteps` controls ghost footsteps. `tension` controls heartbeat. `boss-bar`, `spawn-titles`, `warnings` and `trails` have their own toggles, creature lists and radius/settings. `achievements` controls kill milestones, sound/broadcast and optional Vault payments; `leaderboards` controls periodically updated PlaceholderAPI rankings. Enabling these features does not enable disabled creatures.

Placeholders include `%caveslite_kills%`, `%caveslite_kills_<mob>%`, `%caveslite_next_threshold%`, `%caveslite_last_achievement%`, `%caveslite_top_kills_1%`, `%caveslite_top_kills_1_name%`, `%caveslite_top_kills_<mob>_1%` and its `_name` variant. Vault without an economy provider is skipped.

## Languages and dialogs

Select `language: es` or `en`. Both files are exported without overwriting edits. Per-key fallback: user's selected file → bundled selected language → user Spanish → bundled Spanish → bundled English. Unknown codes fall back; unsafe path names are rejected.

Display values can use `@lang:<key>`. Stock 1.12.1 text is recognized in memory; custom text is preserved. IDs/materials/commands/gameplay values are not translated. Saving an unchanged name preserves its language reference. Dialog rendering applies white/grays even to old/RGB custom text; active names are white, disabled names gray struck-through. Old active-prefix/inactive-prefix keys are obsolete. Item icons/client backgrounds retain native appearance.

## Commands

See the complete quick-command table in the README. `/caveslite` aliases `/clite`; `spawn` aliases `summon`, `option`/`opciones` alias `options`, and `r` aliases `reload`. Console requires a target player for Herobrine/kills and explicit world coordinates for mob spawning. Dialogs require a player. `/clite kill [mob]` removes recognized CavesLite entities/artifacts; it is not a generic vanilla kill command. Follow the [QA checklist](TESTING.md) before approving the preview.
