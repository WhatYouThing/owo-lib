package io.wispforest.owo.braid.core.events;

public sealed interface UserEvent permits
        CloseEvent,
        CharInputEvent,
        KeyPressEvent,
        KeyReleaseEvent,
        MouseButtonPressEvent,
        MouseButtonReleaseEvent,
        MouseMoveEvent,
        MouseScrollEvent {
}
