/*
 * Copyright (c) 2013 Atlanmod.
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v2.0 which accompanies
 * this distribution, and is available at https://www.eclipse.org/legal/epl-2.0/
 */

package fr.inria.atlanmod.neoemf.data.mapdb.config;

import fr.inria.atlanmod.neoemf.bind.FactoryBinding;
import fr.inria.atlanmod.neoemf.config.BaseConfig;
import fr.inria.atlanmod.neoemf.config.Config;
import fr.inria.atlanmod.neoemf.data.mapdb.MapDbBackendFactory;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * A {@link fr.inria.atlanmod.neoemf.config.Config} that creates MapDB specific configuration.
 * <p>
 * The mapping is the only required option. All the others are optional: configuration can be created using all or none
 * of them.
 */
@Component(service = Config.class, scope = ServiceScope.PROTOTYPE)
@FactoryBinding(factory = MapDbBackendFactory.class)
@ParametersAreNonnullByDefault
public class MapDbConfig extends BaseConfig<MapDbConfig> {

    /**
     * Constructs a new {@code MapDbConfig} with default settings.
     * <p>
     * <b>NOTE:</b> This configuration has several possible mappings: no mapping is defined by default.
     *
     * @see #withIndices()
     * @see #withLists()
     * @see #withArrays()
     */
    public MapDbConfig() {
        // Don't set a default mapping for a multi-mapping configuration.
    }

    // region Mapping

    /**
     * Defines the mapping to use for the created {@link fr.inria.atlanmod.neoemf.data.mapdb.MapDbBackend}.
     * <p>
     * This mapping corresponds to a simple representation of multi-valued features, by using the {@link
     * fr.inria.atlanmod.neoemf.data.bean.ManyFeatureBean#position()}.
     * <p>
     * <b>NOTE:</b> This is the default mapping.
     *
     * @return this configuration (for chaining)
     *
     * @see fr.inria.atlanmod.neoemf.data.mapping.ManyValueWithIndices
     */
    @Nonnull
    public MapDbConfig withIndices() {
        return setMappingWithCheck("fr.inria.atlanmod.neoemf.data.mapdb.MapDbBackendIndices", false);
    }

    /**
     * Defines the mapping to use for the created {@link fr.inria.atlanmod.neoemf.data.mapdb.MapDbBackend}.
     * <p>
     * This mapping corresponds to an {@link Object}[] representation of multi-valued features.
     *
     * @return this configuration (for chaining)
     *
     * @see fr.inria.atlanmod.neoemf.data.mapping.ManyValueWithArrays
     */
    @Nonnull
    public MapDbConfig withArrays() {
        return setMappingWithCheck("fr.inria.atlanmod.neoemf.data.mapdb.MapDbBackendArrays", false);
    }

    /**
     * Defines the mapping to use for the created {@link fr.inria.atlanmod.neoemf.data.mapdb.MapDbBackend}.
     * <p>
     * This mapping corresponds to a {@link java.util.List} representation of multi-valued features.
     *
     * @return this configuration (for chaining)
     *
     * @see fr.inria.atlanmod.neoemf.data.mapping.ManyValueWithLists
     */
    @Nonnull
    public MapDbConfig withLists() {
        return setMappingWithCheck("fr.inria.atlanmod.neoemf.data.mapdb.MapDbBackendLists", false);
    }

    // endregion
}
