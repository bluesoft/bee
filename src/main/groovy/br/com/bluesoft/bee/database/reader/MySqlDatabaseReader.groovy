package br.com.bluesoft.bee.database.reader

import br.com.bluesoft.bee.model.*

class MySqlDatabaseReader implements DatabaseReader {

	def sql
	def schema

	MySqlDatabaseReader (def sql, String schema) {
		this.sql = sql
		this.schema = schema
	}

	Schema getSchema(objectName = null) {
		def schema = new Schema()
		selectDatabase()
		schema.tables = getTables(objectName)
		schema.sequences = getSequences(objectName)
		schema.views = getViews(objectName)
		schema.procedures = getProcedures(objectName)
		schema.packages = getPackages(objectName)
		schema.triggers = getTriggers(objectName)
		return schema
	}


	def getTables(objectName) {
		def tables = fillTables(objectName)
		fillColumns(tables, objectName)
		fillIndexes(tables, objectName)
		fillIndexColumns(tables, objectName)
		fillConstraints(tables, objectName)
		fillConstraintsColumns(tables, objectName)
		return tables
	}
	
	private void selectDatabase() {
		sql.execute('use `' + schema + '`')
	}

	static final def TABLES_QUERY = ''' 
		select ut.table_name , 'N' as temporary, ut.table_comment AS 'comments' 
		from information_schema.tables ut
		where ut.table_schema = ?
		order by ut.table_name
	'''
	static final def TABLES_QUERY_BY_NAME = '''
		select ut.table_name , 'N' as temporary, ut.table_comment AS 'comments' 
		from information_schema.tables ut
		where ut.table_schema = ?
		and ut.table_name = ?
		order by ut.table_name
	'''
	private def fillTables(objectName) {
		def tables = [:]
		def rows
		if(objectName) {
			rows = sql.rows(TABLES_QUERY_BY_NAME, [schema, objectName])
		} else {
			rows = sql.rows(TABLES_QUERY, [schema])
		}
		rows.each({
			def name = it.table_name
			def temporary = it.temporary == 'Y' ? true : false
			def comment = it.comments
			tables[name] = new Table(name:name, temporary:temporary, comment:comment)
		})
		return tables
	}

	static final def TABLES_COLUMNS_QUERY = '''
		select uc.table_name, uc.column_name, uc.column_type, uc.is_nullable as nullable, coalesce(uc.numeric_precision, uc.character_maximum_length) data_size,
		if(uc.extra='auto_increment', 'auto_increment', null) as auto_increment, coalesce(uc.numeric_scale, 0) data_scale, 
		if(uc.extra='on update CURRENT_TIMESTAMP', 'on update CURRENT_TIMESTAMP', null) as onUpdateCurrentTimestamp,
		uc.column_default as data_default, uc.ordinal_position as column_id
		from information_schema.columns uc
		inner join information_schema.tables ut on uc.table_name = ut.table_name
		where uc.table_schema = ?  
		group by table_name, column_name
		order by table_name, uc.ordinal_position
	'''	
	static final def TABLES_COLUMNS_QUERY_BY_NAME = '''
		select uc.table_name, uc.column_name, uc.column_type, uc.is_nullable as nullable, coalesce(uc.numeric_precision, uc.character_maximum_length) data_size,
		if(uc.extra='auto_increment', 'auto_increment', null) as auto_increment, coalesce(uc.numeric_scale, 0) data_scale, 
		if(uc.extra='on update CURRENT_TIMESTAMP', 'on update CURRENT_TIMESTAMP', null) as onUpdateCurrentTimestamp,
		uc.column_default as data_default, uc.ordinal_position as column_id
		from information_schema.columns uc
		inner join information_schema.tables ut on uc.table_name = ut.table_name
		where uc.table_schema = ?  
		and uc.table_name = ?
		group by table_name, column_name
		order by table_name, uc.ordinal_position
	'''
	private def fillColumns(tables, objectName) {
		def rows
		if(objectName) {
			rows = sql.rows(TABLES_COLUMNS_QUERY_BY_NAME, [schema, objectName])
		} else {
			rows = sql.rows(TABLES_COLUMNS_QUERY, [schema])
		}
		rows.each({
			def table = tables[it.table_name]
			def column = new TableColumn()
			column.name = it.column_name
			column.type = it.column_type
			column.size = it.data_size
			column.scale = it.data_scale
			column.nullable = it.nullable == 'NO' ? false : true
			column.autoIncrement = it.auto_increment
			column.onUpdateCurrentTimestamp = it.onUpdateCurrentTimestamp
			def defaultValue = it.data_default
			if(defaultValue) {
				column.defaultValue = defaultValue?.trim()?.toUpperCase() == 'NULL' ? null : defaultValue?.trim()
			}
			table.columns[column.name] = column
		})
	}
	

