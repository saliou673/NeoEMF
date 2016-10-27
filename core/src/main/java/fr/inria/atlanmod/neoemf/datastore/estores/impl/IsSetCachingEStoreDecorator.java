/*
 * Copyright (c) 2013 Atlanmod INRIA LINA Mines Nantes.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Atlanmod INRIA LINA Mines Nantes - initial API and implementation
 */

package fr.inria.atlanmod.neoemf.datastore.estores.impl;

import fr.inria.atlanmod.neoemf.datastore.estores.PersistentEStore;

import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.InternalEObject;

/**
 * A {@link PersistentEStore} wrapper that caches the presence of a value.
 * 
 */
public class IsSetCachingEStoreDecorator extends AbstractEStoreDecorator {

	private ValueCache<Boolean> cache;

	public IsSetCachingEStoreDecorator(PersistentEStore eStore) {
		super(eStore);
		cache = new ValueCache<>();
	}

	public IsSetCachingEStoreDecorator(PersistentEStore eStore, int cacheSize) {
		super(eStore);
		cache = new ValueCache<>(cacheSize);
	}
	
	@Override
	public void unset(InternalEObject object, EStructuralFeature feature) {
		cache.cacheValue(object, feature, false);
		super.unset(object, feature);
	}

	@Override
	public boolean isSet(InternalEObject object, EStructuralFeature feature) {
		Boolean isSet = cache.getValueFromCache(object, feature);
		return isSet != null ? isSet : super.isSet(object, feature);
	}

	@Override
	public void add(InternalEObject object, EStructuralFeature feature, int index, Object value) {
		cache.cacheValue(object, feature, true);
		super.add(object, feature, index, value);
	}

	@Override
	public Object remove(InternalEObject object, EStructuralFeature feature, int index) {
		cache.invalidateValue(object, feature);
		return super.remove(object, feature, index);
	}
	
	@Override
	public Object set(InternalEObject object, EStructuralFeature feature, int index, Object value) {
		cache.cacheValue(object, feature, true);
		return super.set(object, feature, index, value);
	}

	@Override
	public void clear(InternalEObject object, EStructuralFeature feature) {
		cache.cacheValue(object, feature, false);
		super.clear(object, feature);
	}
	
	@Override
	public Object move(InternalEObject object, EStructuralFeature feature, int targetIndex, int sourceIndex) {
		cache.cacheValue(object, feature, true);
		return super.move(object, feature, targetIndex, sourceIndex);
	}
	
	@Override
	public boolean contains(InternalEObject object, EStructuralFeature feature, Object value) {
		boolean returnValue = super.contains(object, feature, value);
		if (returnValue) {
			cache.cacheValue(object, feature, true);
		}
		return returnValue;
	}
	
	@Override
	public Object get(InternalEObject object, EStructuralFeature feature, int index) {
		Object returnValue = super.get(object, feature, index);
		if (returnValue != null) {
			cache.cacheValue(object, feature, true);
		}
		return returnValue;
	}
}