package fuzs.diagonalblocks.common.api.v2.client;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import fuzs.diagonalblocks.common.api.v2.block.type.DiagonalBlockType;
import fuzs.diagonalblocks.common.impl.client.resources.model.MultiPartAppender;
import net.minecraft.client.renderer.block.model.multipart.Selector;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.List;
import java.util.Map;

/**
 * 1.19.2 backport: operates on the list of multipart {@link Selector selectors} of the base block's
 * {@link net.minecraft.client.renderer.block.model.multipart.MultiPart}, as the newer
 * {@code BlockStateModelDispatcher.MultiPartDefinition} does not exist yet.
 */
public class MultiPartTranslator {
    private static final Map<DiagonalBlockType, MultiPartTranslator> TRANSLATORS = Maps.newConcurrentMap();

    protected final DiagonalBlockType diagonalBlockType;

    protected MultiPartTranslator(DiagonalBlockType diagonalBlockType) {
        this.diagonalBlockType = diagonalBlockType;
    }

    public static void register(DiagonalBlockType diagonalBlockType, MultiPartTranslator multiPartTranslator) {
        TRANSLATORS.put(diagonalBlockType, multiPartTranslator);
    }

    public static MultiPartTranslator get(DiagonalBlockType diagonalBlockType) {
        return TRANSLATORS.computeIfAbsent(diagonalBlockType, MultiPartTranslator::new);
    }

    protected Comparable<?> getNewPropertyValue(Property<?> oldProperty, Property<?> newProperty, Comparable<?> oldValue) {
        return oldValue;
    }

    public List<Selector> apply(List<Selector> baseSelectors, MultiPartAppender.RotatedModelRegistrar registrar) {
        return this.applyAdditionalSelectors(this.getModelFromBase(baseSelectors), registrar);
    }

    protected List<Selector> getModelFromBase(List<Selector> selectors) {
        return Lists.newArrayList(selectors);
    }

    protected List<Selector> applyAdditionalSelectors(List<Selector> selectors, MultiPartAppender.RotatedModelRegistrar registrar) {
        return MultiPartAppender.appendDiagonalSelectors(selectors, false, registrar);
    }

    public boolean allowBaseModelAsFallback() {
        return true;
    }

    @Override
    public String toString() {
        return "MultiPartTranslator[" + this.diagonalBlockType + "]";
    }
}
