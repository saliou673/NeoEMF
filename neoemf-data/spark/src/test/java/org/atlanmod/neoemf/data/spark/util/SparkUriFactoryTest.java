package org.atlanmod.neoemf.data.spark.util;

import fr.inria.atlanmod.neoemf.context.Context;
import fr.inria.atlanmod.neoemf.util.AbstractUriFactoryTest;

import org.atlanmod.neoemf.data.spark.context.SparkContext;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * A test-case about {@link SparkUriFactory}.
 */
@ParametersAreNonnullByDefault
class SparkUriFactoryTest extends AbstractUriFactoryTest {

    @Nonnull
    @Override
    protected Context context() {
        return SparkContext.getDefault();
    }
}
