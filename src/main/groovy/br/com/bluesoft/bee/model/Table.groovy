/*
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is mozilla.org code.
 *
 * The Initial Developer of the Original Code is
 * Bluesoft Consultoria em Informatica Ltda.
 * Portions created by the Initial Developer are Copyright (C) 2011
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 */
package br.com.bluesoft.bee.model

import br.com.bluesoft.bee.model.message.Message
import br.com.bluesoft.bee.model.message.MessageLevel
import br.com.bluesoft.bee.model.message.MessageType
import com.fasterxml.jackson.annotation.JsonAutoDetect
import groovy.transform.AutoClone

@AutoClone
@com.fasterxml.jackson.annotation.JsonAutoDetect(isGetterVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.ANY)
class Table implements Validator {

    String name
    Boolean temporary = false
    String comment
    String distStyle

    Map<String, TableColumn> columns = [:] as LinkedHashMap
    Map<String, Index> indexes = [:]
    Map<String, Constraint> constraints = [:]

    Boolean shouldImportTheData() {
        this.comment ? this.comment?.toUpperCase()?.contains('#CORE') : false
    }

    List validateWithMetadata(metadataTable) {
		if (!metadataTable instanceof Table) {
			return []
		}

        def messages = []
        if (metadataTable) {
            messages.addAll validatePresenceOfColumns(metadataTable)
            messages.addAll validatePresenceOfIndexes(metadataTable)
            messages.addAll validatePresenceOfConstraints(metadataTable)
            messages.addAll validateTemporary(metadataTable)
            messages.addAll validateColumns(metadataTable)
            messages.addAll validateIndexes(metadataTable)
            messages.addAll validateConstraints(metadataTable)
        }
        return messages
    }

    private def validatePresenceOfColumns(Table metadataTable) {
        def messages = []
        def databaseMissingColumns = metadataTable.columns.keySet() - this.columns.keySet()
        def aditionalDatabaseColumns = this.columns.keySet() - metadataTable.columns.keySet()
        databaseMissingColumns.each {
            def messageText = "The table ${name} is missing the column ${it}.";
            def message = new Message(objectName: "${name}.${it}", level: MessageLevel.ERROR, objectType: ObjectType.TABLE_COLUMN, messageType: MessageType.PRESENCE, message: messageText)
            messages << message
        }
        aditionalDatabaseColumns.each {
            def messageText = "The table ${name} has the additional column ${it}.";
            def message = new Message(objectName: "${name}.${it}", level: MessageLevel.ERROR, objectType: ObjectType.TABLE_COLUMN, messageType: MessageType.PRESENCE, message: messageText)
            messages << message
        }
        return messages
    }

    private def validatePresenceOfIndexes(Table metadataTable) {
        def messages = []
        def databaseMissingIndexes = metadataTable.indexes.keySet() - this.indexes.keySet()
        def aditionalDatabaseIndexes = this.indexes.keySet() - metadataTable.indexes.keySet()
        databaseMissingIndexes.each {
            def messageText = "The table ${name} is missing the index ${it}.";
            def message = new Message(objectName: "${it}", level: MessageLevel.ERROR, objectType: ObjectType.INDEX, messageType: MessageType.PRESENCE, message: messageText)
            messages << message
        }
        aditionalDatabaseIndexes.each {
            def messageText = "The table ${name} has the additional index ${it}.";
            def message = new Message(objectName: "${it}", level: MessageLevel.ERROR, objectType: ObjectType.INDEX, messageType: MessageType.PRESENCE, message: messageText)
            messages << message
        }
        return messages
    }

    private def validatePresenceOfConstraints(Table metadataTable) {
        def messages = []
        def databaseMissingConstraints = metadataTable.constraints.keySet() - this.constraints.keySet()
        def aditionalDatabaseConstraints = this.constraints.keySet() - metadataTable.constraints.keySet()
        databaseMissingConstraints.each {
            def messageText = "The table ${name} is missing the constraint ${it}.";
            def message = new Message(objectName: "${it}", level: MessageLevel.ERROR, objectType: ObjectType.CONSTRAINT, messageType: MessageType.PRESENCE, message: messageText)
            messages << message
        }
        aditionalDatabaseConstraints.each {
            def messageText = "The table ${name} has the additional constraint ${it}.";
            def message = new Message(objectName: "${it}", level: MessageLevel.ERROR, objectType: ObjectType.CONSTRAINT, messageType: MessageType.PRESENCE, message: messageText)
            messages << message
        }
        return messages
    }

    private def validateTemporary(Table metadataTable) {
        def messages = []
        if (metadataTable.temporary != this.temporary) {
            def message = new Message(objectName: name, level: MessageLevel.ERROR, objectType: ObjectType.TABLE, messageType: MessageType.TEMPORARY)
            if (metadataTable.temporary) {
                message.message = "The table ${this.name} should be temporary.";
            } else {
                message.message = "The table ${this.name} should not be temporary.";
            }
            messages << message
        }
        return messages
    }

    private def validateColumns(Table metadataTable) {
        return validateElements('columns', metadataTable)
    }

    private def validateIndexes(Table metadataTable) {
        return validateElements('indexes', metadataTable)
    }

    private def validateConstraints(Table metadataTable) {
        return validateElements('constraints', metadataTable)
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

}
