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

package fr.inria.atlanmod.neoemf.tests.io;

import fr.inria.atlanmod.neoemf.data.PersistenceBackend;
import fr.inria.atlanmod.neoemf.data.PersistenceBackendFactoryRegistry;
import fr.inria.atlanmod.neoemf.data.berkeleydb.BerkeleyDbContext;
import fr.inria.atlanmod.neoemf.data.hbase.HBaseContext;
import fr.inria.atlanmod.neoemf.data.mapdb.MapDbContext;
import fr.inria.atlanmod.neoemf.io.Importer;
import fr.inria.atlanmod.neoemf.io.persistence.PersistenceHandler;
import fr.inria.atlanmod.neoemf.io.persistence.PersistenceHandlerFactory;
import fr.inria.atlanmod.neoemf.resource.PersistentResourceFactory;
import fr.inria.atlanmod.neoemf.tests.AbstractBackendTest;
import fr.inria.atlanmod.neoemf.util.logging.NeoLogger;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.BasicExtendedMetaData;
import org.eclipse.emf.ecore.util.ExtendedMetaData;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceImpl;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import javax.annotation.Nonnull;

import static java.util.Objects.isNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

public class ImportTest extends AbstractBackendTest {

    /**
     * A {@link java.util.Set} holding all tested {@link Object}s.
     */
    private HashSet<Object> testedObjects;

    /**
     * A {@link java.util.Set} holding tested {@link EStructuralFeature}s.
     */
    private HashSet<EStructuralFeature> testedFeatures;

    /**
     * Retrieves a child element from the {@code root} following the given {@code indexes}.
     *
     * @param root    the element from which to start the search
     * @param indexes the indexes of contained elements. The first index represents the index of the element from the
     *                root element, the second represents the index of the element from the previous,...
     *
     * @return the object
     */
    @Nonnull
    private static EObject getChildFrom(EObject root, int... indexes) {
        if (indexes.length == 0) {
            throw new IllegalArgumentException("You must define at least one index");
        }

        EObject element = root;
        for (int index : indexes) {
            element = element.eContents().get(index);
        }
        assertThat(element).isNotNull();
        return element;
    }

    /**
     * Returns the test XMI file that uses XPath as references.
     *
     * @return the test file
     */
    protected static File getXmiStandard() {
        return getResourceFile("/io/xmi/sampleStandard.xmi");
    }

    /**
     * Returns the test XMI file that uses {@code xmi:id} as references.
     *
     * @return the test file
     */
    protected static File getXmiWithId() {
        return getResourceFile("/io/xmi/sampleWithId.xmi");
    }

    /**
     * Returns a test file according to the given {@code path}.
     *
     * @param path the resource path
     *
     * @return the test file
     */
    protected static File getResourceFile(String path) {
        return new File(ImportTest.class.getResource(path).getFile());
    }

    /**
     * Registers a EPackage in {@link EPackage.Registry} according to its {@code prefix} and {@code uri}, from an Ecore
     * file.
     * <p>
     * The targeted Ecore file must be present in {@code /resources/ecore}.
     *
     * @param prefix the prefix of the URI. It is used to retrieve the {@code ecore} file
     * @param uri    the URI of the {@link EPackage}
     */
    protected static void registerEPackageFromEcore(String prefix, String uri) {
        File file = getResourceFile("/io/ecore/{name}.ecore".replaceAll("\\{name\\}", prefix));

        EPackage ePackage = null;

        Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("ecore", new EcoreResourceFactoryImpl());

        ResourceSet rs = new ResourceSetImpl();

        final ExtendedMetaData extendedMetaData = new BasicExtendedMetaData(rs.getPackageRegistry());
        rs.getLoadOptions().put(XMLResource.OPTION_EXTENDED_META_DATA, extendedMetaData);

        Resource r = rs.getResource(URI.createFileURI(file.toString()), true);
        EObject eObject = r.getContents().get(0);
        if (eObject instanceof EPackage) {
            ePackage = (EPackage) eObject;
            rs.getPackageRegistry().put(ePackage.getNsURI(), ePackage);
        }

        assertThat(ePackage).isNotNull(); // "EPackage does not exist"

        EPackage.Registry.INSTANCE.put(uri, ePackage);
    }

