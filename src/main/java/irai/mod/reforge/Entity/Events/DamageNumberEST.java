package irai.mod.reforge.Entity.Events;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.protocol.CombatTextUpdate;
import com.hypixel.hytale.protocol.EntityUIType;
import com.hypixel.hytale.protocol.UIComponentsUpdate;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.modules.entity.tracker.EntityTrackerSystems.EntityViewer;
import com.hypixel.hytale.server.core.modules.entity.tracker.EntityTrackerSystems.Visible;
import com.hypixel.hytale.server.core.modules.entityui.EntityUIModule;
import com.hypixel.hytale.server.core.modules.entityui.UIComponentList;
import com.hypixel.hytale.server.core.modules.entityui.asset.EntityUIComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import irai.mod.DynamicFloatingDamageFormatter.DamageNumberMeta;
import irai.mod.DynamicFloatingDamageFormatter.DamageNumbers;

/**
 * Minimal adapter system for standalone usage.
 * Emits combat text using the formatter config (no particle rendering).
 */
public class DamageNumberEST extends DamageEventSystem {
    private static final float NON_DOT_RANDOM_JITTER_DEGREES = 240f;

    private volatile ComponentType<EntityStore, Visible> visibleComponentType;
    private volatile ComponentType<EntityStore, UIComponentList> uiComponentListComponentType;
    private final Query<EntityStore> query;

    public DamageNumberEST() {
        ComponentType<EntityStore, Visible> visibleType = null;
        ComponentType<EntityStore, UIComponentList> uiType = null;
        try {
            EntityModule entityModule = EntityModule.get();
            if (entityModule != null) {
                visibleType = entityModule.getVisibleComponentType();
            }
        } catch (Throwable ignored) {
            // Module may not be ready during plugin init.
        }
        try {
            EntityUIModule uiModule = EntityUIModule.get();
            if (uiModule != null) {
                uiType = uiModule.getUIComponentListType();
            }
        } catch (Throwable ignored) {
            // Module may not be ready during plugin init.
        }
        this.visibleComponentType = visibleType;
        this.uiComponentListComponentType = uiType;
        this.query = (visibleType != null && uiType != null) ? Query.and(visibleType, uiType) : Query.any();
    }

    @Override
    public SystemGroup<EntityStore> getGroup() {
        return DamageModule.get().getInspectDamageGroup();
    }

    @Override
    public Query<EntityStore> getQuery() {
        return query;
    }

    @Override
    public void handle(int index,
                       ArchetypeChunk<EntityStore> chunk,
                       Store<EntityStore> store,
                       CommandBuffer<EntityStore> commandBuffer,
                       Damage damage) {
        if (damage == null || damage.getAmount() <= 0f) {
            return;
        }
        if (DamageNumberMeta.shouldSkipCombatText(damage)) {
            return;
        }

        ensureComponentTypes();
        if (visibleComponentType == null || uiComponentListComponentType == null) {
            return;
        }

        Ref<EntityStore> targetRef = chunk.getReferenceTo(index);
        Visible visible = (Visible) commandBuffer.getComponent(targetRef, visibleComponentType);
        if (visible == null) {
            visible = store.getComponent(targetRef, visibleComponentType);
        }
        UIComponentList uiList = (UIComponentList) commandBuffer.getComponent(targetRef, uiComponentListComponentType);
        if (uiList == null) {
            uiList = store.getComponent(targetRef, uiComponentListComponentType);
        }
        if (visible == null || uiList == null) {
            return;
        }

        EntityViewer[] viewers = resolveViewers(visible);
        if (viewers.length == 0) {
            return;
        }

        String kindId = DamageNumbers.resolveKindId(damage);
        float angle = resolveAngle(kindId);
        String text = DamageNumbers.format(damage.getAmount(), kindId);
        CombatTextUpdate update = new CombatTextUpdate(angle, text);

        for (EntityViewer viewer : viewers) {
            if (viewer == null) {
                continue;
            }
            queueCombatTextComponentSwap(viewer, targetRef, uiList, kindId);
            viewer.queueUpdate(targetRef, update);
        }
    }

