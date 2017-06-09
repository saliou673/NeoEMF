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

package fr.inria.atlanmod.neoemf.io;

import fr.inria.atlanmod.neoemf.io.util.IOResourceManager;

import org.eclipse.emf.common.util.URI;

import javax.annotation.Nonnull;

public class XmiReaderStandardTest extends AbstractInputTest {

    @Nonnull
    @Override
    protected URI getSample() {
        return IOResourceManager.xmiStandard();
    }

    @Override
    protected boolean useIds() {
        return false;
    }
}
