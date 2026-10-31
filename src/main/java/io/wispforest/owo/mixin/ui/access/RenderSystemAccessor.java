package io.wispforest.owo.mixin.ui.access;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RenderSystem.class)
public interface RenderSystemAccessor {
    @Accessor("shaderLightDirections")
    static GpuBufferSlice owo$getShaderLightDirections() {
        throw new UnsupportedOperationException();
    }
}
