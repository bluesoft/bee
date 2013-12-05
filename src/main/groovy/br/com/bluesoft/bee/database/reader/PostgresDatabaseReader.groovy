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

class PostgresDatabaseReader implements DatabaseReader {

	def sql

	PostgresDatabaseReader (def sql) {
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
	    select t.table_name, 'N'as t.temporary 
		from information_schema.tables t
		where t.table_type = 'BASE TABLE' and table_schema not in ('pg_catalog', 'information_schema')
		order by table_name
	'''
	static final def TABLES_QUERY_BY_NAME = '''
	    select t.table_name, 'N'as t.temporary 
		from information_schema.tables t
		where t.table_type = 'BASE TABLE' and table_schema not in ('pg_catalog', 'information_schema')
		and t.table_name = upper(?)
		order by table_name
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
			def name = it.table_name.toLowerCase()
			def temporary = it.temporary == 'Y' ? true : false
			def comment = '' 
			tables[name] = new Table(name:name, temporary:temporary, comment:comment)
		})
		return tables
	}


	static final def TABLES_COLUMNS_QUERY = '''
		select ic.table_name, ic.column_name, ic.data_type, ic.is_nullable as nullable, ic.numeric_precision as data_size, 
			ic.numeric_scale as data_scale, ic.column_default as data_default
		from information_schema.columns ic
		where ic.table_schema not in ('pg_catalog' , 'information_schema')
		order by ic.table_name, ic.ordinal_position;
		'''
	static final def TABLES_COLUMNS_QUERY_BY_NAME = '''
		select ic.table_name, ic.column_name, ic.data_type, ic.is_nullable as nullable, ic.numeric_precision as data_size, 
			ic.numeric_scale as data_scale, ic.column_default as data_default
		from information_schema.columns ic
		where ic.table_schema not in ('pg_catalog' , 'information_schema')
        and ic.table_name = upper(?)
		order by ic.table_name, ic.ordinal_position;
	'''
	private def fillColumns(tables, objectName) {
		def rows
		if(objectName) {
			rows = sql.rows(TABLES_COLUMNS_QUERY_BY_NAME, [objectName])
		} else {
			rows = sql.rows(TABLES_COLUMNS_QUERY)
		}	
		rows.each({
			def table = tables[it.table_name.toLowerCase()]
			def column = new TableColumn()
			column.name = it.column_name.toLowerCase()
			column.type = getColumnType(it.data_type)
			column.size = it.data_size
			column.scale = it.data_scale
			column.nullable = it.nullable == 'N' ? false : true
			def defaultValue = it.data_default
			if(defaultValue) {
				column.defaultValue = defaultValue?.trim()?.toUpperCase() == 'NULL' ? null : defaultValue?.trim()
			}
			table.columns[column.name] = column
		})
	}

	private def getColumnType(String oracleColumnType){
		switch (oracleColumnType.toLowerCase()) {
			case "varchar2":
				return "varchar"
				break
			default:
				return oracleColumnType.toLowerCase()
		}
	}
	
	final static def INDEXES_QUERY = '''
		select
		    t.relname as table_name,
		    i.relname as index_name,
		    ix.indisunique as uniqueness
		from
		    pg_class t,
		    pg_class i,
		    pg_index ix,
		    pg_attribute a
		where
		    t.oid = ix.indrelid
		    and i.oid = ix.indexrelid
		    and a.attrelid = t.oid
		    and a.attnum = ANY(ix.indkey)
		    and t.relkind = 'r'
		group by
		    t.relname,
		    i.relname,
		    ix.indisunique
		order by
		    t.relname,
		    i.relname,
		    ix.indisunique
	'''
	final static def INDEXES_QUERY_BY_NAME = '''
		select ui.table_name, ui.index_name, ui.index_type, uniqueness
		from   user_indexes ui
			   left join user_constraints uc on ui.index_name = uc.index_name
		where  uc.index_name is null
		  and  index_type <> 'LOB'
		  and  ui.table_name = upper(?)
		order  by table_name, index_name

		select
		    t.relname as table_name,
		    i.relname as index_name,
		    ix.indisunique as uniqueness
		from
		    pg_class t,
		    pg_class i,
		    pg_index ix,
		    pg_attribute a
		where
		    t.oid = ix.indrelid
		    and i.oid = ix.indexrelid
		    and a.attrelid = t.oid
		    and a.attnum = ANY(ix.indkey)
		    and t.relkind = 'r'
		   and t.relname = upper(?)
		group by
		    t.relname,
		    i.relname,
		    ix.indisunique
		order by
		    t.relname,
		    i.relname,
		    ix.indisunique
	'''

	private def fillIndexes(tables, objectName) {
		def rows
		if(objectName) {
			rows = sql.rows(INDEXES_QUERY_BY_NAME, [objectName])
		} else {
			rows = sql.rows(INDEXES_QUERY)
		}

		rows.each({
			def tableName = it.table_name.toLowerCase()
			def table = tables[tableName]
			def index = new Index()
			index.name = it.index_name.toLowerCase()
			index.type = it.index_type[0].toLowerCase()
			index.unique = it.uniqueness == 'UNIQUE' ? true : false
			table.indexes[index.name] = index
		})
	}

	final static def INDEXES_COLUMNS_QUERY = '''
		select uic.table_name, uic.index_name, uic.column_name column_name, utc.data_default,
			   uic.descend
		from   user_ind_columns uic
			   join user_indexes ui on ui.index_name = uic.index_name
			   left join user_constraints uc on uic.index_name = uc.index_name
			   join user_tab_cols utc on (uic.column_name = utc.column_name and uic.table_name = utc.table_name)
		where  uc.index_name is null
		order  by uic.index_name, uic.column_position
	'''
	final static def INDEXES_COLUMNS_QUERY_BY_NAME = '''
		select uic.table_name, uic.index_name, uic.column_name column_name, utc.data_default,
			   uic.descend
		from   user_ind_columns uic
			   join user_indexes ui on ui.index_name = uic.index_name
			   left join user_constraints uc on uic.index_name = uc.index_name
			   join user_tab_cols utc on (uic.column_name = utc.column_name and uic.table_name = utc.table_name)
		where  uc.index_name is null
		  and  ui.table_name = upper(?)
		order  by uic.index_name, uic.column_position
	'''
	private def fillIndexColumns(tables, objectName) {
		def rows
		if(objectName) {
			rows = sql.rows(INDEXES_COLUMNS_QUERY_BY_NAME , [objectName])
		} else {
			rows = sql.rows(INDEXES_COLUMNS_QUERY)
		}

		rows.each({

			def table = tables[it.table_name.toLowerCase()]
			def index = table.indexes[it.index_name.toLowerCase()]

			def indexColumn = new IndexColumn()
			indexColumn.name = it.column_name?.startsWith('SYS_NC') ? it.data_default?.trim() : it.column_name.toLowerCase()
			indexColumn.descend = it.descend.toLowerCase() == 'asc' ? false : true

			index.columns << indexColumn
		})
	}

	final static def CONSTRAINTS_QUERY = '''
		select uc.table_name, uc.constraint_name, uc.constraint_type, uc2.table_name ref_table,
		   uc.index_name, uc.delete_rule, uc.status
		from   user_constraints uc
			   left join user_constraints uc2 on uc.r_constraint_name = uc2.constraint_name
			   join user_tables ut on uc.table_name = ut.table_name
		where  uc.constraint_type <> 'C'
		order  by uc.table_name, uc.constraint_type, uc.constraint_name
	'''
	final static def CONSTRAINTS_QUERY_BY_NAME = '''
		select uc.table_name, uc.constraint_name, uc.constraint_type, uc2.table_name ref_table,
		   uc.index_name, uc.delete_rule, uc.status
		from   user_constraints uc
			   left join user_constraints uc2 on uc.r_constraint_name = uc2.constraint_name
			   join user_tables ut on uc.table_name = ut.table_name
		where  uc.constraint_type <> 'C'
		  and  uc.table_name = upper(?)
		order  by uc.table_name, uc.constraint_type, uc.constraint_name
	'''
	private def fillCostraints(tables, objectName) {
		def rows
		if(objectName) {
			rows = sql.rows(CONSTRAINTS_QUERY_BY_NAME, [objectName])
		} else {
			rows = sql.rows(CONSTRAINTS_QUERY)
		}

		rows.each({
			def tableName = it.table_name.toLowerCase()
			def table = tables[tableName]

			def constraint = new Constraint()
			constraint.name = it.constraint_name.toLowerCase()
			constraint.refTable = it.ref_table?.toLowerCase()
			constraint.type = it.constraint_type
			def onDelete = it.delete_rule?.toLowerCase()
			constraint.onDelete = onDelete == 'no action' ? null : onDelete
			def status = it.status?.toLowerCase()
			constraint.status = status
			table.constraints[constraint.name] = constraint
		})
	}

	final static def CONSTRAINTS_COLUMNS_QUERY = '''
		select ucc.table_name, ucc.constraint_name, ucc.column_name
		from   user_cons_columns ucc
			   join user_constraints uc on ucc.constraint_name = uc.constraint_name
			   join user_tables ut on uc.table_name = ut.table_name
		where  uc.constraint_type <> 'C'
		order  by ucc.table_name, ucc.constraint_name, ucc.position
	'''
	final static def CONSTRAINTS_COLUMNS_QUERY_BY_NAME = '''
		select ucc.table_name, ucc.constraint_name, ucc.column_name
		from   user_cons_columns ucc
			   join user_constraints uc on ucc.constraint_name = uc.constraint_name
			   join user_tables ut on uc.table_name = ut.table_name
		where  uc.constraint_type <> 'C'
		  and  uc.table_name = upper(?)
		order  by ucc.table_name, ucc.constraint_name, ucc.position
	'''
	private def fillCostraintsColumns(tables, objectName) {
		def rows
		if(objectName) {
			rows = sql.rows(CONSTRAINTS_COLUMNS_QUERY_BY_NAME, [objectName])
		} else {
			rows = sql.rows(CONSTRAINTS_COLUMNS_QUERY)
		}
		rows.each({
			def tableName = it.table_name.toLowerCase()
			def table = tables[tableName]
			def constraint = table.constraints[it.constraint_name.toLowerCase()]
			constraint.columns << it.column_name.toLowerCase()
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
		def sequences = [:]
		def rows
		if(objectName) {
			rows = sql.rows(SEQUENCES_QUERY_BY_NAME, [objectName])
		} else {
			rows = sql.rows(SEQUENCES_QUERY)
		}
		rows.each({
			def sequence = new Sequence()
			sequence.name = it.sequence_name.toLowerCase()
			sequence.minValue = it.min_value
			sequences[sequence.name] = sequence
		})
		return sequences
	}

	final static def VIEWS_QUERY = '''
		select table_name as view_name, view_definition as text 
		from information_schema.views 
		where table_schema = 'public'
		order by view_name;
	'''
	final static def VIEWS_QUERY_BY_NAME = '''
		select table_name as view_name, view_definition as text 
		from information_schema.views 
		where table_schema = 'public' and view_name = upper(?)
		order by view_name
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
			view.name = it.view_name.toLowerCase()
			view.text = it.text
			views[view.name] = view
		})
		return views
	}

	final static def PROCEDURES_NAME_QUERY = '''
		select distinct name
		from user_source
		where type in ('PROCEDURE', 'FUNCTION')
	'''
	final static def PROCEDURES_NAME_QUERY_BY_NAME = '''
		select distinct name
		from user_source
		where type in ('PROCEDURE', 'FUNCTION')
		  and name = upper(?)
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
			def procedure = new Procedure(name: it.name.toLowerCase())
			procedures[procedure.name] = procedure
		})

		getProceduresBody(procedures, objectName)

		return procedures
	}

	final static def PROCEDURES_BODY_QUERY = '''
		select name, text
		from user_source
		where type in ('PROCEDURE', 'FUNCTION')
		order by name, line	
	'''
	final static def PROCEDURES_BODY_QUERY_BY_NAME = '''
		select name, text
		from user_source
		where type in ('PROCEDURE', 'FUNCTION')
		  and name = upper(?)
		order by name, line	
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
			if (it.name.toLowerCase() != name) {
				if (name) {
					def procedure = procedures[name]
					if (procedure)
						procedure.text = body
				}
				name = it.name.toLowerCase()
				body = it.text
			}
			body += it.text
		})
		def procedure = procedures[name]
		if (procedure)
			procedure.text = body
	}

	final static def TRIGGERS_QUERY = '''
		SELECT trigger_name as name, action_statement as text 
		FROM information_schema.triggers 
		where trigger_schema = 'public'
		order by name
	'''
	final static def TRIGGERS_QUERY_BY_NAME = '''
		SELECT trigger_name as name, action_statement as text 
		FROM information_schema.triggers 
		where trigger_schema = 'public'
		and  name = upper(?)
		order by name
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
			def triggerName = it.name.toLowerCase()
			def trigger = triggers[triggerName] ?: new Trigger()
			trigger.name = triggerName
			trigger.text += it.text
			triggers[triggerName] = trigger
		})

		return triggers
	}
}
