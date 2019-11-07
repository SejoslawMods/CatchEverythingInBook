package com.github.sejoslaw.catchEverythingInBook;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;

public class CatchEverythingInBook implements ModInitializer {
    public void onInitialize() {
        // Block
        UseBlockCallback.EVENT.register(new CatchBlockHandler());

        // Entity
        UseEntityCallback.EVENT.register(new CatchEntityHandler());
        UseBlockCallback.EVENT.register(new CatchEntityHandler());
    }
}
