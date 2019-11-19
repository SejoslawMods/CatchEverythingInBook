package com.github.sejoslaw.catchEverythingInBook;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class CatchBlockHandler implements UseBlockCallback {
    public static final String BLOCK_NBT_TAG = "BLOCK_NBT_TAG";
    public static final String BLOCK_NBT_STATE_ID = "BLOCK_NBT_STATE_ID";

    private int counter = 0;

    public ActionResult interact(PlayerEntity playerEntity, World world, Hand hand, BlockHitResult blockHitResult) {
        if (counter > 0) {
            counter = 0;
            return ActionResult.PASS;
        }

        if (!playerEntity.isSneaking() || !(playerEntity instanceof ServerPlayerEntity)) {
            return ActionResult.PASS;
        }

        ItemStack bookStack = playerEntity.getMainHandStack();
        CompoundTag bookNbt = bookStack.getOrCreateTag();

        if (bookStack.getItem() == Items.BOOK && bookStack.getCount() == 1) {
            handleSave(world, playerEntity, blockHitResult.getBlockPos());
        } else if (bookStack.getItem() == Items.ENCHANTED_BOOK && bookNbt.contains(BLOCK_NBT_TAG)) {
            Direction offset = blockHitResult.getSide();
            handleLoad(world, playerEntity, bookStack, blockHitResult.getBlockPos(), offset);
        }

        counter++;
        return ActionResult.SUCCESS;
    }

    private void handleSave(World world, PlayerEntity player, BlockPos pos) {
        BlockState blockState = world.getBlockState(pos);
        Block block = blockState.getBlock();

        ItemStack bookWithDataStack = new ItemStack(Items.ENCHANTED_BOOK);
        bookWithDataStack.setCustomName(new LiteralText("Block: " + new TranslatableText(block.getTranslationKey(), new Object[0]).asFormattedString()));

        CompoundTag bookWithDataNbt = bookWithDataStack.getTag();
        bookWithDataNbt.put(BLOCK_NBT_TAG, new CompoundTag());
        bookWithDataNbt = bookWithDataNbt.getCompound(BLOCK_NBT_TAG);

        int stateId = Block.getRawIdFromState(blockState);
        bookWithDataNbt.putInt(BLOCK_NBT_STATE_ID, stateId);

        BlockEntity tileEntity = world.getBlockEntity(pos);

        if (tileEntity != null) {
            tileEntity.toTag(bookWithDataNbt);
            bookWithDataStack.setCustomName(new LiteralText("[BlockEntity] " + bookWithDataStack.getName().asFormattedString()));
            world.removeBlockEntity(pos);
            bookWithDataNbt.remove("x");
            bookWithDataNbt.remove("y");
            bookWithDataNbt.remove("z");
        }

        player.setStackInHand(Hand.MAIN_HAND, bookWithDataStack);
        world.setBlockState(pos, Blocks.AIR.getDefaultState());
    }

    private void handleLoad(World world, PlayerEntity player, ItemStack bookStack, BlockPos pos, Direction offset) {
        CompoundTag bookNbt = bookStack.getTag();

        if (!bookNbt.contains(BLOCK_NBT_TAG)) {
            return;
        }

        bookNbt = bookNbt.getCompound(BLOCK_NBT_TAG);
        pos = pos.offset(offset);

        if (!world.isAir(pos)) {
            return;
        }

        int stateId = bookNbt.getInt(BLOCK_NBT_STATE_ID);
        BlockState blockState = Block.getStateFromRawId(stateId);

        world.setBlockState(pos, blockState);
        world.updateNeighborsAlways(pos, blockState.getBlock());

        BlockEntity tileEntity = world.getBlockEntity(pos);

        if (tileEntity != null) {
            bookNbt.putInt("x", pos.getX());
            bookNbt.putInt("y", pos.getY());
            bookNbt.putInt("z", pos.getZ());
            tileEntity.fromTag(bookNbt);
        }

        player.setStackInHand(Hand.MAIN_HAND, new ItemStack(Items.BOOK));
    }
}
