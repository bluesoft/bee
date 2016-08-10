package br.com.bluesoft.bee.dbchange

import br.com.bluesoft.bee.dbchange.DbChangeManager
import br.com.bluesoft.bee.model.Options
import br.com.bluesoft.bee.service.BeeWriter


class BeeDbChangeOpenAction {

	Options options
	BeeWriter out

	def run() {
		new DbChangeManager(configFile: Options.instance.configFile, path: Options.instance.dataDir.absolutePath, fileName: Options.instance.arguments[0], logger: out).open()
	}
}
