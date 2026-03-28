package irai.mod.DynamicFloatingDamageFormatter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import irai.mod.reforge.Entity.Events.DamageNumberEST;

/**
 * Standalone, configurable floating damage number registry + formatter.
 * Other mods can register kinds and aliases via the public API.
 */
public final class DamageNumbers {

    public enum FormatStyle {
        MESSAGE,
        TOOLTIP,
        PLAIN
    }

    public enum Rounding {
        ROUND,
        FLOOR,
        CEIL,
        NONE
    }

    public record Defaults(String format,
                           FormatStyle style,
                           Rounding rounding,
                           int precision,
                           double minAmount,
                           boolean labelByDefault) {
        public static Defaults defaultValues() {
            return new Defaults("{label} {amount}", FormatStyle.PLAIN, Rounding.ROUND, 2, 1.0d, true);
        }
    }

    public record KindStyle(String id,
                            String label,
                            String format,
                            String colorHex,
                            String uiComponentId,
                            String uiComponentAltId,
                            Boolean dot,
                            Boolean labelByDefault,
                            Rounding rounding,
                            Integer precision,
                            Double minAmount,
                            FormatStyle styleOverride) {
    }

    private static final String KIND_FLAT = "FLAT";
    private static final Map<String, KindStyle> KINDS = new ConcurrentHashMap<>();
    private static final Map<String, String> ALIASES = new ConcurrentHashMap<>();
    private static final Map<String, AtomicBoolean> ALT_TOGGLES = new ConcurrentHashMap<>();
    private static final Map<Damage, Ref<EntityStore>> DAMAGE_TARGETS =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static volatile Defaults defaults = Defaults.defaultValues();

    static {
        // Ensure baseline FLAT kind exists even before config load.
        registerKind(new KindStyle(KIND_FLAT, "", "{amount}", null, null, null,
                false, Boolean.FALSE, null, null, null, null));
    }

    private DamageNumbers() {}

    // ── Public API (mod integration) ──────────────────────────────────────────

    public static KindBuilder kind(String id) {
        return new KindBuilder(id);
    }

    public static void emit(Store<EntityStore> store,
                            Ref<EntityStore> targetRef,
                            float amount,
                            String kindId) {
        DamageNumberEST.queueCombatTextDirect(store, targetRef, amount, kindId);
    }

    public static void emit(Store<EntityStore> store,
                            Ref<EntityStore> targetRef,
                            Damage damage) {
        if (store == null || targetRef == null || damage == null) {
            return;
        }
        if (DamageNumberMeta.shouldSkipCombatText(damage)) {
            return;
        }
        float amount = damage.getAmount();
        if (amount <= 0f) {
            return;
        }
        String kindId = resolveKindId(damage);
        DamageNumberEST.queueCombatTextDirect(store, targetRef, amount, kindId);
    }

    public static void emit(Ref<EntityStore> targetRef,
                            Damage damage) {
        if (targetRef == null) {
            return;
        }
        emit(targetRef.getStore(), targetRef, damage);
    }

    public static void emit(Damage damage) {
        if (damage == null) {
            return;
        }
        Ref<EntityStore> targetRef = DAMAGE_TARGETS.remove(damage);
        if (targetRef == null) {
            return;
        }
        emit(targetRef.getStore(), targetRef, damage);
    }

    public static void attachTarget(Damage damage, Ref<EntityStore> targetRef) {
        if (damage == null || targetRef == null) {
            return;
        }
        DAMAGE_TARGETS.put(damage, targetRef);
    }

    public static void clearTarget(Damage damage) {
        if (damage == null) {
            return;
        }
        DAMAGE_TARGETS.remove(damage);
    }

    public static void emit(Store<EntityStore> store,
                            Ref<EntityStore> targetRef,
                            float amount,
                            KindStyle kindStyle) {
        DamageNumberEST.queueCombatTextDirect(store, targetRef, amount, kindStyle);
    }

    public static void markKind(Damage damage, String kindId) {
        DamageNumberMeta.markKind(damage, kindId);
    }

    public static void markCritical(Damage damage) {
        DamageNumberMeta.markCritical(damage);
    }

    public static void markSkipCombatText(Damage damage) {
        DamageNumberMeta.markSkipCombatText(damage);
    }