    public static void queueCombatTextDirect(Store<EntityStore> store,
                                             Ref<EntityStore> targetRef,
                                             float amount,
                                             String kindId) {
        if (store == null || targetRef == null || amount <= 0f) {
            return;
        }
        ComponentType<EntityStore, Visible> visibleType = null;
        ComponentType<EntityStore, UIComponentList> uiType = null;
        try {
            EntityModule entityModule = EntityModule.get();
            visibleType = entityModule == null ? null : entityModule.getVisibleComponentType();
        } catch (Throwable ignored) {
            visibleType = null;
        }
        try {
            EntityUIModule uiModule = EntityUIModule.get();
            uiType = uiModule == null ? null : uiModule.getUIComponentListType();
        } catch (Throwable ignored) {
            uiType = null;
        }
        if (visibleType == null || uiType == null) {
            return;
        }
        Visible visible = store.getComponent(targetRef, visibleType);
        UIComponentList uiList = store.getComponent(targetRef, uiType);
        if (visible == null || uiList == null) {
            return;
        }

        EntityViewer[] viewers = resolveViewers(visible);
        if (viewers.length == 0) {
            return;
        }
        String resolvedKind = (kindId == null || kindId.isBlank()) ? "FLAT" : kindId;
        float angle = resolveAngle(resolvedKind);
        String text = DamageNumbers.format(amount, resolvedKind);
        CombatTextUpdate update = new CombatTextUpdate(angle, text);

        for (EntityViewer viewer : viewers) {
            if (viewer == null) {
                continue;
            }
            queueCombatTextComponentSwap(viewer, targetRef, uiList, resolvedKind);
            viewer.queueUpdate(targetRef, update);
        }
    }

    public static void queueCombatTextDirect(Store<EntityStore> store,
                                             Ref<EntityStore> targetRef,
                                             float amount,
                                             DamageNumbers.KindStyle kindStyle) {
        String kindId = kindStyle == null ? null : kindStyle.id();
        queueCombatTextDirect(store, targetRef, amount, kindId);
    }

    private static float resolveAngle(String kindId) {
        float angle;
        if (DamageNumbers.isDotKind(kindId)) {
            angle = (ThreadLocalRandom.current().nextFloat() * 360f) - 180f;
        } else {
            angle = (ThreadLocalRandom.current().nextFloat() - 0.5f) * NON_DOT_RANDOM_JITTER_DEGREES;
        }
        return normalizeAngle(angle);
    }

    private static void queueCombatTextComponentSwap(EntityViewer viewer,
                                                     Ref<EntityStore> targetRef,
                                                     UIComponentList uiList,
                                                     String kindId) {
        if (viewer == null || uiList == null) {
            return;
        }
        int[] baseComponentIds = uiList.getComponentIds();
        if (baseComponentIds == null || baseComponentIds.length == 0) {
            return;
        }
        IndexedLookupTableAssetMap<String, EntityUIComponent> assetMap = EntityUIComponent.getAssetMap();
        if (assetMap == null) {
            return;
        }
        int desiredIndex = resolveDesiredCombatTextIndex(assetMap, kindId);
        int[] componentIds = buildSingleCombatTextList(baseComponentIds, assetMap, desiredIndex);
        if (componentIds == null || componentIds.length == 0) {
            return;
        }
        viewer.queueUpdate(targetRef, new UIComponentsUpdate(componentIds));
    }

