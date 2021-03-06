/*
 * Copyright (c) 2013 Atlanmod.
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v2.0 which accompanies
 * this distribution, and is available at https://www.eclipse.org/legal/epl-2.0/
 */

package fr.inria.atlanmod.neoemf.benchmarks.query;

import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.gmt.modisco.java.BodyDeclaration;
import org.eclipse.gmt.modisco.java.ClassDeclaration;
import org.eclipse.gmt.modisco.java.MethodDeclaration;
import org.eclipse.gmt.modisco.java.TypeAccess;
import org.eclipse.gmt.modisco.java.emf.meta.JavaPackage;

import java.util.Collection;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * A {@link Query}
 */
@ParametersAreNonnullByDefault
class GetThrownExceptions extends AbstractQuery<Collection<TypeAccess>> {

    @Nonnull
    @Override
    public Collection<TypeAccess> executeOn(Resource resource) {
        Collection<TypeAccess> result = createUniqueCollection();

        Iterable<ClassDeclaration> classDeclarations = allInstancesOf(resource, JavaPackage.eINSTANCE.getClassDeclaration());

        for (ClassDeclaration type : classDeclarations) {
            appendThrownExceptions(type, result);
        }

        return result;
    }

    /**
     * @param type
     * @param thrownExceptions
     */
    protected void appendThrownExceptions(ClassDeclaration type, Collection<TypeAccess> thrownExceptions) {
        for (BodyDeclaration body : type.getBodyDeclarations()) {
            if (body instanceof MethodDeclaration) {
                MethodDeclaration method = (MethodDeclaration) body;
                thrownExceptions.addAll(method.getThrownExceptions());
            }
        }
    }
}
