package br.com.bluesoft.bee.database

import br.com.bluesoft.bee.util.PropertiesUtil
import groovy.sql.Sql

class ConnectionInfo {

	String host
	String username
	String password
	String port = 1521
	String serviceName = "serverdb"

	Sql getSqlInstanceForOracle() {
		Sql.newInstance("jdbc:oracle:thin:@${host}:${port}:${serviceName}", username, password, "oracle.jdbc.driver.OracleDriver")
	}

	static def createDatabaseConnection(File configFile, def key) {
		def config = PropertiesUtil.readDatabaseConfig(configFile, key)
		if (config != null) {
			return Sql.newInstance(config.url, config.user, config.password, config.driver)
		}
		return null
	}
}
