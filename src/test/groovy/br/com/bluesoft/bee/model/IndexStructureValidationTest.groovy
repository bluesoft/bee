package br.com.bluesoft.bee.model

import br.com.bluesoft.bee.model.message.Message
import br.com.bluesoft.bee.model.message.MessageLevel
import br.com.bluesoft.bee.model.message.MessageType

class IndexStructureValidationTest extends spock.lang.Specification {

    def "it should return an error for each difference between the database and the reference metadata"() {

        given:
        def table = new Table(name: 'people')

        expect:
        Message message = databaseIndex.validateWithMetadata(table, metadataIndex)[0]
        message.message == messageText
        message.level == MessageLevel.ERROR
        message.objectType == ObjectType.INDEX
        message.messageType == messageType
        message.objectName == databaseIndex.name

        where:
        databaseIndex                                                                   | metadataIndex                                                | messageType                  | messageText
        new Index(name: 'idx_person', unique: true)                                     | new Index(unique: false)                                     | MessageType.INDEX_UNIQUENESS | 'The uniqueness of the index idx_person of the table people should be false but it is true'
        new Index(name: 'uk_person', columns: [indexColumn('id'), indexColumn('name')]) | new Index(columns: [indexColumn('name'), indexColumn('id')]) | MessageType.INDEX_COLUMNS    | 'The columns of the index uk_person of the table people should be [name asc, id asc] but it is [id asc, name asc]'
        new Index(name: 'idx_age', columns: [indexColumn('name')])                      | new Index(columns: [indexColumn('age')])                     | MessageType.INDEX_COLUMNS    | 'The columns of the index idx_age of the table people should be [age asc] but it is [name asc]'
    }

    def "it should return no message if there is no differente between the database and reference metadata"() {

        given:
        def table = new Table(name: 'people')

        expect:
        databaseIndex.validateWithMetadata(table, metadataIndex)[0] == null

        where:
        databaseIndex                      | metadataIndex
        new Index(type: 'N')               | new Index(type: 'N')
        new Index(unique: true)            | new Index(unique: true)
        new Index(columns: ['id', 'name']) | new Index(columns: ['id', 'name'])
    }

    def indexColumn(name) {
        return new IndexColumn(name: name)
    }
}
