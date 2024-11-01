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

import java.text.MessageFormat

@AutoClone
@JsonAutoDetect(isGetterVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.ANY)
class TableColumn {

    String name
    String type
    Integer size
    String sizeType
    Integer scale = 0
    Boolean nullable = false
    String defaultValue
    String autoIncrement
    String onUpdateCurrentTimestamp
    Boolean virtual = false
    Boolean ignore = false
    Integer sortKeyOrder = 0

    static String messageTemplate = 'The {0} of the column {1} of the table {2} should be {3} but it is {4}'

    def validateWithMetadata(Table table, TableColumn metadataColumn) {
        def messages = []

        if (metadataColumn.type != this.type) {
            messages << createMessage(table.name, metadataColumn, MessageType.DATA_TYPE, 'type')
        }

        if (metadataColumn.size != this.size) {
            messages << createMessage(table.name, metadataColumn, MessageType.DATA_SIZE, 'size')
        }

        if (metadataColumn.scale != this.scale) {
            messages << createMessage(table.name, metadataColumn, MessageType.DATA_SCALE, 'scale')
        }

        if (metadataColumn.nullable != this.nullable) {
            messages << createMessage(table.name, metadataColumn, MessageType.NULLABILITY, 'nullable')
        }

        if (metadataColumn.defaultValue != this.defaultValue) {
            messages << createMessage(table.name, metadataColumn, MessageType.DATA_DEFAULT, 'defaultValue')
        }

        if (metadataColumn.autoIncrement != this.autoIncrement) {
            messages << createMessage(table.name, metadataColumn, MessageType.AUTO_INCREMENT, 'autoIncrement')
        }

        if (metadataColumn.onUpdateCurrentTimestamp != this.onUpdateCurrentTimestamp) {
            messages << createMessage(table.name, metadataColumn, MessageType.ON_UPDATE_CURRENT_TIMESTAMP, 'onUpdateCurrentTimestamp')
        }

        return messages
    }

    private def createMessage(tableName, metadataColumn, messageType, info) {
        def messageText = MessageFormat.format(messageTemplate, info, name, tableName, metadataColumn[info], this[info])
        new Message(objectName: "${name}", level: MessageLevel.ERROR, objectType: ObjectType.TABLE_COLUMN, messageType: messageType, message: messageText)
    }

    def compareType(def col) {
		if (!col instanceof TableColumn) {
			return false
		};

		if (col.type != type || col.size != size || col.scale != scale) {
			return false
		}
        return true
    }
}
