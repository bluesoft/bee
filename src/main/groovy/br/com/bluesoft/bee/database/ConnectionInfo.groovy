package br.com.bluesoft.bee.database

import br.com.bluesoft.bee.util.PropertiesUtil
import groovy.sql.Sql

class ConnectionInfo {
    static def createDatabaseConnection(File configFile, def key) {
        def config = PropertiesUtil.readDatabaseConfig(configFile, key)
        if (config != null) {
            def from = Sql.newInstance(config.url, config.user, config.password, config.driver)
            from.withStatement { stmt -> stmt.fetchSize = 1000 }
            return from
        }
        return null
    }
}
