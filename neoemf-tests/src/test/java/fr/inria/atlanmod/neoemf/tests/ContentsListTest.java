/*
 * Copyright (c) 2013 Atlanmod.
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v2.0 which accompanies
 * this distribution, and is available at https://www.eclipse.org/legal/epl-2.0/
 */

package fr.inria.atlanmod.neoemf.tests;

import fr.inria.atlanmod.neoemf.context.Context;
import fr.inria.atlanmod.neoemf.resource.PersistentResource;
import fr.inria.atlanmod.neoemf.tests.provider.ContextProvider;
import fr.inria.atlanmod.neoemf.tests.sample.Node;
import fr.inria.atlanmod.neoemf.tests.sample.Tree;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import javax.annotation.ParametersAreNonnullByDefault;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * A test-case about {@link fr.inria.atlanmod.neoemf.core.internal.collect.ContentsList}.
 */
@ParametersAreNonnullByDefault
class ContentsListTest extends AbstractResourceBasedTest {

    /**
     * The expected number of {@link Tree} contained in {@link #tree}.
     */
    private static final int TREES_COUNT = 5;

    /**
     * The expected number of {@link Node} contained in {@link #tree}.
     */
    private static final int NODES_COUNT = 3;

    /**
     * The root {@link Tree}.
     */
    private Tree tree;

    /**
     * The {@link Tree}s contained in {@link #tree}.
     */
    private List<Tree> treeChildren;

    /**
     * The owned content of {@link #tree}.
     */
    private List<Node> treeNodes;

    @ParameterizedTest
    @ArgumentsSource(ContextProvider.All.class)
    void testEObjectEContents(Context context) throws IOException {
        try (PersistentResource resource = createPersistentResource(context)) {
            fillResource(resource);

            List<EObject> eContents = tree.eContents();
            assertThat(eContents).hasSize(TREES_COUNT + NODES_COUNT);

            IntStream.range(0, NODES_COUNT).forEachOrdered(i ->
                    assertThat(eContents.get(i)).isEqualTo(treeNodes.get(i))
            );

            IntStream.range(0, TREES_COUNT).forEachOrdered(i ->
                    assertThat(eContents.get(i + NODES_COUNT)).isEqualTo(treeChildren.get(i))
            );
        }
    }

    @ParameterizedTest
    @ArgumentsSource(ContextProvider.All.class)
    void testEObjectEmptyEContentsSize(Context context) throws IOException {
        try (PersistentResource resource = createPersistentResource(context)) {
            fillResourceWithEmpty(resource);

            List<EObject> eContents = tree.eContents();
            assertThat(eContents).isEmpty();
        }
    }

    @ParameterizedTest
    @ArgumentsSource(ContextProvider.All.class)
    void testEObjectEmptyEContentsGet(Context context) throws IOException {
        try (PersistentResource resource = createPersistentResource(context)) {
            fillResourceWithEmpty(resource);

            assertThat(
                    catchThrowable(() -> tree.eContents().get(0))
            ).isInstanceOf(IndexOutOfBoundsException.class);
        }
    }

    /**
     * Fills the {@code resource}.
     *
     * @param resource the resource to fill
     */
    private void fillResource(Resource resource) {
        treeChildren = new ArrayList<>();
        treeNodes = new ArrayList<>();

        Tree rootTree = EFACTORY.createTree();
        rootTree.setName("RootTree");

        tree = EFACTORY.createTree();
        tree.setName("Tree0");
        tree.setParent(rootTree);

        IntStream.range(0, NODES_COUNT).forEachOrdered(i -> {
            Node node = EFACTORY.createPhysicalNode();
            node.setLabel("Node0-" + i);
            tree.getNodes().add(node);
            treeNodes.add(node);
        });

        IntStream.range(0, TREES_COUNT).forEachOrdered(i -> {
            Tree subTree = EFACTORY.createTree();
            subTree.setName("Tree0-" + i);
            tree.getChildren().add(subTree);
            treeChildren.add(subTree);
        });

        resource.getContents().add(tree);
    }

    /**
     * Fills the {@code resource} with an empty {@link Tree}.
     *
     * @param resource the resource to fill
     */
    private void fillResourceWithEmpty(Resource resource) {
        tree = EFACTORY.createTree();
        tree.setName("EmptyTree");
        resource.getContents().add(tree);
    }
}
