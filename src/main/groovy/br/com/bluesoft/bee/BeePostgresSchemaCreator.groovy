package br.com.bluesoft.bee


class BeePostgresSchemaCreator extends BeeSchemaCreator {
	
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
			println "Table ${it.value.name}"
			def table = it.value
			def indexes = table.indexes*.value.findAll { it.type == 'n' }
			def primaryKeys = table.constraints*.value.findAll {it.type == 'P'}
			def uniqueKeys = table.constraints*.value.findAll {it.type == 'U'}
			indexes.each {
				def indexName = it.name
				def existPrimaryKeyWithThisName = primaryKeys.findAll {it.name.equals(indexName)}.size() == 1
				def existUniqueKeyWithThisName = uniqueKeys.findAll {it.name.equals(indexName)}.size() == 1
				if (!existPrimaryKeyWithThisName && !existUniqueKeyWithThisName) {
					file << createIndex(table.name, it) 
				}
			}
		}

		file << "\n"
	}
}
