package com.github.sejoslaw.catchEverythingInBook;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class CatchEntityHandler {
    public static final String ENTITY_NBT = "ENTITY_NBT";
    public static final String ENTITY_CLASS = "ENTITY_CLASS";

    private int counterCapture = 0;
    private int counterRelease = 0;

    @SubscribeEvent
    public void captureEntity(PlayerInteractEvent.EntityInteract event) {
        if (counterCapture > 0) {
            counterCapture = 0;
            return;
        }

        PlayerEntity player = event.getPlayer();
        Entity target = event.getTarget();

        if (!player.isSneaking() || !(player instanceof ServerPlayerEntity)) {
            return;
        }

        ItemStack bookStack = player.getHeldItemMainhand();

        if (bookStack.getItem() == Items.BOOK && bookStack.getCount() == 1) {
            handleCapture(player, target);
        }

        counterCapture++;
    }

    @SubscribeEvent
    public void releaseEntity(PlayerInteractEvent.RightClickBlock event) {
        if (counterRelease > 0) {
            counterRelease = 0;
            return;
        }

        PlayerEntity player = event.getPlayer();
        World world = event.getWorld();
        BlockPos pos = event.getPos();

        if (!player.isSneaking() || !(player instanceof ServerPlayerEntity)) {
            return;
        }

        ItemStack bookStack = player.getHeldItemMainhand();
        CompoundNBT bookTag = bookStack.getOrCreateTag();

        if (bookStack.getItem() == Items.ENCHANTED_BOOK && bookTag.contains(ENTITY_NBT)) {
            Direction offset = event.getFace();
            handleRelease(world, player, bookStack, pos, offset);
        }

        counterRelease++;
    }

    private void handleCapture(PlayerEntity player, Entity target) {
        ItemStack bookStack = new ItemStack(Items.ENCHANTED_BOOK);
        CompoundNBT bookNbt = bookStack.getOrCreateTag();

        target.writeWithoutTypeId(bookNbt);
        bookNbt.putString(ENTITY_NBT, target.getEntityString());
        bookNbt.putString(ENTITY_CLASS, target.getClass().getCanonicalName());
        bookStack.setDisplayName(new StringTextComponent("Entity: " + target.getName().getFormattedText()));
        target.remove();

        player.setItemStackToSlot(EquipmentSlotType.MAINHAND, bookStack);
    }

    private void handleRelease(World world, PlayerEntity player, ItemStack bookStack, BlockPos pos, Direction offset) {
        pos = pos.offset(offset);

        CompoundNBT bookNbt = bookStack.getOrCreateTag();
        String entityClass = bookNbt.getString(ENTITY_CLASS);
        String entityTypeName = bookNbt.getString(ENTITY_NBT);
        EntityType<?> entityType = EntityType.byKey(entityTypeName).get(); // TODO: Fix
        Entity entity;

        try {
            entity = (Entity) Class.forName(entityClass).getConstructor(EntityType.class, World.class).newInstance(entityType, world);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        entity.read(bookNbt);
        entity.dimension = player.dimension;
        entity.setLocationAndAngles(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 0, 0);
        ((ServerWorld)world).func_217460_e(entity);

        player.setItemStackToSlot(EquipmentSlotType.MAINHAND, new ItemStack(Items.BOOK));
    }
}
