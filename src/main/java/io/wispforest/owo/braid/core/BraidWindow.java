package io.wispforest.owo.braid.core;

import com.google.common.base.Suppliers;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.DisplayData;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.platform.WindowEventHandler;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.device.GpuBackend;
import com.mojang.renderpearl.api.device.GpuSurface;
import com.mojang.renderpearl.api.device.SurfaceException;
import com.mojang.renderpearl.backend.opengl.GlTexture;
import io.wispforest.owo.Owo;
import io.wispforest.owo.braid.core.cursor.CursorController;
import io.wispforest.owo.braid.core.cursor.CursorStyle;
import io.wispforest.owo.braid.core.events.*;
import io.wispforest.owo.braid.framework.widget.Widget;
import io.wispforest.owo.braid.util.BraidGuiRenderer;
import io.wispforest.owo.mixin.braid.MinecraftAccessor;
import io.wispforest.owo.util.EventSource;
import io.wispforest.owo.util.EventStream;
import net.minecraft.client.Minecraft;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL32;
import org.lwjgl.sdl.SDLEvents;
import org.lwjgl.sdl.SDLVideo;
import org.lwjgl.sdl.SDL_Event;
import org.lwjgl.system.NativeResource;

import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

// TODO: consider somehow getting notified or polling
//       for changes in the gui scale option so we can react
//       instantly when it changes rather than on next resize
public class BraidWindow implements Surface {

    public static final AtomicReference<GpuBackend> GPU_BACKEND = new AtomicReference<>();

    public final EventBinding eventBinding = new WindowEventBinding(this);

    public final Window backendWindow;
    private final Swapchain swapchain;

    private final List<NativeResource> resources = new ArrayList<>();

    private final EventStream<ResizeCallback> onResize = ResizeCallback.newStream();
    private TextureTarget remoteTarget;

    public final BraidGuiRenderer guiRenderer;

    private final CursorController cursorController;

    private int scaleFactor;

    public BraidWindow(String title, int width, int height) {
        this.backendWindow = new Window(
                new WindowEventHandler() {
                    @Override
                    public void framebufferSizeChanged() {
                        withContext(Minecraft.getInstance().getWindow().handle(), () -> {
                            remoteTarget.destroyBuffers();
                            remoteTarget = new TextureTarget("braid window", backendWindow.getWidth(), backendWindow.getHeight(), GpuFormat.RGBA8_UNORM, GpuFormat.RGBA8_UNORM);
                        });

                        onResize.sink().onResize(backendWindow.getGuiScaledWidth(), backendWindow.getGuiScaledHeight());

                        resizeSwapchain();
                    }

                    @Override
                    public void resizeGui() {
                    }

                    @Override
                    public void cursorEntered() {
                    }

                    @Override
                    public void fullscreenStateChanged(boolean fullscreen) {

                    }
                },
                new DisplayData(width, height, OptionalInt.empty(), OptionalInt.empty(), false),
                null,
                false,
                title,
                ((MinecraftAccessor) Minecraft.getInstance()).owo$getMonitorManager(),
                GPU_BACKEND.get()
        );

        SDLVideo.SDL_ShowWindow(this.backendWindow.handle());

        this.swapchain = IS_VULKAN.get()
                ? new VulkanSwapchain()
                : new GlSwapchain();

        this.cursorController = new CursorController(this.backendWindow.handle());
        this.guiRenderer = new BraidGuiRenderer(Minecraft.getInstance());

        this.remoteTarget = new TextureTarget("braid window", this.backendWindow.getWidth(), this.backendWindow.getHeight(), GpuFormat.RGBA8_UNORM, GpuFormat.RGBA8_UNORM);
        this.resizeSwapchain();

        Thread.startVirtualThread(() -> {
            boolean open = true;
            SDL_Event event = null;
            while (open) {
                while (SDLEvents.SDL_PollEvent(event)) {
                    switch (event.type()) {
                        case SDLEvents.SDL_EVENT_QUIT -> {
                            this.eventBinding.add(CloseEvent.INSTANCE);
                            open = false;
                        }
                        // TODO: handle mouse event modifiers some other way since they are not passed by SDL
                        case SDLEvents.SDL_EVENT_MOUSE_BUTTON_DOWN ->
                                this.eventBinding.add(new MouseButtonPressEvent(event.button().button(), new KeyModifiers(0)));
                        case SDLEvents.SDL_EVENT_MOUSE_BUTTON_UP ->
                                this.eventBinding.add(new MouseButtonReleaseEvent(event.button().button(), new KeyModifiers(0)));
                        case SDLEvents.SDL_EVENT_MOUSE_MOTION -> this.eventBinding.add(new MouseMoveEvent(
                                event.motion().x() / this.scaleFactor,
                                event.motion().y() / this.scaleFactor
                        ));
                        case SDLEvents.SDL_EVENT_MOUSE_WHEEL ->
                                this.eventBinding.add(new MouseScrollEvent(event.wheel().x(), event.wheel().y()));
                        case SDLEvents.SDL_EVENT_KEY_DOWN ->
                                this.eventBinding.add(new KeyPressEvent(event.key().key(), event.key().scancode(), new KeyModifiers(event.key().mod())));
                        case SDLEvents.SDL_EVENT_KEY_UP ->
                                this.eventBinding.add(new KeyReleaseEvent(event.key().key(), event.key().scancode(), new KeyModifiers(event.key().mod())));
                        case SDLEvents.SDL_EVENT_TEXT_INPUT -> {
                            String string = event.text().textString();
                            if (string != null) {
                                for (char character : string.toCharArray()) {
                                    this.eventBinding.add(new CharInputEvent(character, new KeyModifiers(0)));
                                }
                            }
                        }
                        case SDLEvents.SDL_EVENT_DROP_FILE -> {
                            var path = event.drop().dataString();
                            if (path != null) {
                                try {
                                    this.eventBinding.add(new FileDroppedEvent(Paths.get(path)));
                                } catch (InvalidPathException e) {
                                    Owo.LOGGER.error("Failed to parse path '{}'", path, e);
                                }
                            }
                        }
                    }
                }
            }
        });
    }

