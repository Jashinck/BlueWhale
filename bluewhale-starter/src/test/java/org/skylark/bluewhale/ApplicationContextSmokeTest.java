package org.skylark.bluewhale;

import org.junit.jupiter.api.Test;
import org.skylark.bluewhale.application.orchestration.KnowledgeWorkflowEngine;
import org.skylark.bluewhale.domain.port.ConnectorPort;
import org.skylark.bluewhale.domain.port.OntologyPort;
import org.skylark.bluewhale.domain.port.WikiStorePort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
        "bluewhale.store.mode=memory",
        "bluewhale.wiki.repo-dir=./target/wiki-smoke"
})
class ApplicationContextSmokeTest {

    @Autowired
    KnowledgeWorkflowEngine workflowEngine;

    @Autowired
    OntologyPort ontologyPort;

    @Autowired
    WikiStorePort wikiStorePort;

    @Autowired
    ConnectorPort connectorPort;

    @Test
    void contextLoadsAndKnowledgeBeansAreWired() {
        assertNotNull(workflowEngine);
        assertNotNull(wikiStorePort);
        assertNotNull(connectorPort);
        assertTrue(ontologyPort.load().hasProperty("deposit_refund_condition"));
    }
}