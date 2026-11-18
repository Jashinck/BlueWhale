package org.skylark.bluewhale.infrastructure;

import org.skylark.bluewhale.domain.ontology.OntologySchema;
import org.skylark.bluewhale.domain.ontology.PropertyDef;
import org.skylark.bluewhale.domain.port.OntologyPort;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class YamlOntologyAdapter implements OntologyPort {

    private final OntologySchema schema;

    public YamlOntologyAdapter(String classpathLocation) {
        this.schema = parseResource(classpathLocation);
    }

    @Override
    public OntologySchema load() {
        return schema;
    }

    private static OntologySchema parseResource(String classpathLocation) {
        try (InputStream in = YamlOntologyAdapter.class.getClassLoader().getResourceAsStream(classpathLocation)) {
            if (in == null) {
                throw new IllegalStateException("ontology resource not found: " + classpathLocation);
            }
            return parse(in);
        } catch (IOException e) {
            throw new IllegalStateException("cannot read ontology resource: " + classpathLocation, e);
        }
    }

    @SuppressWarnings("unchecked")
    static OntologySchema parse(InputStream in) {
        Map<String, Object> root = new Yaml().load(in);
        if (root == null) {
            throw new IllegalArgumentException("ontology yaml is empty");
        }
        String version = String.valueOf(root.get("ontologyVersion"));
        List<PropertyDef> properties = new ArrayList<>();
        Object rawProperties = root.get("properties");
        if (rawProperties instanceof List<?> list) {
            for (Object item : list) {
                Map<String, Object> map = (Map<String, Object>) item;
                properties.add(new PropertyDef(
                        String.valueOf(map.get("id")),
                        String.valueOf(map.get("type")),
                        stringList(map.get("values")),
                        Boolean.TRUE.equals(map.get("bindsRequired"))));
            }
        }
        return new OntologySchema(version, properties,
                stringList(root.get("entityTypes")), stringList(root.get("relationTypes")));
    }

    private static List<String> stringList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return list.stream().map(String::valueOf).toList();
    }
}