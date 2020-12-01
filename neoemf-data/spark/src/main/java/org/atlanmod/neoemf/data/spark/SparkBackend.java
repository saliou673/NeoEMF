package org.atlanmod.neoemf.data.spark;

import fr.inria.atlanmod.neoemf.data.Backend;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * A {@link fr.inria.atlanmod.neoemf.data.Backend} that is responsible of low-level access to a Spark database.
 * <p>
 * It wraps an existing Spark database and provides facilities to create and retrieve elements.
 * <p>
 * <b>Note:</b> Instances of {@code SparkBackend} are created by {@link SparkBackendFactory} that
 * provides an usable database that can be manipulated by this wrapper.
 *
 * @see SparkBackendFactory
 */
@ParametersAreNonnullByDefault
public interface SparkBackend extends Backend {

    @Override
    default boolean isPersistent() {
        // TODO Implement this method
        return true;
    }

    @Override
    default boolean isDistributed() {
        // TODO Implement this method
        return false;
    }
}
