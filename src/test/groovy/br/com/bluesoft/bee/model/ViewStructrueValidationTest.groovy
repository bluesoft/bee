package br.com.bluesoft.bee.model

import br.com.bluesoft.bee.model.message.Message
import br.com.bluesoft.bee.model.message.MessageLevel
import br.com.bluesoft.bee.model.message.MessageType

public class ViewStructrueValidationTest extends spock.lang.Specification {

    def "it should return an error for each difference between the database and the reference metadata"() {
        given:
        def metadataView = new View(name: 'vw1', text: 'xyz')

        expect:
        Message message = databaseView.validateWithMetadata(metadataView)[0]
        message.message == messageText
        message.level == MessageLevel.ERROR
        message.objectType == ObjectType.VIEW
        message.messageType == messageType

        where:
        databaseView                        | messageType           | messageText
        new View(name: 'vw1', text: 'xyzw') | MessageType.VIEW_BODY | "The body of the view vw1 differs from metadata."
    }
}
