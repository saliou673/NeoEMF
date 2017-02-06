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

package fr.inria.atlanmod.neoemf.data.map.core.store;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

import fr.inria.atlanmod.neoemf.core.Id;
import fr.inria.atlanmod.neoemf.core.PersistentEObject;
import fr.inria.atlanmod.neoemf.data.PersistenceBackend;
import fr.inria.atlanmod.neoemf.data.map.core.MapBackend;
import fr.inria.atlanmod.neoemf.data.store.AbstractPersistentStoreDecorator;
import fr.inria.atlanmod.neoemf.data.store.DefaultDirectWriteStore;
import fr.inria.atlanmod.neoemf.data.store.PersistentStore;
import fr.inria.atlanmod.neoemf.data.structure.FeatureKey;
import fr.inria.atlanmod.neoemf.resource.PersistentResource;

import org.apache.commons.collections4.CollectionUtils;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.InternalEObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nonnull;

import static com.google.common.base.Preconditions.checkPositionIndex;
import static java.util.Objects.isNull;

/**
 * A {@link fr.inria.atlanmod.neoemf.data.store.DirectWriteStore} that uses Java {@link List}s instead of arrays to
 * persist multi-valued {@link EAttribute}s and {@link EReference}s.
 * <p>
 * Using a {@link List}-based implementation allows to benefit from the rich Java {@link Collection} API, with the cost
 * of a small memory overhead compared to raw arrays.
 * <p>
 * This class re-implements {@link EStructuralFeature} accessors and mutators as well as {@link Collection} operations
 * such as {@code size}, {@code clear}, or {@code indexOf}.
 * <p>
 * This store can be used as a base store that can be complemented by plugging decorator stores on top of it (see {@link
 * AbstractPersistentStoreDecorator} subclasses) to provide additional features such as caching or logging.
 *
 * @see fr.inria.atlanmod.neoemf.data.store.DirectWriteStore
 * @see MapBackend
 * @see AbstractPersistentStoreDecorator
 */
@Deprecated
// TODO Unusable: must be completely reviewed
public class DirectWriteMapStoreWithLists extends DefaultDirectWriteStore<PersistenceBackend> {

    /**
     * In-memory cache that holds multi-valued {@link EStructuralFeature}s wrapped in a {@link List}, identified by
     * their associated {@link FeatureKey}.
     */
    protected final LoadingCache<FeatureKey, Object> objectsCache = Caffeine.newBuilder()
            .initialCapacity(1_000)
            .maximumSize(10_000)
            .build(new FeatureKeyCacheLoader());

    /**
     * Constructs a new {@code DirectWriteMapStoreWithLists} between the given {@code resource} and the {@code backend}.
     *
     * @param resource the resource to persist and access
     * @param backend  the persistence back-end used to store the model
     */
    public DirectWriteMapStoreWithLists(PersistentResource resource, PersistenceBackend backend) {
        super(resource, backend);
    }

    /**
     * Casts the {@code value} as a {@link List}.
     *
     * @param value the object to cast
     *
     * @return a list
     */
    @SuppressWarnings("unchecked") // Unchecked cast: 'Object' to 'List<...>'
    protected List<Object> manyValueFrom(Object value) {
        return (List<Object>) value;
    }

    @Override
    public boolean isSet(InternalEObject internalObject, EStructuralFeature feature) {
        FeatureKey featureKey = FeatureKey.from(internalObject, feature);
        return backend.hasValue(featureKey);
    }

    @Override
    public void unset(InternalEObject internalObject, EStructuralFeature feature) {
        FeatureKey featureKey = FeatureKey.from(internalObject, feature);
        backend.unsetValue(featureKey);
    }

    @Override
    public int size(InternalEObject internalObject, EStructuralFeature feature) {
        PersistentEObject object = PersistentEObject.from(internalObject);
        List<Object> list = manyValueFrom(getFromMap(object, feature));
        return isNull(list) ? 0 : list.size();
    }

    @Override
    public boolean contains(InternalEObject internalObject, EStructuralFeature feature, Object value) {
        return indexOf(internalObject, feature, value) != PersistentStore.NO_INDEX;
    }

