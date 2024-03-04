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
@JsonAutoDetect(isGetterVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.ANY)
class Index {

    String name
    String type = "N" //N,B,F
    Boolean unique = false
    List<IndexColumn> columns = []
    String where

    def validateWithMetadata(Table table, Index metadataIndex) {
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
