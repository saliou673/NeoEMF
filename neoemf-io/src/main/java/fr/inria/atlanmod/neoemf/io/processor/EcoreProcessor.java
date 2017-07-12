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

package fr.inria.atlanmod.neoemf.io.processor;

import fr.inria.atlanmod.common.log.Log;
import fr.inria.atlanmod.neoemf.io.Handler;
import fr.inria.atlanmod.neoemf.io.bean.BasicAttribute;
import fr.inria.atlanmod.neoemf.io.bean.BasicElement;
import fr.inria.atlanmod.neoemf.io.bean.BasicId;
import fr.inria.atlanmod.neoemf.io.bean.BasicMetaclass;
import fr.inria.atlanmod.neoemf.io.bean.BasicNamespace;
import fr.inria.atlanmod.neoemf.io.bean.BasicReference;
import fr.inria.atlanmod.neoemf.util.EObjects;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import static fr.inria.atlanmod.common.Preconditions.checkNotNull;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * A {@link Processor} that creates and links simple elements to an EMF structure.
 */
@ParametersAreNonnullByDefault
public class EcoreProcessor extends AbstractProcessor<Handler> {

    /**
     * A LIFO that holds the current {@link EClass} chain. It contains the current metaclass and the previous.
     */
    @Nonnull
    private final Deque<EClass> previousClasses = new ArrayDeque<>();

    /**
     * A LIFO that holds the current {@link BasicId} chain. It contains the current identifier and the previous.
     */
    @Nonnull
    private final Deque<BasicId> previousIds = new ArrayDeque<>();

    /**
     * An attribute that is waiting a value.
     *
     * @see #onCharacters(String)
     */
    @Nullable
    private BasicAttribute waitingAttribute;

    /**
     * Defines if the previous element was an attribute, or not.
     */
    private boolean previousWasAttribute;

    /**
     * Constructs a new {@code EcoreProcessor} with the given {@code handler}.
     *
     * @param handler the handler to notify
     */
    public EcoreProcessor(Handler handler) {
        super(handler);
    }

    @Override
    public void onStartElement(BasicElement element) {
        // Is root
        if (previousClasses.isEmpty()) {
            processElementAsRoot(element);
        }
        // Is a feature of parent
        else {
            processElementAsFeature(element);
        }
    }

    @Override
    public void onAttribute(BasicAttribute attribute) {
        EClass cls = previousClasses.getLast();
        EStructuralFeature feature = cls.getEStructuralFeature(attribute.name());

        // Checks that the attribute is well a attribute
        if (EObjects.isAttribute(feature)) {
            EAttribute eAttribute = EObjects.asAttribute(feature);
            attribute.isMany(eAttribute.isMany());

            notifyAttribute(attribute);
        }

        // Otherwise redirect to the reference handler
        else if (EObjects.isReference(feature)) {
            onReference(BasicReference.from(attribute));
        }
    }

    @Override
    public void onReference(BasicReference reference) {
        EClass cls = previousClasses.getLast();
        EStructuralFeature feature = cls.getEStructuralFeature(reference.name());

        // Checks that the reference is well a reference
        if (EObjects.isReference(feature)) {
            EReference eReference = EObjects.asReference(feature);

            AtomicInteger index = new AtomicInteger();

            BasicId id = reference.id();
            String name = reference.name();
            boolean isMany = eReference.isMany();
            boolean isContainment = eReference.isContainment();

            EClass referenceType = eReference.getEReferenceType();
            BasicMetaclass metaclassRef = new BasicMetaclass(
                    BasicNamespace.Registry.getInstance().getFromUri(referenceType.getEPackage().getNsURI()),
                    referenceType.getName());

            Arrays.stream(reference.idReference().value().split(" "))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(s -> {
                        BasicReference newRef = new BasicReference(name);

                        newRef.id(id);
                        newRef.idReference(BasicId.original(s));

                        newRef.index(isMany && !isContainment ? index.getAndIncrement() : -1);

                        newRef.isContainment(isContainment);
                        newRef.isMany(isMany);

                        newRef.metaclassReference(metaclassRef);

                        return newRef;
                    })
                    .forEach(this::notifyReference);
        }

        // Otherwise redirect to the attribute handler
        else if (EObjects.isAttribute(feature)) {
            onAttribute(BasicAttribute.from(reference));
        }
    }

    @Override
    public void onCharacters(String characters) {
        // Defines the value of the waiting attribute, if exists
        if (nonNull(waitingAttribute)) {
            waitingAttribute.value(characters);
            onAttribute(waitingAttribute);

            waitingAttribute = null;
        }
        else {
            Log.debug("Ignoring characters: \"{0}\"", characters);
        }
    }

    @Override
    public void onEndElement() {
        if (!previousWasAttribute) {
            previousClasses.removeLast();
            previousIds.removeLast();

            notifyEndElement();
        }
        else {
            Log.warn("An attribute still waiting for a value: it will be ignored");
            waitingAttribute = null;
            previousWasAttribute = false;
        }
    }

