package br.com.bluesoft.bee.dbseed

import br.com.bluesoft.bee.dbseed.DbSeedManager
import br.com.bluesoft.bee.runner.ActionRunnerParameterValidate

class BeeDbSeedCreateAction extends ActionRunnerParameterValidate {

    boolean run() {
        def description = options.arguments[0]

        new DbSeedManager(configFile: options.configFile, path: options.dataDir.absolutePath, logger: out).createDbSeedFile(description)
        true
    }

    @Override
    int maxParameters() {
        return 1
    }
}