    @Before
    public void init() throws IOException {
        registerEPackageFromEcore("java", "http://www.eclipse.org/MoDisco/Java/0.2.incubation/java");
        registerEPackageFromEcore("uml", "http://schema.omg.org/spec/UML/2.1");

        testedObjects = new HashSet<>();
        testedFeatures = new HashSet<>();
    }

    @Test
    public void testCompare() throws IOException {
        if (ignoreWhen(HBaseContext.NAME, MapDbContext.NAME, BerkeleyDbContext.NAME)) {
            return;
        }

        File file = getXmiStandard();

        EObject emfObject = loadWithEMF(file);
        EObject neoObject = loadWithNeoEMF(file);

        assertEqualEObject(neoObject, emfObject);
    }

    /**
     * Check that the elements are properly processed.
     * <p>
     * All elements must have an id and a class name.
     */
    @Test
    public void testElementsAndChildren() throws IOException {
        if (ignoreWhen(HBaseContext.NAME, MapDbContext.NAME, BerkeleyDbContext.NAME)) {
            return;
        }

        EObject eObject;
        EObject eObjectChild;

        EObject root = loadWithNeoEMF(getXmiStandard());
        assertValidElement(root, "Model", 19, "fr.inria.atlanmod.kyanos.tests");
        {
            //@Model/@ownedElements.0/@ownedPackages[4]/@ownedElements.0
            eObject = getChildFrom(root, 0, 0, 0, 0, 0, 0);
            assertValidElement(eObject, "ClassDeclaration", 7, "TestCreateResource");
            {
                //@Model/@ownedElements.0/@ownedPackages[4]/@ownedElements.0/@modifier
                eObjectChild = getChildFrom(eObject, 0);
                assertValidElement(eObjectChild, "Modifier", 0, null);

                //@Model/@ownedElements.0/@ownedPackages[4]/@ownedElements.0/@bodyDeclarations.2
                eObjectChild = getChildFrom(eObject, 3);
                assertValidElement(eObjectChild, "MethodDeclaration", 5, "tearDownAfterClass");
            }

            //@Model/@ownedElements.1
            eObject = getChildFrom(root, 1);
            assertValidElement(eObject, "Package", 5, "java");

            //@Model/@orphanTypes.5
            eObject = getChildFrom(root, 8);
            assertValidElement(eObject, "PrimitiveTypeVoid", 0, "void");

            //@Model/@compilationUnits.1
            eObject = getChildFrom(root, 17);
            assertValidElement(eObject, "CompilationUnit", 16, "TestXmi.java");
            {
                //@Model/@compilationUnits.1/@imports.2
                eObjectChild = getChildFrom(eObject, 2);
                assertValidElement(eObjectChild, "ImportDeclaration", 0, null);
            }
        }
    }

    /**
     * Check that the attributes are properly processed.
     */
    @Test
    public void testAttributes() throws IOException {
        if (ignoreWhen(HBaseContext.NAME, MapDbContext.NAME, BerkeleyDbContext.NAME)) {
            return;
        }

        EObject eObject;

        EObject root = loadWithNeoEMF(getXmiStandard());
        {
            //@Model/@ownedElements.0/@ownedPackages[4]/@ownedElements.0/@modifier
            eObject = getChildFrom(root, 0, 0, 0, 0, 0, 0, 0);
            assertValidAttribute(eObject, "visibility", "public");

            //@Model/@ownedElements.1
            eObject = getChildFrom(root, 1);
            assertValidAttribute(eObject, "proxy", "true");

            //@Model/@compilationUnits.1
            eObject = getChildFrom(root, 17);
            assertValidAttribute(eObject, "originalFilePath", "C:\\Eclipse\\eclipse-SDK-4.3.1-win32-x86_64-Blue\\eclipse\\workspace\\fr.inria.atlanmod.kyanos.tests\\src\\fr\\inria\\atlanmod\\kyanos\\tests\\TestXmi.java");
        }
    }

