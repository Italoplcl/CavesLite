# Validation — 2.0.0-preview.1

Production Java source compiled with JDK 25.0.4 against installed Purpur API 26.3.build.2650-stable and optional Vault/PlaceholderAPI compile dependencies. No external server JARs are bundled.

Six verification mains passed locally: 546 configuration/language/YAML checks; 15 encounter clock/attack timing checks; 8 Herobrine route scenarios; 364 palette checks; 119 Giant geometry/settings/approved-preset checks; 8 Nether-space checks. Total: **1,060 assertions/scenarios**. Verification sources live in tools/verification and are excluded from the plugin JAR.

The timing regression reproduces one attack with a frozen world-clock counter and repeated 24-tick attacks with the independent elapsed clock. Giant collision checks include translated block-local shapes, ceilings/walls/fences, unloaded chunks and wide path-node offsets. Preset checks cover every approved enable state, weight, cooldown and cap.

Documentation links were checked; the repository's original canonical src files matched the reviewed 1.12.4 ZIP, and unique/older root files were archived before cleanup. The 1.12.4 ZIP remains unchanged.

The local environment has no Gradle executable, so the local JAR is assembled from the compiled classes/resources with expanded plugin version metadata. GitHub Actions independently runs gradle build (including the same verifiers) on the preview branch/PR when CI is available. Its actual status is shown on the pull request and is not implied by local compilation.

No Minecraft client/gameplay session was run for this preview. Native Giant pathfinding, actual damage/protection interactions, physical wave effects and minion lifecycle must be reviewed in-game using TESTING.md. Herobrine head tracking and elevation pursuit had previously been confirmed by the user on 1.12.4; the new clock/attacks still need the regression check on the user's server. This is a preview, not a final 2.0 publication.
