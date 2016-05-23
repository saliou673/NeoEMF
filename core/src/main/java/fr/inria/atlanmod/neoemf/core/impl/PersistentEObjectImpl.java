/*******************************************************************************
 * Copyright (c) 2013 Atlanmod INRIA LINA Mines Nantes
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Atlanmod INRIA LINA Mines Nantes - initial API and implementation
 *******************************************************************************/

package fr.inria.atlanmod.neoemf.core.impl;

import fr.inria.atlanmod.neoemf.core.Id;
import fr.inria.atlanmod.neoemf.core.PersistentEObject;
import fr.inria.atlanmod.neoemf.datastore.InternalPersistentEObject;
import fr.inria.atlanmod.neoemf.datastore.estores.impl.OwnedTransientEStoreImpl;
import fr.inria.atlanmod.neoemf.logger.NeoLogger;
import fr.inria.atlanmod.neoemf.resources.PersistentResource;
import fr.inria.atlanmod.neoemf.util.NeoEContentsEList;

import org.eclipse.emf.common.util.BasicEMap;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.impl.EStoreEObjectImpl;
import org.eclipse.emf.ecore.impl.MinimalEStoreEObjectImpl;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.Resource.Internal;
import org.eclipse.emf.ecore.util.EcoreEMap;
import org.eclipse.emf.ecore.util.EcoreUtil;

public class PersistentEObjectImpl extends MinimalEStoreEObjectImpl implements InternalPersistentEObject {

	public static int NUMBER_NEW_STORE_FEATURE_SET = 0;
	public static int NUMBER_NEW_STORE_EOBJECT = 0;

	public static int NUMBER_MANY_FEATURE_SET = 0;
	public static int NUMBER_SINGLE_FEATURE_SET = 0;

	protected static final int UNSETTED_FEATURE_ID = -1;

	protected Id id;

	protected Resource.Internal resource;

	protected boolean isMapped;

	/**
	 * The internal cached value of the eContainer. This information should be
	 * also maintained in the underlying {@link EStore}
	 */
	protected InternalEObject eContainer;

	protected int eContainerFeatureID;

	protected EStore eStore;

	public PersistentEObjectImpl() {
		this.id = new StringId(EcoreUtil.generateUUID());
		this.eContainerFeatureID = UNSETTED_FEATURE_ID;
		this.isMapped = false;
	}


	@Override
	public Id id() {
		return id;
	}
	
	@Override
	public void id(Id id) {
		this.id = id;
	}
	
	@Override
	public boolean isMapped() {
	    return isMapped;
	}

	@Override
	public void setMapped(boolean mapped) {
	    this.isMapped = mapped;
	}

	/**
	 * @return InternalEObject the container of the {@link PersistentEObject}
	 * Do not return the same value as standard EMF implementation if the container
	 * has not been accessed with the public method {@link #eContainer()} before.
	 */
	@Override
	public InternalEObject eInternalContainer() {
	    // Do not load the container from the eStore here:
	    // it creates an important overhead and performance loss
		return eContainer == null ? super.eInternalContainer() : eContainer;
	}
	
	@Override
	public EObject eContainer() {
		EObject returnValue;
		if (resource instanceof PersistentResource) {
			InternalEObject container = eStore().getContainer(this);
			eBasicSetContainer(container);
			eBasicSetContainerFeatureID(eContainerFeatureID());
			returnValue = container;
		} else {
			returnValue = super.eContainer();
		}
		return returnValue;
	}
	
	@Override
	public EList<EObject> eContents() {
	    EList<EObject> e = NeoEContentsEList.createNeoEContentsEList(this);
	    return e;
	}

	@Override
	protected void eBasicSetContainer(InternalEObject newContainer) {
		eContainer = newContainer;
		if (newContainer != null && newContainer.eResource() != resource) {
			resource((Resource.Internal) eContainer.eResource());
		}
	}
	
