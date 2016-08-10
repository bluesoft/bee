package br.com.bluesoft.bee.dbchange

import br.com.bluesoft.bee.dbchange.DbChangeManager
import br.com.bluesoft.bee.dbchange.UpDown
import br.com.bluesoft.bee.model.Options
import br.com.bluesoft.bee.service.BeeWriter


class BeeDbChangeUpAction {
	Options options
	BeeWriter out

	def run() {
		def clientName = options.arguments[0]
		def migrationId = options.arguments[1]

		def manager = new DbChangeManager(configFile: options.configFile, path: options.dataDir.absolutePath, logger: out, clientName: clientName)

		if(migrationId)
			manager.executarDbChange(migrationId, UpDown.UP)
		else {
			def lista = manager.listar()
			manager.executarVariasDbChanges(lista, UpDown.UP)
		}
	}
}
