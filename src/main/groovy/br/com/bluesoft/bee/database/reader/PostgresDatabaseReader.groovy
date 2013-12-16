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
		def Map<Index, String> columnIndexesMap = fillIndexes(tables, objectName)
		fillIndexColumns(tables, objectName, columnIndexesMap)
		fillCostraints(tables, objectName)
		fillCostraintsColumns(tables, objectName)
		return tables
	}
	
	static final def TABLES_QUERY = ''' 
	    select t.table_name, 'N'as temporary 
		from information_schema.tables t
		where t.table_type = 'BASE TABLE' and table_schema not in ('pg_catalog', 'information_schema')
		order by table_name
	'''
	static final def TABLES_QUERY_BY_NAME = '''
	    select t.table_name, 'N'as temporary 
		from information_schema.tables t
		where t.table_type = 'BASE TABLE' and table_schema not in ('pg_catalog', 'information_schema')
		and t.table_name = ?
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
		inner join information_schema.tables it on it.table_name = ic.table_name
		where ic.table_schema not in ('pg_catalog' , 'information_schema')
		and it.table_type = 'BASE TABLE'
		order by ic.table_name, ic.ordinal_position
		'''
	static final def TABLES_COLUMNS_QUERY_BY_NAME = '''
		select ic.table_name, ic.column_name, ic.data_type, ic.is_nullable as nullable, ic.numeric_precision as data_size, 
			ic.numeric_scale as data_scale, ic.column_default as data_default
		from information_schema.columns ic
		inner join information_schema.tables it on it.table_name = ic.table_name
		where ic.table_schema not in ('pg_catalog' , 'information_schema')
		and it.table_type = 'BASE TABLE'
        and ic.table_name = ?
		order by ic.table_name, ic.ordinal_position
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
		    ix.indisunique as uniqueness,
			'n' as index_type,
			ix.indkey
		from
		    pg_class t,
		    pg_class i,
		    pg_index ix,
		    pg_attribute a,
		    pg_namespace n
		where
		    t.oid = ix.indrelid
		    and i.oid = ix.indexrelid
		    and a.attrelid = t.oid
		    and t.relkind = 'r'
		    and n.oid = i.relnamespace
		    AND n.nspname NOT IN ('pg_catalog')
		group by
		    t.relname,
		    i.relname,
		    ix.indisunique,
			ix.indkey
		order by
		    t.relname,
		    i.relname,
		    ix.indisunique
	'''
	final static def INDEXES_QUERY_BY_NAME = '''
		select
		    t.relname as table_name,
		    i.relname as index_name,
		    ix.indisunique as uniqueness,
			'n' as index_type,
			ix.indkey
		from
		    pg_class t,
		    pg_class i,
		    pg_index ix,
		    pg_attribute a,
		    pg_namespace n
		where
		    t.oid = ix.indrelid
		    and i.oid = ix.indexrelid
		    and a.attrelid = t.oid
		    and t.relkind = 'r'
		    and n.oid = i.relnamespace
		    and n.nspname not in ('pg_catalog')
			and t.relname = ? 
		group by
		    t.relname,
		    i.relname,
		    ix.indisunique,
			ix.indkey
		order by
		    t.relname,
		    i.relname,
		    ix.indisunique
	'''

	private def fillIndexes(tables, objectName) {
		def rows
		def Map<Index, String> columnIndexesMap = [:]
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
			columnIndexesMap.put(index, it.indkey.toString())
		})
		columnIndexesMap
	}

	final static def INDEXES_COLUMNS_QUERY = '''
		select t.relname as table_name, i.relname as index_name, a.attname as column_name, a.attnum 
			from pg_class t, pg_class i, pg_index ix, pg_attribute a, pg_namespace n
		where t.oid = ix.indrelid
		  and i.oid = ix.indexrelid
		  and n.oid = i.relnamespace
		  and a.attrelid = t.oid
		  and t.relname = ?
		  and a.attnum = ?
	'''
	
	private def fillIndexColumns(tables, objectName, columnIndexesMap) {
		def rows = []
		tables.each { tableKey, tableValue ->
			tableValue.indexes.each { indexKey, indexValue ->
				def indexMap = columnIndexesMap.find {it.key.name == indexKey}
				def indices = indexMap.value.split(' ')
				indices.each {
					rows.add(sql.rows(INDEXES_COLUMNS_QUERY, [tableKey, it.toInteger()]))
				}
			}
		}
		
		rows.each({
			it.each {
				def table = it.table_name.toLowerCase()
				def indexName = it.index_name.toLowerCase()
				def index = tables.get(table).indexes.get(indexName);
				def indexColumn = new IndexColumn()
				indexColumn.name = it.column_name.toLowerCase()
				indexColumn.descend = true
				index.columns << indexColumn
			}
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

	final static def CONSTRAINTS_QUERY = '''
		select tc.table_name, tc.constraint_name, ccu.table_name as ref_table, tc.constraint_type, rc.delete_rule as delete_rule, 'enabled' as status
		from information_schema.table_constraints tc
			left join information_schema.referential_constraints rc on tc.constraint_catalog = rc.constraint_catalog and tc.constraint_schema = rc.constraint_schema and tc.constraint_name = rc.constraint_name
			left join information_schema.constraint_column_usage ccu on rc.unique_constraint_catalog = ccu.constraint_catalog and rc.unique_constraint_schema = ccu.constraint_schema and rc.unique_constraint_name = ccu.constraint_name
		where lower(tc.constraint_type) in ('primary key','unique', 'foreign key')
	'''
	final static def CONSTRAINTS_QUERY_BY_NAME = '''
		select tc.table_name, tc.constraint_name, ccu.table_name as ref_table, tc.constraint_type, rc.delete_rule as delete_rule, 'enabled' as status
		from information_schema.table_constraints tc
			left join information_schema.referential_constraints rc on tc.constraint_catalog = rc.constraint_catalog and tc.constraint_schema = rc.constraint_schema and tc.constraint_name = rc.constraint_name
			left join information_schema.constraint_column_usage ccu on rc.unique_constraint_catalog = ccu.constraint_catalog and rc.unique_constraint_schema = ccu.constraint_schema and rc.unique_constraint_name = ccu.constraint_name
		where lower(tc.constraint_type) in ('primary key','unique', 'foreign key') and tc.table_name = ?
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
			constraint.type = getConstraintType(it.constraint_type.toLowerCase())
			def onDelete = it.delete_rule?.toLowerCase()
			constraint.onDelete = onDelete == 'no action' ? null : onDelete
			def status = it.status?.toLowerCase()
			constraint.status = status
			table.constraints[constraint.name] = constraint
		})
	}
	
	private getConstraintType(constraint_type) {
		switch (constraint_type) {
			case "primary key":
				return "P"
				break
			case "unique":
				return "U"
				break
			case "foreign key":
				return "R"
				break
			default:
				return constraint_type
		}
	} 

	final static def CONSTRAINTS_COLUMNS_QUERY = '''
		select tc.table_name, tc.constraint_name, kcu.column_name, ccu.table_name as ref_table, ccu.column_name as ref_field
		from information_schema.table_constraints tc 
			left join information_schema.key_column_usage kcu on tc.constraint_catalog = kcu.constraint_catalog and tc.constraint_schema = kcu.constraint_schema and tc.constraint_name = kcu.constraint_name
			left join information_schema.referential_constraints rc on tc.constraint_catalog = rc.constraint_catalog and tc.constraint_schema = rc.constraint_schema and tc.constraint_name = rc.constraint_name
			left join information_schema.constraint_column_usage ccu on rc.unique_constraint_catalog = ccu.constraint_catalog and rc.unique_constraint_schema = ccu.constraint_schema and rc.unique_constraint_name = ccu.constraint_name
		where lower(tc.constraint_type) in ('primary key','unique', 'foreign key')
	'''
	final static def CONSTRAINTS_COLUMNS_QUERY_BY_NAME = '''
		select tc.table_name, tc.constraint_name, kcu.column_name, ccu.table_name as ref_table, ccu.column_name as ref_field
		from information_schema.table_constraints tc 
			left join information_schema.key_column_usage kcu on tc.constraint_catalog = kcu.constraint_catalog and tc.constraint_schema = kcu.constraint_schema and tc.constraint_name = kcu.constraint_name
			left join information_schema.referential_constraints rc on tc.constraint_catalog = rc.constraint_catalog and tc.constraint_schema = rc.constraint_schema and tc.constraint_name = rc.constraint_name
			left join information_schema.constraint_column_usage ccu on rc.unique_constraint_catalog = ccu.constraint_catalog and rc.unique_constraint_schema = ccu.constraint_schema and rc.unique_constraint_name = ccu.constraint_name
		where lower(tc.constraint_type) in ('primary key','unique', 'foreign key') and tc.table_name = ? 
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
			select c.relname as sequence_name, '1' as min_value   
			from pg_class c 
			where c.relkind = 'S'
			order by c.relname
		'''
	final static def SEQUENCES_QUERY_BY_NAME = '''
			select c.relname as sequence_name, '1' as min_value   
			from pg_class c 
			where c.relkind = 'S' and c.relname = upper(?)
			order by c.relname
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
		order by view_name
	'''
	final static def VIEWS_QUERY_BY_NAME = '''
		select table_name as view_name, view_definition as text 
		from information_schema.views 
		where table_schema = 'public' and table_name = upper(?)
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
//		if(objectName) {
//			rows = sql.rows(PROCEDURES_NAME_QUERY_BY_NAME, [objectName])
//		} else {
//			rows = sql.rows(PROCEDURES_NAME_QUERY)
//		}
//
//		rows.each({
//			def procedure = new Procedure(name: it.name.toLowerCase())
//			procedures[procedure.name] = procedure
//		})
//
//		getProceduresBody(procedures, objectName)
//
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

//		if(objectName) {
//			rows = sql.rows(PROCEDURES_BODY_QUERY_BY_NAME, [objectName])
//		} else {
//			rows = sql.rows(PROCEDURES_BODY_QUERY)
//		}
//
//		rows.each({
//			if (it.name.toLowerCase() != name) {
//				if (name) {
//					def procedure = procedures[name]
//					if (procedure)
//						procedure.text = body
//				}
//				name = it.name.toLowerCase()
//				body = it.text
//			}
//			body += it.text
//		})
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

//		if(objectName) {
//			rows = sql.rows(TRIGGERS_QUERY_BY_NAME, [objectName])
//		} else {
//			rows = sql.rows(TRIGGERS_QUERY)
//		}
//
//		rows.each({
//			def triggerName = it.name.toLowerCase()
//			def trigger = triggers[triggerName] ?: new Trigger()
//			trigger.name = triggerName
//			trigger.text += it.text
//			triggers[triggerName] = trigger
//		})

		return triggers
	}
	
	def getPackages(objectName) {
		def packages = [:]
		return packages
	}
}
