package irai.mod.reforge.Util;

/**
 * Minimal stub for standalone formatter builds.
 * Only exposes the DamageKind enum used in metadata helpers.
 */
public final class DamageNumberFormatter {
    public enum DamageKind {
        FLAT,
        CRITICAL,
        ICE,
        BURN,
        BLEED,
        POISON,
        SHOCK,
        WATER,
        VOID,
        HEAL
    }

    private DamageNumberFormatter() {}
}
