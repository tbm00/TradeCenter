package me.spaff.tradecenter.chunkdata;

import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.persistence.PersistentDataType;

public class ChunkData {
    private final Chunk chunk;
    private final NamespacedKey namespacedKey;

    public ChunkData(Block block) {
        this.chunk = block.getChunk();

        int chunkBlockX = block.getX() % 16;
        int chunkBlockY = block.getY(); // 319 limit
        int chunkBlockZ = block.getZ() % 16;

        String serializedChunkCoords = chunk.getX() + "-" + chunk.getZ();
        String serializedBlockCoords = chunkBlockX + "-" + chunkBlockY + "-" + chunkBlockZ;

        namespacedKey = new NamespacedKey(serializedChunkCoords, serializedBlockCoords);
    }

    public void saveData(Object data) {
        chunk.getPersistentDataContainer().set(namespacedKey, PersistentDataType.STRING, data.toString());
    }

    public String getData() {
        return chunk.getPersistentDataContainer().get(namespacedKey, PersistentDataType.STRING);
    }

    public void clearData() {
        chunk.getPersistentDataContainer().remove(namespacedKey);
    }
}