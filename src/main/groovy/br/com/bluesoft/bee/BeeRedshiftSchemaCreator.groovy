package br.com.bluesoft.bee

import java.text.SimpleDateFormat;
import br.com.bluesoft.bee.util.CsvUtil

class BeeRedshiftSchemaCreator extends BeeSchemaCreator {

	def createColumn(def column) {
		def result = "    ${column.name} ${column.type}"
		if (column.type in ['character', 'character varying', 'text']) {
			if (column.size != null) {
				result += "(${column.size})"
			}
		}

		if (column.type in ['decimal','numeric','serial', 'bigserial']) {
			if (column.scale > 0) {
				result += "(${column.size}, ${column.scale})"
			} else if (column.size != null) {
				result += "(${column.size})"
			}
		}

		if(column.defaultValue)
			result += " default ${column.defaultValue}"

		if (!column.nullable) {
			result += ' not null'
		}

		if (column.encoding && column.encoding != 'none') {
			result += " encode ${column.encoding}"
		}

		return result
	}

	def createTable(def table) {
		def columns = []
		def sortkey = [:]
		def distkey = []

		table.columns.each({
			columns << createColumn(it.value)

			if (it.value.sortkey != 0) {
				sortkey.putAt(it.value.sortkey, it.value.name)
			}

			if (it.value.distkey) {
				distkey << it.value.name
			}
		})

		sortkey = sortkey.inject([]) { result, entry ->
		    result << entry.value.toString()
		}

		def temp = table.temporary ? " global temporary" : ""
		def distkey_query = !distkey.empty ? " distkey(${distkey.join(', ')})" : ""
		def sortkey_query = !sortkey.empty ? " sortkey(${sortkey.join(', ')})" : ""
		def result = "create${temp} table ${table.name} (\n" + columns.join(",\n") + "\n)${distkey_query}${sortkey_query};\n\n"
	}

	void createCoreData(def file, def schema, def dataFolderPath) {
		def listFiles = dataFolderPath.listFiles()
		listFiles.each {
			if(it.name.endsWith(".csv")) {
				def tableName = it.name[0..-5]
				def csvFile = new File(dataFolderPath, tableName + ".csv")
				def fileData = CsvUtil.read(csvFile)
				def table = schema.tables[tableName]
				def columnNames = []
				def columns = [:]
				def columnTypes = [:]

				if (table != null) {
					table.columns.each{
						columns[it.value.name] = it.value.type
								columnNames << it.value.name
					}

					def counterColumnNames = 1
					def counterValue = 1

					def query = new StringBuilder()
					query << "insert into ${tableName} ("
					columnNames.eachWithIndex {columName, index ->
						query << columName
						columnTypes[index] = columns[columName]
						if(counterColumnNames < (columnNames.size())) {
							query << ", "
						}
						counterColumnNames++
					}
					query << ") values"

					for (int i = 0; i < fileData.size; i++) {
						if (i > 0) {
							query << ",\n\t"
						}
						query << " ("
						def params = []
						fileData[i].eachWithIndex { columnValue, index2 ->
							def fieldValue = columnValue.toString()
							params.add(fieldValue)
							def columnType = columnTypes[index2]
							def isString = columnType == 'varchar' || columnType == 'varchar2'
							def isDate = columnType == 'date'
							def isNotNumber = !fieldValue?.isNumber()
							if (isNotNumber && !isDate || isString) {
								fieldValue = fieldValue.replaceAll("\'", "\''")
								if (fieldValue != 'null') {
									fieldValue = "\'" + fieldValue + "\'"
								}
							}
							if (isDate && fieldValue != 'null') {
								fieldValue = fieldValue.replaceAll("\'", "")
								SimpleDateFormat inputSdf = new SimpleDateFormat('yyyy-MM-dd')
								SimpleDateFormat outputSdf = new SimpleDateFormat('yyyy-MM-dd')
								def date = inputSdf.parse(fieldValue);
								fieldValue = outputSdf.format(date)
								fieldValue = "\'" + fieldValue + "\'"
							}
							query << fieldValue
							if (counterValue < (columnNames.size())) {
								query << ", "
							}
							counterValue++
						}
						query << ")"
						counterColumnNames = 1
						counterValue = 1
					}
					query << ";\ncommit;\n\n"
					file.append(query.toString(), 'utf-8')
				}
			}
		}
	}

}
