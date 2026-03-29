package irai.mod.DynamicFloatingDamageFormatter;

import javax.annotation.Nonnull;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.util.Config;

/**
 * Standalone plugin entrypoint. Loads the formatter config and
 * tries to register a damage adapter system if one is present.
 */
public class DynamicFloatingDamageFormatterPlugin extends JavaPlugin {
    private final Config<DamageNumberConfig> damageNumberConfig;

    public DynamicFloatingDamageFormatterPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        this.damageNumberConfig = this.withConfig("DamageNumberConfig", DamageNumberConfig.CODEC);
    }

    @Override
    protected void setup() {
        try {
            damageNumberConfig.save().join();
            DamageNumbers.applyConfig(damageNumberConfig.get());
        } catch (Throwable t) {
            System.err.println("[DynamicFloatingDamageFormatter] Failed to load DamageNumberConfig: " + t.getMessage());
            t.printStackTrace();
        }

        tryRegisterDamageSystem("irai.mod.reforge.Entity.Events.DamageNumberEST");
    }

    private void tryRegisterDamageSystem(String className) {
        try {
            Class<?> clazz = Class.forName(className);
            Object system = clazz.getDeclaredConstructor().newInstance();
            Object registry = this.getEntityStoreRegistry();
            java.lang.reflect.Method register = null;
            for (java.lang.reflect.Method method : registry.getClass().getMethods()) {
                if ("registerSystem".equals(method.getName()) && method.getParameterCount() == 1) {
                    register = method;
                    break;
                }
            }
            if (register == null) {
                System.out.println("[DynamicFloatingDamageFormatter] registerSystem(...) not found; skipping adapter.");
                return;
            }
            register.invoke(registry, system);
            System.out.println("[DynamicFloatingDamageFormatter] Registered damage system: " + className);
        } catch (ClassNotFoundException e) {
            System.out.println("[DynamicFloatingDamageFormatter] No adapter system found (" + className + ")." +
                    " Use manual emit or include an adapter system.");
        } catch (Throwable t) {
            System.err.println("[DynamicFloatingDamageFormatter] Failed to register damage system: " + t.getMessage());
            t.printStackTrace();
        }
    }
}