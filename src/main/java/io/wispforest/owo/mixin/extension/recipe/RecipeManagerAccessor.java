package io.wispforest.owo.mixin.extension.recipe;

import net.minecraft.world.item.crafting.RecipeManager;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(RecipeManager.class)
public interface RecipeManagerAccessor {

    // TODO: value got removed
    /*
    @Accessor("RECIPE_LISTER")
    static FileToIdConverter owo$getFinder() {
        throw new UnsupportedOperationException();
    }
     */
}
