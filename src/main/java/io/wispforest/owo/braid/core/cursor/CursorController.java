package io.wispforest.owo.braid.core.cursor;

import org.lwjgl.sdl.SDLMouse;

import java.util.HashMap;
import java.util.Map;

public class CursorController {

    private CursorStyle lastCursorStyle = CursorStyle.NONE;
    private boolean disposed = false;

    public CursorController() {
    }

    @Deprecated // cursor shapes are now global with SDL
    public CursorController(long windowHandle) {
    }

    public CursorStyle currentStyle() {
        return this.lastCursorStyle;
    }

    public void setStyle(CursorStyle style) {
        if (this.disposed || this.lastCursorStyle == style) return;

        if (style == CursorStyle.NONE) {
            SDLMouse.SDL_DestroyCursor(SDLMouse.SDL_GetCursor());
        } else {
            SDLMouse.SDL_SetCursor(style.allocate());
        }

        this.lastCursorStyle = style;
    }

    public void dispose() {
        if (this.disposed) return;
        this.disposed = true;
    }
}
