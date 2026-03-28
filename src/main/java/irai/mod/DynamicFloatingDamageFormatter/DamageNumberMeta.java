package irai.mod.DynamicFloatingDamageFormatter;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.server.core.meta.MetaKey;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;

import irai.mod.reforge.Util.DamageNumberFormatter;

public final class DamageNumberMeta {
    public static final MetaKey<String> META_DAMAGE_KIND =
            Damage.META_REGISTRY.registerMetaObject(d -> "", false, "socketreforge:damage_kind", Codec.STRING);
    public static final MetaKey<Boolean> META_CRITICAL =
            Damage.META_REGISTRY.registerMetaObject(d -> Boolean.FALSE, false, "socketreforge:damage_crit", Codec.BOOLEAN);
    public static final MetaKey<Boolean> META_SKIP_COMBAT_TEXT =
            Damage.META_REGISTRY.registerMetaObject(d -> Boolean.FALSE, false, "socketreforge:damage_skip_text", Codec.BOOLEAN);

    private DamageNumberMeta() {}

    public static void markKind(Damage damage, String kindId) {
        if (damage == null || kindId == null || kindId.isBlank()) {
            return;
        }
        damage.putMetaObject(META_DAMAGE_KIND, kindId);
    }

    public static void markKind(Damage damage, DamageNumberFormatter.DamageKind kind) {
        if (damage == null || kind == null) {
            return;
        }
        markKind(damage, kind.name());
    }

    public static String readKindId(Damage damage) {
        if (damage == null) {
            return null;
        }
        Object raw = damage.getIfPresentMetaObject(META_DAMAGE_KIND);
        if (!(raw instanceof String value) || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    public static DamageNumberFormatter.DamageKind readKind(Damage damage) {
        String value = readKindId(damage);
        if (value == null) {
            return null;
        }
        try {
            return DamageNumberFormatter.DamageKind.valueOf(value.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public static void markCritical(Damage damage) {
        if (damage == null) {
            return;
        }
        damage.putMetaObject(META_CRITICAL, Boolean.TRUE);
    }

    public static boolean isCritical(Damage damage) {
        if (damage == null) {
            return false;
        }
        return Boolean.TRUE.equals(damage.getIfPresentMetaObject(META_CRITICAL));
    }

    public static void markSkipCombatText(Damage damage) {
        if (damage == null) {
            return;
        }
        damage.putMetaObject(META_SKIP_COMBAT_TEXT, Boolean.TRUE);
    }

    public static boolean shouldSkipCombatText(Damage damage) {
        if (damage == null) {
            return false;
        }
        return Boolean.TRUE.equals(damage.getIfPresentMetaObject(META_SKIP_COMBAT_TEXT));
    }
}