	@Override
	public int eContainerFeatureID() {
		if (eContainerFeatureID == UNSETTED_FEATURE_ID && resource instanceof PersistentResource) {
			EReference containingFeature = (EReference) eStore().getContainingFeature(this);
			if (containingFeature != null) {
				EReference oppositeFeature = containingFeature.getEOpposite();
				if (oppositeFeature != null) {
					eBasicSetContainerFeatureID(eClass().getFeatureID(oppositeFeature));
				} else {
					eBasicSetContainerFeatureID(
							InternalEObject.EOPPOSITE_FEATURE_BASE -
							eInternalContainer().eClass().getFeatureID(containingFeature));
				}
			}
		}
		return eContainerFeatureID;
	}
	
	@Override
	protected void eBasicSetContainerFeatureID(int newContainerFeatureID) {
		eContainerFeatureID = newContainerFeatureID;
	}
	
	@Override
	public Resource eResource() {
		return resource != null ? resource : super.eResource();
	}
	
	@Override
	public Internal resource() {
		return resource;
	}

	@Override
	public void resource(Internal resource) {
	    boolean updated = false;
		this.resource = resource;
		EStore oldStore = eStore;
		// Set the new EStore
		if (resource instanceof PersistentResource) {
			eStore = ((PersistentResource) resource).eStore();
		} else {
			eStore = new OwnedTransientEStoreImpl(this);
		}
		// Move contents from oldStore to eStore
		if (oldStore != null && eStore != null && eStore != oldStore) {
			// If the new store is different, initialize the new store
			// with the data stored in the old store
			for (EStructuralFeature feature : eClass().getEAllStructuralFeatures()) {
				if (oldStore.isSet(this, feature)) {
					if (!feature.isMany()) {
						Object v = oldStore.get(this,feature,EStore.NO_INDEX);
						if(v == null) {
						    NeoLogger.debug("A null value has been detected in the old store (Feature {0}.{1})", ((EClassifier)feature.eContainer()).getName(), feature.getName());
						    // Do nothing
						}else{
							if(feature instanceof EReference) {
								EReference eRef = (EReference)feature;
								if(eRef.isContainment()) {
									InternalPersistentEObject internalElement = NeoEObjectAdapterFactoryImpl.getAdapter(v, InternalPersistentEObject.class);
									if(internalElement.resource() != resource()) {
										internalElement.resource(resource());
									}
								}
							}
							NUMBER_NEW_STORE_FEATURE_SET++;
							NUMBER_SINGLE_FEATURE_SET++;
							updated = true;
							eStore.set(this, feature, EStore.NO_INDEX, v);
						}
					} else {
						eStore.clear(this, feature);
						int size = oldStore.size(this, feature);
						for (int i = 0; i < size; i++) {
							Object v = oldStore.get(this,feature,i);
							if(v == null) {
							    NeoLogger.debug("A null value has been detected in the old store (Feature {0}.{1})", ((EClassifier)feature.eContainer()).getName(), feature.getName());
								// Do nothing
							}else{
								if(feature instanceof EReference) {
									EReference eRef = (EReference)feature;
									if(eRef.isContainment()) {
										InternalPersistentEObject internalElement = NeoEObjectAdapterFactoryImpl.getAdapter(v, InternalPersistentEObject.class);
										if(internalElement.resource() != resource()) {
											internalElement.resource(resource());
										}
									}
								}
								NUMBER_NEW_STORE_FEATURE_SET++;
								NUMBER_MANY_FEATURE_SET++;
								updated = true;
								eStore.add(this, feature, i, v);
							}
						}
					}
				}
			}
		}
		if(updated) NUMBER_NEW_STORE_EOBJECT++;
	}

	@Override
	public EStore eStore() {
		if (eStore == null) {
			eStore = new OwnedTransientEStoreImpl(this);
		}
		return eStore;
	}

	@Override
	protected boolean eIsCaching() {
		return false;
	}
	
