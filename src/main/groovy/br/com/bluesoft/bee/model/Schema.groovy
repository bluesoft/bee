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
import br.com.bluesoft.bee.model.rule.Rule
import br.com.bluesoft.bee.util.RDBMS
import groovy.transform.AutoClone

@AutoClone
class Schema {

    String databaseVersion
    RDBMS rdbms
    Map<String, Table> tables = [:]
    Map<String, View> views = [:]
    Map sequences = [:]
    Map procedures = [:]
    Map packages = [:]
    Map triggers = [:]
    Map mviews = [:]
    Map userTypes = [:]
    Map<RDBMS, Rule> rules = [:]
    boolean filtered

    def validateWithMetadata(Schema metadataSchema) {

        def databaseObjects = this.getAllObjects()
        def metadataObjects = metadataSchema.getAllObjects()

        def messages = []
        messages.addAll getMissingObjectsMessages(databaseObjects, metadataObjects)
        messages.addAll getAdditionalObjectsMessages(databaseObjects, metadataObjects)
        messages.addAll getWrongTypesObjectMessages(databaseObjects, metadataObjects)
        databaseObjects.each { objName, obj ->
            if (obj instanceof Validator) {
                def target = metadataSchema.getAllObjects()[objName]
				if (target) {
					messages.addAll obj.validateWithMetadata(target)
				}
            }
        }

        return messages
    }

    private def getWrongTypesObjectMessages(databaseObjects, metadataObjects) {
        def messages = []
        databaseObjects.each { objName, obj ->
            def objectsWithWrongType = metadataObjects.find { it.key == objName && it.value.class != obj.class }
            objectsWithWrongType.each {
                def messageText = "The database contain one ${obj.class.simpleName} with name ${objName}, but reference metadata too contain a ${it.value.class.simpleName} with same name.";
                def message = new Message(objectName: objName, level: MessageLevel.ERROR, objectType: ObjectType.TABLE, messageType: MessageType.PRESENCE, message: messageText)
                messages << message
            }
        }
        return messages
    }

    private def getMissingObjectsMessages(databaseObjects, metadataObjects) {
        def messages = []
        def databaseMissingObjects = metadataObjects.keySet() - databaseObjects.keySet()
        databaseMissingObjects.each {
            def object = metadataObjects[it]
            def objectType = ObjectType.getType(object)
            def messageText = "The database is missing the ${objectType.description} ${it}.";
            def message = new Message(objectName: it, level: MessageLevel.ERROR, objectType: objectType, messageType: MessageType.PRESENCE, message: messageText)
            messages << message
        }
        return messages
    }

    private def getAdditionalObjectsMessages(databaseObjects, metadataObjects) {
        def messages = []
        def databaseMissingObjects = databaseObjects.keySet() - metadataObjects.keySet()
        databaseMissingObjects.each {
            def object = databaseObjects[it]
            def objectType = ObjectType.getType(object)
            def messageText = "The ${objectType.description} ${it} exists in the database but does not exist in the reference metadata.";
            def message = new Message(objectName: it, level: MessageLevel.WARNING, objectType: objectType, messageType: MessageType.PRESENCE, message: messageText)
            messages << message
        }
        return messages
    }

    private def listFilter(def lista) {
        lista.collectEntries({ [it.key , it.value.getCanonical(rdbms)] } ).findAll { it.value.text }
    }

    def getAllObjects() {
        def allObjects = [:]
        allObjects.putAll tables
        allObjects.putAll sequences
        allObjects.putAll listFilter(views)
        allObjects.putAll listFilter(procedures)
        allObjects.putAll packages
        allObjects.putAll listFilter(triggers)
        allObjects.putAll listFilter(mviews)
        allObjects.putAll userTypes
        return allObjects
    }

    def filter(String objectName) {
        def Schema schema = new Schema()
        schema.tables.putAll tables.findAll { it.key == objectName }
        schema.views.putAll views.findAll { it.key == objectName }
        schema.sequences.putAll sequences.findAll { it.key == objectName }
        schema.procedures.putAll procedures.findAll { it.value.name == objectName }
        schema.packages.putAll packages.findAll { it.key == objectName }
        schema.triggers.putAll triggers.findAll { it.key == objectName }
        schema.mviews.putAll mviews.findAll { it.key == objectName }
        schema.userTypes.putAll userTypes.findAll { it.key == objectName }
        schema.rules = rules
        schema.rdbms = rdbms
        schema.filtered = true
        return schema
    }
}


