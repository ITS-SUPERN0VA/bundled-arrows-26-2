package com.astralis.bundledarrows.mixin;

import com.astralis.bundledarrows.BundledArrowsMod;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BowItem.class)
public class BowItemMixin {

	@Inject(method = "use", at = @At("HEAD"), cancellable = true)
	private void onBowUse(Level world, Player user, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
		ItemStack bowStack = user.getItemInHand(hand);
		ItemStack arrowStack = user.getProjectile(bowStack); // VERIFY: method name (Yarn: getProjectileType)

		if (arrowStack.isEmpty()) {
			ItemStack bundleArrow = BundledArrowsMod.findArrowInBundle(user);

			if (!bundleArrow.isEmpty()) {
				user.startUsingItem(hand); // VERIFY: method name (Yarn: setCurrentHand)
				cir.setReturnValue(InteractionResult.CONSUME);
			}
		}
	}
}
