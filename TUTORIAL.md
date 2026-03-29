# DynamicFloatingDamageFormatter Tutorial

This is a practical, end-to-end guide for using the formatter in your own mod. It covers:

- Plug-and-play usage (no code).
- Core API usage (call from your own damage events).
- Config and UI asset customization.

---

**Quick Install (No Code)**

Use the adapter build if you want it to automatically hook damage events.

1. Drop `DynamicFloatingDamageFormatter-with-adapter.jar` into your server `mods/` folder.
2. Copy `examples/Server/Entity/UI/*.json` into your asset pack.
3. Copy `examples/Server/Config/DamageNumberConfig.json` into your asset pack.

That’s it. The adapter will emit combat text for any damage that matches your config.

---

**Core API Usage (Code Integration)**

If you want full control, depend on the core jar and emit numbers yourself.

Example: emit a custom kind when your damage code runs.

```java
import irai.mod.DynamicFloatingDamageFormatter.DamageNumbers;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public void onMyDamage(Store<EntityStore> store, Ref<EntityStore> target, float amount) {
    DamageNumbers.emit(store, target, amount, "POISON");
}
```

If you already have a `Damage` object and want the formatter to pick a kind:

```java
import irai.mod.DynamicFloatingDamageFormatter.DamageNumbers;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public void onDamage(Damage damage, Ref<EntityStore> target) {
    DamageNumbers.attachTarget(damage, target);
    DamageNumbers.emit(damage);
}
```

---

**Marking Damage Metadata**

You can guide the formatter by attaching metadata to the `Damage` object.

```java
DamageNumbers.markKind(damage, "BLEED");      // Force a kind
DamageNumbers.markCritical(damage);           // Force CRITICAL kind
DamageNumbers.markSkipCombatText(damage);     // Suppress base combat text
```

This is especially useful if you already use `DamageCause` but want custom styling.

---

**Config Basics**

Config lives at:

`Server/Config/DamageNumberConfig.json`

It uses three arrays:

- `DEFAULTS` – global formatting rules
- `KINDS` – per-kind styling
- `ALIASES` – map damage cause ids to kinds

Example:

```json
{
  "DEFAULTS": [
    "format={label} {amount}",
    "rounding=ROUND",
    "min=1",
    "precision=2",
    "style=PLAIN",
    "labelByDefault=true"
  ],
  "KINDS": [
    "POISON|label=Poison|icon=<item is=\"Ingredient_Poison\"/>|format={icon}{label} {amount}|color=#008700|ui=SocketReforge_CombatText_Poison|dot=true"
  ],
  "ALIASES": [
    "poison=POISON",
    "toxic=POISON"
  ]
}
```

Supported `KINDS` keys:

- `label`
- `icon` (raw markup inserted into `{icon}`)
- `iconBg` (background markup inserted into `{iconBg}`)
- `iconOverlay` (overlay markup inserted into `{iconOverlay}`)
- `format` (supports `{iconBg}`, `{icon}`, `{iconOverlay}`, `{label}`, `{amount}`, `{kind}`)
- `color`
- `ui` (primary UI component id)
- `uiAlt` (alternate UI id, toggles every hit)
- `dot` (treat as DoT for angle/randomization)
- `rounding` (`ROUND`, `FLOOR`, `CEIL`, `NONE`)
- `precision`
- `min`
- `style` (`PLAIN`, `MESSAGE`, `TOOLTIP`)
- `labelByDefault`

Aliases match `DamageCause.getId()` by **substring**.

---

**UI Asset Basics**

Each `ui=...` points to a `Server/Entity/UI/*.json` asset with `"Type": "CombatText"`.

Key fields you can tweak:

- `RandomPositionOffsetRange` – random X/Y offset on each hit
- `HitboxOffset` – base offset
- `AnimationEvents` – scale, fade, and timing
- `HitAngleModifierStrength` – how much the hit angle affects motion

If you set `uiAlt`, the formatter alternates between primary and alt components to reduce overlap.

Icon strings are injected verbatim into the combat text. If your client supports it, you can use:

- `<item is="Item_Id"/>` for item icons
- `<img src="texture.png" width="16" height="16"/>` for image tags

To layer background/overlay with the icon, include `{iconBg}` and `{iconOverlay}` in your format string, e.g.:

`format={iconBg}{icon}{iconOverlay}{label} {amount}`

---

**DoT Example**

On each tick of a DoT, emit the number directly:

```java
DamageNumbers.emit(store, targetRef, perTickDamage, "BURN");
```

Set `dot=true` in config to allow wider angle randomization for DoT ticks.

---

**Formatting Only (No Combat Text)**

If you just want the formatted string:

```java
String text = DamageNumbers.format(amount, "POISON");
```

---

**Troubleshooting**

- **Base white damage numbers still show:** call `DamageNumbers.markSkipCombatText(damage)` or disable base combat text.
- **No randomization:** ensure your UI JSON has `RandomPositionOffsetRange` and your `dot=true` kinds are actually tagged as DoT.

---
