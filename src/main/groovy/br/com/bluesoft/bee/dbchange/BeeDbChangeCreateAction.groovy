package br.com.bluesoft.bee.dbchange

import br.com.bluesoft.bee.runner.ActionRunnerParameterValidate

class BeeDbChangeCreateAction extends ActionRunnerParameterValidate {

	boolean run() {
		def description = options.arguments[0]
		def group = options.arguments[1]

		new DbChangeManager(configFile: options.configFile, path: options.dataDir.absolutePath, logger: out).createDbChangeFile(description, group)
		true
	}
}
