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
package br.com.bluesoft.bee.model.postgres

import br.com.bluesoft.bee.model.ObjectType
import br.com.bluesoft.bee.model.Table
import br.com.bluesoft.bee.model.message.Message
import br.com.bluesoft.bee.model.message.MessageLevel
import br.com.bluesoft.bee.model.message.MessageType

class PostgresTable extends Table {

    Boolean rowSecurity
    Map<String, PostgresPolicy> policies = [:]

    List validateWithMetadata(metadataTable) {
        def messages = super.validateWithMetadata(metadataTable)

        if (metadataTable instanceof PostgresTable) {
            if (metadataTable.rowSecurity != this.rowSecurity) {
                def messageText = "The row security of the table ${metadataTable.name} should be ${metadataTable.rowSecurity} but it is ${this.rowSecurity}"
                messages << new Message(objectName: this.name, level: MessageLevel.ERROR, objectType: ObjectType.TABLE, messageType: MessageType.PRESENCE, message: messageText)
            }

            messages.addAll validatePresenceOfPolicies(metadataTable)
            messages.addAll validateElements('policies', metadataTable)
        }

        return messages
    }

    private def validatePresenceOfPolicies(PostgresTable metadataTable) {
        def messages = []

        def missingPolicies = metadataTable.policies.keySet() - this.policies.keySet()
        def additionalPolicies = this.policies.keySet() - metadataTable.policies.keySet()

        missingPolicies.each {
            def messageText = "The table ${name} is missing the policy ${it}.";
            def message = new Message(objectName: "${name}.${it}", level: MessageLevel.ERROR, objectType: ObjectType.TABLE_COLUMN, messageType: MessageType.PRESENCE, message: messageText)
            messages << message
        }

        additionalPolicies.each {
            def messageText = "The table ${name} has the additional policy ${it}.";
            def message = new Message(objectName: "${name}.${it}", level: MessageLevel.ERROR, objectType: ObjectType.TABLE_COLUMN, messageType: MessageType.PRESENCE, message: messageText)
            messages << message
        }

        return messages
    }
}
