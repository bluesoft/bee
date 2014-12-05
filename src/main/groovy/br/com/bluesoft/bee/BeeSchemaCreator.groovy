package br.com.bluesoft.bee

import java.text.SimpleDateFormat;

import br.com.bluesoft.bee.util.CsvUtil





abstract class BeeSchemaCreator {
	
	void createSequences(def file, def schema) {
		schema.sequences*.value.each { file << "create sequence ${it.name};\n" }
		file << "\n"
	}

	def createColumn(def column) {
		println  'BeeSchemaCreator'
		println column.name
		println column.type

		def result = "    ${column.name} ${column.type}"
		if(column.type in ['char', 'varchar'])
			if(column.sizeType != null)
				result += "(${column.size} ${column.sizeType})"
			else
				result += "(${column.size})"
				
		if(column.type == 'number')
			if(column.scale > 0)
				result += "(${column.size}, ${column.scale})"
			else
				result += "(${column.size})"

		if(column.defaultValue)
			result += " default ${column.defaultValue}"

		if(!column.nullable)
			result += ' not null'
		return result
	}

	def createTable(def table) {
		def columns = []
		table.columns.each({
			columns << createColumn(it.value) 
		})
		def temp = table.temporary ? " global temporary" : ""
		def result = "create${temp} table ${table.name} (\n" + columns.join(",\n") + "\n);\n\n"
	}

	void createTables(def file, def schema) {
		def tables = schema.tables.sort()
		tables.each( { file << createTable(it.value) })
	}

	def createPrimaryKey(table) {
		def constraint = table.constraints.find ({ it.value.type == 'P' })

		if(constraint == null)
			return ""

		constraint = constraint.value

		return "alter table ${table.name} add constraint ${constraint.name} primary key (" + constraint.columns.join(',') + ");\n"
	}

	void createPrimaryKeys(def file, def schema) {
		def tables = schema.tables.sort()
		tables.each {
			file << createPrimaryKey(it.value)
		}

		file << "\n"
	}

	def createUniqueKey(table) {
		def constraints = table.constraints.findAll { it.value.type == 'U' }*.value

		def result = ""

		constraints.each {
			result += "alter table ${table.name} add constraint ${it.name} unique(" + it.columns.join(',') + ");\n" 
		}

		return result
	}

	void createUniqueKeys(def file, def schema) {
		def tables = schema.tables.sort()
		tables.each {
			file << createUniqueKey(it.value)
		}

		file << "\n"
	}

	def createForeignKey(table) {
		def constraints = table.constraints.findAll { it.value.type == 'R' }*.value

		def result = ""

		constraints.each {
			def onDelete = it.onDelete ? "on delete ${it.onDelete}" : ""
			def refColumns = it.refColumns ? "(" + it.refColumns.join(',') + ")" : ""
			result += "alter table ${table.name} add constraint ${it.name} foreign key (" + it.columns.join(',') + ") references ${it.refTable} ${refColumns} ${onDelete};\n"
		}

		return result
	}

	void createForeignKeys(def file, def schema) {
		def tables = schema.tables.sort()
		tables.each {
			file << createForeignKey(it.value)
		}

		file << "\n"
	}

	def createIndex(tableName, index) {
		def result = "create"
		if(index.type == 'b')
			result += ' bitmap'
		if(index.unique)
			result += ' unique'
		result += " index ${index.name} on ${tableName}(" + index.columns.join(',') + ");\n"

		return result
	}

	void createIndexes(def file, def schema) {
		def tables = schema.tables.sort()
		tables.each {
			def table = it.value
			def indexes = table.indexes*.value.findAll { it.type == 'n' }
			indexes.each { file << createIndex(table.name, it) }
		}

		file << "\n"
	}

	void createFunctionalIndexes(def file, def schema) {
		def tables = schema.tables.sort()
		tables.each {
			def table = it.value
			def indexes = table.indexes*.value.findAll { it.type == 'f' }
			indexes.each { file << createIndex(table.name, it) }
		}

		file << "\n"
	}

	void createBitmapIndexes(def file, def schema) {
		def tables = schema.tables.sort()
		tables.each {
			def table = it.value
			def indexes = table.indexes*.value.findAll { it.type == 'b' }
			indexes.each { file << createIndex(table.name, it) }
		}

		file << "\n"
	}

	void createViews(def file, def schema) {
		schema.views*.value.sort().each {
			def view = "create or replace view ${it.name} as ${it.text}\n\n"
			file.append(view.toString(), 'utf-8')
		}
	}

	void createPackages(def file, def schema) {
		schema.packages*.value.sort().each {
			def text = "create or replace ${it.text}\n/\n\n"
			def body = "create or replace ${it.body}\n/\n\n"
			file.append(text.toString(), 'utf-8')
			file.append(body.toString(), 'utf-8')
		}
	}

	void createProcedures(def file, def schema) {
		schema.procedures*.value.sort().each {
			def text = []
			it.text.eachLine { text << it }
			def text2 = text[1..text.size()-1].join("\n")
			def procedure = "create or replace ${text2}\n/\n\n" 
			file.append(procedure.toString(), 'utf-8')
		}
	}

	void createTriggers(def file, def schema) {
		schema.triggers*.value.sort().each {
			def trigger = "create or replace ${it.text}\n/\n\n"
			file.append(trigger.toString(), 'utf-8')
		}
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
					for (int i = 0; i < fileData.size; i++) {
						query << "insert into ${tableName} ("
						columnNames.eachWithIndex {columName, index ->
							query << columName
							columnTypes[index] = columns[columName]
							if(counterColumnNames < (columnNames.size())) {
								query << ", "
							}
							counterColumnNames++
						}
						query << ") "
						query << "values ("
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
						query << ");\n"
						counterColumnNames = 1
						counterValue = 1
					}
					query << "commit;\n"
					file.append(query.toString(), 'utf-8')
				}
			}
		}
	}
	
	def createUserTypes(file, schema){
		
	}
}
