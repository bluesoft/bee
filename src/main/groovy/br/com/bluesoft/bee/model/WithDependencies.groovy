package br.com.bluesoft.bee.model

interface WithDependencies {
    String getName()

    List<String> getDependencies()
}