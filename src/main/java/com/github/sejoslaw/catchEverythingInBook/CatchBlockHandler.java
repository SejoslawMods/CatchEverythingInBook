package com.github.sejoslaw.catchEverythingInBook;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * @author Sejoslaw - https://github.com/Sejoslaw
 */
public class CatchBlockHandler {
    public static final String BLOCK_NBT_TAG = "BLOCK_NBT_TAG";
    public static final String BLOCK_NBT_STATE_ID = "BLOCK_NBT_STATE_ID";

    private int counter = 0;

    @SubscribeEvent
    public void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (counter > 0) {
            counter = 0;
            return;
        }

        PlayerEntity player = event.getPlayer();
        World world = event.getWorld();
        BlockPos pos = event.getPos();

        if (!player.isSneaking() || !(player instanceof ServerPlayerEntity)) {
            return;
        }

        ItemStack bookStack = player.getHeldItemMainhand();
        CompoundNBT bookNbt = bookStack.getOrCreateTag();

        if (bookStack.getItem() == Items.BOOK && bookStack.getCount() == 1) {
            handleSave(world, player, pos);
        } else if (bookStack.getItem() == Items.ENCHANTED_BOOK && bookNbt.contains(BLOCK_NBT_TAG)) {
            Direction offset = event.getFace();
            handleLoad(world, player, bookStack, pos, offset);
        }

        counter++;
    }

    private void handleSave(World world, PlayerEntity player, BlockPos pos) {
        BlockState blockState = world.getBlockState(pos);
        Block block = blockState.getBlock();

        ItemStack bookWithDataStack = new ItemStack(Items.ENCHANTED_BOOK);
        bookWithDataStack.setDisplayName(new StringTextComponent("Block: " + new TranslationTextComponent(block.getTranslationKey()).getString()));

        CompoundNBT bookWithDataNbt = bookWithDataStack.getOrCreateTag();
        bookWithDataNbt.put(BLOCK_NBT_TAG, new CompoundNBT());
        bookWithDataNbt = bookWithDataNbt.getCompound(BLOCK_NBT_TAG);

        int stateId = Block.getStateId(blockState);
        bookWithDataNbt.putInt(BLOCK_NBT_STATE_ID, stateId);

        TileEntity tileEntity = world.getTileEntity(pos);

        if (tileEntity != null) {
            tileEntity.write(bookWithDataNbt);
            bookWithDataStack.setDisplayName(new StringTextComponent("[TileEntity] " + bookWithDataStack.getDisplayName().getString()));
            world.removeTileEntity(pos);
            bookWithDataNbt.remove("x");
            bookWithDataNbt.remove("y");
            bookWithDataNbt.remove("z");
        }

        player.setItemStackToSlot(EquipmentSlotType.MAINHAND, bookWithDataStack);
        world.setBlockState(pos, Blocks.AIR.getDefaultState());
    }

    private void handleLoad(World world, PlayerEntity player, ItemStack bookStack, BlockPos pos, Direction offset) {
        CompoundNBT bookNbt = bookStack.getTag();

        if (!bookNbt.contains(BLOCK_NBT_TAG)) {
            return;
        }

        bookNbt = bookNbt.getCompound(BLOCK_NBT_TAG);
        pos = pos.offset(offset);

        if (!world.isAirBlock(pos)) {
            return;
        }

        int stateId = bookNbt.getInt(BLOCK_NBT_STATE_ID);
        BlockState blockState = Block.getStateById(stateId);

        world.setBlockState(pos, blockState);
        world.notifyNeighborsOfStateChange(pos, blockState.getBlock());

        TileEntity tile = world.getTileEntity(pos);

        if (tile != null) {
            bookNbt.putInt("x", pos.getX());
            bookNbt.putInt("y", pos.getY());
            bookNbt.putInt("z", pos.getZ());
            tile.read(tile.getBlockState(), bookNbt);
        }

        player.setItemStackToSlot(EquipmentSlotType.MAINHAND, new ItemStack(Items.BOOK));
    }
}
