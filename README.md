# DynamicFloatingDamageFormatter

Standalone, configurable floating damage number API for Hytale mods.

This repo ships:
- A small formatting API (`DamageNumbers`) you can call from your own damage logic.
- Example UI + particle assets for combat text and FloatingDamage-style particles.
- Config and tutorials to customize kinds, colors, formats, and assets.

If you want automatic damage-event hooking, you will still need an adapter in your own mod
(e.g., a DamageEventSystem that calls `DamageNumbers.emit(...)`).

## Quick Install (No Code)

1. Put your built jar into `mods/`.
2. Copy these assets into your asset pack:
   - `Server/Config/DamageNumberConfig.json`
   - `Server/Particles/FloatingDamage/**`
   - `Common/Particles/FloatingDamage/Digits.png`
   - `Common/Particles/Textures/CombatText/*.png`
   - `Server/Entity/UI/SocketReforge_CombatText_*.json` (optional fallback UI)
3. Restart the server.

## API Usage (Code Integration)

Add the core jar as a dependency and emit damage numbers when your own damage code runs.

### Register a kind (optional but recommended)

```java
import irai.mod.DynamicFloatingDamageFormatter.DamageNumbers;

DamageNumbers.kind("POISON")
    .label("Poison")
    .icon("<item is=\"Ingredient_Poison\"/>")
    .format("{icon}{label} {amount}")
    .ui("SocketReforge_CombatText_Poison")
    .dot(true)
    .register();
```

### Emit numbers directly

```java
DamageNumbers.emit(store, targetRef, amount, "POISON");
```

### If you already have a Damage object

```java
DamageNumbers.attachTarget(damage, targetRef);
DamageNumbers.emit(damage);
```

### Suppress base combat text (optional)

```java
DamageNumbers.markSkipCombatText(damage);
```

## Configuration

The config file is loaded from:

`Server/Config/DamageNumberConfig.json`

It supports:
- `DEFAULTS` for global formatting rules
- `KINDS` for per-damage styling (label, icon, format, UI asset, dot behavior)
- `ALIASES` for mapping custom cause names to known kinds

Format placeholders:
`{iconBg}`, `{icon}`, `{iconOverlay}`, `{label}`, `{amount}`, `{kind}`.

### Particle-specific keys

Each kind can opt into particle-based digits/icons:

- `particleFont=FloatingDamage_<KIND>`
- `particleIcon=FloatingDamage_Icon_<Kind>`
- `particleBackground=FloatingDamage_Background` (optional)

Example:

```
CRITICAL|label=|format={amount}|color=#FF5555|ui=SocketReforge_CombatText_Critical|particleFont=FloatingDamage_CRITICAL|particleIcon=FloatingDamage_Icon_Critical
```

## FloatingDamage Particle Assets

This repo includes a full FloatingDamage particle set:

- Digit atlas: `Common/Particles/FloatingDamage/Digits.png`
  - 10 frames (0-9), each 128x128.
  - Frame index is baked into each digit spawner.
- Digit spawners (per kind):
  - `Server/Particles/FloatingDamage/Spawners/FloatingDamage_<KIND>_Digit_0..9.particlespawner`
- Digit particle systems (per kind):
  - `Server/Particles/FloatingDamage/FloatingDamage_<KIND>_Digit_0..9.particlesystem`
- Icon spawners/systems:
  - `FloatingDamage_Icon_Fire`, `FloatingDamage_Icon_Ice`, etc.
- Icon textures:
  - `Common/Particles/Textures/CombatText/*.png`

Digits are colored per kind by **baked colors** in the spawners. If you change a kind
color in the config, update/rebuild the digit spawners accordingly.

### Adjusting size

Digit size is controlled by `InitialAnimationFrame.Scale` inside each digit spawner.
Set it uniformly across all `*_Digit_*.particlespawner` files for consistent size.

### Cull distance

All FloatingDamage particle systems include:

```
"CullDistance": 128
```

Increase this if numbers disappear too far away.

## Examples and Tutorial

- Examples: `examples/Server/Entity/UI/*.json`
- Tutorial: `TUTORIAL.md`

## Troubleshooting

- **JSON fails to load (Unexpected character: feff):**
  Make sure JSON files are saved as UTF-8 *without BOM*.
- **Icons not showing:**
  Ensure the PNGs exist in `Common/Particles/Textures/CombatText/` and the spawner
  texture path matches.
- **All digits are the same:**
  Check `FrameIndex` in each digit spawner matches its digit.

## Notes

- This repo is API + assets. You still need an adapter in your mod to hook damage events
  and call `DamageNumbers.emit(...)`.
- UI JSONs are examples. You can replace them with your own styles.