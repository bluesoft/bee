package br.com.bluesoft.bee.dbseed

import br.com.bluesoft.bee.dbchange.UpDown
import br.com.bluesoft.bee.dbseed.DbSeedManager
import br.com.bluesoft.bee.runner.ActionRunnerParameterValidate

class BeeDbSeedDownAction extends ActionRunnerParameterValidate {
	boolean run() {
		def clientName = options.arguments[0]
		def migrationId = options.arguments[1]

		def manager = new DbSeedManager(configFile: options.configFile, path: options.dataDir.absolutePath, logger: out, clientName: clientName)
		manager.executarDbSeed(migrationId, UpDown.DOWN)
		true
	}

	@Override
	int minParameters() {
		return 1
	}
}
