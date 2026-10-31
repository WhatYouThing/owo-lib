package io.wispforest.owo.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = Minecraft.class, priority = 0)
public class MinecraftMixin {

    // TODO: mixin targets are invalid now, not sure how to fix
    /*
    @SuppressWarnings({"MixinAnnotationTarget"})
    @Group(name = "clientFreezeHooks", min = 1, max = 1)
    @Inject(method = "<init>", at = @At(value = "INVOKE", remap = false,
            target = "Lnet/fabricmc/loader/impl/game/minecraft/Hooks;startClient(Ljava/io/File;Ljava/lang/Object;)V", shift = At.Shift.AFTER))
    private void afterFabricHook(GameConfig args, CallbackInfo ci) {
        OwoFreezer.freeze();
    }

    @SuppressWarnings({"MixinAnnotationTarget"})
    @Group(name = "clientFreezeHooks", min = 1, max = 1)
    @Inject(method = "<init>", at = @At(value = "INVOKE", remap = false,
            target = "Lorg/quiltmc/loader/impl/game/minecraft/Hooks;startClient(Ljava/io/File;Ljava/lang/Object;)V", shift = At.Shift.AFTER))
    private void afterQuiltHook(GameConfig args, CallbackInfo ci) {
        OwoFreezer.freeze();
    }
     */

}

