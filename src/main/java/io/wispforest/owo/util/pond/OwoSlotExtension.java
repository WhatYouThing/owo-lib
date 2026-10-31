package io.wispforest.owo.util.pond;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.jetbrains.annotations.Nullable;

public interface OwoSlotExtension {

    void owo$setDisabledOverride(boolean disabled);

    boolean owo$getDisabledOverride();

    void owo$setScissorArea(@Nullable ScreenRectangle scissor);

    @Nullable ScreenRectangle owo$getScissorArea();
}