    private static int resolveDesiredCombatTextIndex(IndexedLookupTableAssetMap<String, EntityUIComponent> assetMap,
                                                     String kindId) {
        if (kindId == null || kindId.isBlank()) {
            return -1;
        }
        DamageNumbers.KindStyle style = DamageNumbers.getKindStyle(kindId);
        String desiredId = style == null ? null : style.uiComponentId();
        String altId = style == null ? null : style.uiComponentAltId();
        if (altId != null && !altId.isBlank()) {
            String chosen = DamageNumbers.resolveUiComponentId(kindId, true);
            int index = chosen == null || chosen.isBlank() ? -1 : assetMap.getIndexOrDefault(chosen, -1);
            if (index >= 0) {
                return index;
            }
            return desiredId == null || desiredId.isBlank()
                    ? -1
                    : assetMap.getIndexOrDefault(desiredId, -1);
        }
        if (desiredId == null || desiredId.isBlank()) {
            return -1;
        }
        return assetMap.getIndexOrDefault(desiredId, -1);
    }

    private static int[] buildSingleCombatTextList(int[] baseComponentIds,
                                                   IndexedLookupTableAssetMap<String, EntityUIComponent> assetMap,
                                                   int desiredIndex) {
        int[] filtered = new int[baseComponentIds.length + (desiredIndex >= 0 ? 1 : 0)];
        int count = 0;
        boolean insertedCombatText = false;
        boolean sawCombatText = false;
        for (int id : baseComponentIds) {
            if (id < 0) {
                continue;
            }
            EntityUIComponent component = assetMap.getAsset(id);
            if (component == null) {
                filtered[count++] = id;
                continue;
            }
            EntityUIType type;
            try {
                type = component.toPacket().type;
            } catch (Throwable ignored) {
                filtered[count++] = id;
                continue;
            }
            if (type == EntityUIType.CombatText) {
                sawCombatText = true;
                if (!insertedCombatText) {
                    filtered[count++] = desiredIndex >= 0 ? desiredIndex : id;
                    insertedCombatText = true;
                }
            } else {
                filtered[count++] = id;
            }
        }
        if (!sawCombatText && desiredIndex >= 0) {
            filtered[count++] = desiredIndex;
        }
        if (count == 0) {
            return Arrays.copyOf(baseComponentIds, baseComponentIds.length);
        }
        return count == filtered.length ? filtered : Arrays.copyOf(filtered, count);
    }

    private static EntityViewer[] resolveViewers(Visible visible) {
        if (visible == null) {
            return new EntityViewer[0];
        }
        EntityViewer[] viewers = collectViewers(visible.visibleTo);
        if (viewers.length == 0) {
            viewers = collectViewers(visible.newlyVisibleTo);
        }
        if (viewers.length == 0) {
            viewers = collectViewers(visible.previousVisibleTo);
        }
        return viewers;
    }

    private static EntityViewer[] collectViewers(java.util.Map<Ref<EntityStore>, EntityViewer> viewersMap) {
        if (viewersMap == null || viewersMap.isEmpty()) {
            return new EntityViewer[0];
        }
        EntityViewer[] viewers = new EntityViewer[viewersMap.size()];
        int count = 0;
        for (EntityViewer viewer : viewersMap.values()) {
            if (viewer == null) {
                continue;
            }
            viewers[count++] = viewer;
        }
        return count == viewers.length ? viewers : Arrays.copyOf(viewers, count);
    }

    private static float normalizeAngle(float angle) {
        if (angle > 180f) {
            return angle - 360f;
        }
        if (angle < -180f) {
            return angle + 360f;
        }
        return angle;
    }

    private void ensureComponentTypes() {
        if (visibleComponentType == null) {
            try {
                EntityModule entityModule = EntityModule.get();
                if (entityModule != null) {
                    visibleComponentType = entityModule.getVisibleComponentType();
                }
            } catch (Throwable ignored) {
                // ignore
            }
        }
        if (uiComponentListComponentType == null) {
            try {
                EntityUIModule uiModule = EntityUIModule.get();
                if (uiModule != null) {
                    uiComponentListComponentType = uiModule.getUIComponentListType();
                }
            } catch (Throwable ignored) {
                // ignore
            }
        }
    }
}
