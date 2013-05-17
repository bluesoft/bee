package br.com.bluesoft.bee

import br.com.bluesoft.bee.database.ConnectionInfo;
import br.com.bluesoft.bee.dbseed.DbSeedManager
import br.com.bluesoft.bee.model.Options;
import br.com.bluesoft.bee.service.BeeWriter;

public class BeeDbSeedCreateCoreDataAction {
	def options
	def sql
	BeeWriter out

	def run() {
		def clientName = options.arguments[0]
		new DbSeedManager(configFile: options.configFile, path: options.dataDir.absolutePath, clientName: clientName, logger: out).createCoreData()
	}

	def getDatabaseConnection(clientName) {
		if(sql != null) {
			return sql
		}
		return ConnectionInfo.createDatabaseConnection(options.configFile, clientName)
	}
}
