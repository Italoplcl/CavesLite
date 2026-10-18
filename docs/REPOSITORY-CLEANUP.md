# Repository cleanup audit

Baseline: `2093a96d989e1b1d6fb514910f577c4ab6ebabf7` (main before this preview).

The reviewed 1.12.4 ZIP matches all remote `src/` files after allowing line-ending normalization. No divergent canonical source changes were discarded. There were 50 loose Java files in the root: 32 matched their original canonical source by Git blob SHA; 18 had older/different content or were verification mains. The latter are archived, not compiled. A total of 37 old root text files are preserved under `docs/history/repository-root/`.

The new root contains README, LICENSE, CHANGELOG, Gradle configuration and .gitignore; code/resources, current guides, history, verification tools and assets have their own directories. Historical compiled JARs leave the current tree and remain recoverable in the parent Git history. New binaries are release/CI artifacts, not tracked source.

| Previous root file | Disposition | Reference |
|---|---|---|
| `AdminDialogs.java` | Archived as reference | `history/repository-root/AdminDialogs.java.txt` |
| `AlphaSpider.java` | Duplicate removed; original canonical source retained in Git history | `src/main/java/dev/caveslite/mobs/defaults/AlphaSpider.java` |
| `AmbientSounds.java` | Duplicate removed; original canonical source retained in Git history | `src/main/java/dev/caveslite/AmbientSounds.java` |
| `CaveGolem.java` | Archived as reference | `history/repository-root/CaveGolem.java.txt` |
| `CavesCommand.java` | Archived as reference | `history/repository-root/CavesCommand.java.txt` |
| `CavesLite.java` | Archived as reference | `history/repository-root/CavesLite.java.txt` |
| `CavesPlaceholders.java` | Archived as reference | `history/repository-root/CavesPlaceholders.java.txt` |
| `ChickenJockeyAnomaly.java` | Duplicate removed; original canonical source retained in Git history | `src/main/java/dev/caveslite/mobs/defaults/ChickenJockeyAnomaly.java` |
| `CryingBat.java` | Duplicate removed; original canonical source retained in Git history | `src/main/java/dev/caveslite/mobs/defaults/CryingBat.java` |
| `CustomMob.java` | Archived as reference | `history/repository-root/CustomMob.java.txt` |
| `DeadMiner.java` | Duplicate removed; original canonical source retained in Git history | `src/main/java/dev/caveslite/mobs/defaults/DeadMiner.java` |
| `Footsteps.java` | Duplicate removed; original canonical source retained in Git history | `src/main/java/dev/caveslite/Footsteps.java` |
| `GiantZombie.java` | Duplicate removed; original canonical source retained in Git history | `src/main/java/dev/caveslite/mobs/defaults/GiantZombie.java` |
| `HerobrineEncounter.java` | Archived as reference | `history/repository-root/HerobrineEncounter.java.txt` |
| `HexedArmor.java` | Duplicate removed; original canonical source retained in Git history | `src/main/java/dev/caveslite/mobs/defaults/HexedArmor.java` |
| `HungeringDarkness.java` | Duplicate removed; original canonical source retained in Git history | `src/main/java/dev/caveslite/mobs/defaults/HungeringDarkness.java` |
| `IllusionerAnomaly.java` | Duplicate removed; original canonical source retained in Git history | `src/main/java/dev/caveslite/mobs/defaults/IllusionerAnomaly.java` |
| `KillLeaderboard.java` | Duplicate removed; original canonical source retained in Git history | `src/main/java/dev/caveslite/KillLeaderboard.java` |
| `KillerBunny.java` | Duplicate removed; original canonical source retained in Git history | `src/main/java/dev/caveslite/mobs/defaults/KillerBunny.java` |
| `Lang.java` | Archived as reference | `history/repository-root/Lang.java.txt` |
| `LavaCreeper.java` | Duplicate removed; original canonical source retained in Git history | `src/main/java/dev/caveslite/mobs/defaults/LavaCreeper.java` |
| `Locations.java` | Duplicate removed; original canonical source retained in Git history | `src/main/java/dev/caveslite/util/Locations.java` |
| `MagmaMonster.java` | Duplicate removed; original canonical source retained in Git history | `src/main/java/dev/caveslite/mobs/defaults/MagmaMonster.java` |
| `Materials.java` | Duplicate removed; original canonical source retained in Git history | `src/main/java/dev/caveslite/util/Materials.java` |
| `Mimic.java` | Duplicate removed; original canonical source retained in Git history | `src/main/java/dev/caveslite/mobs/defaults/Mimic.java` |
| `MobAchievements.java` | Archived as reference | `history/repository-root/MobAchievements.java.txt` |
| `MobBase.java` | Duplicate removed; original canonical source retained in Git history | `src/main/java/dev/caveslite/mobs/MobBase.java` |
| `MobBossBar.java` | Archived as reference | `history/repository-root/MobBossBar.java.txt` |
| `MobManager.java` | Archived as reference | `history/repository-root/MobManager.java.txt` |
| `MobSpawnTitles.java` | Archived as reference | `history/repository-root/MobSpawnTitles.java.txt` |
| `MobTrails.java` | Duplicate removed; original canonical source retained in Git history | `src/main/java/dev/caveslite/MobTrails.java` |
| `MobWarnings.java` | Duplicate removed; original canonical source retained in Git history | `src/main/java/dev/caveslite/MobWarnings.java` |
| `Rng.java` | Duplicate removed; original canonical source retained in Git history | `src/main/java/dev/caveslite/util/Rng.java` |
| `SkeletonHorsemanAnomaly.java` | Duplicate removed; original canonical source retained in Git history | `src/main/java/dev/caveslite/mobs/defaults/SkeletonHorsemanAnomaly.java` |
| `SmokeDemon.java` | Duplicate removed; original canonical source retained in Git history | `src/main/java/dev/caveslite/mobs/defaults/SmokeDemon.java` |
| `Sounds.java` | Duplicate removed; original canonical source retained in Git history | `src/main/java/dev/caveslite/util/Sounds.java` |
| `SpiderJockeyAnomaly.java` | Duplicate removed; original canonical source retained in Git history | `src/main/java/dev/caveslite/mobs/defaults/SpiderJockeyAnomaly.java` |
| `TNTCreeper.java` | Duplicate removed; original canonical source retained in Git history | `src/main/java/dev/caveslite/mobs/defaults/TNTCreeper.java` |
| `TagHelper.java` | Archived as reference | `history/repository-root/TagHelper.java.txt` |
| `Tension.java` | Duplicate removed; original canonical source retained in Git history | `src/main/java/dev/caveslite/Tension.java` |
| `Text.java` | Duplicate removed; original canonical source retained in Git history | `src/main/java/dev/caveslite/util/Text.java` |
| `Utils.java` | Duplicate removed; original canonical source retained in Git history | `src/main/java/dev/caveslite/util/Utils.java` |
| `VaultEconomy.java` | Archived as reference | `history/repository-root/VaultEconomy.java.txt` |
| `VerifyDialogPalette.java` | Archived as reference | `history/repository-root/VerifyDialogPalette.java.txt` |
| `VerifyHerobrineRoute.java` | Archived as reference | `history/repository-root/VerifyHerobrineRoute.java.txt` |
| `VerifyNetherSpace.java` | Archived as reference | `history/repository-root/VerifyNetherSpace.java.txt` |
| `VerifyRelease.java` | Archived as reference | `history/repository-root/VerifyRelease.java.txt` |
| `Watcher.java` | Duplicate removed; original canonical source retained in Git history | `src/main/java/dev/caveslite/mobs/defaults/Watcher.java` |
| `WeightedPool.java` | Duplicate removed; original canonical source retained in Git history | `src/main/java/dev/caveslite/util/WeightedPool.java` |
| `WorldFilter.java` | Duplicate removed; original canonical source retained in Git history | `src/main/java/dev/caveslite/util/WorldFilter.java` |

