package io.wispforest.owo.braid.core.cursor;

import org.lwjgl.sdl.SDLMouse;

import java.util.HashMap;
import java.util.Map;

public class CursorController {

    private final Map<CursorStyle, Long> cursors = new HashMap<>();

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
            if (!this.cursors.containsKey(style)) {
                this.cursors.put(style, style.allocate());
            }

            SDLMouse.SDL_SetCursor(this.cursors.get(style));
        }

        this.lastCursorStyle = style;
    }

    public void dispose() {
        if (this.disposed) return;

        for (var ptr : this.cursors.values()) {
            if (ptr == 0) return;
            SDLMouse.SDL_DestroyCursor(ptr);
        }

        this.disposed = true;
    }
}
