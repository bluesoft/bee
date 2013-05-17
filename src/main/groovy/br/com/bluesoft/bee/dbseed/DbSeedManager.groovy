package br.com.bluesoft.bee.dbseed

import java.io.File;

import br.com.bluesoft.bee.database.ConnectionInfo;
import br.com.bluesoft.bee.database.reader.TableDataReader
import br.com.bluesoft.bee.importer.Importer
import br.com.bluesoft.bee.importer.JsonImporter
import br.com.bluesoft.bee.service.BeeWriter;
import br.com.bluesoft.bee.util.CsvUtil

public class DbSeedManager {

	final static def MESSAGE_COULD_NOT_GET_CONNECTION = "It was not possible to get the database connection"

	def sql
	def directoryFile

	File configFile
	String path
	String clientName
	boolean force
	BeeWriter logger
	Importer importer

	def createCoreData() {
		def sql = getDatabaseConnection()
		if (sql == null) {
			logger.log(MESSAGE_COULD_NOT_GET_CONNECTION)
			return false
		}

		def dataPath = new File(path, 'data')
		def listFiles = dataPath.listFiles()
		listFiles.each {
			if(it.name.endsWith(".csv")) {
				def tableName = it.name[0..-5]
				def file = new File(dataPath, tableName + ".csv")
				def fileData = CsvUtil.read(file)
				def schema = getImporter(path).importMetaData()
				def table = schema.tables[tableName]
				def columnNames = []
				table.columns.each{
					columnNames << it.value.name
				}
				logger.log("Inserting data into the table ${tableName}")

				def counterColumnNames = 1
				def counterValue = 1

				for (int i = 0; i < fileData.size; i++) {
					def query = new StringBuilder()
					query << "insert into ${tableName} ("
					columnNames.each {
						query << it
						if(counterColumnNames < (columnNames.size())) {
							query << ", "
						}
						counterColumnNames++
					}
					query << ") "
					query << "values ("
					def params = []
					fileData[i].each() {
						def fieldValue

						if(it == null) {
							fieldValue = "null"
						} else {
							fieldValue = it.isNumber() ? it : "\'" + it + "\'"
						}
						fieldValue = fieldValue.replaceAll("'", "\'")
						params.add(fieldValue)
						query << "?"
						if(counterValue < (columnNames.size())) {
							query << ", "
						}
						counterValue++
					}
					query << ")"
					counterColumnNames = 1
					counterValue = 1
					String queryString = query.toString()
					sql.execute (queryString, params)
				}
			}
		}
	}

	private def getImporter(path) {
		if(importer == null)
			return new JsonImporter(path)
		return importer
	}

	def getDatabaseConnection() {
		if(sql != null) {
			return sql
		}
		return ConnectionInfo.createDatabaseConnection(configFile.absoluteFile, clientName)
	}
}