	final static def INDEXES_QUERY = '''
		select ui.table_name, ui.index_name, ui.index_type, ui.non_unique as uniqueness FROM information_schema.statistics ui
		left join information_schema.table_constraints tc on ui.index_name = tc.constraint_name
		where ui.table_schema = ? 
		and tc.constraint_type is null
		group by ui.table_name, ui.index_name, ui.index_type, ui.non_unique
		order by ui.table_name, ui.index_name
	'''
	final static def INDEXES_QUERY_BY_NAME = '''
		select ui.table_name, ui.index_name, ui.index_type, ui.non_unique as uniqueness FROM information_schema.statistics ui
		left join information_schema.table_constraints tc on ui.index_name = tc.constraint_name
		where ui.table_schema = ? 
		and tc.constraint_type is null
		and  ui.table_name = ?
		group by ui.table_name, ui.index_name, ui.index_type, ui.non_unique
		order by ui.table_name, ui.index_name
	'''

	private def fillIndexes(tables, objectName) {
		def rows
		if(objectName) {
			rows = sql.rows(INDEXES_QUERY_BY_NAME, [schema, objectName])
		} else {
			rows = sql.rows(INDEXES_QUERY, [schema])
		}
		rows.each({
			def tableName = it.table_name
			def table = tables[tableName]
			def index = new Index()
			index.name = it.index_name
			index.type = getIndexType(it.index_type)
			index.unique = it.uniqueness == '1' ? true : false
			table.indexes[index.name] = index
		})
	}
	
	private def getIndexType(String indexType){
		switch (indexType) {
			case "BTREE":
				return "n"
				break
			default:
				return indexType
		}
	}

	final static def INDEXES_COLUMNS_QUERY = '''
		select ui.table_name, ui.index_name, ui.column_name, c.column_default as data_default, 'asc' as descend 
		from information_schema.statistics ui
		left join information_schema.table_constraints tc on ui.index_name = tc.constraint_name 
		inner join information_schema.columns c on c.column_name = ui.column_name
		where ui.table_schema = ? 
		and tc.constraint_type is null
		group by ui.index_name, ui.column_name
		order by ui.index_name, ui.seq_in_index
	'''
	final static def INDEXES_COLUMNS_QUERY_BY_NAME = '''
		select ui.table_name, ui.index_name, ui.column_name, c.column_default as data_default, 'asc' as descend 
		from information_schema.statistics ui
		left join information_schema.table_constraints tc on ui.index_name = tc.constraint_name 
		inner join information_schema.columns c on c.column_name = ui.column_name
		where ui.table_schema = ? 
		and tc.constraint_type is null
		and ui.table_name = ?
		group by ui.index_name, ui.column_name
		order by ui.index_name, ui.seq_in_index
	'''
	private def fillIndexColumns(tables, objectName) {
		def rows
		if(objectName) {
			rows = sql.rows(INDEXES_COLUMNS_QUERY_BY_NAME , [schema, objectName])
		} else {
			rows = sql.rows(INDEXES_COLUMNS_QUERY, [schema])
		}

		rows.each({

			def table = tables[it.table_name]
			def index = table.indexes[it.index_name]

			def indexColumn = new IndexColumn()
			indexColumn.name = it.column_name
			indexColumn.descend = it.descend.toLowerCase() == 'asc' ? false : true

			index.columns << indexColumn
		})
	}