    public static void applyConfig(DamageNumberConfig config) {
        Defaults nextDefaults = Defaults.defaultValues();
        if (config != null) {
            nextDefaults = parseDefaults(config.getDefaultsEntries(), nextDefaults);
        }
        defaults = nextDefaults;

        KINDS.clear();
        ALIASES.clear();
        ALT_TOGGLES.clear();

        registerKind(new KindStyle(KIND_FLAT, "", "{amount}", null, null, null,
                false, Boolean.FALSE, null, null, null, null));

        if (config == null) {
            return;
        }
        for (String entry : safeArray(config.getKindEntries())) {
            KindStyle style = parseKindEntry(entry);
            if (style != null) {
                registerKind(style);
            }
        }
        for (String entry : safeArray(config.getAliasEntries())) {
            parseAliasEntry(entry);
        }
    }

    public static void registerKind(KindStyle style) {
        if (style == null || style.id() == null || style.id().isBlank()) {
            return;
        }
        String id = normalizeKindId(style.id());
        KINDS.put(id, style);
        if (style.uiComponentAltId() != null && !style.uiComponentAltId().isBlank()) {
            ALT_TOGGLES.put(id, new AtomicBoolean(false));
        }
    }

    public static void registerAlias(String alias, String kindId) {
        if (alias == null || alias.isBlank() || kindId == null || kindId.isBlank()) {
            return;
        }
        ALIASES.put(alias.toLowerCase(Locale.ROOT), normalizeKindId(kindId));
    }

    public static KindStyle getKindStyle(String kindId) {
        if (kindId == null || kindId.isBlank()) {
            return KINDS.get(KIND_FLAT);
        }
        KindStyle style = KINDS.get(normalizeKindId(kindId));
        return style != null ? style : KINDS.get(KIND_FLAT);
    }

