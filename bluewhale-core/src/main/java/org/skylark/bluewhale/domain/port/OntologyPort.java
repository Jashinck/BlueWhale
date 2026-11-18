package org.skylark.bluewhale.domain.port;

import org.skylark.bluewhale.domain.ontology.OntologySchema;

public interface OntologyPort {

    OntologySchema load();
}