/*
 * Copyright (c) 2013-2017 Atlanmod, Inria, LS2N, and IMT Nantes.
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v2.0 which accompanies
 * this distribution, and is available at https://www.eclipse.org/legal/epl-2.0/
 */

package fr.inria.atlanmod.neoemf.io;

import fr.inria.atlanmod.commons.function.Copier;
import fr.inria.atlanmod.neoemf.data.mapping.DataMapper;

import java.io.IOException;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * A {@link Copier} of {@link DataMapper} instances using the direct import/export.
 */
@ParametersAreNonnullByDefault
@SuppressWarnings("unused") // Called dynamically
public final class DirectDataCopier implements Copier<DataMapper> {

    @Override
    public void copy(DataMapper source, DataMapper target) {
        try {
            Migrator.fromMapper(source)
                    .toMapper(target)
                    .migrate();
        }
        catch (IOException e) {
            throw new IllegalStateException(e); // Should never happen with DataMappers
        }
    }
}
