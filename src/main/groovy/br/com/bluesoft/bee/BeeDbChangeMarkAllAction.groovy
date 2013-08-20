package br.com.bluesoft.bee

import br.com.bluesoft.bee.dbchange.DbChangeManager
import br.com.bluesoft.bee.model.Options
import br.com.bluesoft.bee.service.BeeWriter


class BeeDbChangeMarkAllAction {
	Options options
	BeeWriter out

	def run() {
		def clientName = Options.instance.arguments[0]

		def manager = new DbChangeManager(configFile: Options.instance.configFile, path: Options.instance.dataDir.absolutePath, clientName: clientName, logger: out)

		manager.markAll()
	}
}
