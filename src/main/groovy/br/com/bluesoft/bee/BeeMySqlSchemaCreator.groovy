package br.com.bluesoft.bee

import br.com.bluesoft.bee.model.Options;
import br.com.bluesoft.bee.model.TableColumn;
import br.com.bluesoft.bee.service.BeeWriter;
import br.com.bluesoft.bee.importer.JsonImporter

class BeeMySqlSchemaCreator extends BeeSchemaCreator{

	def createColumn(def column) {
		def result = "    ${column.name} ${column.type}"
		if(column.type in ['char', 'varchar'])
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
		if(column.autoIncrement)
			result += ' auto_increment'
		return result
	}
	
	def createTimestampColumn(def column, boolean currentTimestampIsAllowed) {
		def result = "    ${column.name} ${column.type}"
		if (currentTimestampIsAllowed) {
			result += " default CURRENT_TIMESTAMP not null"
		}
		return result
	}

	def createTable(def table) {
		def columns = []
		boolean currentTimestampIsAllowed = true
		boolean createPrimaryKeyColumnIsAllowed = false
		String primaryKeyColumn = null
		
		table.columns.each({
			if (it.value.autoIncrement) {
				 createPrimaryKeyColumnIsAllowed = true
				 primaryKeyColumn = it.value.name
			}
			
			boolean isTimestamp = isTimestamp(it.value)
			
			if (isTimestamp) {
				columns << createTimestampColumn(it.value, currentTimestampIsAllowed)
				currentTimestampIsAllowed = false
			} else {
				columns << createColumn(it.value)
			}
		})
		
		if (createPrimaryKeyColumnIsAllowed)
			createPrimaryKeyColumnOnTableCreateStatement(columns, primaryKeyColumn)
		
		def result = "create table ${table.name} (\n" + columns.join(",\n") + "\n);\n\n"
	}
	
	def isTimestamp(TableColumn column) {
		if (column.type != null && column.type.toUpperCase() == "TIMESTAMP") {
			return true
		} else {
			return false
		}
	}
	
	def createPrimaryKeyColumnOnTableCreateStatement(def columns, def primaryKeyColumn) {
		columns << "    primary key(" + primaryKeyColumn + ")"
	}
	
	def createPrimaryKey(table) {
		def constraint = table.constraints.find ({ it.value.type == 'P'})
		if(constraint == null)
			return ""

		constraint = constraint.value

		return "alter table ${table.name} add primary key (" + constraint.columns.join(',') + ");\n"
	}
	
}
