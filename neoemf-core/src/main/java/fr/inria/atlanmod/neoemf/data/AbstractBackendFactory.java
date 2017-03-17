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

package fr.inria.atlanmod.neoemf.data;

import fr.inria.atlanmod.neoemf.data.store.DirectWriteStore;
import fr.inria.atlanmod.neoemf.data.store.Store;
import fr.inria.atlanmod.neoemf.option.InvalidOptionException;
import fr.inria.atlanmod.neoemf.option.PersistentResourceOptions;
import fr.inria.atlanmod.neoemf.option.PersistentStoreOptions;
import fr.inria.atlanmod.neoemf.resource.PersistentResource;
import fr.inria.atlanmod.neoemf.util.log.Log;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import static fr.inria.atlanmod.neoemf.util.Preconditions.checkNotNull;
import static java.util.Objects.nonNull;

/**
 * An abstract {@link BackendFactory} that processes common store options and manages the configuration.
 */
@ParametersAreNonnullByDefault
public abstract class AbstractBackendFactory implements BackendFactory {

    /**
     * Constructs a new {@code AbstractBackendFactory}.
     */
    protected AbstractBackendFactory() {
    }

    /**
     * Retrieves the mapping to use from the {@code options}.
     *
     * @param options the options containing the mapping
     *
     * @return the class name of the mapping to use
     *
     * @throws InvalidOptionException if the mapping is not defined
     */
    protected static String mappingFrom(Map<String, Object> options) {
        // Defines the mapping
        if (!options.containsKey(PersistentResourceOptions.MAPPING)) {
            throw new InvalidOptionException("No mapping is defined");
        }

        return options.get(PersistentResourceOptions.MAPPING).toString();
    }

    /**
     * Creates a new instance of the represented {@link Store}.
     *
     * @param className  the name of the class to instantiate
     * @param parameters the parameters of the constructor
     *
     * @return a new instance of {@link Store}
     */
    @Nonnull
    @SuppressWarnings("unchecked")
    protected <T> T newInstanceOf(String className, ConstructorParameter... parameters) {
        try {
            Class<?> type = Class.forName(className, false, getClass().getClassLoader());

            List<Class<?>> types = Arrays.stream(parameters)
                    .map(p -> p.type)
                    .collect(Collectors.toList());

            Constructor<?> constructor = type.getDeclaredConstructor(types.toArray(new Class<?>[types.size()]));
            constructor.setAccessible(true);

            List<Object> values = Arrays.stream(parameters)
                    .map(p -> p.value)
                    .collect(Collectors.toList());

            return (T) constructor.newInstance(values.toArray());
        }
        catch (ClassNotFoundException | InvocationTargetException | NoSuchMethodException | IllegalAccessException | InstantiationException e) {
            throw new RuntimeException(e);
        }
    }

    @Nonnull
    @Override
    public Backend createTransientBackend() {
        if (supportsTransient()) {
            return new DefaultTransientBackend();
        }
        else {
            return new InvalidTransientBackend();
        }
    }

    @Nonnull
    @Override
    public Store createStore(Backend backend, PersistentResource resource, Map<String, Object> options) {
        Store store = new DirectWriteStore(backend, resource);

        if (checkNotNull(options).containsKey(PersistentResourceOptions.STORES)) {
            store = decorateStore(store, options);
        }

        return store;
    }

    /**
     * Decorates a {@code store} with other stores, as specified by the {@code options}.
     *
     * @param store   the store to decorate
     * @param options the options defining the stores to use
     *
     * @return the decorated {@code store}
     */
    @Nonnull
    @SuppressWarnings("unchecked")
    private Store decorateStore(Store store, Map<String, Object> options) {
        List<PersistentStoreOptions> storeOptions = (List<PersistentStoreOptions>) options.get(PersistentResourceOptions.STORES);

        if (!storeOptions.isEmpty()) {
            for (PersistentStoreOptions opt : storeOptions.stream().sorted().collect(Collectors.toList())) {
                List<ConstructorParameter> parameters = opt.parameters().stream()
                        .filter(options::containsKey)
                        .map(options::get)
                        .filter(Objects::nonNull)
                        .map(ConstructorParameter::new)
                        .collect(Collectors.toList());

                parameters.add(0,
                        new ConstructorParameter(store, Store.class));

                Log.info("{0}", parameters);

                store = newInstanceOf(opt.className(), parameters.toArray(new ConstructorParameter[parameters.size()]));
            }
        }

        return store;
    }

    /**
     * Creates and saves the NeoEMF configuration.
     * <p>
     * The configuration is stored as a properties file beside a database in order to identify a {@link
     * PersistentBackend}.
     *
     * @param directory the directory where the configuration must be stored
     *
     * @throws InvalidDataStoreException if the configuration cannot be created in the {@code directory}
     */
    protected void processGlobalConfiguration(File directory) {
        Path path = Paths.get(directory.getAbsolutePath()).resolve(CONFIG_FILE);
        Configuration configuration = Configuration.load(path.toFile());

        if (!configuration.containsKey(BACKEND_PROPERTY)) {
            configuration.setProperty(BACKEND_PROPERTY, name());
        }

        configuration.save();
    }

    /**
     * A simple wrapper for constructor parameters.
     *
     * @see #newInstanceOf(String, ConstructorParameter...)
     * @see Class#getDeclaredConstructor(Class[])
     * @see Constructor#newInstance(Object...)
     */
    protected static final class ConstructorParameter {

        /**
         * The value to use in the constructor.
         */
        private final Object value;

        /**
         * The declared type of the value in the constructor.
         */
        private final Class<?> type;

        /**
         * Constructs a new {@code ConstructorParameter} with the value, and the declared type.
         *
         * @param value the value to use in the constructor
         * @param type  the declared type of the value in the constructor
         */
        public ConstructorParameter(Object value, @Nullable Class<?> type) {
            this.value = value;
            this.type = nonNull(type) ? type : value.getClass();
        }

        /**
         * Constructs a new {@code ConstructorParameter} with the value, and the direct {@link Class} of the
         * {@code value}.
         *
         * @param value the value to use in the constructor
         */
        public ConstructorParameter(Object value) {
            this(value, null);
        }
    }
}
