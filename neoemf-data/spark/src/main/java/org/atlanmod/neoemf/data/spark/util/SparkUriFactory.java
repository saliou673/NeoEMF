package org.atlanmod.neoemf.data.spark.util;

import fr.inria.atlanmod.neoemf.bind.FactoryBinding;
import fr.inria.atlanmod.neoemf.util.AbstractUriFactory;
import fr.inria.atlanmod.neoemf.util.UriFactory;

import org.atlanmod.neoemf.data.spark.SparkBackendFactory;

import org.osgi.service.component.annotations.Component;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * A {@link fr.inria.atlanmod.neoemf.util.UriFactory} that creates Spark specific resource {@link org.eclipse.emf.common.util.URI}s.
 *
 * @see SparkBackendFactory
 * @see fr.inria.atlanmod.neoemf.data.BackendFactoryRegistry
 * @see fr.inria.atlanmod.neoemf.resource.PersistentResourceFactory
 */
@Component(service = UriFactory.class)
@FactoryBinding(factory = SparkBackendFactory.class)
@ParametersAreNonnullByDefault
public class SparkUriFactory extends AbstractUriFactory {

    /**
     * Constructs a new {@code SparkUriFactory}.
     */
    public SparkUriFactory() {
        super(true, false);
    }
}
