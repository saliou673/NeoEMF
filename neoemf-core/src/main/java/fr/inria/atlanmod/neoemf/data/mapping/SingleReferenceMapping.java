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

package fr.inria.atlanmod.neoemf.data.mapping;

import fr.inria.atlanmod.neoemf.core.Id;
import fr.inria.atlanmod.neoemf.data.structure.SingleFeatureKey;

import java.util.Optional;

import javax.annotation.Nonnull;

/**
 * ???
 */
public interface SingleReferenceMapping {

    /**
     * ???
     *
     * @param key ???
     *
     * @return ???
     */
    @Nonnull
    Optional<Id> referenceOf(SingleFeatureKey key);

    /**
     * ???
     *
     * @param key ???
     * @param id  ???
     *
     * @return ???
     */
    @Nonnull
    Optional<Id> referenceFor(SingleFeatureKey key, Id id);

    /**
     * ???
     *
     * @param key ???
     */
    void unsetReference(SingleFeatureKey key);

    /**
     * ???
     *
     * @param key ???
     *
     * @return ???
     */
    boolean hasReference(SingleFeatureKey key);
}
