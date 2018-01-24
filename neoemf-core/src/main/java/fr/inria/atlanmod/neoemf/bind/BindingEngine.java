/*
 * Copyright (c) 2013-2018 Atlanmod, Inria, LS2N, and IMT Nantes.
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v2.0 which accompanies
 * this distribution, and is available at https://www.eclipse.org/legal/epl-2.0/
 */

package fr.inria.atlanmod.neoemf.bind;

import fr.inria.atlanmod.commons.Throwables;
import fr.inria.atlanmod.commons.annotation.Static;
import fr.inria.atlanmod.commons.concurrent.MoreExecutors;
import fr.inria.atlanmod.commons.reflect.MoreReflection;
import fr.inria.atlanmod.commons.reflect.ReflectionException;
import fr.inria.atlanmod.neoemf.data.BackendFactory;
import fr.inria.atlanmod.neoemf.util.UriBuilder;

import org.eclipse.emf.common.util.URI;
import org.osgi.framework.BundleContext;
import org.reflections.Configuration;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import java.lang.annotation.Annotation;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import static fr.inria.atlanmod.commons.Preconditions.checkNotNull;
import static java.util.Objects.nonNull;

/**
 * A static utility class for binding and reflection.
 *
 * @see ClasspathCollector
 */
@Static
@ParametersAreNonnullByDefault
// TODO Handle the case of a name/scheme that refers to several factories (`factoryFor`, `findAny`): overriding factory
public final class BindingEngine {

    /**
     * The shared concurrent pool for the scanning of elements in the classpath.
     *
     * @see ConfigurationBuilder#setExecutorService(ExecutorService)
     */
    @Nonnull
    private static final ExecutorService BINDING_POOL = MoreExecutors.newFixedThreadPool("binding-scanner");

    static {
        // Add the default URLs for scanning
        ClasspathCollector.getInstance().register(new SimpleCollector(ClasspathHelper::forJavaClassPath));
        ClasspathCollector.getInstance().register(new SimpleCollector(ClasspathHelper::forManifest));
    }

    private BindingEngine() {
        throw Throwables.notInstantiableClass(getClass());
    }

    /**
     * Returns the shared {@link ExecutorService} for asynchronous tasks related to binding.
     *
     * @return an immutable {@link ExecutorService}
     */
    @Nonnull
    static ExecutorService getBindingPool() {
        return BINDING_POOL;
    }

    /**
     * Adds a {@link BundleContext} to be scanned for binding.
     * <p>
     * <b>NOTE:</b> This method is intended for internal use and should not be call in standard use.
     *
     * @param context the OSGi context to add for scanning
     *
     * @throws NullPointerException if the {@code context} is {@code null}
     * @see fr.inria.atlanmod.neoemf.util.Activator#start(BundleContext)
     */
    public static void withContext(BundleContext context) {
        ClasspathCollector.getInstance().register(new BundleContextCollector(context));
    }

    // region Reflection

    /**
     * Retrieves all types annotated with the specified {@code annotation}.
     *
     * @param annotation the expected annotation
     *
     * @return a set of annotated instances
     *
     * @see Reflections#getTypesAnnotatedWith(Class)
     */
    @Nonnull
    private static Set<Class<?>> typesAnnotatedWith(Class<? extends Annotation> annotation) {
        Configuration conf = createConfiguration().setScanners(new TypeAnnotationsScanner(), new SubTypesScanner());
        return new Reflections(conf).getTypesAnnotatedWith(annotation);
    }

    /**
     * Retrieves all types annotated with the specified {@code annotation}, which are also assignable from the given
     * {@code type}.
     *
     * @param annotation the expected annotation
     * @param type       the type of the expected classes
     * @param <T>        the type of the instances to look for
     *
     * @return a set of annotated instances of {@code type}
     */
    @Nonnull
    @SuppressWarnings("unchecked")
    private static <T> Set<Class<? extends T>> typesAnnotatedWith(Class<? extends Annotation> annotation, Class<? extends T> type) {
        return typesAnnotatedWith(annotation).stream()
                .filter(type::isAssignableFrom)
                .map(c -> (Class<? extends T>) c)
                .collect(Collectors.toSet());
    }

    /**
     * Creates a new default configuration for classpath analysis.
     *
     * @return a new configuration
     */
    @Nonnull
    private static ConfigurationBuilder createConfiguration() {
        return new ConfigurationBuilder()
                .setExecutorService(getBindingPool())
                .setUrls(ClasspathCollector.getInstance().get());
    }

