package com.pwp.coreclient.mixin;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.ByteBuffer;

@Mixin(Minecraft.class)
public class WindowIconMixin {

    @Inject(method = "<init>", at = @At("TAIL"))
    private void pwp_setCustomWindow(CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        long handle = mc.getWindow().getWindow();
        if (handle == 0) return;

        GLFW.glfwSetWindowTitle(handle, "PWP");

        try (InputStream stream = Minecraft.class.getResourceAsStream("/assets/pwp_core_client/icons/icon_32x32.png")) {
            if (stream == null) return;

            BufferedImage img = ImageIO.read(stream);
            if (img == null) return;

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

            GLFWImage.Buffer glfwBuf = GLFWImage.malloc(1);
            glfwBuf.put(0, glfwImg);

            GLFW.glfwSetWindowIcon(handle, glfwBuf);
            glfwBuf.free();
            glfwImg.free();
            MemoryUtil.memFree(buf);
        } catch (Exception e) {
        }
    }
}