    /**
     * Check that the XPath references/id are properly processed.
     * <p>
     * Containment and inverse reference must have been created.
     * References previously detected as attributes, are now well placed.
     */
    @Test
    public void testReferences() throws IOException {
        if (ignoreWhen(HBaseContext.NAME, MapDbContext.NAME, BerkeleyDbContext.NAME)) {
            return;
        }

        EObject eObject;
        EObject eObjectChild;

        EObject root = loadWithNeoEMF(getXmiStandard());
        assertValidReference(root, "ownedElements", 0, "Package", "fr", true, true);
        assertValidReference(root, "orphanTypes", 5, "PrimitiveTypeVoid", "void", true, true);
        {
            //@Model/@ownedElements.0/@ownedPackages[4]/@ownedElements.0
            eObject = getChildFrom(root, 0, 0, 0, 0, 0, 0);
            assertValidReference(eObject, "originalCompilationUnit", 0, "CompilationUnit", "TestCreateResource.java", false, false);
            assertValidReference(eObject, "bodyDeclarations", 0, "FieldDeclaration", null, true, true);
            {
                //@Model/@ownedElements.0/@ownedPackages[4]/@ownedElements.0/@bodyDeclarations.2
                eObjectChild = getChildFrom(eObject, 3);
                assertValidReference(eObjectChild, "originalCompilationUnit", 0, "CompilationUnit", "TestCreateResource.java", false, false);
                assertValidReference(eObjectChild, "modifier", 0, "Modifier", null, false, true);
            }

            //@Model/@ownedElements.1
            eObject = getChildFrom(root, 1);
            assertValidReference(eObject, "ownedPackages", 0, "Package", "io", true, true);

            //@Model/@orphanTypes.5
            eObject = getChildFrom(root, 8);
            assertValidReference(eObject, "usagesInTypeAccess", 0, "TypeAccess", null, true, false);
            assertValidReference(eObject, "usagesInTypeAccess", 9, "TypeAccess", null, true, false);

            //@Model/@compilationUnits.1
            eObject = getChildFrom(root, 17);
            assertValidReference(eObject, "package", 0, "Package", "tests", false, false);
            assertValidReference(eObject, "imports", 0, "ImportDeclaration", null, true, true);
            {
                //@Model/@compilationUnits.1/@imports.2
                eObjectChild = getChildFrom(eObject, 2);
                assertValidReference(eObjectChild, "originalCompilationUnit", 0, "CompilationUnit", "TestXmi.java", false, false);
                assertValidReference(eObjectChild, "importedElement", 2, "ClassDeclaration", "URI", false, false);
            }
        }
    }

    @Test
    @Ignore // FIXME Inverse references don't exist in EMF... It's a problem, or not ?
    public void testImportWithId() throws IOException {
        if (ignoreWhen(HBaseContext.NAME, MapDbContext.NAME, BerkeleyDbContext.NAME)) {
            return;
        }

        File file = getXmiWithId();

        EObject emfObject = loadWithEMF(file);
        EObject neoObject = loadWithNeoEMF(file);

        assertEqualEObject(neoObject, emfObject);
    }

    private void assertEqualEObject(EObject actual, EObject expected) {
        NeoLogger.debug("Actual object     : {0}", actual);
        NeoLogger.debug("Expected object   : {0}", expected);

        if (!testedObjects.contains(expected)) {
            testedObjects.add(expected);

            assertThat(actual.eClass().getName()).isEqualTo(expected.eClass().getName());
            assertThat(actual.eContents()).hasSameSizeAs(expected.eContents());

            for (EAttribute attribute : expected.eClass().getEAttributes()) {
                assertEqualFeature(actual, expected, attribute.getFeatureID());
            }

            for (EReference reference : expected.eClass().getEReferences()) {
                assertEqualFeature(actual, expected, reference.getFeatureID());
            }

            for (int i = 0; i < expected.eContents().size(); i++) {
                assertEqualEObject(actual.eContents().get(i), expected.eContents().get(i));
            }
        }
    }

