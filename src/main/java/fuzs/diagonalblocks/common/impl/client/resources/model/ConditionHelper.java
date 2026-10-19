package fuzs.diagonalblocks.common.impl.client.resources.model;

import com.google.common.collect.ImmutableList;
import fuzs.diagonalblocks.common.impl.DiagonalBlocks;
import net.minecraft.client.renderer.block.model.multipart.AndCondition;
import net.minecraft.client.renderer.block.model.multipart.Condition;
import net.minecraft.client.renderer.block.model.multipart.KeyValueCondition;
import net.minecraft.client.renderer.block.model.multipart.OrCondition;

import org.jetbrains.annotations.Nullable;
import java.util.function.BinaryOperator;
import java.util.function.UnaryOperator;

public final class ConditionHelper {

    private ConditionHelper() {
        // NO-OP
    }

    /**
     * Will return {@code null} when any of the transformers return {@code null} for the string, instead of returning an
     * empty condition.
     * <p>1.19.2 backport: works on the old class based conditions ({@link KeyValueCondition}, {@link AndCondition},
     * {@link OrCondition}); the value of a key value condition is the raw string from the model json (e.g.
     * {@code true}, {@code !low} or {@code low|tall}).
     */
    @Nullable
    public static Condition deepCopy(Condition condition, UnaryOperator<String> keyTransformer, BinaryOperator<String> valueTransformer) {
        if (condition instanceof KeyValueCondition keyValueCondition) {
            String newKey = keyTransformer.apply(keyValueCondition.key);
            if (newKey != null) {
                String newValue = valueTransformer.apply(keyValueCondition.key, keyValueCondition.value);
                if (newValue != null) {
                    return new KeyValueCondition(newKey, newValue);
                }
            }
            return null;
        } else if (condition instanceof AndCondition andCondition) {
            ImmutableList<Condition> newTerms = deepCopyAll(andCondition.conditions, keyTransformer, valueTransformer);
            return newTerms.isEmpty() ? null : new AndCondition(newTerms);
        } else if (condition instanceof OrCondition orCondition) {
            ImmutableList<Condition> newTerms = deepCopyAll(orCondition.conditions, keyTransformer, valueTransformer);
            return newTerms.isEmpty() ? null : new OrCondition(newTerms);
        } else if (condition == Condition.TRUE || condition == Condition.FALSE) {
            return condition;
        } else {
            DiagonalBlocks.LOGGER.warn("Unrecognized condition: {}", condition);
            return condition;
        }
    }

    private static ImmutableList<Condition> deepCopyAll(Iterable<? extends Condition> conditions, UnaryOperator<String> keyTransformer, BinaryOperator<String> valueTransformer) {
        ImmutableList.Builder<Condition> termsBuilder = ImmutableList.builder();
        for (Condition term : conditions) {
            Condition newCondition = deepCopy(term, keyTransformer, valueTransformer);
            if (newCondition != null) {
                termsBuilder.add(newCondition);
            }
        }
        return termsBuilder.build();
    }

    public static Condition negate(Condition condition) {
        return stateDefinition -> condition.getPredicate(stateDefinition).negate();
    }
}