    /**
     * Creates the root element from the given {@code element}.
     *
     * @param element the element representing the root element
     *
     * @throws NullPointerException if the {@code element} does not have a namespace
     */
    private void processElementAsRoot(BasicElement element) {
        BasicNamespace ns = checkNotNull(element.ns(), "The root element must have a namespace");

        // Retrieves the EPackage from NS prefix
        EPackage pkg = checkNotNull(EPackage.class.cast(EPackage.Registry.INSTANCE.get(ns.uri())),
                "EPackage %s is not registered.", ns.uri());

        // Gets the current EClass
        EClass cls = checkNotNull(EClass.class.cast(pkg.getEClassifier(element.name())),
                "Cannot retrieve EClass {0} from the EPackage {1}", element.name(), pkg);

        // Defines the metaclass of the current element if not present
        if (isNull(element.metaclass())) {
            element.metaclass(new BasicMetaclass(ns, cls.getName()));
        }

        // Defines the element as root node
        element.isRoot(true);

        // Notifies next handlers
        notifyStartElement(element);

        // Saves the current EClass
        previousClasses.addLast(cls);

        // Gets the identifier of the element created by next handlers, and save it
        previousIds.addLast(element.id());
    }

    /**
     * Processes a feature and redirects the processing to the associated method according to its type (attribute or
     * reference).
     *
     * @param element the element representing the feature
     *
     * @see #processElementAsAttribute(BasicElement, EAttribute)
     * @see #processElementAsReference(BasicElement, BasicNamespace, EReference, EPackage)
     */
    private void processElementAsFeature(BasicElement element) {
        // Retrieve the parent EClass
        EClass parentCls = previousClasses.getLast();

        // Gets the EPackage from it
        BasicNamespace ns;
        EPackage pkg;

        if (nonNull(element.ns())) {
            ns = element.ns();
            pkg = EPackage.Registry.INSTANCE.getEPackage(ns.uri());
        }
        else {
            pkg = parentCls.getEPackage();
            ns = BasicNamespace.Registry.getInstance().getFromPrefix(pkg.getNsPrefix());
        }

        // Gets the structural feature from the parent, according the its local name (the attr/ref name)
        EStructuralFeature feature = parentCls.getEStructuralFeature(element.name());

        if (EObjects.isAttribute(feature)) {
            processElementAsAttribute(element, EObjects.asAttribute(feature));
        }
        else {
            processElementAsReference(element, ns, EObjects.asReference(feature), pkg);
        }
    }

    /**
     * Processes an attribute.
     *
     * @param element   the element representing the attribute
     * @param attribute the associated EMF attribute
     */
    private void processElementAsAttribute(@SuppressWarnings("unused") BasicElement element, EAttribute attribute) {
        if (nonNull(waitingAttribute)) {
            Log.warn("An attribute still waiting for a value : it will be ignored");
        }

        // Waiting a plain text value
        waitingAttribute = new BasicAttribute(attribute.getName());
        waitingAttribute.id(previousIds.getLast());

        previousWasAttribute = true;
    }

    /**
     * Processes a reference.
     *
     * @param element   the element representing the reference
     * @param ns        the namespace of the class of the reference
     * @param reference the associated EMF reference
     * @param ePackage  the package where to find the class of the reference
     */
    private void processElementAsReference(BasicElement element, BasicNamespace ns, EReference reference, EPackage ePackage) {
        // Gets the type the reference or gets the type from the registered metaclass
        EClass eClass = resolveInstanceOf(element, ns, EClass.class.cast(reference.getEType()), ePackage);

        element.ns(ns);

        // Notify next handlers of new element, and retrieve its identifier
        notifyStartElement(element);
        BasicId currentId = element.id();

        // Create a reference from the parent to this element, with the given local name
        if (reference.isContainment()) {
            BasicReference ref = new BasicReference(reference.getName());
            ref.id(previousIds.getLast());
            ref.idReference(currentId);

            onReference(ref);
        }

        // Save EClass and identifier
        previousClasses.addLast(eClass);
        previousIds.addLast(currentId);
    }

    /**
     * Returns the {@link EClass} associated with the given {@code element}.
     *
     * @param element    the element representing the class
     * @param ns         the namespace of the {@code superClass}
     * @param superClass the super-type of the sought class
     * @param ePackage   the package where to find the class
     *
     * @return a class
     *
     * @throws IllegalArgumentException if the {@code superClass} is not the super-type of the sought class
     */
    @Nonnull
    private EClass resolveInstanceOf(BasicElement element, BasicNamespace ns, EClass superClass, EPackage ePackage) {
        BasicMetaclass metaClass = element.metaclass();

        if (nonNull(metaClass)) {
            EClass subClass = EClass.class.cast(ePackage.getEClassifier(metaClass.name()));

            // Checks that the metaclass is a subtype of the reference type.
            // If true, use it instead of supertype
            if (superClass.isSuperTypeOf(subClass)) {
                superClass = subClass;
            }
            else {
                throw new IllegalArgumentException(
                        String.format("%s is not a subclass of %s", subClass.getName(), superClass.getName()));
            }
        }

        // If not present, create the metaclass from the current class
        else {
            element.metaclass(new BasicMetaclass(ns, superClass.getName()));
        }

        return superClass;
    }
}
