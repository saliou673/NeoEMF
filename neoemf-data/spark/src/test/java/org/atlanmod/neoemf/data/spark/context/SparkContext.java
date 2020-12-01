package org.atlanmod.neoemf.data.spark.context;

import fr.inria.atlanmod.neoemf.config.ImmutableConfig;
import fr.inria.atlanmod.neoemf.context.AbstractLocalContext;
import fr.inria.atlanmod.neoemf.context.Context;
import fr.inria.atlanmod.neoemf.data.BackendFactory;

import org.atlanmod.neoemf.data.spark.SparkBackendFactory;
import org.atlanmod.neoemf.data.spark.config.SparkConfig;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * A specific {@link Context} for the Spark implementation.
 */
@ParametersAreNonnullByDefault
public abstract class SparkContext extends AbstractLocalContext {

    /**
     * Creates a new {@code BerkeleyDbContext}.
     *
     * @return a new context.
     */
    @Nonnull
    public static Context getDefault() {
        return new SparkContext() {
            @Nonnull
            @Override
            public ImmutableConfig config() {
                return new SparkConfig();
            }
        };
    }

    @Nonnull
    @Override
    public String name() {
        return "Spark";
    }

    @Nonnull
    @Override
    public BackendFactory factory() {
        return new SparkBackendFactory();
    }
}
