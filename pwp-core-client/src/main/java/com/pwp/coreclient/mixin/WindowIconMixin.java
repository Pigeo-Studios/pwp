package com.pwp.coreclient.mixin;

import net.minecraft.client.Minecraft;
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

@Mixin(Minecraft.class)
public class WindowIconMixin {

    private static final Logger LOGGER = LoggerFactory.getLogger("PWP");

    @Inject(method = "<init>", at = @At("TAIL"))
    private void pwp_setCustomWindow(CallbackInfo ci) {
        Minecraft mc = (Minecraft)(Object)this;
        long handle = mc.getWindow().getWindow();
        if (handle == 0) {
            LOGGER.warn("Window handle is 0, cannot set icon/title");
            return;
        }

        GLFW.glfwSetWindowTitle(handle, "PWP");

        try {
            setWindowIcon(handle, "icon_16x16.png", "icon_32x32.png");
        } catch (Exception e) {
            LOGGER.error("Failed to set window icon: {}", e.getMessage());
        }
    }

    private static void setWindowIcon(long handle, String icon16, String icon32) {
        GLFWImage.Buffer glfwBuf = GLFWImage.malloc(2);
        List<ByteBuffer> buffers = new ArrayList<>();
        int count = 0;

        for (String name : new String[]{icon16, icon32}) {
            String path = "/assets/pwp_core_client/icons/" + name;
            try (InputStream stream = WindowIconMixin.class.getResourceAsStream(path)) {
                if (stream == null) {
                    LOGGER.warn("Icon not found: {}", path);
                    continue;
                }
                BufferedImage img = ImageIO.read(stream);
                if (img == null) {
                    LOGGER.warn("Failed to decode icon: {}", path);
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
            } catch (Exception e) {
                LOGGER.error("Error loading icon {}: {}", name, e.getMessage());
            }
        }

        if (count > 0) {
            GLFW.glfwSetWindowIcon(handle, glfwBuf);
        }

        for (ByteBuffer buf : buffers) {
            MemoryUtil.memFree(buf);
        }
        glfwBuf.free();
    }
}
