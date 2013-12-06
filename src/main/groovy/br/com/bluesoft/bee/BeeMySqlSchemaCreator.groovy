package br.com.bluesoft.bee

import br.com.bluesoft.bee.model.TableColumn


class BeeMySqlSchemaCreator extends BeeSchemaCreator{

	def createColumn(def column) {
		def result = "    ${column.name} ${column.type}"
		
		if (column.defaultValue)
			if ( (column.defaultValue != null && column.defaultValue.isNumber()) || column.defaultValue == 'CURRENT_TIMESTAMP' || column.defaultValue == 'NULL' ) {
				result += " default ${column.defaultValue}"
			} else if(column.defaultValue != null) {
				result += " default '${column.defaultValue}'"
			}
		if (column.onUpdateCurrentTimestamp)
			result += " on update CURRENT_TIMESTAMP"
		if (!column.nullable) {
			result += ' not null'
		} else {
			result += ' null'
		}
		if (column.autoIncrement)
			result += ' auto_increment'
		return result
	}
	
	def createTable(def table) {
		def columns = []
		boolean createPrimaryKeyColumnIsAllowed = false
		String primaryKeyColumn = null
		
		table.columns.each({
			if (it.value.autoIncrement) {
				 createPrimaryKeyColumnIsAllowed = true
				 primaryKeyColumn = it.value.name
			}
			columns << createColumn(it.value)
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
		
		def constraintRefColumn = table.columns.find({ it.value.name == constraint.columns.first()})
		
		if (constraintRefColumn != null && constraintRefColumn.value.autoIncrement == "auto_increment") {
			return ""
		}
		
		return "alter table ${table.name} add primary key (" + constraint.columns.join(',') + ");\n"
	}
	
}
