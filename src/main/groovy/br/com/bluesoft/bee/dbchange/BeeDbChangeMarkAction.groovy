package br.com.bluesoft.bee.dbchange

import br.com.bluesoft.bee.runner.ActionRunnerParameterValidate

class BeeDbChangeMarkAction extends ActionRunnerParameterValidate {

	boolean run() {
		def migrationId = options.arguments[1]
		new DbChangeManager(configFile: options.configFile, path: options.dataDir.absolutePath, clientName: options.arguments[0], logger: out).mark(migrationId)
		true
	}

	@Override
	int minParameters() {
		return 2
	}
}
