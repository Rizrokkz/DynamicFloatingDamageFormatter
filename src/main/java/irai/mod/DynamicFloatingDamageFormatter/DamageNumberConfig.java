package irai.mod.DynamicFloatingDamageFormatter;

import static com.hypixel.hytale.codec.Codec.STRING_ARRAY;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

/**
 * Configuration for the floating damage number library.
 * Loaded from resources/Server/Config/DamageNumberConfig.json via ReforgePlugin.
 */
@SuppressWarnings("removal")
public class DamageNumberConfig {

    public static final BuilderCodec<DamageNumberConfig> CODEC =
            BuilderCodec.<DamageNumberConfig>builder(DamageNumberConfig.class, DamageNumberConfig::new)
                    .append(
                            new KeyedCodec<>("DEFAULTS", STRING_ARRAY),
                            (cfg, v) -> cfg.defaultsEntries = v,
                            cfg -> cfg.defaultsEntries == null ? new String[0] : cfg.defaultsEntries
                    ).add()
                    .append(
                            new KeyedCodec<>("KINDS", STRING_ARRAY),
                            (cfg, v) -> cfg.kindEntries = v,
                            cfg -> cfg.kindEntries == null ? new String[0] : cfg.kindEntries
                    ).add()
                    .append(
                            new KeyedCodec<>("ALIASES", STRING_ARRAY),
                            (cfg, v) -> cfg.aliasEntries = v,
                            cfg -> cfg.aliasEntries == null ? new String[0] : cfg.aliasEntries
                    ).add()
                    .build();

    private String[] defaultsEntries = new String[] {
            "format={label} {amount}",
            "rounding=ROUND",
            "min=1",
            "precision=2",
            "style=PLAIN",
            "labelByDefault=true"
    };

    // Format: "KIND|key=value|key=value"
    // Supported keys: label, icon, iconBg, iconOverlay, format, color, ui, uiAlt, vfx,
    // particleFont, particleIcon, particleBackground, dot, rounding, precision, min, style, labelByDefault
    private String[] kindEntries = new String[] {
            "FLAT|label=|format={amount}|color=#FFFFFF|ui=SocketReforge_CombatText_Flat|particleFont=FloatingDamage",
            "CRITICAL|label=|color=#FF5555|ui=SocketReforge_CombatText_Critical|particleFont=FloatingDamage|particleIcon=FloatingDamage_Icon_Critical",
            "ICE|label=|color=#55FFFF|ui=SocketReforge_CombatText_Ice|particleFont=FloatingDamage|particleIcon=FloatingDamage_Icon_Ice",
            "BURN|label=|color=#FFAA00|ui=SocketReforge_CombatText_Burn|uiAlt=SocketReforge_CombatText_Burn_Alt|dot=true|particleFont=FloatingDamage|particleIcon=FloatingDamage_Icon_Fire",
            "BLEED|label=|color=#AA55FF|ui=SocketReforge_CombatText_Bleed|uiAlt=SocketReforge_CombatText_Bleed_Alt|dot=true|particleFont=FloatingDamage|particleIcon=FloatingDamage_Icon_Bleed",
            "POISON|label=|color=#008700|ui=SocketReforge_CombatText_Poison|uiAlt=SocketReforge_CombatText_Poison_Alt|dot=true|particleFont=FloatingDamage|particleIcon=FloatingDamage_Icon_Poison",
            "SHOCK|label=|color=#FFFF55|ui=SocketReforge_CombatText_Shock|particleFont=FloatingDamage|particleIcon=FloatingDamage_Icon_Shock",
            "WATER|label=|color=#5555FF|ui=SocketReforge_CombatText_Water|particleFont=FloatingDamage",
            "VOID|label=|color=#8000FF|ui=SocketReforge_CombatText_Void|particleFont=FloatingDamage|particleIcon=FloatingDamage_Icon_Void",
            "HEAL|label=|color=#55FF55|ui=SocketReforge_CombatText_Heal|particleFont=FloatingDamage"
    };

    // Format: "alias=KIND"
    private String[] aliasEntries = new String[] {
            "crit=CRITICAL",
            "critical=CRITICAL",
            "burn=BURN",
            "fire=BURN",
            "poison=POISON",
            "toxic=POISON",
            "bleed=BLEED",
            "ice=ICE",
            "freeze=ICE",
            "frost=ICE",
            "shock=SHOCK",
            "lightning=SHOCK",
            "electric=SHOCK",
            "water=WATER",
            "void=VOID",
            "heal=HEAL",
            "regen=HEAL"
    };

    public String[] getDefaultsEntries() { return defaultsEntries; }
    public String[] getKindEntries() { return kindEntries; }
    public String[] getAliasEntries() { return aliasEntries; }

    public void setDefaultsEntries(String[] v) { this.defaultsEntries = v; }
    public void setKindEntries(String[] v) { this.kindEntries = v; }
    public void setAliasEntries(String[] v) { this.aliasEntries = v; }
}

