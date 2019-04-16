package br.com.bluesoft.bee.dbchange

import br.com.bluesoft.bee.model.Options
import br.com.bluesoft.bee.runner.ActionRunnerParameterValidate

class BeeDbChangeFindAction extends ActionRunnerParameterValidate {

	boolean run() {
		new DbChangeManager(configFile: Options.instance.configFile, path: Options.instance.dataDir.absolutePath, fileName: Options.instance.arguments[0], logger: out).search()
	}
}
