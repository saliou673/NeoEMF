package org.atlanmod.neoemf.data.spark.config;

import fr.inria.atlanmod.neoemf.bind.FactoryBinding;
import fr.inria.atlanmod.neoemf.config.BaseConfig;
import fr.inria.atlanmod.neoemf.config.Config;

import org.atlanmod.neoemf.data.spark.SparkBackendFactory;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * A {@link fr.inria.atlanmod.neoemf.config.Config} that creates Spark specific configuration.
 * <p>
 * All features are all optional: configuration can be created using all or none of them.
 */
@Component(service = Config.class, scope = ServiceScope.PROTOTYPE)
@FactoryBinding(factory = SparkBackendFactory.class)
@ParametersAreNonnullByDefault
public class SparkConfig extends BaseConfig<SparkConfig> {

    /**
     * Constructs a new {@code SparkConfig}.
     */
    public SparkConfig() {
        withDefault();

        // TODO Declare all default values
    }

    /**
     * Defines the mapping to use for the created {@link org.atlanmod.neoemf.data.spark.SparkBackend}.
     *
     * @return this configuration (for chaining)
     */
    @Nonnull
    protected SparkConfig withDefault() {
        return setMappingWithCheck("org.atlanmod.neoemf.data.spark.DefaultSparkBackend", false);
    }

    // TODO Add mapping declarations

    // TODO Add methods specific to your database
}
