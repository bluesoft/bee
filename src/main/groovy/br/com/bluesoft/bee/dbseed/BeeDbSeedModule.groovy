package br.com.bluesoft.bee.dbseed;

import br.com.bluesoft.bee.service.BeeWriter

public class BeeDbSeedModule implements BeeWriter {

	def parameterCount = [
		"create": [1, 1],
		"status": [1, 1],
		"up": [1, 2],
		"down": [2, 2],
		"mark": [2, 2],
		"markall": [1, 1],
		"unmark": [2, 2]]

	def usage() {
		println "usage: bee <options> dbseed:action <parameters>"
		println "Actions:"
		println "         dbseed:create description - creates a dbseed file"
		println "         dbseed:status connection - lists dbseeds to run"
		println "         dbseed:up connection <file> - runs all pending dbseed files, or one if specified"
		println "         dbseed:down connection file - runs a dbseed rollback action"
		println "         dbseed:mark connection file - mark a file as executed"
		println "         dbseed:markall connection - mark All files as executed"
		println "         dbseed:unmark connection file - unmark a file as executed"
	}

	def parseOptions(options) {
		def action = options.actionName
		def arguments = options.arguments

		if(parameterCount[action] == null) {
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
				actionRunner = new BeeDbSeedCreateAction(options: options, out: this)
				break;
			case "status":
				actionRunner = new BeeDbSeedStatusAction(options: options, out: this)
				break;
			case "up":
				actionRunner = new BeeDbSeedUpAction(options: options, out: this)
				break;
			case "down":
				actionRunner = new BeeDbSeedDownAction(options: options, out: this)
				break;
			case "mark":
				actionRunner = new BeeDbSeedMarkAction(options: options, out: this)
				break;
			case "markall":
				actionRunner = new BeeDbSeedMarkAllAction(options: options, out: this)
				break;
			case "unmark":
				actionRunner = new BeeDbSeedUnmarkAction(options: options, out: this)
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
