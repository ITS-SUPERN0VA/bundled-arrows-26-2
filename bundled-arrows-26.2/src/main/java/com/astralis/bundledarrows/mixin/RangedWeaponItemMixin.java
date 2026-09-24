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

import java.util.function.Predicate;

@Mixin(ProjectileWeaponItem.class)
public class RangedWeaponItemMixin {

	@Inject(method = "getProjectile", at = @At("RETURN"), cancellable = true)
	private static void getProjectileFromBundle(ItemStack weaponStack, ItemStack projectileStack, LivingEntity shooter, boolean multishot, CallbackInfoReturnable<ItemStack> cir) {
		if (cir.getReturnValue().isEmpty() && shooter instanceof Player player) {

			int arrowCount = BundledArrowsMod.countArrowsInInventory(player);

			if (arrowCount == 0) {
				ItemStack bundleArrow = BundledArrowsMod.findArrowInBundle(player);

				if (!bundleArrow.isEmpty()) {
					cir.setReturnValue(bundleArrow.copy());
				}
			}
		}
	}

	@Inject(method = "getHeldProjectile", at = @At("RETURN"), cancellable = true)
	private static void getHeldProjectileFromBundle(LivingEntity entity, Predicate<ItemStack> predicate, CallbackInfoReturnable<ItemStack> cir) {
		if (cir.getReturnValue().isEmpty() && entity instanceof Player player) {

			int arrowCount = BundledArrowsMod.countArrowsInInventory(player);

			if (arrowCount == 0) {
				ItemStack bundleArrow = BundledArrowsMod.findArrowInBundle(player);

				if (!bundleArrow.isEmpty() && predicate.test(bundleArrow)) {
					cir.setReturnValue(bundleArrow.copy());
				}
			}
		}
	}
}
