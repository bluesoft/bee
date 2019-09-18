package br.com.bluesoft.bee.database

import br.com.bluesoft.bee.util.PropertiesUtil
import groovy.sql.Sql

class ConnectionInfo {
    static Sql createDatabaseConnection(File configFile, def key) {
        def config = PropertiesUtil.readDatabaseConfig(configFile, key)
        if (config != null) {

            Properties props = new Properties()
            props.put("useFetchSizeWithLongColumn", "true")
            props.put("user", config.user)
            props.put("password", config.password)

            Sql from = Sql.newInstance(
                    url: config.url,
                    driver: config.driver,
                    properties: props
            )

            from.withStatement { stmt ->
                stmt.fetchSize = 5000
            }
            return from
        }
        return null
    }
}