    // endregion

    /**
     * Retrieves the {@link URI} scheme for the speficied {@code type}.
     * <p>
     * The {@code type} must be a sub-class of {@link BackendFactory}, or must be annotated with {@link
     * FactoryBinding}.
     *
     * @param type the type
     *
     * @return the {@link URI} scheme
     *
     * @throws BindingException if no scheme is defined for this {@code type}
     */
    @Nonnull
    public static String schemeOf(Class<?> type) {
        return UriBuilder.createScheme(nameOf(type));
    }

    /**
     * Retrieves the {@link URI} scheme for the specified {@code factory}.
     *
     * @param factory the factory
     *
     * @return the {@link URI} scheme
     *
     * @throws BindingException if no scheme is defined for this {@code type}
     */
    @Nonnull
    public static String schemeOf(BackendFactory factory) {
        return UriBuilder.createScheme(factory.name());
    }

    /**
     * Retrieves the name for the specified {@code type}.
     * <p>
     * The {@code type} must be a sub-class of {@link BackendFactory}, or must be annotated with {@link
     * FactoryBinding}.
     *
     * @param type the type
     *
     * @return the name
     *
     * @throws BindingException if no name is defined for this {@code type}
     */
    @Nonnull
    public static String nameOf(Class<?> type) {
        return factoryFor(type).name();
    }

    /**
     * Retrieves the {@link BackendFactory} associated to the {@code type}.
     * <p>
     * The {@code type} <b>must</b> be annotated with {@link FactoryBinding}.
     *
     * @param type the type of the instance to look for
     *
     * @return a new instance of {@link BackendFactory}
     *
     * @throws BindingException    if no instance of {@link BackendFactory} is found for the {@code type}
     * @throws ReflectionException if an error occurs during the instantiation
     */
    @Nonnull
    @SuppressWarnings("unchecked")
    public static BackendFactory factoryFor(Class<?> type) {
        checkNotNull(type, "type");

        Class<? extends BackendFactory> factoryType = null;

        if (BackendFactory.class.isAssignableFrom(type)) {
            factoryType = (Class<? extends BackendFactory>) type;
        }
        else if (type.isAnnotationPresent(FactoryBinding.class)) {
            factoryType = type.getAnnotation(FactoryBinding.class).value();
        }

        return Optional.ofNullable(factoryType)
                .map(MoreReflection::newInstance)
                .orElseThrow(() -> new BindingException(
                        String.format("%s is not annotated with %s: Unable to retrieve the associated factory", type.getName(), FactoryBinding.class.getName())));
    }

    /**
     * Returns all {@link BackendFactory} instances that are visible in the classpath.
     *
     * @return a set of initialized factories
     *
     * @throws ReflectionException if an error occurs during the instantiation of any factory
     */
    @Nonnull
    public static Set<BackendFactory> allFactories() {
        return typesAnnotatedWith(FactoryBinding.class, UriBuilder.class)
                .stream()
                .map(t -> t.getAnnotation(FactoryBinding.class).value())
                .distinct()
                .map(MoreReflection::newInstance)
                .collect(Collectors.toSet());
    }

    /**
     * Retrieves the instance of the {@code type} that is bound to a {@link fr.inria.atlanmod.neoemf.data.BackendFactory}
     * with the given {@code value}, by using the speficied {@code valueMapping}.
     * <p>
     * The {@code type} <b>must</b> be annotated with {@link FactoryBinding}.
     *
     * @param type         the type of the instance to look for
     * @param value        the expected value
     * @param valueMapping the function used to retrieve the current value of a factory
     * @param <T>          the type of the instance
     *
     * @return a new instance of the {@code type}
     *
     * @throws BindingException    if no instance of {@code type} is found for the {@code value} by using the {@code valueMapping}
     * @throws ReflectionException if an error occurs during the instantiation
     */
    @Nonnull
    public static <T> T findBy(Class<? super T> type, String value, Function<Class<? extends BackendFactory>, String> valueMapping) {
        return (T) typesAnnotatedWith(FactoryBinding.class, type)
                .stream()
                .filter(t -> {
                    FactoryBinding a = t.getDeclaredAnnotation(FactoryBinding.class);
                    return nonNull(a) && Objects.equals(value, valueMapping.apply(a.value()));
                })
                .findFirst()
                .map(MoreReflection::newInstance)
                .orElseThrow(() -> new BindingException(
                        String.format("Unable to find a %s instance for value \"%s\"", type.getName(), value)));
    }
}
