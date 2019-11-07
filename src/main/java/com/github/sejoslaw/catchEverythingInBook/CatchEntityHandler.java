package com.github.sejoslaw.catchEverythingInBook;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class CatchEntityHandler implements UseEntityCallback, UseBlockCallback {
    public static final String ENTITY_NBT = "ENTITY_NBT";
    public static final String ENTITY_CLASS = "ENTITY_CLASS";

    private int counterCapture = 0;
    private int counterRelease = 0;

    // Capture Entity
    public ActionResult interact(PlayerEntity playerEntity, World world, Hand hand, Entity entity, EntityHitResult entityHitResult) {
        if (counterCapture > 0) {
            counterCapture = 0;
            return ActionResult.PASS;
        }

        if (!playerEntity.isSneaking() || !(playerEntity instanceof ServerPlayerEntity)) {
            return ActionResult.PASS;
        }

        ItemStack bookStack = playerEntity.getMainHandStack();

        if (bookStack.getItem() == Items.BOOK && bookStack.getCount() == 1) {
            handleCapture(playerEntity, entity);
        }

        counterCapture++;
        return ActionResult.SUCCESS;
    }

    // Release Entity
    public ActionResult interact(PlayerEntity playerEntity, World world, Hand hand, BlockHitResult blockHitResult) {
        if (counterRelease > 0) {
            counterRelease = 0;
            return ActionResult.PASS;
        }

        if (!playerEntity.isSneaking() || !(playerEntity instanceof ServerPlayerEntity)) {
            return ActionResult.PASS;
        }

        ItemStack bookStack = playerEntity.getMainHandStack();
        CompoundTag bookTag = bookStack.getOrCreateTag();

        if (bookStack.getItem() == Items.ENCHANTED_BOOK && bookTag.contains(ENTITY_NBT)) {
            Direction offset = blockHitResult.getSide();
            handleRelease(world, playerEntity, bookStack, blockHitResult.getBlockPos(), offset);
        }

        counterRelease++;
        return ActionResult.SUCCESS;
    }

    private void handleCapture(PlayerEntity player, Entity target) {
        ItemStack bookStack = new ItemStack(Items.ENCHANTED_BOOK);
        CompoundTag bookNbt = bookStack.getOrCreateTag();

        target.toTag(bookNbt);
        bookNbt.putString(ENTITY_NBT, EntityType.getId(target.getType()).toString());
        bookNbt.putString(ENTITY_CLASS, target.getClass().getCanonicalName());
        bookStack.setCustomName(new LiteralText("Entity: " + target.getName().asFormattedString()));
        target.remove();

        player.setStackInHand(Hand.MAIN_HAND, bookStack);
    }

    private void handleRelease(World world, PlayerEntity player, ItemStack bookStack, BlockPos pos, Direction offset) {
        pos = pos.offset(offset);

        CompoundTag bookNbt = bookStack.getOrCreateTag();
        String entityClass = bookNbt.getString(ENTITY_CLASS);
        String entityTypeName = bookNbt.getString(ENTITY_NBT);
        System.out.println(entityTypeName);
        EntityType<?> entityType = EntityType.get(entityTypeName).get();
        Entity entity;

        try {
            entity = (Entity) Class.forName(entityClass).getConstructor(EntityType.class, World.class).newInstance(entityType, world);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        entity.fromTag(bookNbt);
        entity.dimension = player.dimension;
        entity.setPositionAndAngles(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 0, 0);
        world.spawnEntity(entity);

        player.setStackInHand(Hand.MAIN_HAND, new ItemStack(Items.BOOK));
    }
}
