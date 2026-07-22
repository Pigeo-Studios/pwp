package net.pwp.early;

import net.minecraftforge.fml.loading.ImmediateWindowProvider;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVidMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

public class PWPEarlyWindowProvider implements ImmediateWindowProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger("PWP-EARLY");

    private long window;
    private int winWidth = 854;
    private int winHeight = 480;
    private int fbWidth = 854;
    private int fbHeight = 480;
    private int winX;
    private int winY;
    private String glVersion = "3.2";
    private Method loadingOverlay;
    private Runnable periodicTick = () -> {};

    @Override
    public String name() {
        return "pwp";
    }

    @Override
    public Runnable initialize(String[] arguments) {
        initWindow();
        periodicTick = () -> GLFW.glfwPollEvents();
        return periodicTick;
    }

    private void initWindow() {
        if (!GLFW.glfwInit()) {
            throw new RuntimeException("Failed to initialize GLFW");
        }

        GLFW.glfwDefaultWindowHints();
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 2);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE);

        window = GLFW.glfwCreateWindow(winWidth, winHeight, "PWP", 0L, 0L);
        if (window == 0L) {
            throw new RuntimeException("Failed to create GLFW window");
        }

        GLFW.glfwMakeContextCurrent(window);
        GLFW.glfwSwapInterval(1);
        org.lwjgl.opengl.GL.createCapabilities();
        glVersion = org.lwjgl.opengl.GL11C.glGetString(org.lwjgl.opengl.GL11C.GL_VERSION);

        org.lwjgl.opengl.GL11C.glClearColor(0.05f, 0.07f, 0.09f, 1.0f);

        GLFWVidMode vidmode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor());
        if (vidmode != null) {
            winX = (vidmode.width() - winWidth) / 2;
            winY = (vidmode.height() - winHeight) / 2;
            GLFW.glfwSetWindowPos(window, winX, winY);
        }

        GLFW.glfwShowWindow(window);
        GLFW.glfwMakeContextCurrent(0);

        GLFW.glfwSetFramebufferSizeCallback(window, (w, width, height) -> {
            fbWidth = width;
            fbHeight = height;
        });
        GLFW.glfwSetWindowPosCallback(window, (w, x, y) -> {
            winX = x;
            winY = y;
        });
        GLFW.glfwSetWindowSizeCallback(window, (w, width, height) -> {
            winWidth = width;
            winHeight = height;
        });

        LOGGER.info("PWP Early Display initialized ({}x{})", winWidth, winHeight);
    }

    @Override
    public void updateFramebufferSize(IntConsumer width, IntConsumer height) {
        width.accept(fbWidth);
        height.accept(fbHeight);
    }

    @Override
    public long setupMinecraftWindow(IntSupplier width, IntSupplier height, Supplier<String> title, LongSupplier monitor) {
        GLFW.glfwMakeContextCurrent(window);
        GLFW.glfwSetWindowTitle(window, title.get());
        GLFW.glfwSwapInterval(0);

        var fbCallback = GLFW.glfwSetFramebufferSizeCallback(window, null);
        if (fbCallback != null) fbCallback.free();
        var wpCallback = GLFW.glfwSetWindowPosCallback(window, null);
        if (wpCallback != null) wpCallback.free();
        var wsCallback = GLFW.glfwSetWindowSizeCallback(window, null);
        if (wsCallback != null) wsCallback.free();

        return window;
    }

    @Override
    public boolean positionWindow(Optional<Object> monitor, IntConsumer widthSetter, IntConsumer heightSetter, IntConsumer xSetter, IntConsumer ySetter) {
        widthSetter.accept(winWidth);
        heightSetter.accept(winHeight);
        xSetter.accept(winX);
        ySetter.accept(winY);
        return true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Supplier<T> loadingOverlay(Supplier<?> mc, Supplier<?> ri, Consumer<Optional<Throwable>> ex, boolean fade) {
        try {
            return (Supplier<T>) loadingOverlay.invoke(null, mc, ri, ex, this);
        } catch (Throwable e) {
            throw new RuntimeException("Failed to create loading overlay", e);
        }
    }

    @Override
    public void updateModuleReads(ModuleLayer layer) {
        Optional<Module> forgeModule = layer.findModule("forge");
        if (forgeModule.isPresent()) {
            getClass().getModule().addReads(forgeModule.get());
        }

        try {
            ClassLoader cl = ImmediateWindowProvider.class.getClassLoader();
            Class<?> clz = forgeModule.isPresent()
                ? Class.forName(forgeModule.get(), "net.minecraftforge.client.loading.ForgeLoadingOverlay")
                : Class.forName("net.minecraftforge.client.loading.ForgeLoadingOverlay", false, cl);
            for (Method mtd : clz.getDeclaredMethods()) {
                if (Modifier.isStatic(mtd.getModifiers()) && "newInstance".equals(mtd.getName())) {
                    loadingOverlay = mtd;
                    break;
                }
            }
            if (loadingOverlay == null) {
                LOGGER.warn("ForgeLoadingOverlay.newInstance not found");
            }
        } catch (Exception e) {
            LOGGER.error("Failed to find ForgeLoadingOverlay", e);
        }
    }

    @Override
    public void periodicTick() {
        periodicTick.run();
    }

    @Override
    public String getGLVersion() {
        return glVersion;
    }
}
