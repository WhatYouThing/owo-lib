package io.wispforest.owo.braid.core.events;

public sealed interface UserEvent permits CharInputEvent, CloseEvent, FileDroppedEvent, KeyPressEvent, KeyReleaseEvent, MouseButtonPressEvent, MouseButtonReleaseEvent, MouseMoveEvent, MouseScrollEvent {
}
