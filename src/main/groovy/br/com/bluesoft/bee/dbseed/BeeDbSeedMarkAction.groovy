package br.com.bluesoft.bee.dbseed

import br.com.bluesoft.bee.dbseed.DbSeedManager
import br.com.bluesoft.bee.model.Options
import br.com.bluesoft.bee.service.BeeWriter


class BeeDbSeedMarkAction {

	Options options
	BeeWriter out

	def run() {
		def migrationId = options.arguments[1]
		new DbSeedManager(configFile: options.configFile, path: options.dataDir.absolutePath, clientName: options.arguments[0], logger: out).mark(migrationId)
	}
}
