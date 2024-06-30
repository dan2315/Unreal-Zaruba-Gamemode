package com.dod.UnrealZaruba.Gamemodes;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "unrealzaruba", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ScheduledTask {
    private static int tickCount = 0;
    private static final int WAIT_TICKS = 200;

    public static void start() {
        tickCount = 0; //сбрасывает счетчик
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            tickCount++;
            if (tickCount >= WAIT_TICKS) {
                tickCount = 0; //сбрасывает если нужно еще 200 тиков
            }
        }    
    }
}
