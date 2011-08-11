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
package br.com.bluesoft.bee;

import br.com.bluesoft.bee.dbchange.DbChangeManager
import br.com.bluesoft.bee.dbchange.UpDown
import br.com.bluesoft.bee.service.BeeWriter


public class BeeDbChangeRunner implements BeeWriter {
	def usage() {
		println "usage: bee <options> dbchange:action <parameters>"
		println "Actions:"
		println "         dbchange:create description - creates a dbchange file"
		println "         dbchange:status connection - lists dbchanges to run"
		println "         dbchange:up connection <file> - runs all pending dbchange files, or one if specified"
		println "         dbchange:down connection file - runs a dbchange rollback action"
	}

	def createAction(action, options) {
		def arguments = options.arguments()

		if(arguments.size != 2) {
			usage()
			System.exit 0
		}

		def description = arguments[1]

		action.createDbChangeFile(description)
	}

	def statusAction(action, options) {
		def arguments = options.arguments()

		if(arguments.size != 2) {
			usage()
			System.exit 0
		}

		action.clientName = arguments[1]

		action.listar()
	}

	def upAction(action, options) {
		def arguments = options.arguments()

		if(arguments.size > 3 || arguments.size < 2) {
			usage()
			System.exit 0
		}

		action.clientName = arguments[1]

		def migrationId = arguments.size == 3 ? arguments[2] : null

		if(migrationId) {
			action.executarDbChange(migrationId, UpDown.UP)
		} else {
			def lista = action.listar()
			action.executarVariasDbChanges(lista, UpDown.UP)
		}
	}

	def downAction(action, options) {
		def arguments = options.arguments()

		if(arguments.size != 3) {
			usage()
			System.exit 0
		}

		action.clientName = arguments[1]
		def migrationId = arguments[2]

		action.executarDbChange(migrationId, UpDown.DOWN)
	}

	def run(options) {
		def arguments = options.arguments()
		if(arguments.size < 2 || !arguments[0].contains(":")) {
			usage()
			System.exit 0
		}

		def actionName = arguments[0].split(":")[1]

		def action = new DbChangeManager(logger: this)

		if(options.c) {
			action.configName = options.c
		} else {
			action.configName = "bee.properties"
		}

		if(options.d) {
			action.path = options.d
		} else {
			action.path = "bee"
		}

		switch(actionName) {
			case "create":
				createAction(action, options)
				break
			case "status":
				statusAction(action, options)
				break;
			case "up":
				upAction(action, options)
				break;
			case "down":
				downAction(action, options)
				break;
		}
	}

	void log(String msg) {
		println msg
	}
}
