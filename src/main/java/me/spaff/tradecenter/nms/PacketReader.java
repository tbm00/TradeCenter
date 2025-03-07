package me.spaff.tradecenter.nms;

import io.netty.channel.*;
import me.spaff.tradecenter.Constants;
import me.spaff.tradecenter.Main;
import me.spaff.tradecenter.chunkdata.ChunkData;
import me.spaff.tradecenter.tradecenter.TradeCenter;
import me.spaff.tradecenter.utils.ReflectionUtils;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import org.bukkit.*;
import org.bukkit.craftbukkit.v1_21_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

public class PacketReader {
    private static Channel channel;

    public static void injectPlayer(Player player) {
        UUID uuid = player.getUniqueId();

        ChannelDuplexHandler channelDuplexHandler = new ChannelDuplexHandler() {
            @Override
            public void channelRead(ChannelHandlerContext channelHandlerContext, Object packet) throws Exception {
                super.channelRead(channelHandlerContext, packet);
            }

            @Override
            public void write(ChannelHandlerContext channelHandlerContext, Object packet, ChannelPromise promise) throws Exception {
                if (packet instanceof ClientboundLevelChunkWithLightPacket lightPacket) {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            int chunkX = lightPacket.getX();
                            int chunkZ = lightPacket.getZ();

                            World world = player.getWorld();

                            Chunk chunk = world.getChunkAt(chunkX, chunkZ);
                            ChunkSnapshot chunkSnapshot = chunk.getChunkSnapshot(true, false , false);

                            // world.getChunkAt(chunkX, chunkZ).getTileEntities()
                            for (int x = 0; x < 16; x++) {
                                for (int y = 0; y < 319; y++) {
                                    for (int z = 0; z < 16; z++) {
                                        if (chunkSnapshot.getBlockType(x, y, z) != Constants.TRADE_CENTER_BLOCK_TYPE) continue;
                                        ChunkData chunkData = new ChunkData(chunk.getBlock(x, y, z)); // Check if correct chunk block coords
                                        if (chunkData.getData() == null || !chunkData.getData().equals(Constants.TRADE_CENTER_DATA_ID)) continue;

                                        TradeCenter tradeCenter = new TradeCenter(chunk.getBlock(x, y, z).getLocation());

                                        tradeCenter.clearModels(player);
                                        tradeCenter.spawnModel(player);
                                    }
                                }
                            }
                        }
                    }.runTaskLater(Main.getInstance(), 5);
                }

                super.write(channelHandlerContext, packet, promise);
            }
        };

        // ServerCommonPacketListenerImpl: e -> connection
        // Connection: n -> channel

        Connection conn = null;
        
        // Get player connection
        ServerCommonPacketListenerImpl connection = ((CraftPlayer) player).getHandle().connection;

        // Get connection field
        conn = (Connection) ReflectionUtils.getField(ServerCommonPacketListenerImpl.class, "e", connection);

        // Get channel field
        channel = (Channel) ReflectionUtils.getField(Connection.class, "n", conn);

        String pipelineName = Main.getInstance().getName() + uuid.toString();
        ChannelPipeline pipeline = channel.pipeline();

        if (isInjected(player))
            pipeline.remove(pipelineName);

        pipeline.addBefore("packet_handler", pipelineName, channelDuplexHandler);
    }

    public static void uninjectPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        if (channel == null)
            return;

        String pipelineName = Main.getInstance().getName() + uuid.toString();

        channel.eventLoop().submit(() -> {
            if (channel.pipeline().get(pipelineName) != null)
                channel.pipeline().remove(pipelineName);
            return null;
        });
    }

    public static void pipelineHandler(Player player) {
        if (isInjected(player))
            uninjectPlayer(player);
        else
            injectPlayer(player);
    }

    public static void pipelineHandler() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            pipelineHandler(player);
        }
    }

    private static boolean isInjected(Player player) {
        UUID uuid = player.getUniqueId();
        String pipelineName = Main.getInstance().getName() + uuid.toString();

        if (channel == null)
            return false;

        ChannelPipeline pipeline = channel.pipeline();
        if (pipeline.get(pipelineName) != null)
            return true;

        return false;
    }
}