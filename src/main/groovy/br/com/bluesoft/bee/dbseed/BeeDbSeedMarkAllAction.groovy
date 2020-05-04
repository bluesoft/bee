package br.com.bluesoft.bee.dbseed

import br.com.bluesoft.bee.dbseed.DbSeedManager
import br.com.bluesoft.bee.model.Options
import br.com.bluesoft.bee.runner.ActionRunnerParameterValidate

class BeeDbSeedMarkAllAction extends ActionRunnerParameterValidate {

    boolean run() {
        def clientName = Options.instance.arguments[0]

        def manager = new DbSeedManager(configFile: Options.instance.configFile, path: Options.instance.dataDir.absolutePath, clientName: clientName, logger: out)

        manager.markAll()
        true
    }

    @Override
    int maxParameters() {
        return 1
    }
}