    private void resizeSwapchain() {
        this.swapchain.resize();
        this.recalculateScale();
    }

    private void recalculateScale() {
        var guiScale = Minecraft.getInstance().options.guiScale().get();
        var forceUnicodeFont = Minecraft.getInstance().options.forceUnicodeFont().get();

        this.scaleFactor = this.backendWindow.calculateScale(guiScale, forceUnicodeFont);
        this.backendWindow.setGuiScale(scaleFactor);
    }

    public static OpenResult open(String title, int width, int height, Widget widget) {
        var window = new BraidWindow(title, width, height);
        var app = new AppState(
                Owo.LOGGER,
                AppState.formatName("BraidWindow", widget, title),
                Minecraft.getInstance(),
                window,
                window.eventBinding,
                widget
        );

        BraidWindowScheduler.add(window, app);
        return new OpenResult(app, window);
    }

    // ---

    @Override
    public void dispose() {
        this.backendWindow.close();
        this.swapchain.dispose();
        this.cursorController.dispose();

        this.guiRenderer.close();

        this.remoteTarget.destroyBuffers();

        for (var resource : this.resources) {
//            resource.free();
        }
    }

    // ---

    @Override
    public int width() {
        return this.backendWindow.getGuiScaledWidth();
    }

    @Override
    public int height() {
        return this.backendWindow.getGuiScaledHeight();
    }

    @Override
    public double scaleFactor() {
        return this.scaleFactor;
    }

    @Override
    public EventSource<ResizeCallback> onResize() {
        return this.onResize.source();
    }

    @Override
    public CursorStyle currentCursorStyle() {
        return this.cursorController.currentStyle();
    }

    @Override
    public void setCursorStyle(CursorStyle style) {
        this.cursorController.setStyle(style);
    }

    // ---

    @Override
    public void beginRendering() {
        this.swapchain.prepareFrame();

        RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(
                this.remoteTarget.getColorTexture(),
                new Vector4f(0, 0, 0, 1),
                this.remoteTarget.getDepthTexture(),
                1
        );
    }

    @Override
    public void endRendering() {
        this.guiRenderer.render(new BraidGuiRenderer.Target(
                this.remoteTarget,
                this
        ));

        // ---

        this.swapchain.present();
    }

    // ---

    private <R extends NativeResource> R storeNativeResource(R resource) {
        this.resources.add(resource);
        return resource;
    }

