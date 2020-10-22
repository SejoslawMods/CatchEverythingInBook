package com.github.sejoslaw.catchEverythingInBook;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * @author Sejoslaw - https://github.com/Sejoslaw
 */
public class CatchEntityHandler {
    public static final String ENTITY_NBT = "ENTITY_NBT";
    public static final String ENTITY_TYPE = "ENTITY_TYPE";
    public static final String ENTITY_NAME = "ENTITY_NAME";

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

        CompoundNBT entityNbt = new CompoundNBT();
        target.writeUnlessRemoved(entityNbt);

        bookNbt.put(ENTITY_NBT, entityNbt);
        bookNbt.putString(ENTITY_TYPE, target.getType().getRegistryName().toString());
        bookNbt.putString(ENTITY_NAME, target.getName().getString());
        bookStack.setDisplayName(new StringTextComponent("Entity: " + target.getName().getString()));

        target.remove();

        player.setItemStackToSlot(EquipmentSlotType.MAINHAND, bookStack);
    }

    private void handleRelease(World world, PlayerEntity player, ItemStack bookStack, BlockPos pos, Direction offset) {
        CompoundNBT bookNbt = bookStack.getOrCreateTag();

        String type = bookNbt.getString(ENTITY_TYPE);
        Entity entity = ForgeRegistries.ENTITIES.getValue(new ResourceLocation(type)).create(world);
        entity.read(bookNbt.getCompound(ENTITY_NBT));

        BlockPos spawnPos = pos.offset(offset);
        entity.setLocationAndAngles(spawnPos.getX() + 0.5D, spawnPos.getY() + 0.5d, spawnPos.getZ() + 0.5D, 0, 0);
        world.addEntity(entity);

        player.setItemStackToSlot(EquipmentSlotType.MAINHAND, new ItemStack(Items.BOOK));
    }
}
