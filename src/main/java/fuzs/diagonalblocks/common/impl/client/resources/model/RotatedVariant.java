package fuzs.diagonalblocks.common.impl.client.resources.model;

import com.mojang.datafixers.util.Pair;
import com.mojang.math.Matrix4f;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import com.mojang.math.Vector4f;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.block.model.Variant;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;
import java.util.*;
import java.util.function.Function;

/**
 * 1.19.2 backport: implemented as an {@link UnbakedModel} wrapping a single {@link Variant} (instead of the newer
 * {@code BlockStateModel.Unbaked}), and operating on the raw vertex data of {@link BakedQuad BakedQuads} (instead of
 * Puzzles Lib's {@code MutableBakedQuad}).
 */
public record RotatedVariant(Variant variant, Direction direction) implements UnbakedModel {
    static final float ROTATION_ANGLE = -45.0F;
    /**
     * Scale factor at a 45-degree rotation.
     */
    static final float SCALE_ROTATION_45 = 1.0F / (float) Math.cos(Math.PI / 4.0) - 1.0F;
    static final Vector3f ROTATION_ORIGIN = new Vector3f(0.5F, 0.5F, 0.5F);
    static final Matrix4f ROTATION_MATRIX = new Matrix4f(new Quaternion(new Vector3f(0.0F, 1.0F, 0.0F),
            ROTATION_ANGLE,
            true));
    static final Direction[] VALID_QUAD_FACES = {
            null, Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST
    };

    @Override
    public Collection<ResourceLocation> getDependencies() {
        return Collections.singleton(this.variant.getModelLocation());
    }

    @Override
    public Collection<Material> getMaterials(Function<ResourceLocation, UnbakedModel> modelGetter, Set<Pair<String, String>> missingTextureErrors) {
        return modelGetter.apply(this.variant.getModelLocation()).getMaterials(modelGetter, missingTextureErrors);
    }

    @Nullable
    @Override
    public BakedModel bake(ModelBakery modelBakery, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelState, ResourceLocation location) {
        BakedModel bakedModel = modelBakery.bake(this.variant.getModelLocation(), this.variant);
        return bakedModel != null ? rotateBakedModel(bakedModel, this.direction) : null;
    }

    /**
     * Duplicate and rotate all contained {@link BakedQuad BakedQuads} 45 degrees clockwise from the given
     * {@link BakedModel} to produce a new variant for a diagonal side.
     */
    private static BakedModel rotateBakedModel(BakedModel bakedModel, Direction direction) {
        Map<Direction, List<BakedQuad>> quads = new HashMap<>();
        List<BakedQuad> unculledQuads = new ArrayList<>();
        RandomSource randomSource = RandomSource.create();
        for (Direction face : VALID_QUAD_FACES) {
            randomSource.setSeed(42L);
            List<BakedQuad> newQuads = face == null ? unculledQuads : new ArrayList<>();
            for (BakedQuad bakedQuad : bakedModel.getQuads(null, face, randomSource)) {
                BakedQuad copiedBakedQuad = duplicateQuad(bakedQuad);
                rotateQuad(copiedBakedQuad, direction);
                newQuads.add(copiedBakedQuad);
            }
            if (face != null) {
                quads.put(face, newQuads);
            }
        }

        return new RotatedBakedModel(bakedModel, unculledQuads, quads);
    }

    /**
     * Create a deep-copy of the given {@link BakedQuad}.
     */
    private static BakedQuad duplicateQuad(BakedQuad bakedQuad) {
        int[] vertices = bakedQuad.getVertices().clone();
        return new BakedQuad(vertices,
                bakedQuad.getTintIndex(),
                bakedQuad.getDirection(),
                bakedQuad.getSprite(),
                bakedQuad.isShade());
    }

