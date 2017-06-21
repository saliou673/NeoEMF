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

package fr.inria.atlanmod.neoemf.data.structure;

import fr.inria.atlanmod.neoemf.core.PersistentEObject;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.Immutable;

import static fr.inria.atlanmod.common.Preconditions.checkNotNull;
import static java.util.Objects.isNull;

/**
 * A simple representation of a {@link EClass}.
 */
@Immutable
@ParametersAreNonnullByDefault
public class ClassDescriptor implements Serializable {

    @SuppressWarnings("JavaDoc")
    private static final long serialVersionUID = 3630220484508625215L;

    /**
     * The name of the metaclass.
     */
    @Nonnull
    private final String name;

    /**
     * The literal representation of the {@link URI} of the metaclass.
     */
    @Nonnull
    private final String uri;

    /**
     * The represented {@link EClass}.
     */
    @Nullable
    private transient EClass eClass;

    /**
     * Constructs a new {@code ClassDescriptor} with the given {@code name} and {@code uri}, which are used as a
     * simple representation of a an {@link EClass}.
     *
     * @param name the name of the {@link EClass}
     * @param uri  the literal representation of the {@link URI} of the {@link EClass}
     */
    protected ClassDescriptor(String name, String uri) {
        this.name = checkNotNull(name);
        this.uri = checkNotNull(uri);
    }

    /**
     * Constructs a new {@code ClassDescriptor} for the represented {@code eClass}.
     *
     * @param eClass the represented {@link EClass}
     */
    private ClassDescriptor(EClass eClass) {
        this(eClass.getName(), eClass.getEPackage().getNsURI());
        this.eClass = eClass;
    }

    /**
     * Creates a new {@code ClassDescriptor} from the given {@code object}. The {@link EClass} will be found by
     * calling the {@link PersistentEObject#eClass()} method.
     * <p>
     * This method behaves like: {@code of(eClass.getName(), eClass.getEPackage().getNsURI())}.
     *
     * @param object the object from which the {@link EClass} has to be retrieve with the {@link
     *               PersistentEObject#eClass()} method
     *
     * @return a new {@code ClassDescriptor}
     *
     * @throws NullPointerException if any argument is {@code null}
     * @see #from(EClass)
     */
    @Nonnull
    public static ClassDescriptor from(PersistentEObject object) {
        return from(object.eClass());
    }

    /**
     * Creates a new {@code ClassDescriptor} from the given {@code eClass}.
     * <p>
     * This method behaves like: {@code of(eClass.getName(), eClass.getEPackage().getNsURI())}.
     *
     * @param eClass the {@link EClass}
     *
     * @return a new {@code ClassDescriptor}
     *
     * @throws NullPointerException if any argument is {@code null}
     */
    @Nonnull
    public static ClassDescriptor from(EClass eClass) {
        return new ClassDescriptor(eClass);
    }

    /**
     * Creates a new {@code ClassDescriptor} with the given {@code name} and {@code uri}, which are used as a simple
     * representation of a an {@link EClass}.
     *
     * @param name the name of the {@link EClass}
     * @param uri  the literal representation of the {@link URI} of the {@link EClass}
     *
     * @return a new {@code ClassDescriptor}
     *
     * @throws NullPointerException if any argument is {@code null}
     */
    @Nonnull
    public static ClassDescriptor of(String name, String uri) {
        return new ClassDescriptor(name, uri);
    }

    /**
     * Returns the name of this {@code ClassDescriptor}.
     *
     * @return the name
     */
    @Nonnull
    public String name() {
        return name;
    }

    /**
     * Returns the literal representation of the {@link URI} of this {@code ClassDescriptor}.
     *
     * @return the URI
     */
    @Nonnull
    public String uri() {
        return uri;
    }

    /**
     * Returns whether this {@code ClassDescriptor} represents an abstract class.
     *
     * @return {@code true} if this {@code ClassDescriptor} represents an abstract class, {@code false} otherwise
     */
    public boolean isAbstract() {
        return get().isAbstract();
    }

    /**
     * Returns whether this {@code ClassDescriptor} represents an interface.
     *
     * @return {@code true} if this {@code ClassDescriptor} represents an interface, {@code false} otherwise
     */
    public boolean isInterface() {
        return get().isInterface();
    }

    /**
     * Retrieves the superclass of this {@code ClassDescriptor}.
     *
     * @return an {@link Optional} containing the representation of the direct superclass, or {@link Optional#empty()}
     * if the class has no superclass
     */
    @Nonnull
    public Optional<ClassDescriptor> inheritFrom() {
        return get().getESuperTypes()
                .parallelStream()
                .filter(c -> !c.isInterface())
                .map(ClassDescriptor::from)
                .findAny();
    }

    /**
     * Retrieves all subclasses of this {@code ClassDescriptor}.
     *
     * @return a immutable {@link Set} containing the representation of all non-abstract subclasses that inherit,
     * directly and indirectly, from this {@code ClassDescriptor}
     */
    @Nonnull
    public Set<ClassDescriptor> inheritedBy() {
        return get().getEPackage().getEClassifiers()
                .parallelStream()
                .filter(EClass.class::isInstance)
                .map(EClass.class::cast)
                .filter(c -> get().isSuperTypeOf(c))
                .filter(c -> !c.isAbstract())
                .filter(c -> !c.isInterface())
                .map(ClassDescriptor::from)
                .collect(Collectors.toSet());
    }

    /**
     * Retrieves the {@link EClass} corresponding to this {@code ClassDescriptor}.
     *
     * @return a class, or {@code null} if it cannot be found
     */
    public EClass get() {
        if (isNull(eClass)) {
            eClass = Optional.ofNullable(EPackage.Registry.INSTANCE.getEPackage(uri))
                    .map(p -> p.getEClassifier(name))
                    .map(EClass.class::cast)
                    .<NullPointerException>orElseThrow(() ->
                            new NullPointerException(String.format("Unable to find EPackage for URI: %s", uri)));
        }
        return eClass;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, uri);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!ClassDescriptor.class.isInstance(o)) {
            return false;
        }

        ClassDescriptor that = ClassDescriptor.class.cast(o);
        return Objects.equals(name, that.name)
                && Objects.equals(uri, that.uri);
    }

    @Override
    public String toString() {
        return String.format("ClassDescriptor {%s @ %s}", name, uri);
    }
}

