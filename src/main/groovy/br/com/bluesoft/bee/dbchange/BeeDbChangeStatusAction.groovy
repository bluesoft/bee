package br.com.bluesoft.bee.dbchange

import br.com.bluesoft.bee.dbchange.DbChangeManager
import br.com.bluesoft.bee.model.Options
import br.com.bluesoft.bee.service.BeeWriter


class BeeDbChangeStatusAction {

	Options options
	BeeWriter out

	def run() {
		def group = options.arguments[1]

		new DbChangeManager(configFile: Options.instance.configFile, path: Options.instance.dataDir.absolutePath, clientName: Options.instance.arguments[0], logger: out).listar(group)
	}
}