## Original archived blob IDs

- `docs/history/repository-root/AdminDialogs.java.txt`: `7416f1728324f24a3b20bae3e2e3df3c71a520df`
- `docs/history/repository-root/CaveGolem.java.txt`: `fa17c979a0070628013de354d8cb6c3c23b2454a`
- `docs/history/repository-root/CavesCommand.java.txt`: `57b8c7b730694f60a77629afa8f37fbdbbf76563`
- `docs/history/repository-root/CavesLite.java.txt`: `58895f47585686c7fd3f24f049b3ac6140ff5bbe`
- `docs/history/repository-root/CavesPlaceholders.java.txt`: `9c7abafd573525fd672c79bbf61d71c1f70d46bb`
- `docs/history/repository-root/CustomMob.java.txt`: `ac0a02e1e10bbf1477c17051a275d5d2c435cf7a`
- `docs/history/repository-root/DOCUMENTACION.md`: `db62b1ac164d0264bab1c9fbcf98462eb051fe14`
- `docs/history/repository-root/DOCUMENTATION.md`: `6a785eeacc126bf334630b9ee9bf416185e28c94`
- `docs/history/repository-root/HEROBRINE-1.9.0.md`: `09a7cc6f791da464e715666e8ba5018b419fd189`
- `docs/history/repository-root/HEROBRINE-1.9.1.md`: `c50980be3f045fe617c97508da301443ad991a0a`
- `docs/history/repository-root/HerobrineEncounter.java.txt`: `15947a0b32cfefba78a213528cf26c8f86dd1a1d`
- `docs/history/repository-root/Lang.java.txt`: `f67094fb59b0029dc8d74c0ab708a32087437db9`
- `docs/history/repository-root/MobAchievements.java.txt`: `057a1429989cace22fd799fcd796979044b591b3`
- `docs/history/repository-root/MobBossBar.java.txt`: `0625215a8c0794a18e1982b3dea091a97862cc6f`
- `docs/history/repository-root/MobManager.java.txt`: `fc3d10e2cb8c639ac9a05ab5e1ea25aa3120f21e`
- `docs/history/repository-root/MobSpawnTitles.java.txt`: `ec809cf5e5f6110d133800e8a563c4956c39665e`
- `docs/history/repository-root/README.md`: `ccf2dde2d216982493b937a05845d59279e02a8a`
- `docs/history/repository-root/RELEASE-1.12.2.md`: `eec951abfce2135add8b4ac793980b7012806893`
- `docs/history/repository-root/RELEASE-1.12.3.md`: `7d03596aaedf8e3836b09301c9a84efe3f7bab47`
- `docs/history/repository-root/RELEASE-1.12.4.md`: `50d0532d599e8ef3f66212a501baabfd0be327e0`
- `docs/history/repository-root/TESTING-1.10.0.md`: `d981cea11bd73bd668f1b913350ff4e65165e2bf`
- `docs/history/repository-root/TESTING-1.10.1.md`: `dbf1d4d15673feb777958a6bbc3d2e1480d87ab9`
- `docs/history/repository-root/TESTING-1.9.2.md`: `c15f51e431313916e9ae89eedb6497bbe8ee8210`
- `docs/history/repository-root/TESTING.md`: `80e86d26cdd4997be1060e08028dcbd72c5f6421`
- `docs/history/repository-root/TagHelper.java.txt`: `a2e4d8800902a433482ac135360de6c54b00cb8f`
- `docs/history/repository-root/VaultEconomy.java.txt`: `fa691044676f9b0aac2d1f8974bf30178cfdadbf`
- `docs/history/repository-root/VerifyDialogPalette.java.txt`: `e35ebeb3e57a48aad2bf36552f91a5c3cb780dcb`
- `docs/history/repository-root/VerifyHerobrineRoute.java.txt`: `c2376a81d853c71cefcce4a36fbcd8333950d69e`
- `docs/history/repository-root/VerifyNetherSpace.java.txt`: `b6ff18b60966739295678f4fcc6a77ab2ff25ac4`
- `docs/history/repository-root/VerifyRelease.java.txt`: `78ecf5f5fcc5e8b2543de3e9a71e8d95ad06a07b`
- `docs/history/repository-root/build.yml.txt`: `edc98e407938f13529248069a7ea9f8bad2c7727`
- `docs/history/repository-root/config.yml.txt`: `369ef9b42716c30d893f0138f512f16cbf85af6e`
- `docs/history/repository-root/dialogs.yml.txt`: `92e89826cf59c57f5f6c4de03ebd1466a8c20663`
- `docs/history/repository-root/download.txt`: `44aed2be044ba49729cdf9e489ebf0e2a7ae1ff5`
- `docs/history/repository-root/en.yml.txt`: `b4bb718ed8f16bbeb43554103d14ae4e71c912f5`
- `docs/history/repository-root/es.yml.txt`: `a220776d76aabad6e9cbccd4949d90babd4acc80`
- `docs/history/repository-root/plugin.yml.txt`: `60d0fa14ef5ae9a30605125976d952a7cf5a6fbc`

