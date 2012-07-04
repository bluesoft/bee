package br.com.bluesoft.bee

import br.com.bluesoft.bee.dbchange.DbChangeManager
import br.com.bluesoft.bee.model.Options
import br.com.bluesoft.bee.service.BeeWriter


class BeeDbChangeCreateAction {
	Options options
	BeeWriter out

	def run() {
		def description = options.arguments[0]

		new DbChangeManager(configFile: options.configFile, path: options.dataDir.absolutePath, logger: out).createDbChangeFile(description)
	}
}
