package io.wispforest.owo.mixin.braid;

import com.mojang.blaze3d.platform.DisplayData;
import com.mojang.blaze3d.platform.MonitorManager;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.platform.WindowEventHandler;
import com.mojang.renderpearl.api.device.GpuBackend;
import io.wispforest.owo.braid.core.BraidWindow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Window.class)
public class WindowMixin {

    @Inject(method = "<init>(Lcom/mojang/blaze3d/platform/WindowEventHandler;Lcom/mojang/blaze3d/platform/DisplayData;Ljava/lang/String;ZLjava/lang/String;Lcom/mojang/blaze3d/platform/MonitorManager;Lcom/mojang/renderpearl/api/device/GpuBackend;I)V", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;createWindow(Lcom/mojang/renderpearl/api/device/GpuBackend;IILjava/lang/String;)J"))
    private void onBeforeCreateWindow(WindowEventHandler eventHandler, DisplayData displayData, String fullscreenVideoModeString, boolean exclusiveFullscreen, String title, MonitorManager monitorManager, GpuBackend backend, int maximumSize, CallbackInfo ci) {
        BraidWindow.GPU_BACKEND.set(backend);
    }
}
