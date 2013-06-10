package br.com.bluesoft.bee

import br.com.bluesoft.bee.dbseed.DbSeedManager
import br.com.bluesoft.bee.model.Options
import br.com.bluesoft.bee.service.BeeWriter


class BeeDbSeedStatusAction {

	Options options
	BeeWriter out

	def run() {
		new DbSeedManager(configFile: Options.instance.configFile, path: Options.instance.dataDir.absolutePath, clientName: Options.instance.arguments[0], logger: out).listar()
	}
}
