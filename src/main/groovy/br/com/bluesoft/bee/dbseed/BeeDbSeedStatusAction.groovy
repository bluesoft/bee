package br.com.bluesoft.bee.dbseed

import br.com.bluesoft.bee.dbseed.DbSeedManager
import br.com.bluesoft.bee.model.Options
import br.com.bluesoft.bee.runner.ActionRunnerParameterValidate
import br.com.bluesoft.bee.service.BeeWriter


class BeeDbSeedStatusAction extends ActionRunnerParameterValidate {

	boolean run() {
		new DbSeedManager(configFile: Options.instance.configFile, path: Options.instance.dataDir.absolutePath, clientName: Options.instance.arguments[0], logger: out).listar()
		true
	}

	@Override
	int maxParameters() {
		return 1
	}
}