    /**
     * Rotate the given {@link BakedQuad} 45 degrees clockwise and recalculate its vertex normals.
     *
     * @author <a href="https://github.com/XFactHD">XFactHD</a>
     */
    private static void rotateQuad(BakedQuad bakedQuad, Direction direction) {

        Vector3f scaleMultiplier = new Vector3f(Math.abs(direction.getStepX()), 1.0F, Math.abs(direction.getStepZ()));
        Vector3f scaleVector = new Vector3f(1.0F, 0.0F, 1.0F);
        scaleVector.mul(SCALE_ROTATION_45);
        scaleVector.mul(scaleMultiplier.x(), scaleMultiplier.y(), scaleMultiplier.z());
        scaleVector.add(1.0F, 1.0F, 1.0F);

        int[] vertices = bakedQuad.getVertices();
        float[][] positions = unpackQuadPositions(vertices);
        for (int i = 0; i < 4; i++) {
            Vector4f transformVector = new Vector4f(positions[i][0] - ROTATION_ORIGIN.x(),
                    positions[i][1] - ROTATION_ORIGIN.y(),
                    positions[i][2] - ROTATION_ORIGIN.z(),
                    1.0F);
            transformVector.mul(scaleVector);
            transformVector.transform(ROTATION_MATRIX);
            positions[i][0] = transformVector.x() + ROTATION_ORIGIN.x();
            positions[i][1] = transformVector.y() + ROTATION_ORIGIN.y();
            positions[i][2] = transformVector.z() + ROTATION_ORIGIN.z();
        }

        packQuadPositions(vertices, positions);
        fillNormal(vertices, positions);
    }

    private static float[][] unpackQuadPositions(int[] vertices) {
        float[][] positions = new float[4][3];
        // this is needed to support the extended vertex formats used by shaders
        int step = vertices.length / 4;
        for (int i = 0; i < 4; i++) {
            int offset = i * step;
            positions[i][0] = Float.intBitsToFloat(vertices[offset]);
            positions[i][1] = Float.intBitsToFloat(vertices[offset + 1]);
            positions[i][2] = Float.intBitsToFloat(vertices[offset + 2]);
        }
        return positions;
    }

    private static void packQuadPositions(int[] vertices, float[][] positions) {
        int step = vertices.length / 4;
        for (int i = 0; i < 4; i++) {
            int offset = i * step;
            vertices[offset] = Float.floatToIntBits(positions[i][0]);
            vertices[offset + 1] = Float.floatToIntBits(positions[i][1]);
            vertices[offset + 2] = Float.floatToIntBits(positions[i][2]);
        }
    }

    /**
     * Calculate face normals from vertex positions and write them to the given vertex data.
     */
    private static void fillNormal(int[] vertices, float[][] positions) {
        Vector3f v1 = new Vector3f(positions[3][0], positions[3][1], positions[3][2]);
        Vector3f t1 = new Vector3f(positions[1][0], positions[1][1], positions[1][2]);
        Vector3f v2 = new Vector3f(positions[2][0], positions[2][1], positions[2][2]);
        Vector3f t2 = new Vector3f(positions[0][0], positions[0][1], positions[0][2]);
        v1.sub(t1);
        v2.sub(t2);
        v2.cross(v1);
        v2.normalize();
        int x = ((byte) Math.round(v2.x() * 127)) & 0xFF;
        int y = ((byte) Math.round(v2.y() * 127)) & 0xFF;
        int z = ((byte) Math.round(v2.z() * 127)) & 0xFF;
        int normal = x | (y << 0x08) | (z << 0x10);
        int step = vertices.length / 4;
        for (int i = 0; i < 4; i++) {
            vertices[i * step + 7] = normal;
        }
    }

    record RotatedBakedModel(BakedModel bakedModel, List<BakedQuad> unculledQuads,
                             Map<Direction, List<BakedQuad>> culledQuads) implements BakedModel {

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState blockState, @Nullable Direction direction, RandomSource randomSource) {
            return direction == null ? this.unculledQuads : this.culledQuads.getOrDefault(direction,
                    Collections.emptyList());
        }

        @Override
        public boolean useAmbientOcclusion() {
            return this.bakedModel.useAmbientOcclusion();
        }

        @Override
        public boolean isGui3d() {
            return this.bakedModel.isGui3d();
        }

        @Override
        public boolean usesBlockLight() {
            return this.bakedModel.usesBlockLight();
        }

        @Override
        public boolean isCustomRenderer() {
            return this.bakedModel.isCustomRenderer();
        }

        @Override
        public TextureAtlasSprite getParticleIcon() {
            return this.bakedModel.getParticleIcon();
        }

        @Override
        public ItemTransforms getTransforms() {
            return this.bakedModel.getTransforms();
        }

        @Override
        public ItemOverrides getOverrides() {
            return this.bakedModel.getOverrides();
        }
    }
}