    @SuppressWarnings("unchecked") // Unchecked method 'hasSameSizeAs(Iterable<?>)' invocation
    private void assertEqualFeature(EObject actual, EObject expected, int featureId) {
        EStructuralFeature feature = expected.eClass().getEStructuralFeature(featureId);

        if (!testedFeatures.contains(feature)) {
            testedFeatures.add(feature);

            Object expectedValue = expected.eGet(feature);
            Object actualValue = actual.eGet(actual.eClass().getEStructuralFeature(featureId));

            NeoLogger.debug("Actual feature    : {0}", actualValue);
            NeoLogger.debug("Expected feature  : {0}", expectedValue);

            if (expectedValue instanceof EObject) {
                assertEqualEObject((EObject) actualValue, (EObject) expectedValue);
            }
            else if (expectedValue instanceof List<?>) {
                List<?> expectedList = (List<?>) expectedValue;
                List<?> actualList = (List<?>) actualValue;

                assertThat(actualList).hasSameSizeAs(expectedList);

                for (int i = 0; i < expectedList.size(); i++) {
                    assertEqualEObject((EObject) actualList.get(i), (EObject) expectedList.get(i));
                }
            }
            else {
                assertThat(actualValue).isEqualTo(expectedValue);
            }
        }
    }

    private void assertValidElement(EObject eObject, String className, int size, Object name) {
        assertThat(eObject.eClass().getName()).isEqualTo(className);
        assertThat(eObject.eContents()).hasSize(size);
        if (isNull(name)) {
            Throwable thrown = catchThrowable(() -> eObject.eGet(eObject.eClass().getEStructuralFeature("name")));
            assertThat(thrown).isInstanceOf(NullPointerException.class);
        }
        else {
            assertThat(eObject.eGet(eObject.eClass().getEStructuralFeature("name"))).isEqualTo(name);
        }
    }

    @SuppressWarnings("unchecked") // Unchecked cast: 'Object' to 'EList<...>'
    private void assertValidReference(EObject eObject, String name, int index, String referenceClassName, String referenceName, boolean many, boolean containment) {
        EReference reference = (EReference) eObject.eClass().getEStructuralFeature(name);

        Object objectReference = eObject.eGet(reference);
        EObject eObjectReference;

        if (many) {
            EList<EObject> eObjectList = (EList<EObject>) objectReference;
            eObjectReference = eObjectList.get(index);
        }
        else {
            eObjectReference = (EObject) objectReference;
        }

        assertThat(eObjectReference.eClass().getName()).isEqualTo(referenceClassName);

        if (isNull(referenceName)) {
            try {
                EAttribute attribute = (EAttribute) eObjectReference.eClass().getEStructuralFeature("name");
                assertThat(eObjectReference.eGet(attribute)).isEqualTo(attribute.getDefaultValue());
            }
            catch (NullPointerException ignore) {
                // It's good
            }
        }
        else {
            EAttribute attribute = (EAttribute) eObjectReference.eClass().getEStructuralFeature("name");
            assertThat(eObjectReference.eGet(attribute).toString()).isEqualTo(referenceName);
        }

        assertThat(reference.isContainment()).isEqualTo(containment);
        assertThat(reference.isMany()).isEqualTo(many);
    }

    private void assertValidAttribute(EObject eObject, String name, Object value) {
        EAttribute attribute = (EAttribute) eObject.eClass().getEStructuralFeature(name);

        if (isNull(value)) {
            assertThat(eObject.eGet(attribute)).isEqualTo(attribute.getDefaultValue());
        }
        else {
            assertThat(eObject.eGet(attribute).toString()).isEqualTo(value);
        }
    }

    private EObject loadWithEMF(File file) throws IOException {
        Resource resource = new XMIResourceImpl();
        resource.load(new FileInputStream(file), Collections.emptyMap());
        return resource.getContents().get(0);
    }

    private EObject loadWithNeoEMF(File file) throws IOException {
        try (PersistenceBackend backend = createPersistenceBackend()) {
            PersistenceHandler handler = PersistenceHandlerFactory.newNaiveHandler(backend);
            Importer.fromXmi(new FileInputStream(file), handler);
        }

        ResourceSet resourceSet = new ResourceSetImpl();
        resourceSet.getResourceFactoryRegistry()
                .getProtocolToFactoryMap().put(context().uriScheme(), PersistentResourceFactory.getInstance());

        Resource resource = resourceSet.createResource(context().createFileURI(file()));
        resource.load(context().defaultOptions());

        return resource.getContents().get(0);
    }

    private PersistenceBackend createPersistenceBackend() {
        PersistenceBackendFactoryRegistry.register(context().uriScheme(), context().persistenceBackendFactory());
        return context().persistenceBackendFactory().createPersistentBackend(context().createFileURI(file()), context().defaultOptions());
    }
}