    public static String resolveKindId(Damage damage) {
        if (damage == null) {
            return KIND_FLAT;
        }
        if (DamageNumberMeta.isCritical(damage)) {
            return "CRITICAL";
        }
        String explicit = DamageNumberMeta.readKindId(damage);
        if (explicit != null && !explicit.isBlank()) {
            return normalizeKindId(explicit);
        }
        DamageCause cause = damage.getCause();
        if (cause == null || cause.getId() == null) {
            return KIND_FLAT;
        }
        String causeId = cause.getId().toLowerCase(Locale.ROOT);
        for (Map.Entry<String, String> entry : ALIASES.entrySet()) {
            if (causeId.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return KIND_FLAT;
    }

    public static boolean isDotKind(String kindId) {
        KindStyle style = getKindStyle(kindId);
        return style != null && Boolean.TRUE.equals(style.dot());
    }

    public static String format(double amount, String kindId) {
        KindStyle style = getKindStyle(kindId);
        FormatStyle fmtStyle = style.styleOverride() != null ? style.styleOverride() : defaults.style();
        boolean includeLabel = style.labelByDefault() != null ? style.labelByDefault() : defaults.labelByDefault();
        return format(amount, style, fmtStyle, includeLabel);
    }

    public static String format(double amount, String kindId, FormatStyle formatStyle, boolean includeLabel) {
        KindStyle style = getKindStyle(kindId);
        FormatStyle fmtStyle = formatStyle != null ? formatStyle : defaults.style();
        return format(amount, style, fmtStyle, includeLabel);
    }

    public static String format(double amount, KindStyle style, FormatStyle formatStyle, boolean includeLabel) {
        KindStyle resolved = style != null ? style : getKindStyle(KIND_FLAT);
        String amountText = formatAmount(amount, resolved);
        String label = includeLabel ? defaultString(resolved.label()) : "";
        String format = resolved.format() != null && !resolved.format().isBlank()
                ? resolved.format()
                : defaults.format();
        String text = applyTemplate(format, label, amountText, resolved.id());
        return wrap(text, resolved.colorHex(), formatStyle);
    }

    public static String resolveUiComponentId(String kindId, boolean toggleAlt) {
        KindStyle style = getKindStyle(kindId);
        if (style == null) {
            return null;
        }
        String primary = style.uiComponentId();
        String alt = style.uiComponentAltId();
        if (!toggleAlt || alt == null || alt.isBlank()) {
            return primary;
        }
        AtomicBoolean toggle = ALT_TOGGLES.get(normalizeKindId(style.id()));
        if (toggle == null) {
            return primary;
        }
        boolean useAlt = toggle.getAndSet(!toggle.get());
        return useAlt ? alt : primary;
    }

    public static String getPrimaryUiComponentId(String kindId) {
        KindStyle style = getKindStyle(kindId);
        return style == null ? null : style.uiComponentId();
    }

    public static String getAltUiComponentId(String kindId) {
        KindStyle style = getKindStyle(kindId);
        return style == null ? null : style.uiComponentAltId();
    }

    public static Defaults getDefaults() {
        return defaults;
    }

    public static final class KindBuilder {
        private final String id;
        private String label;
        private String format;
        private String colorHex;
        private String uiComponentId;
        private String uiComponentAltId;
        private Boolean dot;
        private Boolean labelByDefault;
        private Rounding rounding;
        private Integer precision;
        private Double minAmount;
        private FormatStyle styleOverride;

        private KindBuilder(String id) {
            this.id = id;
        }

        public KindBuilder label(String value) {
            this.label = value;
            return this;
        }

        public KindBuilder format(String value) {
            this.format = value;
            return this;
        }

        public KindBuilder color(String value) {
            this.colorHex = value;
            return this;
        }

        public KindBuilder ui(String value) {
            this.uiComponentId = value;
            return this;
        }

        public KindBuilder uiAlt(String value) {
            this.uiComponentAltId = value;
            return this;
        }

        public KindBuilder dot(boolean value) {
            this.dot = value;
            return this;
        }

        public KindBuilder labelByDefault(boolean value) {
            this.labelByDefault = value;
            return this;
        }

        public KindBuilder rounding(Rounding value) {
            this.rounding = value;
            return this;
        }

        public KindBuilder precision(int value) {
            this.precision = value;
            return this;
        }

        public KindBuilder min(double value) {
            this.minAmount = value;
            return this;
        }

        public KindBuilder style(FormatStyle value) {
            this.styleOverride = value;
            return this;
        }

        public KindStyle build() {
            return new KindStyle(id, label, format, colorHex, uiComponentId, uiComponentAltId,
                    dot, labelByDefault, rounding, precision, minAmount, styleOverride);
        }

        public void register() {
            DamageNumbers.registerKind(build());
        }
    }

    private static Defaults parseDefaults(String[] entries, Defaults fallback) {
        Defaults base = fallback == null ? Defaults.defaultValues() : fallback;
        String format = base.format();
        FormatStyle style = base.style();
        Rounding rounding = base.rounding();
        int precision = base.precision();
        double min = base.minAmount();
        boolean labelByDefault = base.labelByDefault();
        for (String entry : safeArray(entries)) {
            if (entry == null || entry.isBlank()) {
                continue;
            }
            String[] parts = entry.split("=", 2);
            if (parts.length < 2) {
                continue;
            }
            String key = parts[0].trim().toLowerCase(Locale.ROOT);
            String value = parts[1].trim();
            switch (key) {
                case "format" -> format = value;
                case "style" -> style = parseFormatStyle(value, style);
                case "rounding" -> rounding = parseRounding(value, rounding);
                case "precision" -> precision = parseInt(value, precision);
                case "min" -> min = parseDouble(value, min);
                case "labelbydefault" -> labelByDefault = parseBoolean(value, labelByDefault);
                default -> {
                    // ignore unknown keys
                }
            }
        }
        return new Defaults(format, style, rounding, precision, min, labelByDefault);
    }

    private static KindStyle parseKindEntry(String entry) {
        if (entry == null || entry.isBlank()) {
            return null;
        }
        String[] tokens = entry.split("\\|");
        if (tokens.length == 0) {
            return null;
        }
        String id = tokens[0].trim();
        if (id.isBlank()) {
            return null;
        }
        String label = null;
        String format = null;
        String color = null;
        String ui = null;
        String uiAlt = null;
        Boolean dot = null;
        Boolean labelByDefault = null;
        Rounding rounding = null;
        Integer precision = null;
        Double min = null;
        FormatStyle styleOverride = null;
        for (int i = 1; i < tokens.length; i++) {
            String token = tokens[i];
            if (token == null || token.isBlank()) {
                continue;
            }
            String[] kv = token.split("=", 2);
            if (kv.length < 2) {
                continue;
            }
            String key = kv[0].trim().toLowerCase(Locale.ROOT);
            String value = kv[1].trim();
            switch (key) {
                case "label" -> label = value;
                case "format" -> format = value;
                case "color" -> color = value;
                case "ui" -> ui = value;
                case "uialt" -> uiAlt = value;
                case "dot" -> dot = parseBoolean(value, null);
                case "rounding" -> rounding = parseRounding(value, null);
                case "precision" -> precision = parseIntObj(value);
                case "min" -> min = parseDoubleObj(value);
                case "style" -> styleOverride = parseFormatStyle(value, null);
                case "labelbydefault" -> labelByDefault = parseBoolean(value, null);
                default -> {
                    // ignore unknown keys
                }
            }
        }
        return new KindStyle(id, label, format, color, ui, uiAlt, dot, labelByDefault,
                rounding, precision, min, styleOverride);
    }

    private static void parseAliasEntry(String entry) {
        if (entry == null || entry.isBlank()) {
            return;
        }
        String[] parts = entry.split("=", 2);
        if (parts.length < 2) {
            return;
        }
        String alias = parts[0].trim();
        String kind = parts[1].trim();
        registerAlias(alias, kind);
    }

    private static String formatAmount(double amount, KindStyle style) {
        if (Double.isNaN(amount) || Double.isInfinite(amount)) {
            return "0";
        }
        Defaults cfg = defaults;
        Rounding rounding = style.rounding() != null ? style.rounding() : cfg.rounding();
        int precision = style.precision() != null ? style.precision() : cfg.precision();
        double min = style.minAmount() != null ? style.minAmount() : cfg.minAmount();
        double adjusted = amount;
        if (min > 0.0d && adjusted > 0.0d && adjusted < min) {
            adjusted = min;
        }
        switch (rounding) {
            case FLOOR -> {
                long floored = (long) Math.floor(adjusted);
                return Long.toString(floored == 0L && adjusted > 0.0d ? 1L : floored);
            }
            case CEIL -> {
                long ceil = (long) Math.ceil(adjusted);
                return Long.toString(ceil == 0L && adjusted > 0.0d ? 1L : ceil);
            }
            case NONE -> {
                BigDecimal bd = BigDecimal.valueOf(adjusted);
                bd = bd.setScale(Math.max(0, precision), RoundingMode.HALF_UP);
                String text = bd.stripTrailingZeros().toPlainString();
                if ("0".equals(text) && adjusted > 0.0d) {
                    return "1";
                }
                return text;
            }
            case ROUND -> {
                long rounded = Math.round(adjusted);
                if (rounded == 0L && adjusted > 0.0d) {
                    rounded = 1L;
                }
                return Long.toString(rounded);
            }
            default -> {
                long rounded = Math.round(adjusted);
                if (rounded == 0L && adjusted > 0.0d) {
                    rounded = 1L;
                }
                return Long.toString(rounded);
            }
        }
    }

    private static String applyTemplate(String format, String label, String amountText, String kindId) {
        if (format == null || format.isBlank()) {
            String text = (label == null || label.isBlank()) ? amountText : (label + " " + amountText);
            return text.trim();
        }
        String resolved = format
                .replace("{label}", defaultString(label))
                .replace("{amount}", defaultString(amountText))
                .replace("{kind}", defaultString(kindId));
        // Collapse multiple spaces.
        String compact = resolved.replace("  ", " ");
        while (!Objects.equals(compact, resolved)) {
            resolved = compact;
            compact = resolved.replace("  ", " ");
        }
        return resolved.trim();
    }

    private static String wrap(String text, String colorHex, FormatStyle style) {
        if (style == null || style == FormatStyle.PLAIN || colorHex == null || colorHex.isBlank()) {
            return text;
        }
        String hex = normalizeHex(colorHex);
        if (style == FormatStyle.TOOLTIP) {
            return "<color is=\"" + hex + "\">" + text + "</color>";
        }
        return "<color=" + hex + ">" + text + "</color>";
    }

    private static String normalizeHex(String colorHex) {
        String trimmed = colorHex.trim();
        String prefixed = trimmed.startsWith("#") ? trimmed : ("#" + trimmed);
        return prefixed.toUpperCase(Locale.ROOT);
    }

    private static String normalizeKindId(String kindId) {
        return kindId.trim().toUpperCase(Locale.ROOT);
    }

    private static FormatStyle parseFormatStyle(String raw, FormatStyle fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return FormatStyle.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }

    private static Rounding parseRounding(String raw, Rounding fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Rounding.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }

    private static int parseInt(String raw, int fallback) {
        try {
            return Integer.parseInt(raw);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static Integer parseIntObj(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static double parseDouble(String raw, double fallback) {
        try {
            return Double.parseDouble(raw);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static Double parseDoubleObj(String raw) {
        try {
            return Double.parseDouble(raw);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Boolean parseBoolean(String raw, Boolean fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        return Boolean.parseBoolean(raw.trim());
    }

    private static String defaultString(String value) {
        return value == null ? "" : value;
    }

    private static String[] safeArray(String[] values) {
        return values == null ? new String[0] : values;
    }
}
