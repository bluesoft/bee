package br.com.bluesoft.bee.model

import br.com.bluesoft.bee.model.message.Message
import br.com.bluesoft.bee.model.message.MessageLevel
import br.com.bluesoft.bee.model.message.MessageType
import groovy.transform.AutoClone

@AutoClone
class MViewIndex {
    String name
    String type = "N" //N,B,F
    Boolean unique = false
    List<MViewIndexColumn> columns = []
    String where

    def validateWithMetadata(MView table, MViewIndex metadataIndex) {
        def messages = []

        if (metadataIndex.unique != this.unique) {
            def messageText = "The uniqueness of the index ${this.name} of the table ${table.name} should be ${metadataIndex.unique} but it is ${this.unique}"
            messages << new Message(objectName: this.name, level: MessageLevel.ERROR, objectType: ObjectType.INDEX, messageType: MessageType.INDEX_UNIQUENESS, message: messageText)
        }

        if (metadataIndex.columns != this.columns) {
            def messageText = "The columns of the index ${this.name} of the table ${table.name} should be ${metadataIndex.columns} but it is ${this.columns}"
            messages << new Message(objectName: this.name, level: MessageLevel.ERROR, objectType: ObjectType.INDEX, messageType: MessageType.INDEX_COLUMNS, message: messageText)
        }

        if (metadataIndex.where != this.where) {
            def messageText = "The filter of the index ${this.name} of the table ${table.name} should be ${metadataIndex.where} but it is ${this.where}"
            messages << new Message(objectName: this.name, level: MessageLevel.ERROR, objectType: ObjectType.INDEX, messageType: MessageType.INDEX_FILTER, message: messageText)
        }

        return messages
    }

}
