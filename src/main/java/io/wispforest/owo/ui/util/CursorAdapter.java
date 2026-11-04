package io.wispforest.owo.ui.util;

import com.mojang.blaze3d.platform.Window;
import io.wispforest.owo.ui.core.CursorStyle;
import net.minecraft.client.Minecraft;
import org.lwjgl.sdl.SDLMouse;

import java.util.EnumMap;

public class CursorAdapter {

    protected static final CursorStyle[] ACTIVE_STYLES = {CursorStyle.POINTER, CursorStyle.TEXT, CursorStyle.HAND, CursorStyle.CROSSHAIR, CursorStyle.MOVE, CursorStyle.HORIZONTAL_RESIZE, CursorStyle.VERTICAL_RESIZE, CursorStyle.NWSE_RESIZE, CursorStyle.NESW_RESIZE, CursorStyle.NOT_ALLOWED};

    protected final long windowHandle;

    protected CursorStyle lastCursorStyle = CursorStyle.POINTER;
    protected boolean disposed = false;

    protected CursorAdapter(long windowHandle) {
        this.windowHandle = windowHandle;
    }

    public static CursorAdapter ofClientWindow() {
        return new CursorAdapter(Minecraft.getInstance().getWindow().handle());
    }

    public static CursorAdapter ofWindow(Window window) {
        return new CursorAdapter(window.handle());
    }

    public static CursorAdapter ofWindow(long windowHandle) {
        return new CursorAdapter(windowHandle);
    }

    public void applyStyle(CursorStyle style) {
        if (this.disposed || this.lastCursorStyle == style) return;

        if (style == CursorStyle.NONE) {
            SDLMouse.SDL_DestroyCursor(SDLMouse.SDL_GetCursor());
        } else {
            SDLMouse.SDL_SetCursor(SDLMouse.SDL_CreateSystemCursor(style.shape));
        }
        this.lastCursorStyle = style;
    }

    public void dispose() {
        if (this.disposed) return;
        this.disposed = true;
    }
}
