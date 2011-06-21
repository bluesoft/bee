/*
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is mozilla.org code.
 *
 * The Initial Developer of the Original Code is
 * Bluesoft Consultoria em Informatica Ltda.
 * Portions created by the Initial Developer are Copyright (C) 2011
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 */
package br.com.bluesoft.bee.database

import groovy.sql.Sql
import br.com.bluesoft.bee.util.PropertiesUtil


class ConnectionInfo {

	String host
	String username
	String password
	String port = 1521
	String serviceName = "serverdb"

	Sql getSqlInstanceForOracle() {
		Sql.newInstance("jdbc:oracle:thin:@${host}:${port}:${serviceName}", username, password, "oracle.jdbc.driver.OracleDriver")
	}

	static def createDatabaseConnection(def configName, def key) {
		def configFile = new File(configName)
		def config = PropertiesUtil.readDatabaseConfig(configFile, key)
		if (config != null) {
			return Sql.newInstance(config.url, config.user, config.password, config.driver)
		}
		return null
	}
}