    @Override
    public int indexOf(InternalEObject internalObject, EStructuralFeature feature, Object value) {
        int index;
        PersistentEObject object = PersistentEObject.from(internalObject);
        List<Object> list = manyValueFrom(getFromMap(object, feature));
        if (isNull(list)) {
            index = NO_INDEX;
        }
        else if (feature instanceof EAttribute) {
            index = list.indexOf(serialize((EAttribute) feature, value));
        }
        else {
            PersistentEObject childEObject = PersistentEObject.from(value);
            index = list.indexOf(childEObject.id());
        }
        return index;
    }

    @Override
    public int lastIndexOf(InternalEObject internalObject, EStructuralFeature feature, Object value) {
        int index;
        PersistentEObject object = PersistentEObject.from(internalObject);
        List<Object> list = manyValueFrom(getFromMap(object, feature));
        if (isNull(list)) {
            index = NO_INDEX;
        }
        else if (feature instanceof EAttribute) {
            index = list.lastIndexOf(serialize((EAttribute) feature, value));
        }
        else {
            PersistentEObject childEObject = PersistentEObject.from(value);
            index = list.lastIndexOf(childEObject.id());
        }
        return index;
    }

    @Override
    public void clear(InternalEObject internalObject, EStructuralFeature feature) {
        FeatureKey featureKey = FeatureKey.from(internalObject, feature);
        backend.valueFor(featureKey, new ArrayList<>());
    }

    @Override
    public Object[] toArray(InternalEObject internalObject, EStructuralFeature feature) {
        PersistentEObject object = PersistentEObject.from(internalObject);
        Object value = getFromMap(object, feature);
        if (feature.isMany()) {
            int valueLength = ((Object[]) value).length;
            return internalToArray(value, feature, new Object[valueLength]);
        }
        else {
            return internalToArray(value, feature, new Object[1]);
        }
    }

    @Override
    public <T> T[] toArray(InternalEObject internalObject, EStructuralFeature feature, T[] array) {
        PersistentEObject object = PersistentEObject.from(internalObject);
        Object value = getFromMap(object, feature);
        return internalToArray(value, feature, array);
    }

    @Override
    protected Object getAttribute(PersistentEObject object, EAttribute attribute, int index) {
        Object soughtAttribute = getFromMap(object, attribute);
        if (attribute.isMany()) {
            soughtAttribute = manyValueFrom(soughtAttribute).get(index);
        }
        return deserialize(attribute, soughtAttribute);
    }

    @Override
    protected PersistentEObject getReference(PersistentEObject object, EReference reference, int index) {
        PersistentEObject result;
        Object value = getFromMap(object, reference);
        if (isNull(value)) {
            result = null;
        }
        else {
            if (reference.isMany()) {
                List<Object> aList = manyValueFrom(value);
                checkPositionIndex(index, aList.size(), "Invalid index");
                Id id = (Id) aList.get(index);
                result = eObject(id);
            }
            else {
                Id id = (Id) value;
                result = eObject(id);
            }
        }
        return result;
    }

    @Override
    protected Object setAttribute(PersistentEObject object, EAttribute attribute, int index, Object value) {
        Object old;
        FeatureKey featureKey = FeatureKey.from(object, attribute);
        if (!attribute.isMany()) {
            old = backend.valueFor(featureKey, serialize(attribute, value)).orElse(null);
        }
        else {
            List<Object> list = manyValueFrom(backend.valueOf(featureKey).orElse(null));
            old = list.get(index);
            list.set(index, serialize(attribute, value));
            backend.valueFor(featureKey, list.toArray());
            old = deserialize(attribute, old);
        }
        return deserialize(attribute, old);
    }

