package br.com.bluesoft.bee.dbseed

import br.com.bluesoft.bee.runner.ActionRunnerParameterValidate

class BeeDbSeedGenerateAction extends ActionRunnerParameterValidate {
    @Override
    boolean run() {
        def clientName = options.arguments[0]
        def tableName = options.arguments[1]

        new DbSeedManager(configFile: options.configFile, path: options.dataDir.absolutePath, clientName: clientName, logger: out).generate(tableName)
        return true
    }

    @Override
    int minParameters() {
        return 2
    }

}
