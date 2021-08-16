package me.jellysquid.mods.phosphor.common.util.chunk.light;

import net.minecraft.world.chunk.NibbleArray;

public class SkyLightChunkNibbleArray extends ReadonlyChunkNibbleArray {
    public SkyLightChunkNibbleArray(final NibbleArray inheritedLightmap) {
        super(inheritedLightmap.getData());
    }

    @Override
    protected int getIndex(final int x, final int y, final int z) {
        return super.getIndex(x, 0, z);
    }

    @Override
    public byte[] getData() {
        byte[] byteArray = new byte[2048];

        for(int i = 0; i < 16; ++i) {
            System.arraycopy(this.data, 0, byteArray, i * 128, 128);
        }

        return byteArray;
    }
}
