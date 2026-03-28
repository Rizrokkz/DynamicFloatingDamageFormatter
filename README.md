# DynamicFloatingDamageFormatter

Standalone, configurable floating damage number API for Hytale mods.

This package can be shipped as a plugin (with adapter) or used as a pure API (core) that other mods call directly.

## What’s Included

Two jars are produced by `dynamicFormatterDist`:

1. `DynamicFloatingDamageFormatter-core.jar`  
API only. Other mods call `DamageNumbers.emit(...)` themselves.

2. `DynamicFloatingDamageFormatter-with-adapter.jar`  
API + adapter (`DamageNumberEST`) that auto-hooks damage events and spawns combat text.

The dist folder also includes:

- `manifest.json` (standalone plugin manifest)
- `examples/Server/Entity/UI/*.json` (sample combat text UI assets)
- `examples/Server/Config/DamageNumberConfig.json` (sample config)

## Quick Install (No Code)

1. Put `DynamicFloatingDamageFormatter-with-adapter.jar` into your server `mods/`.
2. Copy UI JSONs from `examples/Server/Entity/UI/` into your asset pack.
3. Copy `examples/Server/Config/DamageNumberConfig.json` into your asset pack.

This immediately replaces/augments combat text based on the configured kinds.

## API Usage (Code Integration)

Add the core jar as a dependency and emit damage numbers when your own damage code runs.

### Register a kind (optional but recommended)

```java
import irai.mod.DynamicFloatingDamageFormatter.DamageNumbers;

DamageNumbers.kind("POISON")
    .label("Poison")
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

### Suppress the base combat text (optional)

```java
DamageNumbers.markSkipCombatText(damage);
```

## Configuration

The config file is loaded from:

`Server/Config/DamageNumberConfig.json`

It supports:

- `DEFAULTS` for global formatting rules
- `KINDS` for per-damage styling (label, UI asset, dot behavior)
- `ALIASES` for mapping custom cause names to known kinds

## Build the Distribution

```powershell
.\gradlew.bat dynamicFormatterDist
```

Output folder:

`dist/DynamicFloatingDamageFormatter/`

## Notes

- `with-adapter` is best for plug-and-play usage.
- `core` is best for other mod authors who want full control.
- UI JSONs are only examples. Create your own styles as needed.
