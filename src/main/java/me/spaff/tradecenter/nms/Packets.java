package me.spaff.tradecenter.nms;

import com.mojang.datafixers.util.Pair;
import net.minecraft.network.protocol.game.*;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_21_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_21_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.util.*;

public class Packets {
    private static final Map<String, EntityDataAccessor<?>> cachedAccessors = new HashMap<>();

    public static void sendRemoveEntityPacket(Player player, Entity entity) {
        ServerGamePacketListenerImpl connection = (((CraftPlayer) player).getHandle()).connection;
        connection.send(new ClientboundRemoveEntitiesPacket(entity.getId()));
    }

    public static void sendAddEntityPacket(Player player, Entity entity, Location location) {
        sendAddEntityPacket(player, entity, location, entity.getId());
    }

    public static void sendAddEntityPacket(Player player, Entity entity, Location location, int entityId) {
        ServerCommonPacketListenerImpl connection = (((CraftPlayer) player).getHandle()).connection;

        ClientboundAddEntityPacket addEntityPacket = new ClientboundAddEntityPacket(
                entityId,
                entity.getUUID(),
                location.getX(),
                location.getY(),
                location.getZ(),
                0, // Pitch
                location.getYaw(), // Yaw
                entity.getType(),
                0,
                Vec3.ZERO,
                location.getYaw() // Yaw
        );

        connection.send(addEntityPacket);
    }

    public static void sendPlayerInfoPacket(Player player, ServerPlayer npc, ClientboundPlayerInfoUpdatePacket.Action action) {
        ServerCommonPacketListenerImpl connection = (((CraftPlayer) player).getHandle()).connection;

        ClientboundPlayerInfoUpdatePacket packet =
                new ClientboundPlayerInfoUpdatePacket(action, npc);

        connection.send(packet);
    }

    public static void sendEquipmentPacket(Player player, int entityId, List<Pair<EquipmentSlot, ItemStack>> equipment) {
        ServerCommonPacketListenerImpl connection = (((CraftPlayer) player).getHandle()).connection;

        ClientboundSetEquipmentPacket equipmentPacket =
                new ClientboundSetEquipmentPacket(entityId, equipment); // List.of(Pair.of(slot, CraftItemStack.asNMSCopy(item)))

        connection.send(equipmentPacket);
    }

    public static void sendEquipmentPacket(Player player, Entity entity, List<Pair<EquipmentSlot, ItemStack>> equipment) {
        sendEquipmentPacket(player, entity.getId(), equipment);
    }

    public static void sendTeleportPacket(Player player, Entity entity, Location location) {
        ServerGamePacketListenerImpl connection = (((CraftPlayer) player).getHandle()).connection;

        PositionMoveRotation posRot = new PositionMoveRotation(
                new Vec3(location.getX(), location.getY(), location.getZ()),
                new Vec3(0, 0, 0),
                location.getYaw(),
                location.getPitch()
        );

        Set<Relative> relatives = EnumSet.noneOf(Relative.class);
        connection.send(new ClientboundTeleportEntityPacket(entity.getId(), posRot, relatives, true));
    }

    public static <T> void sendEntityDataPacket(Player player, Entity entity, Class<?> classField, String fieldName, T value) {
        ServerCommonPacketListenerImpl connection = (((CraftPlayer) player).getHandle()).connection;

        String cacheKey = classField.getName() + "." + fieldName;
        EntityDataAccessor<T> accessor = (EntityDataAccessor<T>) cachedAccessors.get(cacheKey);

        if (accessor == null) {
            try {
                Field field = classField.getDeclaredField(fieldName);
                field.setAccessible(true);

                accessor = (EntityDataAccessor<T>) field.get(null);
                cachedAccessors.put(cacheKey, accessor);
            }
            catch (Exception ex) {
                ex.printStackTrace();
                return;
            }
        }

        SynchedEntityData.DataItem<T> dataItem = new SynchedEntityData.DataItem<>(accessor, value);
        ClientboundSetEntityDataPacket packet = new ClientboundSetEntityDataPacket(
                entity.getId(),
                List.of(dataItem.value())
        );

        connection.send(packet);
    }

    public static ServerLevel getServerLevel(World world) {
        return ((CraftWorld) world).getHandle();
    }

    public static CraftPlayer getCraftPlayer(Player player) {
        return ((CraftPlayer) player);
    }
}