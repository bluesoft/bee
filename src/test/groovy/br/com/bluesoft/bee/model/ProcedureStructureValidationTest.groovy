package br.com.bluesoft.bee.model

import br.com.bluesoft.bee.model.message.Message
import br.com.bluesoft.bee.model.message.MessageLevel
import br.com.bluesoft.bee.model.message.MessageType
import spock.lang.Specification

public class ProcedureStructureValidationTest extends Specification {

    def "it should return an error for each difference between the database and the reference metadata"() {
        given:
        def metadataView = new Procedure(name: 'proc1', text: 'xyz')

        expect:
        Message message = databaseProcedure.validateWithMetadata(metadataView)[0]
        message.message == messageText
        message.level == MessageLevel.ERROR
        message.objectType == ObjectType.PROCEDURE
        message.messageType == messageType

        where:
        databaseProcedure                        | messageType                | messageText
        new Procedure(name: 'vw1', text: 'xyzw') | MessageType.PROCEDURE_BODY | "The body of the procedure/function vw1 differs from metadata."
    }
}
