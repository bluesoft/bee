package br.com.bluesoft.bee

import br.com.bluesoft.bee.service.BeeWriter


public class BeeUpgradeModule implements BeeWriter {
	
	def usage() {
		println "usage: bee upgrade"
		println "Actions:"
		println "         upgrade - Update bee version to latest version"
	}

	def parseOptions(options) {
		pritln("BeeUpgradeModule" + options)
		def arguments = options.arguments
		if(arguments.size < 1) {
			usage()
			System.exit 0
		}

		def action = options.actionName
		log(action)

		def actionRunner = null
		switch(action) {
			case "latest":
				if(arguments.size < 2) {
					usage()
					System.exit 0
				}
				actionRunner = new BeeDataGeneratorAction(options: options, out: this)
				break
			case "validate":
				actionRunner = new BeeDataValidatorAction(options: options, out: this)
				break;
		}

		return actionRunner
	}

	void log(String msg) {
		println msg
	}
}
