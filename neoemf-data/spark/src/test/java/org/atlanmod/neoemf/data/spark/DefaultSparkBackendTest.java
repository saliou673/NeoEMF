package org.atlanmod.neoemf.data.spark;

import fr.inria.atlanmod.neoemf.context.Context;
import fr.inria.atlanmod.neoemf.data.mapping.AbstractDataMapperTest;

import org.atlanmod.neoemf.data.spark.context.SparkContext;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * A test-case about {@link DefaultSparkBackend}.
 */
@ParametersAreNonnullByDefault
class DefaultSparkBackendTest extends AbstractDataMapperTest {

    @Nonnull
    @Override
    protected Context context() {
        return SparkContext.getDefault();
    }
}
