package com.pigeostudios.pwp.drone.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

public class FPVShaderHandler {
    private static PostChain shader;
    private static boolean loaded;
    private static boolean loadFailed;
    private static boolean resolved;
    private static Method cachedGetEffect;
    private static Method cachedGetUniform;
    private static Method cachedSetFloat;
    private static int lastWidth = -1;
    private static int lastHeight = -1;

    public static void load() {
        if (loaded || loadFailed) return;
        Minecraft mc = Minecraft.getInstance();
        try {
            ResourceManager resMgr = mc.getResourceManager();
            RenderTarget target = mc.getMainRenderTarget();
            ResourceLocation loc = new ResourceLocation("pwpdrone", "shaders/post/fpv");
            shader = new PostChain(mc.getTextureManager(), resMgr, target, loc);
            if (shader != null) {
                shader.resize(mc.getWindow().getWidth(), mc.getWindow().getHeight());
                lastWidth = mc.getWindow().getWidth();
                lastHeight = mc.getWindow().getHeight();
            }
            loaded = true;
            resolved = false;
            System.out.println("[PWPDrone] FPV Shader loaded");
        } catch (Exception e) {
            loadFailed = true;
            System.err.println("[PWPDrone] FPV Shader load failed: " + e.getMessage());
        }
    }

    public static void render(float partialTick, float dist) {
        if (!loaded || shader == null) return;
        Minecraft mc = Minecraft.getInstance();

        // Check resize
        if (mc.getWindow().getWidth() != lastWidth || mc.getWindow().getHeight() != lastHeight) {
            lastWidth = mc.getWindow().getWidth();
            lastHeight = mc.getWindow().getHeight();
            shader.resize(lastWidth, lastHeight);
        }

        try {
            // Set uniforms via reflection (PostChain internals are private)
            float time = mc.level != null ? (mc.level.getGameTime() + partialTick) * 0.05f % 100 :
                          System.currentTimeMillis() / 850f % 100;
            setUniform("Time", time);
            setUniform("Dist", dist);

            shader.process(partialTick);
            mc.getMainRenderTarget().bindWrite(false);
        } catch (Exception e) {
            System.err.println("[PWPDrone] Shader render error: " + e.getMessage());
            loaded = false;
            loadFailed = true;
            mc.getMainRenderTarget().bindWrite(false);
        }
    }

    public static void renderWithDist(float partialTick, float dist) {
        loadIfNeeded();
        render(partialTick, dist);
    }

    public static void loadIfNeeded() {
        if (!loaded && !loadFailed) load();
    }

    public static void reset() {
        if (shader != null) {
            shader.close();
            shader = null;
        }
        loaded = false;
        loadFailed = false;
        resolved = false;
        cachedGetEffect = null;
        cachedGetUniform = null;
        cachedSetFloat = null;
        lastWidth = -1;
        lastHeight = -1;
    }

    private static void setUniform(String name, float value) {
        if (!resolved) resolve();
        if (cachedGetEffect == null || cachedGetUniform == null || cachedSetFloat == null) return;

        try {
            List<?> passes = getPassesList();
            if (passes == null) return;

            for (Object pass : passes) {
                Object effect = cachedGetEffect.invoke(pass);
                if (effect == null) continue;
                Object uniform = cachedGetUniform.invoke(effect, name);
                if (uniform == null) continue;
                cachedSetFloat.invoke(uniform, value);
            }
        } catch (Exception ignored) {}
    }

    private static void resolve() {
        resolved = true;
        try {
            List<?> passes = getPassesList();
            if (passes == null || passes.isEmpty()) return;

            Object firstPass = passes.get(0);
            Class<?> passClass = firstPass.getClass();

            // Find getEffect() method
            for (Method m : passClass.getDeclaredMethods()) {
                String n = m.getName();
                if ((n.equals("getEffect") || n.equals("getShader"))
                    && m.getParameterCount() == 0) {
                    cachedGetEffect = m;
                    break;
                }
            }

            if (cachedGetEffect == null) return;
            Object effect = cachedGetEffect.invoke(firstPass);
            if (effect == null) return;
            Class<?> effectClass = effect.getClass();

            // Find getUniform(String) method
            for (Method m : effectClass.getDeclaredMethods()) {
                String n = m.getName();
                if ((n.equals("getUniform") || n.startsWith("m_109") || n.startsWith("m_173") || n.startsWith("m_83"))
                    && m.getParameterCount() == 1 && m.getParameterTypes()[0] == String.class) {
                    cachedGetUniform = m;
                    break;
                }
            }
            if (cachedGetUniform == null) {
                for (Method m : effectClass.getDeclaredMethods()) {
                    if (m.getParameterCount() == 1 && m.getParameterTypes()[0] == String.class
                        && (m.getReturnType().getSimpleName().contains("Uniform") || m.getReturnType().getSimpleName().equals("Object"))) {
                        cachedGetUniform = m;
                        break;
                    }
                }
            }

            if (cachedGetUniform == null) return;
            Object timeUniform = cachedGetUniform.invoke(effect, "Time");
            if (timeUniform == null) return;

            // Find set(float) method
            for (Method m : timeUniform.getClass().getDeclaredMethods()) {
                String n = m.getName();
                if ((n.equals("set") || n.startsWith("m_"))
                    && m.getParameterCount() == 1 && m.getParameterTypes()[0] == float.class) {
                    cachedSetFloat = m;
                    break;
                }
            }
            if (cachedSetFloat == null) {
                for (Method m : timeUniform.getClass().getDeclaredMethods()) {
                    if (m.getParameterCount() == 1 && m.getParameterTypes()[0] == float.class
                        && m.getReturnType().getSimpleName().equals("void")) {
                        cachedSetFloat = m;
                        break;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[PWPDrone] resolve error: " + e.getMessage());
        }
    }

    private static List<?> getPassesList() {
        if (shader == null) return null;
        try {
            for (Field f : shader.getClass().getDeclaredFields()) {
                if (f.getType() == List.class) {
                    f.setAccessible(true);
                    Object val = f.get(shader);
                    if (val instanceof List) return (List<?>) val;
                }
            }
        } catch (Exception ignored) {}
        return null;
    }
}
