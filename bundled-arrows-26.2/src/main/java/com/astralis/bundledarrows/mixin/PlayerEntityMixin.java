package com.astralis.bundledarrows.mixin;

import com.astralis.bundledarrows.BundledArrowsMod;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// NOTE: despite the file name (kept from upstream), this mixin targets
// ProjectileWeaponItem, same as in the original 1.21.11 source — not Player.
@Mixin(ProjectileWeaponItem.class)
public class PlayerEntityMixin {

	// RENAMED for 26.2: this method used to be called getProjectile - javap
	// against the real 26.2 jar confirmed it's now useAmmo.
	@Inject(
			method = "useAmmo",
			at = @At(
					value = "INVOKE",
					// VERIFY: still checking this split() call exists inside
					// useAmmo with this exact descriptor - if this @Inject
					// itself fails to find the target, that's the next thing
					// to check with javap (javap -p -c ... to see bytecode,
					// or -private, and look for a call to ItemStack.split).
					target = "Lnet/minecraft/world/item/ItemStack;split(I)Lnet/minecraft/world/item/ItemStack;",
					shift = At.Shift.BEFORE
			)
	)
	private static void beforeSplitProjectile(ItemStack stack, ItemStack projectileStack, LivingEntity shooter, boolean multishot, CallbackInfoReturnable<ItemStack> cir) {
		if (shooter instanceof Player player && !player.getAbilities().instabuild) {

			int arrowCount = BundledArrowsMod.countArrowsInInventory(player);

			if (arrowCount == 0) {
				ItemStack bundleArrow = BundledArrowsMod.findArrowInBundle(player);

				if (!bundleArrow.isEmpty()) {
					boolean hasInfinity = stack.isEnchanted() &&
							stack.getEnchantments().entrySet().stream()
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
