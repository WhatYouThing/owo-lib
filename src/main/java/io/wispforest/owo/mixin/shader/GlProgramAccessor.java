package io.wispforest.owo.mixin.shader;

import com.mojang.renderpearl.backend.opengl.GlProgram;
import com.mojang.renderpearl.backend.opengl.Uniform;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(GlProgram.class)
public interface GlProgramAccessor {

    @Accessor("uniforms")
    List<Uniform> owo$getUniforms();

}