	final static def CONSTRAINTS_QUERY = '''
		select kcu.table_name, kcu.constraint_name, tc.constraint_type, kcu.referenced_table_name as ref_table, if(tc.constraint_type in ('PRIMARY KEY', 'UNIQUE'),tc.constraint_name,null) as index_name, rc.delete_rule
		from information_schema.key_column_usage kcu
		inner join information_schema.table_constraints tc on tc.table_schema = kcu.table_schema and tc.table_name = kcu.table_name and kcu.constraint_name = tc.constraint_name
		inner join information_schema.columns c on c.table_name = kcu.table_name and c.column_name = kcu.column_name
		left join information_schema.referential_constraints rc on rc.table_name = tc.table_name and  rc.constraint_name = tc.constraint_name
		where kcu.constraint_schema = ?
		and c.extra != 'auto_increment'		
		group by kcu.table_name, kcu.constraint_name, tc.constraint_type
		order by kcu.table_name, kcu.constraint_name, tc.constraint_type;				
	'''
	final static def CONSTRAINTS_QUERY_BY_NAME = '''
		select kcu.table_name, kcu.constraint_name, tc.constraint_type, kcu.referenced_table_name as ref_table, if(tc.constraint_type in ('PRIMARY KEY', 'UNIQUE'),tc.constraint_name,null) as index_name, rc.delete_rule
		from information_schema.key_column_usage kcu
		inner join information_schema.table_constraints tc on tc.table_schema = kcu.table_schema and tc.table_name = kcu.table_name and kcu.constraint_name = tc.constraint_name
		inner join information_schema.columns c on c.table_name = kcu.table_name and c.column_name = kcu.column_name
		left join information_schema.referential_constraints rc on rc.table_name = tc.table_name and  rc.constraint_name = tc.constraint_name
		where kcu.constraint_schema = ?
		and kcu.table_name = ?
		and c.extra != 'auto_increment'		
		group by kcu.table_name, kcu.constraint_name, tc.constraint_type
		order by kcu.table_name, kcu.constraint_name, tc.constraint_type;				
	'''
	private def fillConstraints(tables, objectName) {
		def rows
		if(objectName) {
			rows = sql.rows(CONSTRAINTS_QUERY_BY_NAME, [schema, objectName])
		} else {
			rows = sql.rows(CONSTRAINTS_QUERY,[schema])
		}

		rows.each({
			def tableName = it.table_name
			def table = tables[tableName]
			def constraint = new Constraint()
			constraint.name = it.constraint_name
			constraint.refTable = it.ref_table
			constraint.type = getConstraintType(it.constraint_type)
			def onDelete = it.delete_rule?.toLowerCase()
			constraint.onDelete = onDelete
			constraint.status = 'enabled'
			table.constraints[constraint.name] = constraint
		})
	}
	
	private def getConstraintType(String columnType){
		switch (columnType) {
			case "PRIMARY KEY":
				return "P"
				break
			case "UNIQUE":
				return "U"
				break
			case "FOREIGN KEY":
				return "R"
				break
			default:
				return columnType
		}
	}

	final static def CONSTRAINTS_COLUMNS_QUERY = '''
		select kcu.table_name, kcu.constraint_name, kcu.column_name, kcu.referenced_column_name as ref_column_name
		from information_schema.key_column_usage kcu
		inner join information_schema.columns c on c.column_name = kcu.column_name and c.table_name = kcu.table_name		
		where kcu.table_schema = ?
		and c.extra != 'auto_increment'
		group by kcu.table_name, kcu.constraint_name, kcu.column_name				
		order by kcu.table_name, kcu.constraint_name, kcu.ordinal_position;
	'''
	final static def CONSTRAINTS_COLUMNS_QUERY_BY_NAME = '''
		select kcu.table_name, kcu.constraint_name, kcu.column_name, kcu.referenced_column_name as ref_column_name
		from information_schema.key_column_usage kcu
		inner join information_schema.columns c on c.column_name = kcu.column_name and c.table_name = kcu.table_name		
		where kcu.table_schema = ?
		and kcu.table_name = ?
		and c.extra != 'auto_increment'
		group by kcu.table_name, kcu.constraint_name, kcu.column_name				
		order by kcu.table_name, kcu.constraint_name, kcu.ordinal_position;
	'''
	
	private def fillConstraintsColumns(tables, objectName) {
		def rows
		if(objectName) {
			rows = sql.rows(CONSTRAINTS_COLUMNS_QUERY_BY_NAME, [schema,objectName])
		} else {
			rows = sql.rows(CONSTRAINTS_COLUMNS_QUERY, [schema])
		}
		rows.each({
			def tableName = it.table_name
			def table = tables[tableName]
			def constraint = table.constraints[it.constraint_name]
			constraint.columns << it.column_name
			constraint.refColumns << it.ref_column_name
		})
	}

