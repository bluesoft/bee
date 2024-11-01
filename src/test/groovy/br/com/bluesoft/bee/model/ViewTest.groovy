package br.com.bluesoft.bee.model

import br.com.bluesoft.bee.model.message.Message
import br.com.bluesoft.bee.model.message.MessageLevel
import br.com.bluesoft.bee.model.message.MessageType
import br.com.bluesoft.bee.util.RDBMS

public class ViewTest extends spock.lang.Specification {

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

    def getCanonicalTest() {
        given:
        def view_default = new View(name: "test", text: "default", text_oracle: "oracle", text_postgres: "postgres", text_redshift: "redshift")
        def view_nondefault = new View(name: "test", text_oracle: "oracle", text_postgres: "postgres", text_redshift: "redshift")

        expect:
        view_default.getCanonical(RDBMS.ORACLE).text == "oracle"
        view_default.getCanonical(RDBMS.POSTGRES).text == "postgres"
        view_default.getCanonical(RDBMS.MYSQL).text == "default"
        view_default.getCanonical(RDBMS.REDSHIFT).text == "redshift"

        view_nondefault.getCanonical(RDBMS.ORACLE).text == "oracle"
        view_nondefault.getCanonical(RDBMS.POSTGRES).text == "postgres"
        view_nondefault.getCanonical(RDBMS.MYSQL).text == null
        view_nondefault.getCanonical(RDBMS.REDSHIFT).text == "redshift"
    }
}
