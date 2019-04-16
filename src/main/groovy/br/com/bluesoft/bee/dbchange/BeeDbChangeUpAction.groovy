package br.com.bluesoft.bee.dbchange

import br.com.bluesoft.bee.runner.ActionRunnerParameterValidate

class BeeDbChangeUpAction extends ActionRunnerParameterValidate {

	boolean run() {
		def clientName = options.arguments[0]
		def migrationId = options.arguments[1]

		def manager = new DbChangeManager(configFile: options.configFile, path: options.dataDir.absolutePath, logger: out, clientName: clientName)

		if(migrationId)
			manager.executarDbChange(migrationId, UpDown.UP)
		else {
			def lista = manager.listar()
			manager.executarVariasDbChanges(lista, UpDown.UP)
		}
		true
	}
}
