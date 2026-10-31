package io.wispforest.owo.braid.core.cursor;

import org.lwjgl.sdl.SDLMouse;

public final class SystemCursorStyle implements CursorStyle {
    public final Integer shape;

    SystemCursorStyle(Integer shape) {
        this.shape = shape;
    }

    @Override
    public long allocate() {
        return SDLMouse.SDL_CreateSystemCursor(this.shape);
    }
}
