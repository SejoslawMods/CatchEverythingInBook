package com.github.sejoslaw.catchEverythingInBook;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;

@Mod(CatchEverythingInBook.MODID)
public class CatchEverythingInBook {
    public static final String MODID = "catcheverythinginbook";

    public CatchEverythingInBook() {
        MinecraftForge.EVENT_BUS.register(new CatchBlockHandler());
        MinecraftForge.EVENT_BUS.register(new CatchEntityHandler());
    }
}
