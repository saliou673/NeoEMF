/*
 * Copyright (c) 2013 Atlanmod.
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v2.0 which accompanies
 * this distribution, and is available at https://www.eclipse.org/legal/epl-2.0/
 */

package fr.inria.atlanmod.neoemf.benchmarks.query;

import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.gmt.modisco.java.ASTNode;
import org.eclipse.gmt.modisco.java.Comment;
import org.eclipse.gmt.modisco.java.CompilationUnit;
import org.eclipse.gmt.modisco.java.Javadoc;
import org.eclipse.gmt.modisco.java.Model;
import org.eclipse.gmt.modisco.java.TagElement;
import org.eclipse.gmt.modisco.java.TextElement;

import java.util.Collection;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * A {@link Query}
 */
@ParametersAreNonnullByDefault
class GetTagComments extends AbstractQuery<Collection<TextElement>> {

    @Nonnull
    @Override
    public Collection<TextElement> executeOn(Resource resource) {
        Collection<TextElement> result = createOrderedCollection();

        Model model = getRoot(resource);

        for (CompilationUnit cu : model.getCompilationUnits()) {
            for (Comment comment : cu.getCommentList()) {
                if (comment instanceof Javadoc) {
                    Javadoc javadoc = (Javadoc) comment;
                    for (TagElement tag : javadoc.getTags()) {
                        for (ASTNode node : tag.getFragments()) {
                            if (node instanceof TextElement) {
                                TextElement text = (TextElement) node;
                                result.add(text);
                            }
                        }
                    }
                }
            }
        }

        return result;
    }
}
