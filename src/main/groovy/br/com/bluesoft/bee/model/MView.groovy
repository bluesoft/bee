package br.com.bluesoft.bee.model

import br.com.bluesoft.bee.model.message.Message
import br.com.bluesoft.bee.model.message.MessageLevel
import br.com.bluesoft.bee.model.message.MessageType
import br.com.bluesoft.bee.util.RDBMS
import br.com.bluesoft.bee.util.StringUtil
import groovy.transform.AutoClone

@AutoClone
class MView implements Validator {
    def name
    def text
    def text_oracle
    def text_postgres
    def text_mysql
    def text_redshift
    Map<String, MViewIndex> indexes = [:]

    @Override
    List validateWithMetadata(metadataView) {
        if (!(metadataView instanceof MView)) {
            return []
        }

        def messages = []
        if (!StringUtil.compare(metadataView.text, this.text)) {
            def message = new Message(objectName: name, level: MessageLevel.ERROR, objectType: ObjectType.VIEW, messageType: MessageType.VIEW_BODY, message: "The body of the view ${this.name} differs from metadata.")
            messages << message
        }

        if (metadataView) {
            messages.addAll validatePresenceOfIndexes(metadataView)
            messages.addAll validateIndexes(metadataView)
        }
        return messages
    }

    private def validateElements(elements, metadataTable) {
        def messages = []
        def metadataElementsMap = metadataTable[elements]
        def databaseElementsMap = this[elements]

        metadataElementsMap.each { elementName, element ->
            if (databaseElementsMap[elementName]) {
                messages.addAll(databaseElementsMap[elementName].validateWithMetadata(metadataTable, element))
            }
        }
        return messages
    }

    private def validatePresenceOfIndexes(MView metadataTable) {
        def messages = []
        def databaseMissingIndexes = metadataTable.indexes.keySet() - this.indexes.keySet()
        def aditionalDatabaseIndexes = this.indexes.keySet() - metadataTable.indexes.keySet()
        databaseMissingIndexes.each {
            def messageText = "The materialized view ${name} is missing the index ${it}.";
            def message = new Message(objectName: "${it}", level: MessageLevel.ERROR, objectType: ObjectType.INDEX, messageType: MessageType.PRESENCE, message: messageText)
            messages << message
        }
        aditionalDatabaseIndexes.each {
            def messageText = "The materialized view ${name} has the additional index ${it}.";
            def message = new Message(objectName: "${it}", level: MessageLevel.ERROR, objectType: ObjectType.INDEX, messageType: MessageType.PRESENCE, message: messageText)
            messages << message
        }
        return messages
    }

    private def validateIndexes(MView metadataTable) {
        return validateElements('indexes', metadataTable)
    }

    MView getCanonical(RDBMS rdbms) {
        if(rdbms == null) {
            return this
        }

        MView result = this.clone()

        String text
        switch(rdbms) {
            case RDBMS.ORACLE:
                result.text = text_oracle ?: this.text
                break
            case RDBMS.POSTGRES:
                result.text = text_postgres ?: this.text
                break
            case RDBMS.MYSQL:
                result.text = text_mysql ?: this.text
                break
            case RDBMS.REDSHIFT:
                result.text = text_redshift ?: this.text
                break
        }

        result
    }

}
