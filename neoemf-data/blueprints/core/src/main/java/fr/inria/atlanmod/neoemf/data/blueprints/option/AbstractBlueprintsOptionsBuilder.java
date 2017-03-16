/*
 * Copyright (c) 2013-2017 Atlanmod INRIA LINA Mines Nantes.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Atlanmod INRIA LINA Mines Nantes - initial API and implementation
 */

package fr.inria.atlanmod.neoemf.data.blueprints.option;

import fr.inria.atlanmod.neoemf.data.structure.ManyFeatureKey;
import fr.inria.atlanmod.neoemf.option.AbstractPersistenceOptionsBuilder;
import fr.inria.atlanmod.neoemf.option.PersistenceOptions;
import fr.inria.atlanmod.neoemf.option.PersistenceOptionsBuilder;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * An abstract {@link PersistenceOptionsBuilder} that provides utility methods to create generic Blueprints options.
 * <p>
 * Created options can be {@link BlueprintsResourceOptions} if they define resource-level features.
 * <p>
 * All features are all optional: options can be created using all or none of them.
 *
 * @param <B> the "self"-type of this {@link PersistenceOptionsBuilder}
 * @param <O> the type of {@link PersistenceOptions} built by this builder
 *
 * @see BlueprintsResourceOptions
 */
@ParametersAreNonnullByDefault
public abstract class AbstractBlueprintsOptionsBuilder<B extends AbstractBlueprintsOptionsBuilder<B, O>, O extends AbstractBlueprintsOptions> extends AbstractPersistenceOptionsBuilder<B, O> {

    /**
     * Constructs a new {@code AbstractBlueprintsOptionsBuilder}.
     */
    protected AbstractBlueprintsOptionsBuilder() {
    }

    /**
     * Adds the given {@code graphType} in the created options.
     *
     * @param graphType the type of the Blueprints graph
     *
     * @return this builder (for chaining)
     *
     * @see BlueprintsResourceOptions#GRAPH_TYPE
     */
    protected B graph(String graphType) {
        return option(BlueprintsResourceOptions.GRAPH_TYPE, graphType);
    }

    /**
     * Defines the mapping to use for the created {@link fr.inria.atlanmod.neoemf.data.blueprints.BlueprintsBackend}.
     * <p>
     * This mapping corresponds to a simple representation of multi-valued features, by using the {@link
     * ManyFeatureKey#position()}.
     * <p>
     * <b>Note:</b> This is the default mapping.
     *
     * @return this builder (for chaining)
     */
    public B withIndices() {
        return mapping(BlueprintsResourceOptions.MAPPING_INDICES);
    }
}
