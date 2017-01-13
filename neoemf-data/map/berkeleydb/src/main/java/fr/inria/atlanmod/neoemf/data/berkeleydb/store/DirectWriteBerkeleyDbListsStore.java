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

package fr.inria.atlanmod.neoemf.data.berkeleydb.store;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import fr.inria.atlanmod.neoemf.core.Id;
import fr.inria.atlanmod.neoemf.core.PersistentEObject;
import fr.inria.atlanmod.neoemf.data.berkeleydb.BerkeleyDbPersistenceBackend;
import fr.inria.atlanmod.neoemf.data.structure.FeatureKey;

import org.apache.commons.collections4.CollectionUtils;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.resource.Resource;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static java.util.Objects.isNull;

public class DirectWriteBerkeleyDbListsStore extends DirectWriteBerkeleyDbStore {

    private final Cache<FeatureKey, Object> objectsCache;

    /**
     * Instantiates a new {@code DirectWriteBerkeleyDbListsStore} between the given {@code resource} and the
     * {@code backend}.
     *
     * @param resource the resource to persist and access
     * @param backend the persistence backend used to store the model
     */
    public DirectWriteBerkeleyDbListsStore(Resource.Internal resource, BerkeleyDbPersistenceBackend backend) {
        super(resource, backend);
        this.objectsCache = Caffeine.newBuilder().maximumSize(10000).build();
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
            index = list.indexOf(serializeToProperty((EAttribute) feature, value));
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
            index = list.lastIndexOf(serializeToProperty((EAttribute) feature, value));
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
        backend.storeValue(featureKey, new ArrayList<>());
    }

    @Override
    protected Object getAttribute(PersistentEObject object, EAttribute attribute, int index) {
        Object soughtAttribute = getFromMap(object, attribute);
        if (attribute.isMany()) {
            soughtAttribute = manyValueFrom(soughtAttribute).get(index);
        }
        return parseProperty(attribute, soughtAttribute);
    }

    @Override
    protected Object getReference(PersistentEObject object, EReference reference, int index) {
        Object soughtReference = getFromMap(object, reference);
        if (reference.isMany()) {
            soughtReference = eObject((Id) manyValueFrom(soughtReference).get(index));
        }
        return eObject((Id) soughtReference);
    }

    @Override
    protected Object setAttribute(PersistentEObject object, EAttribute attribute, int index, Object value) {
        Object old;
        FeatureKey featureKey = FeatureKey.from(object, attribute);
        if (!attribute.isMany()) {
            old = backend.storeValue(featureKey, serializeToProperty(attribute, value));
        }
        else {
            List<Object> list = manyValueFrom(getFromMap(featureKey));
            old = list.get(index);
            list.set(index, serializeToProperty(attribute, value));
            backend.storeValue(featureKey, list.toArray());
            old = parseProperty(attribute, old);
        }
        return parseProperty(attribute, old);
    }

    @Override
    protected Object setReference(PersistentEObject object, EReference reference, int index, PersistentEObject value) {
        Object oldId;
        FeatureKey featureKey = FeatureKey.from(object, reference);
        updateContainment(object, reference, value);
        updateInstanceOf(value);
        if (!reference.isMany()) {
            oldId = backend.storeValue(featureKey, value.id());
        }
        else {
            List<Object> list = manyValueFrom(getFromMap(featureKey));
            oldId = list.get(index);
            list.set(index, value.id());
            backend.storeValue(featureKey, list.toArray());
        }
        return isNull(oldId) ? null : eObject((Id) oldId);
    }

    @Override
    protected void addAttribute(PersistentEObject object, EAttribute attribute, int index, Object value) {
        FeatureKey featureKey = FeatureKey.from(object, attribute);
        List<Object> list = manyValueFrom(getFromMap(featureKey));
        list.add(index, serializeToProperty(attribute, value));
        backend.storeValue(featureKey, list.toArray());
    }

    @Override
    protected void addReference(PersistentEObject object, EReference reference, int index, PersistentEObject referencedObject) {
        FeatureKey featureKey = FeatureKey.from(object, reference);
        updateContainment(object, reference, referencedObject);
        updateInstanceOf(referencedObject);
        List<Object> list = manyValueFrom(getFromMap(featureKey));
        list.add(index, referencedObject.id());
        backend.storeValue(featureKey, list.toArray());
    }

    @Override
    protected Object removeAttribute(PersistentEObject object, EAttribute attribute, int index) {
        FeatureKey featureKey = FeatureKey.from(object, attribute);
        List<Object> list = manyValueFrom(getFromMap(featureKey));
        Object old = list.get(index);
        list.remove(index);
        backend.storeValue(featureKey, list.toArray());
        return parseProperty(attribute, old);
    }

    @Override
    protected Object removeReference(PersistentEObject object, EReference reference, int index) {
        FeatureKey featureKey = FeatureKey.from(object, reference);
        List<Object> list = manyValueFrom(getFromMap(featureKey));
        Object oldId = list.get(index);
        list.remove(index);
        backend.storeValue(featureKey, list.toArray());
        return eObject((Id) oldId);
    }

    @Override
    public int size(InternalEObject internalObject, EStructuralFeature feature) {
        PersistentEObject object = PersistentEObject.from(internalObject);
        List<Object> list = manyValueFrom(getFromMap(object, feature));
        return isNull(list) ? 0 : list.size();
    }

    @Override
    protected Object getFromMap(PersistentEObject object, EStructuralFeature feature) {
        Object value;
        FeatureKey featureKey = FeatureKey.from(object, feature);
        if (!feature.isMany()) {
            value = backend.valueOf(featureKey);
        }
        else {
            value = objectsCache.get(featureKey, new FeatureKeyCacheLoader());
        }
        return value;
    }

    @SuppressWarnings("unchecked") // Unchecked cast: 'Object' to 'List<...>'
    private List<Object> manyValueFrom(Object value) {
        return (List<Object>) value;
    }

    private class FeatureKeyCacheLoader implements Function<FeatureKey, Object> {

        private static final int ARRAY_SIZE_OFFSET = 10;

        @Override
        public Object apply(FeatureKey key) {
            Object value = backend.valueOf(key);
            if (isNull(value)) {
                value = new ArrayList<>();
            }
            else if (value instanceof Object[]) {
                Object[] array = (Object[]) value;
                List<Object> list = new ArrayList<>(array.length + ARRAY_SIZE_OFFSET);
                CollectionUtils.addAll(list, array);
                value = list;
            }
            return value;
        }
    }
}
