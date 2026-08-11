package com.pwp.coreclient.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Mixin(Minecraft.class)
public class WindowIconMixin {

    private static final Logger LOGGER = LoggerFactory.getLogger("PWP");

    private static final ResourceLocation ICON_16 = new ResourceLocation("pwp_core_client", "icons/icon_16x16.png");
    private static final ResourceLocation ICON_32 = new ResourceLocation("pwp_core_client", "icons/icon_32x32.png");

    private static boolean iconSet;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void pwp_setCustomWindow(CallbackInfo ci) {
        Minecraft mc = (Minecraft) (Object) this;
        long handle = mc.getWindow().getWindow();
        if (handle == 0) {
            LOGGER.warn("Window handle is 0, cannot set icon/title");
            return;
        }

        GLFW.glfwSetWindowTitle(handle, "PWP");
        setWindowIcon(mc, handle);
    }

    /**
     * Иконки читаем через resource manager, а не Class#getResourceAsStream: в проде jar-моды
     * Forge подгружает module-layer'ом, и classpath-ресурсы "/assets/..." из них не видны.
     * В конструкторе менеджер ресурсов может ещё не содержать mod_resources — тогда
     * пробуем ещё раз на первом клиентском тике (initial reload к этому моменту завершён).
     */
    private static void setWindowIcon(Minecraft mc, long handle) {
        if (iconSet) return;
        if (loadWindowIcon(handle, mc.getResourceManager())) return;
        MinecraftForge.EVENT_BUS.addListener(WindowIconMixin::onFirstTick);
    }

    @SubscribeEvent
    private static void onFirstTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || iconSet) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.getWindow().getWindow() == 0) return;
        loadWindowIcon(mc.getWindow().getWindow(), mc.getResourceManager());
    }

    private static boolean loadWindowIcon(long handle, ResourceManager resourceManager) {
        if (resourceManager == null) return false;

        GLFWImage.Buffer glfwBuf = GLFWImage.malloc(2);
        List<ByteBuffer> buffers = new ArrayList<>();
        int count = 0;

        try {
            for (ResourceLocation loc : new ResourceLocation[]{ICON_16, ICON_32}) {
                try {
                    Optional<Resource> res = resourceManager.getResource(loc);
                    if (res.isEmpty()) {
                        LOGGER.warn("Icon not found: {}", loc);
                        continue;
                    }
                    try (InputStream stream = res.get().open()) {
                        BufferedImage img = ImageIO.read(stream);
                        if (img == null) {
                            LOGGER.warn("Failed to decode icon: {}", loc);
                            continue;
                        }

                        int w = img.getWidth();
                        int h = img.getHeight();
                        int[] pixels = new int[w * h];
                        img.getRGB(0, 0, w, h, pixels, 0, w);

                        ByteBuffer buf = MemoryUtil.memAlloc(w * h * 4);
                        for (int y = 0; y < h; y++) {
                            for (int x = 0; x < w; x++) {
                                int pixel = pixels[y * w + x];
                                buf.put((byte) ((pixel >> 16) & 0xFF));
                                buf.put((byte) ((pixel >> 8) & 0xFF));
                                buf.put((byte) (pixel & 0xFF));
                                buf.put((byte) ((pixel >> 24) & 0xFF));
                            }
                        }
                        buf.flip();

                        GLFWImage glfwImg = GLFWImage.malloc();
                        glfwImg.set(w, h, buf);
                        glfwBuf.put(count, glfwImg);
                        glfwImg.free();
                        buffers.add(buf);
                        count++;
                    }
                } catch (Exception e) {
                    LOGGER.error("Error loading icon {}: {}", loc, e.getMessage());
                }
            }

            if (count > 0) {
                GLFW.glfwSetWindowIcon(handle, glfwBuf);
                iconSet = true;
            }
        } finally {
            for (ByteBuffer buf : buffers) {
                MemoryUtil.memFree(buf);
            }
            glfwBuf.free();
        }
        return iconSet;
    }
}