	@Override
	public void dynamicSet(int dynamicFeatureID, Object value) {
		EStructuralFeature feature = eDynamicFeature(dynamicFeatureID);
		if (feature.isMany()) {
			eStore().unset(this, feature);
			@SuppressWarnings("rawtypes")
			EList collection = (EList) value;
			for (int index = 0; index < collection.size(); index++) {
				eStore().set(this, feature, index, value);
			}
		} else {
			eStore().set(this, feature, InternalEObject.EStore.NO_INDEX, value);
		}
	}
	
	@Override
	public Object dynamicGet(int dynamicFeatureID) {
		Object returnValue;
		final EStructuralFeature feature = eDynamicFeature(dynamicFeatureID);
		final EClassifier eType = feature.getEType();
		if (feature.isMany()) {
		    if(eType.getInstanceClassName() != null && eType.getInstanceClassName().equals("java.util.Map$Entry")) {
				returnValue = new EStoreEcoreEMap(eType, feature);
		    }
		    else {
				returnValue = new EStoreEObjectImpl.BasicEStoreEList<Object>(this,feature) {
		            @Override
		            public boolean contains(Object object) {
		                return delegateContains(object);
		            }
		        };
//		        return new EStoreEObjectImpl.BasicEStoreEList<Object>(this, feature);
		    }
		} else {
			returnValue = eStore().get(this, feature, EStore.NO_INDEX);
		}
		return returnValue;
	}
	
	@Override
	public void dynamicUnset(int dynamicFeatureID) {
		EStructuralFeature feature = eDynamicFeature(dynamicFeatureID);
		eStore().unset(this, feature);
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		} else if (obj == null || getClass() != obj.getClass()) {
			return false;
		} else {
			PersistentEObject other = (PersistentEObject) obj;
			if (id == null) {
				if (other.id() != null) {
					return false;
				}
			} else if (!id.equals(other.id())) {
				return false;
			}
			return true;
		}
	}
	
	@Override
	public String toString() {
	    StringBuilder result = new StringBuilder(getClass().getName());
	    result.append('@');
	    result.append(Integer.toHexString(hashCode()));

	    if (eIsProxy())
	    {
	      result.append(" (eProxyURI: ");
	      result.append(eProxyURI());
	      if (eDynamicClass() != null)
	      {
	        result.append(" eClass: ");
	        result.append(eDynamicClass());
	      }
	      result.append(')');
	    }
	    else if (eDynamicClass() != null)
	    {
	      result.append(" (eClass: ");
	      result.append(eDynamicClass());
	      result.append(')');
	    } else if (eStaticClass() != null) {
		      result.append(" (eClass: ");
		      result.append(eStaticClass());
		      result.append(')');
	    }
		return result.toString();
	}

	private class EStoreEcoreEMap extends EcoreEMap<Object, Object> {
		private static final long serialVersionUID = 1L;

		public EStoreEcoreEMap(EClassifier eType, EStructuralFeature feature)
		{
			super ((EClass)eType, BasicEMap.Entry.class, null);
			delegateEList = new EntryBasicEStoreEList(feature);
			size = delegateEList.size();
		}

		private class EntryBasicEStoreEList extends EStoreEObjectImpl.BasicEStoreEList<Entry<Object, Object>> {
			private static final long serialVersionUID = 1L;

			public EntryBasicEStoreEList(EStructuralFeature feature) {
				super(PersistentEObjectImpl.this, feature);
			}

			@Override
            protected void didAdd(int index, Entry<Object, Object> newObject)
            {
                doPut(newObject);
            }

			@Override
            protected void didSet(int index, Entry<Object, Object> newObject, Entry<Object, Object> oldObject)
            {
                didRemove(index, oldObject);
                didAdd(index, newObject);
            }

			@Override
            protected void didRemove(int index, Entry<Object, Object> oldObject)
            {
                EStoreEcoreEMap.this.doRemove(oldObject);
            }

			@Override
            protected void didClear(int size, Object [] oldObjects)
            {
                EStoreEcoreEMap.this.doClear();
            }

			@Override
            protected void didMove(int index, Entry<Object, Object> movedObject, int oldIndex)
            {
                EStoreEcoreEMap.this.doMove(movedObject);
            }
		}
	}
}
