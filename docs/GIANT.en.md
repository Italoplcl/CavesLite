# Giant Zombie — 2.0 preview

CavesLite owns the goals/navigation of tagged `giant-zombie` entities through the Paper API. It does not enable or change Purpur's global Giant AI flags and requires no external AI plugin.

## Ground navigation

The Giant chases the nearest eligible Survival/Adventure player within 32 blocks. Native ground navigation handles detours, steps and collisions; full-body space checks validate path waypoints using the entity's actual dimensions. Impossible routes are stopped/recalculated, never bypassed with teleportation. It cannot climb vertical walls or deliberately enter undersized tunnels.

Nominal body: 3.6 blocks wide and 12 high. Summons require room, followed by an actual-size check; server scale changes can need more room. Checks reject solid intersections, liquids inside the body and unloaded chunks. Raw path node origins are offset to the center appropriate to the entity width.

`movement.enabled`, `speed` (movement attribute, default 0.23), `follow-range` and `repath-ticks` control pursuit; initial step height is one block. Disabling chase leaves nearby attacks independently enabled. Complex terrain must still be tested in Minecraft; no route should mean waiting rather than forced movement.

## Attacks

| Attack | Base damage | Radius | Cooldown | Warning |
|---|---:|---:|---:|---:|
| Basic | 6 points | 3 blocks | 30 ticks | 0 ticks |
| Stomp | 8 points | 4.5 blocks | 200 ticks | 24 ticks |
| Earthquake | 6 points | 8 blocks | 360 ticks | 32 ticks |

Two damage points equal one heart before reductions; 20 ticks are about one second at 20 TPS. Cooldowns use an elapsed monotonic plugin clock. Armor, shields, absorption and protection plugins retain normal effects.

Basic attacks hit one nearby player with moderate 0.45 knockback. Stomp stops navigation, warns using sounds/particles, then hits nearby players with 0.7 knockback. Earthquake warns then sends an expanding ring; each grounded player can be hit at most once per wave, with 0.6 knockback. Jumping as the wave passes can avoid it. The effect is particles/sound/physical impact, not client camera animation.

Attacks require vertical proximity, line of sight and an additional feet-level block ray trace to prevent hitting through floors/walls. Extra knockback requires an actual decrease in health/absorption. The Giant does not pursue or interleave other attacks during a wind-up/wave. No real explosion or block modification is used.

Each of `attacks.basic`, `attacks.stomp` and `attacks.quake` supports `enabled`, `damage`, `radius`, `knockback`, `cooldown-ticks`, `warning-ticks`. `attacks.quake.wave-step` advances the ring per plugin update (every four server ticks). `sounds` configures warning/impact keys, volume and pitch; `effects.particles` toggles particles. Empty sound keys/zero volume silence them.

## Reinforcements

Defaults: two zombies per attempt, three active loaded minions per Giant, at most two waves per entity, 30-second summon cooldown and 45-second minion lifetime. Attempts begin after the cooldown. Minion health is 16. These are CUSTOM spawns and do not use the natural replacement pool.

Positions need loaded space and solid support. Zombies receive a target; door breaking, item pickup and the native reinforcement attribute are disabled. Minions do not provide their own CavesLite death loot/XP. They are removed when their owner dies/disappears, on expiry or plugin cleanup; owner/expiry are rechecked when loading. Consumed waves are stored on the Giant so a settings reload does not replenish them.

Settings: `enabled`, `amount`, `max-active`, `max-waves`, `cooldown-seconds`, `lifetime-seconds`, `health`. Work is bounded under malformed/extreme config: at most 8 per attempt, 12 active and 10 waves; approved defaults are substantially smaller.

## Configurable death loot

`loot` is an ordinary customizable death-drop table, not private per-player rewards. It combines with vanilla drops and common `extra-drops`, unless `replace-vanilla` is true.

```yaml
loot:
  enabled: true
  replace-vanilla: false
  experience: 30
  items:
    - material: ROTTEN_FLESH
      min: 4
      max: 8
      chance: 100
    - material: IRON_INGOT
      min: 1
      max: 2
      chance: 25
```

Entries roll independently at 0–100 percent, with material/quantity range and optional custom `name`. Invalid materials are skipped and amounts are bounded by stack size. Disabling this table does not automatically remove vanilla/common extra drops. The common console death-command option also remains available.

## Spawning and administration

Basic preset: enabled, weight 1, natural cooldown 600 seconds, cap 1, Nether OFF. Overworld requires nighttime, sky exposure and Y 80–320. Opted-in Nether ignores those Overworld context limits but still requires space and structure exclusions.

`/clite` → Giant Zombie exposes main movement/attack/reinforcement/loot switches and settings. YAML supplies advanced values. Test in open space using `/clite summon giant-zombie`, inspect with `/clite debug mobs giant-zombie`, and remove it/minions with `/clite kill giant-zombie`.

Giant abilities do not destroy terrain; other creatures/plugins retain their own block-damage rules.