	def getSequences(objectName){
		return [:]
	}

	final static def VIEWS_QUERY = ''' 
		select table_name as view_name, view_definition as text 
		from information_schema.views where table_schema = ? order by table_name
	'''
	final static def VIEWS_QUERY_BY_NAME = ''' 
		select table_name as view_name, view_definition as text from information_schema.views
		where table_schema = ? and table_name = ? order by table_name
	'''
	
	def getViews(objectName){
		def views = [:]
		def rows
		if(objectName) {
			rows = sql.rows(VIEWS_QUERY_BY_NAME, [schema, objectName])
		} else {
			rows = sql.rows(VIEWS_QUERY, [schema])
		}

		rows.each({
			def view = new View()
			view.name = it.view_name
			view.text = it.text
			views[view.name] = view
		})
		return views
	}

	final static def PROCEDURES_NAME_QUERY = ''' 
		select distinct r.routine_name as name from `information_schema`.`routines` r
		where r.routine_type in ('FUNCTION' ,'PROCEDURE') and r.routine_schema = ? order by r.routine_name
	'''
	
	final static def PROCEDURES_NAME_QUERY_BY_NAME = ''' 
		select distinct r.routine_name as name from `information_schema`.`routines` r 
		where r.routine_type in ('FUNCTION' ,'PROCEDURE') and r.routine_schema = ? and r.routine_name = ? order by r.routine_name
	'''

	def getProcedures(objectName) {
		def procedures = [:]
		def rows
		if(objectName) {
			rows = sql.rows(PROCEDURES_NAME_QUERY_BY_NAME, [schema, objectName])
		} else {
			rows = sql.rows(PROCEDURES_NAME_QUERY, [schema])
		}

		rows.each({
			def procedure = new Procedure(name: it.name)
			procedures[procedure.name] = procedure
		})

		getProceduresBody(procedures, objectName)

		return procedures
	}

	final static def PROCEDURES_BODY_QUERY = ''' 
		select r.routine_name as name, r.routine_definition as text from `information_schema`.`routines` r 
		where r.routine_type in ('FUNCTION' ,'PROCEDURE') and r.routine_schema = ? order by r.routine_name
	'''
	
	final static def PROCEDURES_BODY_QUERY_BY_NAME = ''' 
		select r.routine_name as name, r.routine_definition as text from `information_schema`.`routines` r
		where r.routine_type in ('FUNCTION' ,'PROCEDURE') and r.routine_schema = ? and r.routine_name = ? order by r.routine_name
	'''
	
	def getProceduresBody(procedures, objectName) {
		def body = ''
		def name
		def rows

		if(objectName) {
			rows = sql.rows(PROCEDURES_BODY_QUERY_BY_NAME, [schema, objectName])
		} else {
			rows = sql.rows(PROCEDURES_BODY_QUERY, [schema])
		}

		rows.each({
			if (it.name != name) {
				if (name) {
					def procedure = procedures[name]
					if (procedure)
						procedure.text = body
				}
				name = it.name
				body = it.text
			}
			body += it.text
		})
		def procedure = procedures[name]
		if (procedure)
			procedure.text = body
	}

	def getPackages(objectName) {
		def packages = [:]
		return packages
	}

	final static def TRIGGERS_QUERY = '''  
		select t.trigger_name as name, t.action_statement as text from `information_schema`.`triggers` t
		where t.trigger_schema = ? order by t.trigger_name
	'''
	
	final static def TRIGGERS_QUERY_BY_NAME = ''' 
		select t.trigger_name as name, t.action_statement as text from `information_schema`.`triggers` t
		where t.trigger_schema = ? and t.trigger_name = ? order by t.trigger_name
	'''
	
	def getTriggers(objectName) {
		def triggers = [:]
		def rows

		if(objectName) {
			rows = sql.rows(TRIGGERS_QUERY_BY_NAME, [schema, objectName])
		} else {
			rows = sql.rows(TRIGGERS_QUERY, [schema])
		}

		rows.each({
			def triggerName = it.name
			def trigger = triggers[triggerName] ?: new Trigger()
			trigger.name = triggerName
			trigger.text += it.text
			triggers[triggerName] = trigger
		})

		return triggers
	}
}