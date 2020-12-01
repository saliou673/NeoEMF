package org.atlanmod.neoemf.data.spark;

import fr.inria.atlanmod.neoemf.context.Context;
import fr.inria.atlanmod.neoemf.data.AbstractBackendFactoryTest;

import org.atlanmod.neoemf.data.spark.config.SparkConfig;
import org.atlanmod.neoemf.data.spark.context.SparkContext;

import org.junit.jupiter.params.provider.Arguments;

import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * A test-case about {@link SparkBackendFactory}.
 */
@ParametersAreNonnullByDefault
class SparkBackendFactoryTest extends AbstractBackendFactoryTest {

    @Nonnull
    @Override
    protected Context context() {
        return SparkContext.getDefault();
    }

    @Nonnull
    @Override
    protected Stream<Arguments> allMappings() {
        return Stream.of(
                Arguments.of(new SparkConfig(), DefaultSparkBackend.class)

                // TODO Fill with other mappings
        );
    }
}
