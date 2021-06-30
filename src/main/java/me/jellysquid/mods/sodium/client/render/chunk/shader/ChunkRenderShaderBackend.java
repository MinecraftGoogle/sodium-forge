package me.jellysquid.mods.sodium.client.render.chunk.shader;

import com.mojang.blaze3d.matrix.MatrixStack;
import it.unimi.dsi.fastutil.objects.ObjectArrayFIFOQueue;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexFormat;
import me.jellysquid.mods.sodium.client.gl.shader.GlProgram;
import me.jellysquid.mods.sodium.client.gl.shader.GlShader;
import me.jellysquid.mods.sodium.client.gl.shader.ShaderLoader;
import me.jellysquid.mods.sodium.client.gl.shader.ShaderType;
import me.jellysquid.mods.sodium.client.model.vertex.type.ChunkVertexType;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkGraphicsState;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderBackend;
import me.jellysquid.mods.sodium.client.render.chunk.format.ChunkMeshAttribute;
import me.jellysquid.mods.sodium.common.util.collections.IntPool;
import me.jellysquid.mods.sodium.common.util.collections.TrackedArray;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Matrix4f;

import java.util.EnumMap;

public abstract class ChunkRenderShaderBackend<T extends ChunkGraphicsState, P extends ChunkProgram>
        implements ChunkRenderBackend {
    private final EnumMap<ChunkFogMode, P> programs = new EnumMap<>(ChunkFogMode.class);

    protected final ChunkVertexType vertexType;
    protected final GlVertexFormat<ChunkMeshAttribute> vertexFormat;

    protected final TrackedArray<T> stateStorage;
    protected final IntPool stateIds;

    protected final ObjectArrayFIFOQueue<T> pendingUnloads = new ObjectArrayFIFOQueue<>();

    protected P activeProgram;

    public ChunkRenderShaderBackend(Class<T> graphicsType, ChunkVertexType vertexType) {
        this.stateStorage = new TrackedArray<>(graphicsType, 4096);
        this.stateIds = new IntPool();

        this.vertexType = vertexType;
        this.vertexFormat = vertexType.getCustomVertexFormat();
    }

    @Override
    public final void createShaders() {
        this.programs.put(ChunkFogMode.NONE, this.createShader(ChunkFogMode.NONE, this.vertexFormat));
        this.programs.put(ChunkFogMode.LINEAR, this.createShader(ChunkFogMode.LINEAR, this.vertexFormat));
        this.programs.put(ChunkFogMode.EXP2, this.createShader(ChunkFogMode.EXP2, this.vertexFormat));
    }

    private P createShader(ChunkFogMode fogMode, GlVertexFormat<ChunkMeshAttribute> format) {
        GlShader vertShader = this.createVertexShader(fogMode);
        GlShader fragShader = this.createFragmentShader(fogMode);

        try {
            return GlProgram.builder(new ResourceLocation("sodium", "chunk_shader"))
                    .attachShader(vertShader)
                    .attachShader(fragShader)
                    .bindAttribute("a_Pos", format.getAttribute(ChunkMeshAttribute.POSITION))
                    .bindAttribute("a_Color", format.getAttribute(ChunkMeshAttribute.COLOR))
                    .bindAttribute("a_TexCoord", format.getAttribute(ChunkMeshAttribute.TEXTURE))
                    .bindAttribute("a_LightCoord", format.getAttribute(ChunkMeshAttribute.LIGHT))
                    .build((program, name) -> this.createShaderProgram(program, name, fogMode));
        } finally {
            vertShader.delete();
            fragShader.delete();
        }
    }

    private GlShader createVertexShader(ChunkFogMode fogMode) {
        return ShaderLoader.loadShader(ShaderType.VERTEX, new ResourceLocation("sodium", "chunk_gl20.v.glsl"),
               fogMode.getDefines());
    }

    private GlShader createFragmentShader(ChunkFogMode fogMode) {
        return ShaderLoader.loadShader(ShaderType.FRAGMENT, new ResourceLocation("sodium", "chunk_gl20.f.glsl"),
                fogMode.getDefines());
    }

    protected abstract P createShaderProgram(ResourceLocation name, int handle, ChunkFogMode fogMode);

    @Override
    public void begin(MatrixStack matrixStack, Matrix4f projection) {
        this.unloadPending();

        this.activeProgram = this.programs.get(ChunkFogMode.getActiveMode());
        this.activeProgram.bind(matrixStack);
        this.activeProgram.setup(matrixStack, this.vertexType.getModelScale(), this.vertexType.getTextureScale(), projection);
    }

    @Override
    public void end(MatrixStack matrixStack) {
        this.activeProgram.unbind();
    }

    @Override
    public void delete() {
        for (P shader : this.programs.values()) {
            shader.delete();
        }

        this.unloadPending();
    }

    @Override
    public ChunkVertexType getVertexType() {
        return this.vertexType;
    }

    private void unloadPending() {
        while (!this.pendingUnloads.isEmpty()) {
            T state = this.pendingUnloads.dequeue();
            state.delete();

            this.stateIds.deallocateId(state.getId());
        }
    }

    @Override
    public void deleteGraphicsState(int id) {
        this.pendingUnloads.enqueue(this.stateStorage.remove(id));
    }
}
