package cn.meowsmp.carpetmeowaddition.mixin.rule;

import cn.meowsmp.carpetmeowaddition.CarpetMeowAdditionSettings;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.recipe.RepairItemRecipe;
import net.minecraft.util.collection.DefaultedList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(RecipeManager.class)
public class RecipeManagerMixin<C extends Inventory> {
    @SuppressWarnings({"MixinExtrasOperationParameters", "rawtypes"})
    @WrapOperation(method = "getRemainingStacks", at = @At(value = "INVOKE", target = "Lnet/minecraft/recipe/Recipe;getRemainder(Lnet/minecraft/inventory/Inventory;)Lnet/minecraft/util/collection/DefaultedList;"))
    private DefaultedList<ItemStack> getRemainingStacks(Recipe instance, C inventory, Operation<DefaultedList<ItemStack>> original) {
        // 不能是物品修复配方
        if (CarpetMeowAdditionSettings.elytraRecipeRemainder && !(instance instanceof RepairItemRecipe)) {
            return getRemainder(inventory);
        }
        return original.call(instance, inventory);
    }

    @Unique
    private DefaultedList<ItemStack> getRemainder(C inventory) {
        DefaultedList<ItemStack> defaultedList = DefaultedList.ofSize(inventory.size(), ItemStack.EMPTY);
        for (int i = 0; i < defaultedList.size(); ++i) {
            Item item = inventory.getStack(i).getItem();
            if (item.hasRecipeRemainder() && item.getRecipeRemainder() != null) {
                defaultedList.set(i, new ItemStack(item.getRecipeRemainder()));
            } else if (item == Items.ELYTRA) {
                defaultedList.set(i, inventory.getStack(i).copy());
            }
        }
        return defaultedList;
    }
}
