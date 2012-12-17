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
package br.com.bluesoft.bee.database.reader

import br.com.bluesoft.bee.model.*

class MySqlDatabaseReader implements DatabaseReader {

	def sql

	MySqlDatabaseReader (def sql) {
		this.sql = sql
	}

	Schema getSchema(objectName = null) {
		def schema = new Schema()
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
		fillCostraints(tables, objectName)
		fillCostraintsColumns(tables, objectName)
		return tables
	}

	static final def TABLES_QUERY = ''' 
		select ut.table_name , 'N' as temporary, ut.table_comment AS 'comments' 
		from information_schema.tables ut
		where ut.table_schema not in ('mysql', 'information_schema', 'performance_schema')	
	'''
	static final def TABLES_QUERY_BY_NAME = '''
		select ut.table_name , 'N' as temporary, ut.table_comment AS 'comments' 
		from information_schema.tables ut
		where ut.table_schema not in ('mysql', 'information_schema', 'performance_schema')
		and ut.table_name = ?
		order by table_name;
	'''
	private def fillTables(objectName) {
		def tables = [:]
		def rows
		if(objectName) {
			rows = sql.rows(TABLES_QUERY_BY_NAME, [objectName])
		} else {
			rows = sql.rows(TABLES_QUERY)
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
		select uc.table_name, uc.column_name, uc.data_type, uc.is_nullable as nullable, coalesce(uc.numeric_precision, uc.character_maximum_length) data_size, 
		coalesce(uc.numeric_scale, 0) data_scale, uc. column_default as data_default, uc.ordinal_position as column_id
		from information_schema.columns uc
		where uc.table_schema not in ('mysql', 'information_schema', 'performance_schema')
		order by table_name, column_id;
	'''
	static final def TABLES_COLUMNS_QUERY_BY_NAME = '''
		select uc.table_name, uc.column_name, uc.data_type, uc.is_nullable as nullable, coalesce(uc.numeric_precision, uc.character_maximum_length) data_size, 
		coalesce(uc.numeric_scale, 0) data_scale, uc. column_default as data_default, uc.ordinal_position as column_id
		from information_schema.columns uc
		where uc.table_schema not in ('mysql', 'information_schema', 'performance_schema')
		and uc.table_name = ?
		order by table_name, column_id;
	'''
	private def fillColumns(tables, objectName) {
		def rows
		if(objectName) {
			rows = sql.rows(TABLES_COLUMNS_QUERY_BY_NAME, [objectName])
		} else {
			rows = sql.rows(TABLES_COLUMNS_QUERY)
		}
		rows.each({
			def table = tables[it.table_name]
			def column = new TableColumn()
			column.name = it.column_name
			column.type = it.data_type
			column.size = it.data_size
			column.scale = it.data_scale
			column.nullable = it.nullable == 'NO' ? false : true
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
		where ui.table_schema not in ('mysql', 'information_schema', 'performance_schema')
		and (tc.constraint_type is null or tc.constraint_type = 'UNIQUE')
		group by ui.table_name, ui.index_name, ui.index_type, ui.non_unique
		order by ui.table_name, ui.index_name;
	'''
	final static def INDEXES_QUERY_BY_NAME = '''
		select ui.table_name, ui.index_name, ui.index_type, ui.non_unique as uniqueness FROM information_schema.statistics ui 
		left join information_schema.table_constraints tc on ui.index_name = tc.constraint_name 
		where ui.table_schema not in ('mysql', 'information_schema', 'performance_schema')
		and tc.constraint_type is null
		and  ui.table_name = ?
		group by ui.table_name, ui.index_name, ui.index_type, ui.non_unique
		order by ui.table_name, ui.index_name;
	'''

	private def fillIndexes(tables, objectName) {
		def rows
		if(objectName) {
			rows = sql.rows(INDEXES_QUERY_BY_NAME, [objectName])
		} else {
			rows = sql.rows(INDEXES_QUERY)
		}

		rows.each({
			def tableName = it.table_name
			def table = tables[tableName]
			def index = new Index()
			index.name = it.index_name
			index.type = it.index_type[0]
			index.unique = it.uniqueness == '1' ? true : false
			table.indexes[index.name] = index
		})
	}

	final static def INDEXES_COLUMNS_QUERY = '''
		select ui.table_name, ui.index_name, ui.column_name, c.column_default as data_default, 'asc' as descend FROM information_schema.statistics ui 
		left join information_schema.table_constraints tc on ui.index_name = tc.constraint_name 
		inner join information_schema.columns c on c.column_name = ui.column_name
		where ui.table_schema not in ('mysql', 'information_schema', 'performance_schema')
		and (tc.constraint_type is null or tc.constraint_type = 'UNIQUE')
		group by ui.index_name, ui.column_name
		order by ui.index_name, ui.seq_in_index
	'''
	final static def INDEXES_COLUMNS_QUERY_BY_NAME = '''
		select ui.table_name, ui.index_name, ui.column_name, c.column_default as data_default, 'asc' as descend FROM information_schema.statistics ui 
		left join information_schema.table_constraints tc on ui.index_name = tc.constraint_name 
		inner join information_schema.columns c on c.column_name = ui.column_name
		where ui.table_schema not in ('mysql', 'information_schema', 'performance_schema')
		and (tc.constraint_type is null or tc.constraint_type = 'UNIQUE')
		and ui.table_name = ?
		group by ui.index_name, ui.column_name
		order by ui.index_name, ui.seq_in_index
	'''
	private def fillIndexColumns(tables, objectName) {
		def rows
		if(objectName) {
			rows = sql.rows(INDEXES_COLUMNS_QUERY_BY_NAME , [objectName])
		} else {
			rows = sql.rows(INDEXES_COLUMNS_QUERY)
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
		select ui.table_name, ui.index_name as constraint_name, tc.constraint_type, rc.referenced_table_name as ref_table ,ui.index_name, rc.delete_rule FROM information_schema.statistics ui 
		inner join information_schema.table_constraints tc on ui.index_name = tc.constraint_name 
		inner join information_schema.columns c on c.column_name = ui.column_name
		left join information_schema.referential_constraints rc on rc.constraint_name = ui.index_name
		where ui.table_schema not in ('mysql', 'information_schema', 'performance_schema')
		and (tc.constraint_type is not null or tc.constraint_type = 'UNIQUE')
		group by ui.table_name, ui.index_name
		order by ui.index_name, ui.seq_in_index;
	'''
	final static def CONSTRAINTS_QUERY_BY_NAME = '''
		select ui.table_name, ui.index_name as constraint_name, tc.constraint_type, rc.referenced_table_name as ref_table ,ui.index_name, rc.delete_rule FROM information_schema.statistics ui 
		inner join information_schema.table_constraints tc on ui.index_name = tc.constraint_name 
		inner join information_schema.columns c on c.column_name = ui.column_name
		left join information_schema.referential_constraints rc on rc.constraint_name = ui.index_name
		where ui.table_schema not in ('mysql', 'information_schema', 'performance_schema')
		and (tc.constraint_type is not null or tc.constraint_type = 'UNIQUE')
		and ui.table_name = ?
		group by ui.table_name, ui.index_name
		order by ui.index_name, ui.seq_in_index;
	'''
	private def fillCostraints(tables, objectName) {
		def rows
		if(objectName) {
			rows = sql.rows(CONSTRAINTS_QUERY_BY_NAME, [objectName])
		} else {
			rows = sql.rows(CONSTRAINTS_QUERY)
		}

		rows.each({
			def tableName = it.table_name
			def table = tables[tableName]

			def constraint = new Constraint()
			constraint.name = it.constraint_name
			constraint.refTable = it.ref_table
			constraint.type = it.constraint_type
			def onDelete = it.delete_rule
			constraint.onDelete = onDelete == null ? null : onDelete
			table.constraints[constraint.name] = constraint
		})
	}

	final static def CONSTRAINTS_COLUMNS_QUERY = '''
		select ui.table_name, ui.index_name as constraint_name, ui.column_name FROM information_schema.statistics ui 
		inner join information_schema.table_constraints tc on ui.index_name = tc.constraint_name 
		inner join information_schema.columns c on c.column_name = ui.column_name
		where ui.table_schema not in ('mysql', 'information_schema', 'performance_schema')
		and (tc.constraint_type is not null or tc.constraint_type = 'UNIQUE')
		group by ui.index_name, ui.column_name
		order by ui.index_name, ui.seq_in_index;		
	'''
	final static def CONSTRAINTS_COLUMNS_QUERY_BY_NAME = '''
		select ui.table_name, ui.index_name as constraint_name, ui.column_name FROM information_schema.statistics ui 
		inner join information_schema.table_constraints tc on ui.index_name = tc.constraint_name 
		inner join information_schema.columns c on c.column_name = ui.column_name
		where ui.table_schema not in ('mysql', 'information_schema', 'performance_schema')
		and (tc.constraint_type is not null or tc.constraint_type = 'UNIQUE')
		and ui.table_name = ?
		group by ui.index_name, ui.column_name
		order by ui.index_name, ui.seq_in_index;
	'''
	private def fillCostraintsColumns(tables, objectName) {
		def rows
		if(objectName) {
			rows = sql.rows(CONSTRAINTS_COLUMNS_QUERY_BY_NAME, [objectName])
		} else {
			rows = sql.rows(CONSTRAINTS_COLUMNS_QUERY)
		}
		rows.each({
			def tableName = it.table_name
			def table = tables[tableName]
			def constraint = table.constraints[it.constraint_name]
			constraint.columns << it.column_name
		})
	}

	final static def SEQUENCES_QUERY = '''
			select us.sequence_name, us.min_value
			from   user_sequences us
			order  by sequence_name
		'''
	final static def SEQUENCES_QUERY_BY_NAME = '''
			select us.sequence_name, us.min_value
			from   user_sequences us
			where  sequence_name = upper(?)
			order  by sequence_name
		'''
	def getSequences(objectName){
		// Not exists in mysql
		def sequences = [:]
		return sequences
	}

	final static def VIEWS_QUERY = '''
		select table_name as view_name, view_definition as text
		from views
		order  by table_name;
	'''
	final static def VIEWS_QUERY_BY_NAME = '''
		select table_name as view_name, view_definition as text
		from views
		where table_name = ?
		order  by table_name;
	'''
	def getViews(objectName){
		def views = [:]
		def rows
		if(objectName) {
			rows = sql.rows(VIEWS_QUERY_BY_NAME, [objectName])
		} else {
			rows = sql.rows(VIEWS_QUERY)
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
		select distinct routine_name 
		from `information_schema`.`routines` 
		where routine_type in ('FUNCTION' ,'PROCEDURE')
		order by routine_name;
	'''
	final static def PROCEDURES_NAME_QUERY_BY_NAME = '''
		select distinct routine_name 
		from `information_schema`.`routines` 
		where routine_type in ('FUNCTION' ,'PROCEDURE')
		and routine_name = ?  
		order by routine_name;
	'''
	def getProcedures(objectName) {
		def procedures = [:]
		def rows
		if(objectName) {
			rows = sql.rows(PROCEDURES_NAME_QUERY_BY_NAME, [objectName])
		} else {
			rows = sql.rows(PROCEDURES_NAME_QUERY)
		}

		rows.each({
			def procedure = new Procedure(name: it.name)
			procedures[procedure.name] = procedure
		})

		getProceduresBody(procedures, objectName)

		return procedures
	}

	final static def PROCEDURES_BODY_QUERY = '''
		select routine_name, routine_definition
		from `information_schema`.`routines` 
		where routine_type in ('FUNCTION' ,'PROCEDURE')
		order by routine_name;
	'''
	final static def PROCEDURES_BODY_QUERY_BY_NAME = '''
		select routine_name, routine_definition
		from `information_schema`.`routines` 
		where routine_type in ('FUNCTION' ,'PROCEDURE')
		and routine_name = ?
		order by routine_name;
	'''
	def getProceduresBody(procedures, objectName) {
		def body = ''
		def name
		def rows

		if(objectName) {
			rows = sql.rows(PROCEDURES_BODY_QUERY_BY_NAME, [objectName])
		} else {
			rows = sql.rows(PROCEDURES_BODY_QUERY)
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
		select trigger_name as name, action_statement as text
		from `information_schema`.`triggers`
		order by trigger_name;
	'''
	final static def TRIGGERS_QUERY_BY_NAME = '''
		select trigger_name as name, action_statement as text
		from `information_schema`.`triggers`
		where trigger_name = ?
		order by trigger_name;
	'''
	def getTriggers(objectName) {
		def triggers = [:]
		def rows

		if(objectName) {
			rows = sql.rows(TRIGGERS_QUERY_BY_NAME, [objectName])
		} else {
			rows = sql.rows(TRIGGERS_QUERY)
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