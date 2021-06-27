package me.jellysquid.mods.sodium.client.render.chunk;

import com.mojang.blaze3d.matrix.MatrixStack;
import me.jellysquid.mods.sodium.client.gl.SodiumVertexFormats.ChunkMeshAttribute;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexFormat;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildResult;
import me.jellysquid.mods.sodium.client.render.chunk.lists.ChunkRenderListIterator;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPassManager;
import net.minecraft.util.math.vector.Matrix4f;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * The chunk render backend takes care of managing the graphics resource state of chunk render containers. This includes
 * the handling of uploading their data to the graphics card and rendering responsibilities.
 * @param <T> The type of graphics state to be used in chunk render containers
 */
public interface ChunkRenderBackend<T extends ChunkGraphicsState> {

    /**
     * Drains the iterator of items and processes each build task's result serially. After this method returns, all
     * drained results should be processed.
     */
    void upload(Iterator<ChunkBuildResult<T>> queue);

    /**
     * Renders the given chunk render list to the active framebuffer.
     *
     * @param renders An iterator over the list of chunks to be rendered
     * @param camera The camera context containing chunk offsets for the current render
     * @param projection Projection Matrix precalculated by Vanilla Minecraft
     */
    void render(ChunkRenderListIterator<T> renders, ChunkCameraContext camera, Matrix4f projection);

    void createShaders();

    void begin(MatrixStack matrixStack, Matrix4f projection);

    void end(MatrixStack matrixStack);

    /**
     * Deletes this render backend and any resources attached to it.
     */
    void delete();

    /**
     * Returns the vertex format used by this chunk render backend for rendering meshes.
     */
    GlVertexFormat<ChunkMeshAttribute> getVertexFormat();

    /**
     * Returns the type used to store graphics state in {@link ChunkRenderContainer} for this render backend.
     */
    Class<T> getGraphicsStateType();

    default String getRendererName() {
        return this.getClass().getSimpleName();
    }

    default List<String> getDebugStrings() {
        return Collections.emptyList();
    }
}