    private static void withContext(long contextHandle, Runnable fn) {
        if (IS_VULKAN.get()) {
            fn.run();
            return;
        }

        var activeContext = SDLVideo.SDL_GL_GetCurrentContext();

        try { // may or may not work
            SDLVideo.SDL_GL_MakeCurrent(Minecraft.getInstance().getWindow().handle(), contextHandle);
            fn.run();
        } finally {
            SDLVideo.SDL_GL_MakeCurrent(Minecraft.getInstance().getWindow().handle(), activeContext);
        }
    }

    private static final Supplier<Boolean> IS_VULKAN = Suppliers.memoize(() -> !RenderSystem.getDevice().getDeviceInfo().backendName().equals("OpenGL"));

    public static class WindowEventBinding extends EventBinding {

        public final BraidWindow window;

        public WindowEventBinding(BraidWindow window) {
            this.window = window;
        }

        @Override
        public boolean isKeyPressed(int keyCode) {
            return InputConstants.isKeyDown(keyCode);
        }
    }

    public record OpenResult(AppState state, BraidWindow window) {
    }

    private sealed interface Swapchain permits GlSwapchain, VulkanSwapchain {
        void prepareFrame();

        void present();

        void resize();

        void dispose();
    }

    private final class GlSwapchain implements Swapchain {

        private int localFbo;

        @Override
        public void prepareFrame() {
        }

        @Override
        public void present() {
            withContext(backendWindow.handle(), () -> {
                GL32.glBindFramebuffer(GL32.GL_READ_FRAMEBUFFER, this.localFbo);
                GL32.glBindFramebuffer(GL32.GL_DRAW_FRAMEBUFFER, 0);

                GL32.glBlitFramebuffer(
                        0, 0, backendWindow.getWidth(), backendWindow.getHeight(),
                        0, 0, backendWindow.getWidth(), backendWindow.getHeight(),
                        GL32.GL_COLOR_BUFFER_BIT,
                        GL32.GL_NEAREST
                );

                SDLVideo.SDL_GL_SwapWindow(backendWindow.handle());
            });
        }

        @Override
        public void resize() {
            withContext(backendWindow.handle(), () -> {
                if (this.localFbo != 0) {
                    GL32.glDeleteFramebuffers(this.localFbo);
                }

                this.localFbo = GL32.glGenFramebuffers();
                GL32.glBindFramebuffer(GL32.GL_FRAMEBUFFER, this.localFbo);
                GL32.glFramebufferTexture2D(GL32.GL_FRAMEBUFFER, GL32.GL_COLOR_ATTACHMENT0, GL32.GL_TEXTURE_2D, ((GlTexture) remoteTarget.getColorTexture()).glId(), 0);

                if (GL32.glCheckFramebufferStatus(GL32.GL_FRAMEBUFFER) != GL32.GL_FRAMEBUFFER_COMPLETE) {
                    throw new UnsupportedOperationException("Failed to initialize local FBO");
                }
            });
        }

        @Override
        public void dispose() {
            GL32.glDeleteFramebuffers(this.localFbo);
        }
    }

    private final class VulkanSwapchain implements Swapchain {

        private final GpuSurface backendWindowSurface = RenderSystem.getDevice().createSurface(backendWindow.handle(), backendWindow::isIconified);
        private boolean surfaceValid = false;

        @Override
        public void prepareFrame() {
            if (!this.surfaceValid) {
                try {
                    this.backendWindowSurface.configure(new GpuSurface.Configuration(
                            backendWindow.getWidth(),
                            backendWindow.getHeight(),
                            GpuSurface.PresentMode.getSupportedVsyncMode(this.backendWindowSurface.supportedPresentModes(), false)
                    ));
                    this.surfaceValid = true;
                } catch (SurfaceException e) {
                    Owo.LOGGER.warn("Failed to resize braid window");
                }
            }

            if (!this.surfaceValid) {
                return;
            }

            try {
                this.backendWindowSurface.acquireNextTexture();
            } catch (SurfaceException e) {
                Owo.LOGGER.warn("Failed to acquire texture");
            }
        }

        @Override
        public void present() {
            if (!this.backendWindowSurface.isAcquired()) {
                return;
            }

            this.backendWindowSurface.blitFromTexture(
                    RenderSystem.getDevice().createCommandEncoder(),
                    remoteTarget.getColorTextureView()
            );

            RenderSystem.getDevice().createCommandEncoder().submit();
            this.backendWindowSurface.present();
        }

        @Override
        public void resize() {
            this.surfaceValid = false;
        }

        @Override
        public void dispose() {
            this.backendWindowSurface.close();
        }
    }
}
