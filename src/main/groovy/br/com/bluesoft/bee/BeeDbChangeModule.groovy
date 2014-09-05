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

import br.com.bluesoft.bee.service.BeeWriter


public class BeeDbChangeModule implements BeeWriter {

	def parameterCount = [
		"create": [1, 1],
		"status": [1, 1],
		"up": [1, 2],
		"down": [2, 2],
		"mark": [2, 2],
		"markall": [1, 1],
		"unmark": [2, 2],
		"find": [1, 2],
		"open": [1, 2]
		]

	def usage() {
		println "usage: bee <options> dbchange:action <parameters>"
		println "Actions:"
		println "         dbchange:create description - creates a dbchange file"
		println "         dbchange:status connection - lists dbchanges to run"
		println "         dbchange:up connection <file> - runs all pending dbchange files, or one if specified"
		println "         dbchange:down connection file - runs a dbchange rollback action"
		println "         dbchange:mark connection file - mark a file as executed"
		println "         dbchange:markall connection - mark All files as executed"
		println "         dbchange:unmark connection file - unmark a file as executed"
		println "         dbchange:find name - search file"
		println "         dbchange:open name - open file with EDITOR"
	}

	def parseOptions(options) {
		def action = options.actionName
		def arguments = options.arguments

		if(parameterCount[action] == null || parameterCount[action] == 0) {
			usage()
			System.exit 0
		}

		def min = parameterCount[action][0]
		def max = parameterCount[action][1]

		if(arguments.size > max || arguments.size < min) {
			usage()
			System.exit 0
		}

		def actionRunner = null
		switch(action) {
			case "create":
				actionRunner = new BeeDbChangeCreateAction(options: options, out: this)
				break;
			case "status":
				actionRunner = new BeeDbChangeStatusAction(options: options, out: this)
				break;
			case "up":
				actionRunner = new BeeDbChangeUpAction(options: options, out: this)
				break;
			case "down":
				actionRunner = new BeeDbChangeDownAction(options: options, out: this)
				break;
			case "mark":
				actionRunner = new BeeDbChangeMarkAction(options: options, out: this)
				break;
			case "markall":
				actionRunner = new BeeDbChangeMarkAllAction(options: options, out: this)
				break;
			case "unmark":
				actionRunner = new BeeDbChangeUnmarkAction(options: options, out: this)
				break;
			case "find":
				actionRunner = new BeeDbChangeFindAction(options: options, out: this)
				break;
			case "open":
				actionRunner = new BeeDbChangeOpenAction(options: options, out: this)
				break;
		}

		return actionRunner;
	}

	def run(options) {
		def actionRunner = parseOptions(options)
		if(actionRunner)
			if(!actionRunner.run())
				System.exit(1)
	}

	void log(String msg) {
		println msg
	}
}
