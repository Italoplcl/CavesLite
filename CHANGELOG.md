# Changelog

## 2.0.0-preview.1

This is a preview for review before the final 2.0 publication.

- Apply the approved sparse basic preset: disable alpha-spider, crying-bat, hexed-armor, lava-creeper, watcher and illusioner; enable the other 12 with selected weights. Basic replacement chance 10%, ordinary cooldown 120 seconds/cap 2; darkness/Mimic/giant 600 seconds/cap 1.
- Enable Herobrine at randomized 15–25 minute intervals per player with one active encounter per player. Nether remains opt-in. Export basic/performance presets without overwriting existing files.
- Use a monotonic Herobrine clock independent of world clocks; fix the four-second escape duration incorrectly forced to twenty seconds. Keep movement/head tracking and improve combat target/cap handling. Remove entities before optional disappearance effects.
- Group diagnostics under `/clite debug mobs` and `/clite debug herobrine`; retain old Herobrine syntax as an alias. Update help, completion and translated feedback.
- Give tagged giants independent Paper goals/navigation, full-body collision checks, melee/stomp/earthquake attacks, warnings, bounded reinforcements and customizable death loot. Do not alter global giant AI settings or terrain.
- Apply world-to-block-local collision coordinates in space checks and account for wide-entity path node centers. Reject giant summons when the body cannot fit.
- Ensure ore-triggered golems obey their enable switch, cooldown and cap; remove stale entity IDs from loaded active counts.
- Consolidate current ES/EN documentation, starter/performance guidance and QA instructions; move old notes and verifier sources out of the root. Archive unique legacy repository files before cleanup.

## Previous versions

Historical notes in [docs/history](docs/history/) are reference only. Current behavior is documented in the configuration and giant guides. The separately generated 1.12.5 package was not approved as a release; its timer correction is now explicitly incorporated into this authorized preview.
