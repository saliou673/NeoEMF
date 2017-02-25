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

package fr.inria.atlanmod.neoemf.data.berkeleydb.util;

import fr.inria.atlanmod.neoemf.data.PersistenceBackendFactoryRegistry;
import fr.inria.atlanmod.neoemf.data.berkeleydb.BerkeleyDbBackendFactory;
import fr.inria.atlanmod.neoemf.resource.PersistentResourceFactory;
import fr.inria.atlanmod.neoemf.util.PersistenceURI;

import org.apache.commons.io.FileUtils;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.ResourceSet;

import java.io.File;
import java.text.MessageFormat;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A specific {@link PersistenceURI} that creates BerkeleyDB specific resource {@link URI}s from a {@link File}
 * descriptor or an existing {@link URI}.
 * <p>
 * The class defines a BerkeleyDB specific {@link URI} scheme that is used to register {@link
 * BerkeleyDbBackendFactory} in {@link PersistenceBackendFactoryRegistry} and configure the {@code protocol
 * to factory} map of an existing {@link ResourceSet} with a {@link PersistentResourceFactory}.
 *
 * @see PersistenceBackendFactoryRegistry
 * @see BerkeleyDbBackendFactory
 * @see PersistentResourceFactory
 */
@ParametersAreNonnullByDefault
public class BerkeleyDbURI extends PersistenceURI {

    /**
     * The scheme associated to the URI. This scheme is used to register {@link BerkeleyDbBackendFactory}
     * and provide a {@link PersistentResourceFactory} to an existing {@link ResourceSet}.
     *
     * @see PersistenceBackendFactoryRegistry
     * @see BerkeleyDbBackendFactory
     * @see PersistentResourceFactory
     */
    @Nonnull
    public static final String SCHEME = "neo-berkeleydb";

    /**
     * This class should not be instantiated.
     *
     * @throws IllegalStateException every time
     */
    protected BerkeleyDbURI() {
        super();
    }

    /**
     * Creates a new {@code BerkeleyDbURI} from the given {@code uri}.
     * <p>
     * This method checks that the scheme of the provided {@code uri} can be used to create a new {@code BerkeleyDbURI}.
     *
     * @param uri the base {@link URI}
     *
     * @return the created {@link URI}
     *
     * @throws NullPointerException     if the {@code uri} is {@code null}
     * @throws IllegalArgumentException if the scheme of the provided {@code uri} is not {@link #SCHEME} or {@link
     *                                  #FILE_SCHEME}
     * @see #createFileURI(File)
     */
    @Nonnull
    public static URI createURI(URI uri) {
        checkNotNull(uri);

        if (Objects.equals(PersistenceURI.FILE_SCHEME, uri.scheme())) {
            return createFileURI(FileUtils.getFile(uri.toFileString()));
        }
        else if (Objects.equals(SCHEME, uri.scheme())) {
            return PersistenceURI.createURI(uri);
        }

        throw new IllegalArgumentException(MessageFormat.format("Can not create {0} from the URI scheme {1}", BerkeleyDbURI.class.getSimpleName(), uri.scheme()));
    }

    /**
     * Creates a new {@code BerkeleyDbURI} from the given {@link File} descriptor.
     *
     * @param file the {@link File} to build a {@link URI} from
     *
     * @return the created {@link URI}
     *
     * @throws NullPointerException if the {@code file} is {@code null}
     */
    @Nonnull
    public static URI createFileURI(File file) {
        return createFileURI(checkNotNull(file), SCHEME);
    }
}
