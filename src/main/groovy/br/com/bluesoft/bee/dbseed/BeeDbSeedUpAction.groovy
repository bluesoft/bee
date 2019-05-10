package br.com.bluesoft.bee.dbseed

import br.com.bluesoft.bee.dbseed.DbSeedManager
import br.com.bluesoft.bee.dbchange.UpDown
import br.com.bluesoft.bee.model.Options
import br.com.bluesoft.bee.runner.ActionRunnerParameterValidate
import br.com.bluesoft.bee.service.BeeWriter


class BeeDbSeedUpAction extends ActionRunnerParameterValidate {
	boolean run() {
		def clientName = options.arguments[0]
		def migrationId = options.arguments[1]

		def manager = new DbSeedManager(configFile: options.configFile, path: options.dataDir.absolutePath, logger: out, clientName: clientName)

		if(migrationId)
			manager.executarDbSeed(migrationId, UpDown.UP)
		else {
			def lista = manager.listar()
			manager.executarVariasDbSeeds(lista, UpDown.UP)
		}
		true
	}
}
