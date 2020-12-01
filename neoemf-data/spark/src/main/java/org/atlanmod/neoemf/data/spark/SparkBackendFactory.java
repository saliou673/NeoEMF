package org.atlanmod.neoemf.data.spark;

import fr.inria.atlanmod.neoemf.data.AbstractBackendFactory;
import fr.inria.atlanmod.neoemf.data.Backend;
import fr.inria.atlanmod.neoemf.data.BackendFactory;

import org.atlanmod.neoemf.data.spark.config.SparkConfig;

import org.osgi.service.component.annotations.Component;

import java.net.URL;
import java.nio.file.Path;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * A {@link fr.inria.atlanmod.neoemf.data.BackendFactory} that creates {@link SparkBackend} instances.
 */
@Component(service = BackendFactory.class)
@ParametersAreNonnullByDefault
public class SparkBackendFactory extends AbstractBackendFactory<SparkConfig> {

    /**
     * Constructs a new {@code SparkBackendFactory}.
     */
    public SparkBackendFactory() {
        super("spark");
    }

    @Nonnull
    @Override
    protected Backend createLocalBackend(Path directory, SparkConfig config) throws Exception {
        final boolean isReadOnly = config.isReadOnly();

        // TODO Start/Create the database

        return createMapper(config.getMapping());
    }

    @Nonnull
    @Override
    protected Backend createRemoteBackend(URL url, SparkConfig config) throws Exception {
        final boolean isReadOnly = config.isReadOnly();

        // TODO Start/Create the database

        return createMapper(config.getMapping());
    }
}
