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

class Constraint {

	String name
	String refTable
	String type
	String onDelete
	def columns = []
	def refColumns = []

	def validateWithMetadata(Table table, Constraint metadataConstraint) {
		def messages = []

		if (metadataConstraint.type != this.type) {
			def messageText = "The type of the constraint ${this.name} of the table ${table.name} should be ${metadataConstraint.type} but it is ${this.type}"
			messages << new Message(objectName:this.name, level:MessageLevel.ERROR, objectType:ObjectType.CONSTRAINT, messageType:MessageType.CONSTRAINT_TYPE, message:messageText)
		}

		if (metadataConstraint.refTable != this.refTable) {
			def messageText = "The referenced table of the constraint ${this.name} of the table ${table.name} should be ${metadataConstraint.refTable} but it is ${this.refTable}"
			messages << new Message(objectName:this.name, level:MessageLevel.ERROR, objectType:ObjectType.CONSTRAINT, messageType:MessageType.CONSTRAINT_REF_TABLE, message:messageText)
		}

		if (metadataConstraint.columns != this.columns) {
			def messageText = "The columns of the constraint ${this.name} of the table ${table.name} should be ${metadataConstraint.columns} but it is ${this.columns}"
			messages << new Message(objectName:this.name, level:MessageLevel.ERROR, objectType:ObjectType.CONSTRAINT, messageType:MessageType.CONSTRAINT_COLUMNS, message:messageText)
		}
		
		if (metadataConstraint.refColumns != this.refColumns) {
			def messageText = "The columns of the constraint ${this.name} of the table ${table.name} should be ${metadataConstraint.refColumns} but it is ${this.refColumns}"
			messages << new Message(objectName:this.name, level:MessageLevel.ERROR, objectType:ObjectType.CONSTRAINT, messageType:MessageType.CONSTRAINT_REF_COLUMNS, message:messageText)
		}

		if (this.type == 'R' && metadataConstraint.onDelete != this.onDelete) {
			def messageText = "The delete action of the constraint ${this.name} should be ${metadataConstraint.onDelete} but it is ${this.onDelete}"
			messages << new Message(objectName:this.name, level:MessageLevel.ERROR, objectType:ObjectType.CONSTRAINT, messageType:MessageType.CONSTRAINT_DELETE_RULE, message:messageText)
		}

		return messages
	}
}
