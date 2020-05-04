package br.com.bluesoft.bee.model

import br.com.bluesoft.bee.model.message.Message
import br.com.bluesoft.bee.model.message.MessageLevel
import br.com.bluesoft.bee.model.message.MessageType
import spock.lang.Specification

public class PackageStructureValidationTest extends Specification {

    def "it should return an error for each difference between the database and the reference metadata"() {
        given: 'a metadata package'
        def metadataPackage = new Package(name: 'proc1', text: 'xyz', body: 'abc')

        and: "a database package"
        def databasePackage = new Package(name: 'proc1', text: 'aaa', body: 'bbb')

        when: "a package is validated"
        def messages = databasePackage.validateWithMetadata(metadataPackage)

        then: "is shoud return error a error message because the package header are different"
        def Message message1 = messages.find { it.messageType == MessageType.PACKAGE_HEADER }
        message1.objectType == ObjectType.PACKAGE
        message1.level == MessageLevel.ERROR
        message1.message == 'The header of the package proc1 differs from metadata.'

        and: "is shoud return error a error message because the package body are different"
        def Message message2 = messages.find { it.messageType == MessageType.PACKAGE_BODY }
        message2.objectType == ObjectType.PACKAGE
        message2.level == MessageLevel.ERROR
        message2.message == 'The body of the package proc1 differs from metadata.'
    }
}
