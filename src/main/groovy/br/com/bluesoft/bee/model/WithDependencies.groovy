package br.com.bluesoft.bee.model

import br.com.bluesoft.bee.util.RDBMS

interface WithDependencies {
    String getName()

    List<String> getDependencies(RDBMS rdbms)
}