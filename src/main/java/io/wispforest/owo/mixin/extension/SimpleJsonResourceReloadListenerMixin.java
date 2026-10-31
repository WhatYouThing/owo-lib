package io.wispforest.owo.mixin.extension;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.serialization.JsonOps;
import io.wispforest.owo.Owo;
import io.wispforest.owo.util.RecipeRemainderStorage;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.io.Reader;
import java.util.HashMap;

@Mixin(SimpleJsonResourceReloadListener.class)
public abstract class SimpleJsonResourceReloadListenerMixin {

    @Shadow
    @Final
    private FileToIdConverter lister;

    @WrapOperation(
            method = "prepare(Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)Ljava/util/Map;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/StrictJsonParser;parse(Ljava/io/Reader;)Lcom/google/gson/JsonElement;"
            )
    )
    private JsonElement loadRecipeExtensions(
            Reader reader,
            Operation<JsonElement> original,
            @Local(name = "id") Identifier recipeId
    ) {
        var element = original.call(reader);

        if (/* RecipeManagerAccessor.owo$getFinder() == this.lister && */ element instanceof JsonObject json) {
            if (json.has(Owo.id("remainders").toString())) {
                var remainders = new HashMap<Item, ItemStackTemplate>();

                for (var remainderEntry : json.getAsJsonObject(Owo.id("remainders").toString()).entrySet()) {
                    var item = GsonHelper.convertToItem(new JsonPrimitive(remainderEntry.getKey()), remainderEntry.getKey());

                    if (remainderEntry.getValue().isJsonObject()) {
                        var remainderStack = ItemStackTemplate.CODEC.parse(
                                JsonOps.INSTANCE,
                                remainderEntry.getValue().getAsJsonObject()
                        ).getOrThrow(JsonParseException::new);
                        remainders.put(item.value(), remainderStack);
                    } else {
                        var remainderItem = GsonHelper.convertToItem(remainderEntry.getValue(), "item");
                        remainders.put(item.value(), new ItemStackTemplate(remainderItem, 1, DataComponentPatch.EMPTY));
                    }
                }

                if (!remainders.isEmpty()) RecipeRemainderStorage.store(recipeId, remainders);
            }
        }

        return element;
    }
}
