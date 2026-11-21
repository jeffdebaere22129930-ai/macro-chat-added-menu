package com.primitive;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;

public class PrimitiveClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Keys.init();
        Store.load();

        ClientTickEvents.START_CLIENT_TICK.register(MacroEngine::startTick);
        ClientTickEvents.END_CLIENT_TICK.register(MacroEngine::endTick);
        HudRenderCallback.EVENT.register(Hud::render);

        // chat typed while recording becomes part of the macro
        ClientSendMessageEvents.CHAT.register(MacroEngine::onChat);
        ClientSendMessageEvents.COMMAND.register(MacroEngine::onCommand);
    }
}
