/*
 * Copyright (c) 2013 Atlanmod.
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v2.0 which accompanies
 * this distribution, and is available at https://www.eclipse.org/legal/epl-2.0/
 */

package fr.inria.atlanmod.neoemf.data.mongodb.util;

import fr.inria.atlanmod.neoemf.context.Context;
import fr.inria.atlanmod.neoemf.data.mongodb.context.MongoDbDefaultContext;
import fr.inria.atlanmod.neoemf.util.AbstractUriFactoryTest;

import org.junit.jupiter.api.Disabled;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * A test-case about {@link MongoDbUriFactory}.
 */
@ParametersAreNonnullByDefault
class MongoDbUriTest extends AbstractUriFactoryTest {

    @Nonnull
    @Override
    protected Context context() {
        return new MongoDbDefaultContext();
    }

    @Disabled("Not supported")
    @Override
    public void testCreateUriFromFileUri() {
    }

    @Disabled("Not supported")
    @Override
    public void testCreateUriFromStandardUriInvalidScheme() {
    }
}
