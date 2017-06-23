package fr.inria.atlanmod.neoemf.io.structure;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class BasicNamespaceTest {

    @Before
    public void setUp() throws Exception {
        BasicNamespace.Registry.getInstance().clean();
    }

    @Test
    public void testGetDefault() {
        BasicNamespace ns0 = BasicNamespace.getDefault();
        assertThat(ns0).isNotNull();
        assertThat(ns0.prefix()).isEqualTo("ecore");

        BasicNamespace ns1 = BasicNamespace.getDefault();
        assertThat(ns0).isSameAs(ns1);
    }

    @Test
    public void testPrefixAndUri() {
        String prefix0 = "prefix0";
        String prefix1 = "prefix1";

        String uri0 = "uri0";
        String uri1 = "uri1";

        BasicNamespace ns0 = BasicNamespace.Registry.getInstance().register(prefix0, uri0);
        assertThat(ns0.prefix()).isEqualTo(prefix0);
        assertThat(ns0.uri()).isEqualTo(uri0);

        BasicNamespace ns1 = BasicNamespace.Registry.getInstance().register(prefix1, uri1);
        assertThat(ns1.prefix()).isNotEqualTo(prefix0).isEqualTo(prefix1);
        assertThat(ns1.uri()).isNotEqualTo(uri0).isEqualTo(uri1);
    }

    @Test
    public void testHashCode() {
        String prefix0 = "prefix0";
        String prefix1 = "prefix1";

        String uri0 = "uri0";
        String uri1 = "uri1";

        BasicNamespace ns0 = BasicNamespace.Registry.getInstance().register(prefix0, uri0);
        BasicNamespace ns0Bis = BasicNamespace.Registry.getInstance().register(prefix0, uri0);
        BasicNamespace ns1 = BasicNamespace.Registry.getInstance().register(prefix1, uri1);

        assertThat(ns0.hashCode()).isEqualTo(ns0Bis.hashCode());
        assertThat(ns0.hashCode()).isNotEqualTo(ns1.hashCode());
        assertThat(ns1.hashCode()).isNotEqualTo(ns0Bis.hashCode());
    }

    @Test
    public void testEquals() {
        String prefix0 = "prefix0";
        String prefix1 = "prefix1";

        String uri0 = "uri0";
        String uri1 = "uri1";

        BasicNamespace ns0 = BasicNamespace.Registry.getInstance().register(prefix0, uri0);
        BasicNamespace ns0Bis = BasicNamespace.Registry.getInstance().register(prefix0, uri0);
        BasicNamespace ns1 = BasicNamespace.Registry.getInstance().register(prefix1, uri1);

        assertThat(ns0).isSameAs(ns0Bis);
        assertThat(ns0).isNotSameAs(ns1).isNotEqualTo(ns1);
        assertThat(ns1).isNotSameAs(ns0Bis).isNotEqualTo(ns0Bis);
    }

    @Test
    public void testToString() {
        String prefix0 = "prefix0";
        String uri0 = "uri0";

        BasicNamespace ns0 = BasicNamespace.Registry.getInstance().register(prefix0, uri0);
        assertThat(ns0).hasToString("prefix0@uri0");
    }

    @Test
    public void testRegistry() throws Exception {
        BasicNamespace.Registry registry = BasicNamespace.Registry.getInstance();

        String prefix0 = "prefix0";
        String uri0 = "uri0";

        String prefix1 = "prefix1";
        String uri1 = "uri1";

        BasicNamespace ns0 = registry.register(prefix0, uri0);
        BasicNamespace ns1 = registry.register(prefix1, uri1);

        BasicNamespace ns0Bis = registry.getFromPrefix(prefix0);
        assertThat(ns0Bis).isEqualTo(ns0);

        BasicNamespace ns1Bis = registry.getFromUri(uri1);
        assertThat(ns1Bis).isEqualTo(ns1);

        Iterable<String> prefixes = registry.getPrefixes();
        assertThat(prefixes).containsExactlyInAnyOrder(prefix0, prefix1);

        registry.clean();
        assertThat(prefixes).isEmpty();

        assertThat(registry.getFromPrefix(prefix0)).isNull();
        assertThat(registry.getFromUri(uri1)).isNull();
    }
}