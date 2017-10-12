/*
 * Copyright (c) 2013-2017 Atlanmod, Inria, LS2N, and IMT Nantes.
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v2.0 which accompanies
 * this distribution, and is available at https://www.eclipse.org/legal/epl-2.0/
 */

package fr.inria.atlanmod.neoemf.core.internal;

import fr.inria.atlanmod.neoemf.core.PersistentEObject;
import fr.inria.atlanmod.neoemf.data.store.Store;
import fr.inria.atlanmod.neoemf.data.store.adapter.StoreAdapter;

import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.impl.EStoreEObjectImpl;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import static java.util.Objects.isNull;

/**
 * A {@link List} representing a multi-valued feature which behaves as a proxy and that delegates its operations to the
 * associated {@link Store}.
 *
 * @param <E> the type of elements in this list
 *
 * @see PersistentEObject#eStore()
 */
@ParametersAreNonnullByDefault
public class LazyStoreList<E> extends EStoreEObjectImpl.BasicEStoreEList<E> {

    @SuppressWarnings("JavaDoc")
    private static final long serialVersionUID = 2630358403343923944L;

    /**
     * The owner of this list.
     */
    @Nonnull
    private final PersistentEObject persistentOwner;

    /**
     * Constructs a new {@code LazyStoreList}.
     *
     * @param owner   the owner the {@code feature}
     * @param feature the feature associated with this list
     */
    public LazyStoreList(PersistentEObject owner, EStructuralFeature feature) {
        super(owner, feature);
        this.persistentOwner = owner;
    }

    @Nonnull
    @Override
    protected StoreAdapter eStore() {
        return persistentOwner.eStore();
    }

    @Override
    protected String delegateToString() {
        return eStore().getAll(owner, eStructuralFeature).stream()
                .map(String::valueOf)
                .collect(Collectors.joining(", ", "[", "]"));
    }

    @Override
    public boolean containsAll(Collection<?> collection) {
        if (isNull(collection) || collection.isEmpty()) {
            return false;
        }

        if (collection.size() <= 1) {
            return contains(collection.iterator().next());
        }

        return eStore().getAll(owner, eStructuralFeature).containsAll(collection);
    }

    @Nonnull
    @Override
    public Object[] toArray() {
        return delegateToArray();
    }

    @Nonnull
    @Override
    public <T> T[] toArray(T[] array) {
        return delegateToArray(array);
    }

    @Override
    public boolean contains(Object object) {
        return delegateContains(object);
    }

    @Override
    public int indexOf(Object object) {
        return delegateIndexOf(object);
    }

    @Override
    public int lastIndexOf(Object object) {
        return delegateLastIndexOf(object);
    }

    @Override
    protected boolean doAddAllUnique(Collection<? extends E> collection) {
        return doAddAllUnique(InternalEObject.EStore.NO_INDEX, collection);
    }

    @Override
    protected boolean doAddAllUnique(int index, Collection<? extends E> collection) {
        ++modCount;

        if (collection.isEmpty()) {
            return false;
        }

        int i = eStore().addAll(owner, eStructuralFeature, index, collection);

        for (E object : collection) {
            didAdd(i, object);
            didChange();
            i++;
        }

        return true;
    }

    @Override
    // TODO Re-implement this method
    public boolean removeAll(Collection<?> collection) {
        return super.removeAll(collection);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Override the default implementation which relies on {@link #size()} to compute the insertion index by providing a
     * custom {@link StoreAdapter#NO_INDEX} features, meaning that the {@link fr.inria.atlanmod.neoemf.data.Backend} has
     * to append the result to the existing list.
     * <p>
     * This behavior allows fast write operation on {@link fr.inria.atlanmod.neoemf.data.Backend} which would otherwise
     * need to deserialize the underlying list to add the element at the specified index.
     */
    @Override
    public boolean add(E object) {
        if (isUnique() && contains(object)) {
            return false;
        }
        else {
            // index = NO_INDEX results as a call to #append() in store, without checking the size
            addUnique(InternalEObject.EStore.NO_INDEX, object);
            return true;
        }
    }
}
