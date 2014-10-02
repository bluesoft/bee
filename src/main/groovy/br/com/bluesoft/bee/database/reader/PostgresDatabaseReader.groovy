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
		select ic.table_name, ic.column_name, ic.data_type, ic.is_nullable as nullable,
		case 
			when (ic.numeric_precision_radix is not null) then ic.numeric_precision
			when (ic.character_maximum_length is not null) then ic.character_maximum_length 
		end
		as data_size,
		ic.numeric_scale as data_scale, ic.column_default as data_default
		from information_schema.columns ic
		inner join information_schema.tables it on it.table_name = ic.table_name
		where ic.table_schema not in ('pg_catalog' , 'information_schema')
		and it.table_type = 'BASE TABLE'
		order by ic.table_name, ic.ordinal_position;
		'''

	static final def TABLES_COLUMNS_QUERY_BY_NAME = '''
		select ic.table_name, ic.column_name, ic.data_type, ic.is_nullable as nullable,
		case 
			when (ic.numeric_precision_radix is not null) then ic.numeric_precision
			when (ic.character_maximum_length is not null) then ic.character_maximum_length 
		end
		as data_size,
		ic.numeric_scale as data_scale, ic.column_default as data_default
		from information_schema.columns ic
		inner join information_schema.tables it on it.table_name = ic.table_name
		where ic.table_schema not in ('pg_catalog' , 'information_schema')
		and it.table_type = 'BASE TABLE'
		and ic.table_name = ?
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
			column.scale = it.data_scale == null ? 0 : it.data_scale
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
		select idx2.tablename as table_name,
		       i.relname as index_name,
		       idx.indisunique as uniqueness,
		       am.amname as index_type,
		       array(
		       select pg_get_indexdef(idx.indexrelid, k + 1, true)
		       from generate_subscripts(idx.indkey, 1) as k
		       order by k
		       ) as column_names
		from pg_index as idx
		join pg_class as i on i.oid = idx.indexrelid
		join pg_am as am on i.relam = am.oid
		join pg_namespace as ns on ns.oid = i.relnamespace and i.relname !~ '^(pg_|sql_)'
		inner join pg_indexes as idx2 on idx2.indexname = i.relname
	'''
	
	final static def INDEXES_QUERY_BY_NAME = '''
		select idx2.tablename as table_name,
		       i.relname as index_name,
		       idx.indisunique as uniqueness,
		       am.amname as index_type,
		       array(
		       select pg_get_indexdef(idx.indexrelid, k + 1, true)
		       from generate_subscripts(idx.indkey, 1) as k
		       order by k
		       ) as column_names
		from pg_index as idx
		join pg_class as i on i.oid = idx.indexrelid
		join pg_am as am on i.relam = am.oid
		join pg_namespace as ns on ns.oid = i.relnamespace and i.relname !~ '^(pg_|sql_)'
		inner join pg_indexes as idx2 on idx2.indexname = i.relname 
		where idx2.tablename = ?
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
			index.type = getIndexType(it.index_type)
			index.unique = it.uniqueness
			table.indexes[index.name] = index
			
			it.column_names.getArray().each { column_name ->
				def indexColumn = new IndexColumn()
				indexColumn.name = column_name.toLowerCase()
				indexColumn.descend = true
				index.columns << indexColumn
			}
		})

	}
	
	private def getIndexType(String indexType){
		switch (indexType) {
			case "btree":
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
		order by kcu.ordinal_position, kcu.position_in_unique_constraint
	'''
	final static def CONSTRAINTS_COLUMNS_QUERY_BY_NAME = '''
		select tc.table_name, tc.constraint_name, kcu.column_name, ccu.table_name as ref_table, ccu.column_name as ref_field
		from information_schema.table_constraints tc 
			left join information_schema.key_column_usage kcu on tc.constraint_catalog = kcu.constraint_catalog and tc.constraint_schema = kcu.constraint_schema and tc.constraint_name = kcu.constraint_name
			left join information_schema.referential_constraints rc on tc.constraint_catalog = rc.constraint_catalog and tc.constraint_schema = rc.constraint_schema and tc.constraint_name = rc.constraint_name
			left join information_schema.constraint_column_usage ccu on rc.unique_constraint_catalog = ccu.constraint_catalog and rc.unique_constraint_schema = ccu.constraint_schema and rc.unique_constraint_name = ccu.constraint_name
		where lower(tc.constraint_type) in ('primary key','unique', 'foreign key') and tc.table_name = ?
 		order by kcu.ordinal_position, kcu.position_in_unique_constraint
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
//			println 'before'
//			println it.text
//			view.text = it.text.replaceAll("\\r","\\r").replaceAll("\\n","\\n").replaceAll("\\t","\\t")
//			println 'after'
//			println view.text
			views[view.name] = view
		})
		return views
	}

	final static def PROCEDURES_NAME_QUERY = '''
		select p.proname as name
		from pg_namespace n
		inner join pg_proc p on pronamespace = n.oid
		inner join pg_type pt on (pt.oid = p.prorettype)
		where n.nspname not like 'pg_%'
		and n.nspname not in ('information_schema','pg_catalog','pg_toast')
		order by p.proname
	'''
	final static def PROCEDURES_NAME_QUERY_BY_NAME = '''
		select p.proname as name
		from pg_namespace n
		join pg_proc p on pronamespace = n.oid
		inner join pg_type pt on (pt.oid = p.prorettype)
		where n.nspname not like 'pg_%'
		and n.nspname not in ('information_schema','pg_catalog','pg_toast')
		and p.proname = ?
		order by p.pronames	
'''
	def getProcedures(objectName) {
		def procedures = getProceduresBody(objectName)
		return procedures
	}

	final static def PROCEDURES_BODY_QUERY = '''
		select 
			pp.proname as name,
			pg_get_functiondef(pp.oid) as text
		from pg_proc pp
			inner join pg_namespace pn on (pp.pronamespace = pn.oid)
			inner join pg_type pt on (pt.oid = pp.prorettype)
			inner join pg_language pl on (pp.prolang = pl.oid)
		where pl.lanname NOT IN ('c','internal') 
			and pn.nspname NOT LIKE 'pg_%'
			and pn.nspname <> 'information_schema'
		order by pp.proname
	'''
	final static def PROCEDURES_BODY_QUERY_BY_NAME = '''
		select 
			pp.proname as name,
			pg_get_functiondef(pp.oid) as text
		from pg_proc pp
			inner join pg_namespace pn on (pp.pronamespace = pn.oid)
			inner join pg_type pt on (pt.oid = pp.prorettype)
			inner join pg_language pl on (pp.prolang = pl.oid)
		where pl.lanname NOT IN ('c','internal') 
			and pn.nspname NOT LIKE 'pg_%'
			and pn.nspname <> 'information_schema'
			and pp.proname = ?
		order by pp.proname
	'''
	def getProceduresBody(objectName) {
		def procedures = [:]
		def rows

		if(objectName) {
			rows = sql.rows(PROCEDURES_BODY_QUERY_BY_NAME, [objectName])
		} else {
			rows = sql.rows(PROCEDURES_BODY_QUERY)
		}

		rows.each({
			def procedure = new Procedure(name: it.name.toLowerCase(), text: it.text)
			procedures[procedure.name] = procedure
		})
		return procedures
	}

	final static def TRIGGERS_QUERY = '''
		select 
			trigger_name as name, 
			action_timing, 
			array_to_string(array_agg(event_manipulation::text),' or ') as event_manipulation,
			event_object_table, 
			action_orientation, 
			action_statement 
		from information_schema.triggers
		group by trigger_name, action_timing, event_object_table, action_orientation, action_statement
		order by trigger_name, action_timing, event_object_table, action_orientation, action_statement
	'''
	final static def TRIGGERS_QUERY_BY_NAME = '''
		select 
			trigger_name as name, 
			action_timing, 
			array_to_string(array_agg(event_manipulation::text),' or ') as event_manipulation,
			event_object_table, 
			action_orientation, 
			action_statement 
		from information_schema.triggers
			where trigger_name = ?
		group by trigger_name, action_timing, event_object_table, action_orientation, action_statement
		order by trigger_name, action_timing, event_object_table, action_orientation, action_statement
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
			def text = ""
			text += "CREATE TRIGGER ${it.name}\n"
			text += "${it.action_timing} ${it.event_manipulation} ON ${it.event_object_table}\n"
			text += "FOR EACH ${it.action_orientation}\n"
			text += "${it.action_statement}\n"
			trigger.text = text
			triggers[triggerName] = trigger
		})

		return triggers
	}
	
	def getPackages(objectName) {
		def packages = [:]
		return packages
	}
}
