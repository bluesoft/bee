package br.com.bluesoft.bee.model

import spock.lang.Specification

class MetadataTypeMismatchTest extends Specification {

    def "it should return no messages when the metadata object has a different type"() {
        expect:
        databaseObject.validateWithMetadata(metadataObject) == []

        where:
        databaseObject                                 | metadataObject
        new Package(name: 'obj', text: 'a', body: 'b') | new Procedure(name: 'obj', text: 'x')
        new Procedure(name: 'obj', text: 'a')          | new View(name: 'obj', text: 'x')
        new Trigger(name: 'obj', text: 'a')            | new Package(name: 'obj', text: 'x', body: 'y')
        new Table(name: 'obj')                         | new View(name: 'obj', text: 'x')
    }

    def "compareType should return false when the referenced column is missing"() {
        expect:
        !new TableColumn(name: 'id', type: 'number').compareType(null)
    }
}
