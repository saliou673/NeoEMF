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

package fr.inria.atlanmod.neoemf.data.berkeleydb.option;

import fr.inria.atlanmod.neoemf.data.structure.ManyFeatureKey;
import fr.inria.atlanmod.neoemf.option.AbstractPersistenceOptions;
import fr.inria.atlanmod.neoemf.option.PersistenceOptions;

import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * A {@link PersistenceOptions} that creates BerkeleyDB specific options.
 * <p>
 * All features are all optional: options can be created using all or none of them.
 */
@ParametersAreNonnullByDefault
public class BerkeleyDbOptions extends AbstractPersistenceOptions<BerkeleyDbOptions> {

    /**
     * Constructs a new {@code BerkeleyDbOptions}.
     */
    protected BerkeleyDbOptions() {
        withIndices();
    }

    /**
     * Creates a new {@link Map} containing all default settings of {@code BerkeleyDbOptions}.
     *
     * @return an immutable {@link Map}
     */
    @Nonnull
    public static Map<String, Object> noOption() {
        return newBuilder().asMap();
    }

    /**
     * Constructs a new {@code BerkeleyDbOptions} instance with default settings.
     *
     * @return a new builder
     */
    @Nonnull
    public static BerkeleyDbOptions newBuilder() {
        return new BerkeleyDbOptions();
    }

    /**
     * Defines the mapping to use for the created {@link fr.inria.atlanmod.neoemf.data.berkeleydb.BerkeleyDbBackend}.
     * <p>
     * This mapping corresponds to a simple representation of multi-valued features, by using the {@link
     * ManyFeatureKey#position()}.
     * <p>
     * <b>Note:</b> This is the default mapping.
     *
     * @return this builder (for chaining)
     *
     * @see fr.inria.atlanmod.neoemf.data.mapper.ManyValueWithIndices
     */
    public BerkeleyDbOptions withIndices() {
        return mapping("fr.inria.atlanmod.neoemf.data.berkeleydb.BerkeleyDbBackendIndices");
    }

    /**
     * Defines the mapping to use for the created {@link fr.inria.atlanmod.neoemf.data.berkeleydb.BerkeleyDbBackend}.
     * <p>
     * This mapping corresponds to an {@link Object[]} representation of multi-valued features.
     *
     * @return this builder (for chaining)
     *
     * @see fr.inria.atlanmod.neoemf.data.mapper.ManyValueWithArrays
     */
    public BerkeleyDbOptions withArrays() {
        return mapping("fr.inria.atlanmod.neoemf.data.berkeleydb.BerkeleyDbBackendArrays");
    }

    /**
     * Defines the mapping to use for the created {@link fr.inria.atlanmod.neoemf.data.berkeleydb.BerkeleyDbBackend}.
     * <p>
     * This mapping corresponds to a {@link java.util.List} representation of multi-valued features.
     *
     * @return this builder (for chaining)
     *
     * @see fr.inria.atlanmod.neoemf.data.mapper.ManyValueWithLists
     */
    public BerkeleyDbOptions withLists() {
        return mapping("fr.inria.atlanmod.neoemf.data.berkeleydb.BerkeleyDbBackendLists");
    }
}
