package org.skylark.bluewhale.domain.port;

import org.skylark.bluewhale.domain.knowledge.WikiPage;

import java.util.List;

public interface WikiStorePort {

    WikiPage read(String path);

    void write(WikiPage page);

    List<String> listPages();

    boolean exists(String path);
}