    @Override
    protected PersistentEObject setReference(PersistentEObject object, EReference reference, int index, PersistentEObject value) {
        Object oldId;
        FeatureKey featureKey = FeatureKey.from(object, reference);
        updateContainment(object, reference, value);
        updateInstanceOf(value);
        if (!reference.isMany()) {
            oldId = backend.valueFor(featureKey, value.id()).orElse(null);
        }
        else {
            List<Object> list = manyValueFrom(backend.valueOf(featureKey).orElse(null));
            oldId = list.get(index);
            list.set(index, value.id());
            backend.valueFor(featureKey, list.toArray());
        }
        return isNull(oldId) ? null : eObject((Id) oldId);
    }

    @Override
    protected void addAttribute(PersistentEObject object, EAttribute attribute, int index, Object value) {
        FeatureKey featureKey = FeatureKey.from(object, attribute);
        List<Object> list = manyValueFrom(backend.valueOf(featureKey).orElse(null));
        list.add(index, serialize(attribute, value));
        backend.valueFor(featureKey, list.toArray());
    }

    @Override
    protected void addReference(PersistentEObject object, EReference reference, int index, PersistentEObject referencedObject) {
        FeatureKey featureKey = FeatureKey.from(object, reference);
        updateContainment(object, reference, referencedObject);
        updateInstanceOf(referencedObject);
        List<Object> list = manyValueFrom(backend.valueOf(featureKey).orElse(null));
        list.add(index, referencedObject.id());
        backend.valueFor(featureKey, list.toArray());
    }

    @Override
    protected Object removeAttribute(PersistentEObject object, EAttribute attribute, int index) {
        FeatureKey featureKey = FeatureKey.from(object, attribute);
        List<Object> list = manyValueFrom(backend.valueOf(featureKey).orElse(null));
        Object old = list.get(index);
        list.remove(index);
        backend.valueFor(featureKey, list.toArray());
        return deserialize(attribute, old);
    }

    @Override
    protected PersistentEObject removeReference(PersistentEObject object, EReference reference, int index) {
        FeatureKey featureKey = FeatureKey.from(object, reference);
        List<Object> list = manyValueFrom(backend.valueOf(featureKey).orElse(null));
        Object oldId = list.get(index);
        list.remove(index);
        backend.valueFor(featureKey, list.toArray());
        return eObject((Id) oldId);
    }

    @Deprecated
    protected Object getFromMap(PersistentEObject object, EStructuralFeature feature) {
        Object value;
        FeatureKey featureKey = FeatureKey.from(object, feature);
        if (!feature.isMany()) {
            value = backend.valueOf(featureKey).orElse(null);
        }
        else {
            value = objectsCache.get(featureKey);
        }
        return value;
    }

    /**
     * Reifies the element(s) in {@code value} and put them into {@code output}.
     *
     * @param value   the backend record to reify
     * @param feature the {@link EStructuralFeature} used to reify {@code value}
     * @param output  the array to fill
     *
     * @return {@code output} filled with the reified values
     */
    @SuppressWarnings("unchecked")
    private <T> T[] internalToArray(Object value, EStructuralFeature feature, T[] output) {
        if (feature.isMany()) {
            Object[] storedArray = (Object[]) value;
            if (feature instanceof EReference) {
                for (int i = 0; i < storedArray.length; i++) {
                    output[i] = (T) eObject((Id) storedArray[i]);
                }
            }
            else { // EAttribute
                for (int i = 0; i < storedArray.length; i++) {
                    output[i] = (T) deserialize((EAttribute) feature, storedArray[i]);
                }
            }
        }
        else {
            if (feature instanceof EReference) {
                output[0] = (T) eObject((Id) value);
            }
            else { // EAttribute
                output[0] = (T) deserialize((EAttribute) feature, value);
            }
        }
        return output;
    }

    /**
     * A cache loader to retrieve a {@link Object} stored in the database.
     */
    public class FeatureKeyCacheLoader implements CacheLoader<FeatureKey, Object> {

        @Override
        public Object load(@Nonnull FeatureKey key) {
            Object value = backend.valueOf(key).orElse(new ArrayList<>());
            if (value instanceof Object[]) {
                Object[] array = (Object[]) value;
                List<Object> list = new ArrayList<>(array.length + 10);
                CollectionUtils.addAll(list, array);
                value = list;
            }
            return value;
        }
    }
}