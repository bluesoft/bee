package br.com.bluesoft.bee.dbseed

import br.com.bluesoft.bee.dbseed.DbSeedManager
import br.com.bluesoft.bee.model.Options
import br.com.bluesoft.bee.service.BeeWriter


class BeeDbSeedCreateAction {
	Options options
	BeeWriter out

	def run() {
		def description = options.arguments[0]

		new DbSeedManager(configFile: options.configFile, path: options.dataDir.absolutePath, logger: out).createDbSeedFile(description)
	}
}
