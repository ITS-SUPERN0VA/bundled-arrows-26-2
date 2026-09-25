package com.astralis.bundledarrows;

/*
 * PORT NOTE (read before building):
 * This file is renamed from Yarn mappings (1.21.11) to Mojang's official
 * mappings (26.2 — Minecraft ships unobfuscated as of 26.1, and Fabric no
 * longer maintains Yarn for it). Class-level renames below are high
 * confidence (Player, ItemStack, ArrowItem, DataComponents are stable,
 * well-documented Mojmap names). Two spots are lower confidence because
 * they depend on the exact shape of the Bundle-contents API, which is
 * newer and moves between versions — they're flagged inline with "VERIFY".
 * When you open this in an IDE with the real 26.2 + Mojang-mappings
 * dependencies resolved, any wrong name will be a red underline you can
 * fix with the IDE's own suggestions in seconds. Running Loom's
 * `./gradlew migrateMappings` against the original 1.21.11 source is the
 * more mechanical alternative to this hand-port.
 */

import net.fabricmc.api.ModInitializer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate; // NEW in 26.2: BundleContents now stores these, not ItemStack directly. .create() -> ItemStack.
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BundledArrowsMod implements ModInitializer {
	public static final String MOD_ID = "bundledarrows";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	// Per-player cache of the last-found bundle slot, so we don't rescan the
	// whole inventory on every single tick/hook call.
	private static final Map<UUID, BundleCache> bundleCacheMap = new HashMap<>();

	@Override
	public void onInitialize() {
		LOGGER.info("Bundled Arrows mod initialized!");
	}

	private static class BundleCache {
		int bundleSlot = -1;
		ItemStack arrowStack = ItemStack.EMPTY;
		long timestamp = 0;

		boolean isValid() {
			// Valid for 50ms (~1 game tick).
			return bundleSlot != -1 && System.currentTimeMillis() - timestamp < 50;
		}

		void invalidate() {
			bundleSlot = -1;
			arrowStack = ItemStack.EMPTY;
			timestamp = 0;
		}
	}

	public static void invalidateCache(Player player) {
		bundleCacheMap.remove(player.getUUID());
	}

	private static BundleCache getCache(Player player) {
		return bundleCacheMap.computeIfAbsent(player.getUUID(), k -> new BundleCache());
	}

	/**
	 * Finds an arrow stored in a bundle in the player's inventory, using the
	 * per-tick cache when valid.
	 */
	public static ItemStack findArrowInBundle(Player player) {
		BundleCache cache = getCache(player);

		if (cache.isValid()) {
			return cache.arrowStack.copy();
		}

		cache.invalidate();

		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			ItemStack stack = player.getInventory().getItem(i);

			if (stack.getItem().toString().contains("bundle")) {
				BundleContents bundleContents = stack.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);

				if (!bundleContents.isEmpty()) {
					for (ItemStackTemplate template : bundleContents.items()) {
						ItemStack bundleItem = template.create();
						if (bundleItem.getItem() instanceof ArrowItem) {
							cache.bundleSlot = i;
							cache.arrowStack = bundleItem.copy();
							cache.timestamp = System.currentTimeMillis();
							return cache.arrowStack.copy();
						}
					}
				}
			}
		}

		return ItemStack.EMPTY;
	}

	/**
	 * Consumes one arrow from a bundle, preferring the cached slot.
	 */
	public static boolean consumeArrowFromBundle(Player player) {
		BundleCache cache = getCache(player);

		int startSlot = cache.isValid() ? cache.bundleSlot : 0;
		int endSlot = cache.isValid() ? cache.bundleSlot + 1 : player.getInventory().getContainerSize();

		for (int i = startSlot; i < endSlot; i++) {
			ItemStack stack = player.getInventory().getItem(i);

			if (stack.getItem().toString().contains("bundle")) {
				BundleContents bundleContents = stack.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);

				if (!bundleContents.isEmpty()) {
					for (ItemStackTemplate template : bundleContents.items()) {
						ItemStack bundleItem = template.create();
						if (bundleItem.getItem() instanceof ArrowItem) {
							BundleContents newContents = removeOneArrowFromBundle(bundleContents);
							stack.set(DataComponents.BUNDLE_CONTENTS, newContents);

							cache.invalidate();
							return true;
						}
					}
				}
			}
		}

		if (cache.isValid()) {
			cache.invalidate();
			return consumeArrowFromBundle(player);
		}

		return false;
	}

	public static int countArrowsInInventory(Player player) {
		int count = 0;
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			ItemStack invStack = player.getInventory().getItem(i);
			if (invStack.getItem() instanceof ArrowItem) {
				count += invStack.getCount();
			}
		}
		return count;
	}

	/**
	 * Rebuilds bundle contents with one arrow removed, preserving order.
	 */
	private static BundleContents removeOneArrowFromBundle(BundleContents original) {
		List<ItemStack> items = new ArrayList<>();
		for (ItemStackTemplate template : original.items()) {
			items.add(template.create().copy());
		}

		boolean arrowRemoved = false;
		for (int i = 0; i < items.size(); i++) {
			ItemStack item = items.get(i);
			if (!arrowRemoved && item.getItem() instanceof ArrowItem) {
				if (item.getCount() > 1) {
					item.shrink(1);
				} else {
					items.remove(i);
				}
				arrowRemoved = true;
				break;
			}
		}

		Collections.reverse(items);

		// VERIFY (unresolved): BundleContents.Mutable is the real nested class
		// name (confirmed via mapping history), but its exact constructor and
		// "add items back, then get a BundleContents out" API for 26.2 is not
		// yet confirmed here - see chat.
		BundleContents.Mutable builder = new BundleContents.Mutable(BundleContents.EMPTY);
		for (ItemStack item : items) {
			if (!item.isEmpty()) {
				builder.tryInsert(item);
			}
		}

		return builder.toImmutable(); // VERIFY: method name to get BundleContents back out of Mutable
	}
}
