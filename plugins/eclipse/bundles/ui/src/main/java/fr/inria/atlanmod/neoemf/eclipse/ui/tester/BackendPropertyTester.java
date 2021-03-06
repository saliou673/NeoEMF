/*
 * Copyright (c) 2013 Atlanmod.
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v2.0 which accompanies
 * this distribution, and is available at https://www.eclipse.org/legal/epl-2.0/
 */

package fr.inria.atlanmod.neoemf.eclipse.ui.tester;

import fr.inria.atlanmod.neoemf.config.Config;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IFolder;

import java.nio.file.Path;
import java.util.Objects;

/**
 * A {@link PropertyTester} that tests a {@code Backend}.
 */
public class BackendPropertyTester extends PropertyTester {

    /**
     * A property indicating whether a directory represents a backend.
     */
    private static final String IS = "is";

    @Override
    public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
        if (receiver instanceof IFolder) {
            final IFolder folder = (IFolder) receiver;

            if (Objects.equals(IS, property)) {
                final Path directory = folder.getLocation().toFile().toPath();
                return Config.exists(directory) == (Boolean) expectedValue;
            }
        }

        return false;
    }
}
