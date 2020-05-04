package br.com.bluesoft.bee.model

import spock.lang.Specification

import static org.junit.Assert.assertEquals

class SchemaTest extends Specification {

    def "get all objects should return all the views, tables and sequences"() {
        given:
        Schema schema = new Schema()
        schema.tables = [horses: new Table(name: 'horses')]
        schema.views = [sheeps: new View(name: 'sheeps')]
        schema.sequences = [bee: new Sequence(name: 'bee')]

        when:
        def allObjects = schema.getAllObjects()

        then:
        assertEquals 3, allObjects.size()
        assertEquals 1, allObjects.values().findAll { it.class == Table.class }.size()
        assertEquals 1, allObjects.values().findAll { it.class == View.class }.size()
        assertEquals 1, allObjects.values().findAll { it.class == Sequence.class }.size()
    }

    def "validate with metadata validate the metadata of each table"() {

        given: "a database schema"

        Schema databaseSchema = new Schema()
        Table tableMock1 = Mock(Table)
        Table tableMock2 = Mock(Table)
        databaseSchema.tables = [horses: tableMock1, cats: tableMock2]

        and: "a database schema"
        Schema metadataSchema = new Schema()
        metadataSchema.tables = [horses: new Table(name: 'horses'), cats: new Table(name: 'cats')]
        metadataSchema.views = [sheeps: new View(name: 'sheeps')]
        metadataSchema.sequences = [bee: new Sequence(name: 'bee')]

        when:
        def messages = databaseSchema.validateWithMetadata(metadataSchema)

        then:
        1 * tableMock1.validateWithMetadata(_) >> []
        1 * tableMock2.validateWithMetadata(_) >> []
    }
}
