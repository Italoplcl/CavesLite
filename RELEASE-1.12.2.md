# CavesLite 1.12.2

## English release notes

Spanish and English now cover administration dialogs, command feedback, achievements and configurable display text. `language: es|en` selects the server language. Each key falls back through the user's selected language file, bundled selected language, user Spanish, bundled Spanish and bundled English. Both language files are exported without overwriting edits. Unknown languages fall back to Spanish; unsafe file names are rejected.

Display strings in config/dialogs use `@lang:<key>`. Stock 1.12.1 display values are translated in memory; custom text remains unchanged. Gameplay settings and saved language references are preserved. Editing a name explicitly creates a literal custom name; merely saving a panel preserves its existing reference.

Nether spawning is OFF per mob (`mobs.<id>.nether-enabled`). Enable the mob itself, give it positive priority, enable its Nether checkbox and allow the Nether world in `mobs.worlds`. Natural replacements use the same event, replacement entity types, chance, light setting, cooldown and active cap as Overworld. Add Nether entity types to `mobs.replace-mobs` if desired. No independent spawn scheduler is introduced. The Golem ore-break trigger is restricted to Overworld.

Nether ignores Overworld global/per-mob Y limits and Overworld cave/night/sky context. It retains mob-specific requirements and checks conservative body/passenger envelopes against collision shapes, liquids, loaded chunks and the world's actual height. Giant envelope: 3.6 wide, 12 high. Fortress and bastion bounding boxes exclude spawn positions. Ruined portals are allowed. `mobs.nether.ruined-portal-radius` defaults to 48 (0–128); `giant-zombie-ruined-portal-weight-multiplier` defaults to 3 (0–100), applies only to the Giant's relative weight near generated ruined portals in loaded chunks, and never enables a disabled mob or positive weight from a zero base. Radius uses 3D distance to the structure bounding box. Multiplier 1 is neutral; 0 suppresses the Giant near portals.

The End is excluded from natural replacements and natural Herobrine encounters. Administrative summons retain existing behavior. Herobrine retains its separate encounter engine and configured world/height restrictions. Combat rotation now updates every tick even when movement is blocked or the player stands within the movement dead zone.

GitHub Actions uses v5 checkout, setup-java, setup-gradle and upload-artifact, Ubuntu 24.04, Java 25 and artifact name `CavesLite`.

## Validation

All Java source compiled successfully using JDK 25.0.4 and the installed Purpur API 26.3.build.2650-stable, Vault and PlaceholderAPI dependencies. Gradle is not installed locally, so this was a direct Java compilation and JAR assembly rather than a Gradle execution. The workflow itself was not run on GitHub.

424 automated checks passed: strict YAML parsing without duplicate keys, language key parity and all display references, user overrides, bundled and unknown-language fallback, placeholders, legacy stock config migration, custom text preservation, unmodified source config and all 18 Nether switches OFF. Eight additional mocked checks passed for physical space, including a Giant above/below its Overworld range, world ceiling, collision, liquid and unloaded chunks. Harness sources are included as `VerifyRelease.java` and `VerifyNetherSpace.java`; these are outside Gradle's production source tree.

No Minecraft gameplay session was run. Verify the mannequin's visible head/body tracking, natural spawning near actual fortress/bastion/portal structures, mounted encounters and multiplayer behavior on a test server. Compilation and mocked checks do not establish those runtime observations.

The ZIP contains complete project sources/resources/docs/workflow, verification source and the compiled plugin in `build/libs/CavesLite-1.12.2.jar`. External server libraries are not bundled.
