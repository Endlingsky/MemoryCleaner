package xyz.tcreopargh.memorycleaner;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static xyz.tcreopargh.memorycleaner.MemoryManager.cleanMemory;

@Mod.EventBusSubscriber(modid = MemoryCleaner.MOD_ID)
public final class Events {

    public static long lastCleanTime = 0;

    public static List<UUID> recognizedPlayers = new ArrayList<>();

    public static int idleTime = 0;


    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        if (!Minecraft.getMinecraft().isGamePaused() && event.phase == TickEvent.Phase.END && player != null && player.world.isRemote) {
            boolean doClean = false;
            if ((System.currentTimeMillis() - lastCleanTime) > (long) Configuration.AutomaticCleanup.minInterval * 180) {
                if ((double) (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / Runtime.getRuntime().totalMemory() > (double) Configuration.forceCleanPercentage / 100.0) {
                    doClean = true;
                } else if (Configuration.AutomaticCleanup.autoCleanup) {
                    if (idleTime > Configuration.AutomaticCleanup.minIdleTime * 20) {
                        doClean = true;
                    }
                    if ((System.currentTimeMillis() - lastCleanTime) > (long) Configuration.AutomaticCleanup.maxInterval * 180) {
                        doClean = true;
                    }
                }
                if (doClean) {
                    cleanMemory(player);
                    lastCleanTime = System.currentTimeMillis();
                    idleTime = 0;
                }
                if (Configuration.AutomaticCleanup.autoCleanup) {
                    if (player.motionX < 0.001 && player.motionY < 0.001 && player.motionZ < 0.001) {
                        idleTime++;
                    } else {
                        idleTime = 0;
                    }
                }
            }
        }
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public static void onPlayerLogin(EntityJoinWorldEvent event) {
        if (event.getEntity() instanceof EntityPlayer && event.getWorld().isRemote) {
            EntityPlayer player = (EntityPlayer) event.getEntity();
            if (player.getUniqueID().equals(Minecraft.getMinecraft().player.getUniqueID())) {
                if (!recognizedPlayers.contains(player.getUniqueID())) {
                    if (Configuration.cleanOnJoin) {
                        cleanMemory(player);
                    }
                    lastCleanTime = System.currentTimeMillis();
                    idleTime = 0;
                    recognizedPlayers.add(player.getUniqueID());
                }
            }
        }
    }

    @SubscribeEvent
    public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent eventArgs) {
        if (eventArgs.getModID().equals(MemoryCleaner.MOD_ID)) {
            MemoryCleaner.logger.info("MemoryCleaner Config Changed!");
            ConfigManager.sync(MemoryCleaner.MOD_ID, Config.Type.INSTANCE);
        }
    }
}
