package com.astralis.bundledarrows.mixin;

import com.astralis.bundledarrows.BundledArrowsMod;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem; // VERIFY: official name for Yarn's RangedWeaponItem
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// NOTE: despite the file name (kept from upstream), this mixin targets
// ProjectileWeaponItem, same as in the original 1.21.11 source — not Player.
@Mixin(ProjectileWeaponItem.class)
public class PlayerEntityMixin {

	@Inject(
			method = "getProjectile",
			at = @At(
					value = "INVOKE",
					// VERIFY: ItemStack's split-stack method may still be
					// named "split", but double-check the descriptor below
					// matches the real 26.2 method signature/return type.
					target = "Lnet/minecraft/world/item/ItemStack;split(I)Lnet/minecraft/world/item/ItemStack;",
					shift = At.Shift.BEFORE
			)
	)
	private static void beforeSplitProjectile(ItemStack stack, ItemStack projectileStack, LivingEntity shooter, boolean multishot, CallbackInfoReturnable<ItemStack> cir) {
		if (shooter instanceof Player player && !player.getAbilities().instabuild) { // VERIFY: creative-mode flag name (Yarn: creativeMode)

			int arrowCount = BundledArrowsMod.countArrowsInInventory(player);

			if (arrowCount == 0) {
				ItemStack bundleArrow = BundledArrowsMod.findArrowInBundle(player);

				if (!bundleArrow.isEmpty()) {
					boolean hasInfinity = stack.isEnchanted() &&
							stack.getEnchantments().entrySet().stream() // VERIFY: enchantment-map accessor name
									.anyMatch(entry -> entry.getKey().getRegisteredName().contains("infinity"));

					boolean isNormalArrow = bundleArrow.getItem().toString().equals("arrow");

					if (!hasInfinity || !isNormalArrow) {
						BundledArrowsMod.consumeArrowFromBundle(player);
					}
				}
			}
		}
	}
}
