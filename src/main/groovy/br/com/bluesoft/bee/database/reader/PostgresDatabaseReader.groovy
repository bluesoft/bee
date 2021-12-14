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

import br.com.bluesoft.bee.model.Constraint
import br.com.bluesoft.bee.model.Index
import br.com.bluesoft.bee.model.IndexColumn
import br.com.bluesoft.bee.model.Procedure
import br.com.bluesoft.bee.model.Schema
import br.com.bluesoft.bee.model.Sequence
import br.com.bluesoft.bee.model.Table
import br.com.bluesoft.bee.model.TableColumn
import br.com.bluesoft.bee.model.Trigger
import br.com.bluesoft.bee.model.View
import br.com.bluesoft.bee.util.VersionHelper

class PostgresDatabaseReader implements DatabaseReader {

    def sql

    PostgresDatabaseReader(def sql) {
        this.sql = sql
    }

    Schema getSchema(objectName = null) {
        def schema = new Schema()
        schema.databaseVersion = getDatabaseVersion()
        schema.tables = getTables(objectName, schema.databaseVersion)
        schema.sequences = getSequences(objectName)
        schema.views = getViews(objectName)
        schema.procedures = getProcedures(objectName)
        schema.packages = getPackages(objectName)
        schema.triggers = getTriggers(objectName)
        return schema
    }

    def getDatabaseVersion() {
        def databaseVersion = null
        try {
            databaseVersion = sql.rows('show server_version')[0].get('server_version')
        } catch (Exception e) {
            databaseVersion = sql.rows("select setting from pg_settings where name = 'server_version'")[0].get('setting')
        }
        databaseVersion = databaseVersion.split(" ")[0]
        return databaseVersion
    }

    def getTables(objectName, databaseVersion) {
        def tables = fillTables(objectName)
        fillColumns(tables, objectName)
        fillIndexes(tables, objectName, databaseVersion)
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
        if (objectName) {
            rows = sql.rows(TABLES_QUERY_BY_NAME, [objectName])
        } else {
            rows = sql.rows(TABLES_QUERY)
        }
        rows.each({
            def name = it.table_name.toLowerCase()
            def temporary = it.temporary == 'Y' ? true : false
            def comment = ''
            tables[name] = new Table(name: name, temporary: temporary, comment: comment)
        })
        return tables
    }


    static final def TABLES_COLUMNS_QUERY = '''
     	select ic.table_name, ic.column_name, ic.data_type, ic.is_nullable as nullable,
        case data_type
            when 'numeric' then ic.numeric_precision
            when 'character varying' then ic.character_maximum_length 
            when 'character' then ic.character_maximum_length 
            when '"char"' then ic.character_maximum_length 
            else 0
        end
		as data_size, is_generated,
		ic.numeric_scale as data_scale, coalesce(ic.column_default, ic.generation_expression) as data_default
		from information_schema.columns ic
		inner join information_schema.tables it on it.table_name = ic.table_name
		where ic.table_schema not in ('pg_catalog' , 'information_schema')
		and it.table_type = 'BASE TABLE\'
		order by ic.table_name, ic.ordinal_position;
		'''

    static final def TABLES_COLUMNS_QUERY_BY_NAME = '''
        select ic.table_name, ic.column_name, ic.data_type, ic.is_nullable as nullable,
        case data_type
            when 'numeric' then ic.numeric_precision
            when 'character varying' then ic.character_maximum_length 
            when 'character' then ic.character_maximum_length 
            when '"char"' then ic.character_maximum_length 
            else 0
        end
        as data_size, is_generated,
        ic.numeric_scale as data_scale, coalesce(ic.column_default, ic.generation_expression) as data_default
        from information_schema.columns ic
        inner join information_schema.tables it on it.table_name = ic.table_name
        where ic.table_schema not in ('pg_catalog' , 'information_schema')
        and it.table_type = 'BASE TABLE\'
        and ic.table_name = ?
        order by ic.table_name, ic.ordinal_position;
	'''

    private def fillColumns(tables, objectName) {
        def rows
        if (objectName) {
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
            column.nullable = it.nullable == 'NO' ? false : true
            column.virtual = it.is_generated == 'ALWAYS'
            def defaultValue = it.data_default
            if (defaultValue) {
                column.defaultValue = defaultValue?.trim()?.toUpperCase() == 'NULL' ? null : defaultValue?.trim()
            }
            table.columns[column.name] = column
        })
    }

    private def getColumnType(String oracleColumnType) {
        switch (oracleColumnType.toLowerCase()) {
            case "varchar2":
                return "varchar"
                break
            default:
                return oracleColumnType.toLowerCase()
        }
    }

    final static def INDEXES_QUERY = '''
		select n.nspname schemaname,  ct.relname as table_name, ci.relname as index_name, i.indisunique as uniqueness, 
		      am.amname as index_type, pg_get_indexdef(ci.oid, (i.keys).n, false) as column_name,
			  case when pg_index_column_has_property(ci.oid,1, 'asc') then 'asc' else 'desc' end as descend
		from pg_class ct 
		join pg_namespace n on (ct.relnamespace = n.oid) 
		join (
		      select i.indexrelid, i.indrelid, i.indoption, i.indisunique, i.indisclustered, i.indpred, i.indexprs, 
		      information_schema._pg_expandarray(i.indkey) as keys 
		      from pg_catalog.pg_index i
		     ) i on (ct.oid = i.indrelid) 
		join pg_class ci on (ci.oid = i.indexrelid) 
		join pg_am am on (ci.relam = am.oid)
		where n.nspname not in ('information_schema', 'pg_catalog', 'pg_toast')
		  and (n.nspname, ci.relname) not in (
		    select constraint_schema, constraint_name 
		    from information_schema.table_constraints)
		order by table_name, index_name, (i.keys).n
	'''

    final static def INDEXES_QUERY_BY_NAME = '''
		select n.nspname schemaname,  ct.relname as table_name, ci.relname as index_name, i.indisunique as uniqueness, 
		      am.amname as index_type, pg_get_indexdef(ci.oid, (i.keys).n, false) as column_name,
			  case when pg_index_column_has_property(ci.oid,1, 'asc') then 'asc' else 'desc' end as descend
		from pg_class ct 
		join pg_namespace n on (ct.relnamespace = n.oid) 
		join (
		      select i.indexrelid, i.indrelid, i.indoption, i.indisunique, i.indisclustered, i.indpred, i.indexprs, 
		      information_schema._pg_expandarray(i.indkey) as keys 
		      from pg_catalog.pg_index i
		     ) i on (ct.oid = i.indrelid) 
		join pg_class ci on (ci.oid = i.indexrelid) 
		join pg_am am on (ci.relam = am.oid)
		where n.nspname not in ('information_schema', 'pg_catalog', 'pg_toast')
		  and (n.nspname, ci.relname) not in (
		    select constraint_schema, constraint_name 
		    from information_schema.table_constraints)
  		and  ct.relname = ? 
		order by table_name, index_name, (i.keys).n	
	'''

    private def fillIndexes(tables, objectName, databaseVersion) {
        def rows = queryIndexesInInformationSchema(objectName, databaseVersion)
        rows.each({
            def tableName = it.table_name.toLowerCase()
            def table = tables[tableName]
            def indexName = it.index_name.toLowerCase()
            def indexAlreadyExists = table.indexes[indexName] ? true : false
            def index = null;
            if (indexAlreadyExists) {
                index = table.indexes[indexName]
            } else {
                index = new Index()
                index.name = indexName
                index.type = getIndexType(it.index_type)
                index.unique = it.uniqueness
                table.indexes[index.name] = index
            }
            def indexColumn = new IndexColumn()
            indexColumn.name = it.column_name.toLowerCase()
            indexColumn.descend = it.descend.toLowerCase() == 'desc' ? true : false
            index.columns << indexColumn
        })
    }

    private queryIndexesInInformationSchema(objectName, databaseVersion) {
        def rows = null
        if (databaseVersion != null) {
            if (objectName) {
                rows = sql.rows(INDEXES_QUERY_BY_NAME, [objectName])
            } else {
                rows = sql.rows(INDEXES_QUERY)
            }
        } else {
            if (objectName) {
                rows = sql.rows(INDEXES_QUERY_BY_NAME, [objectName])
            } else {
                rows = sql.rows(INDEXES_QUERY)
            }
        }
    }

    private def getIndexType(String indexType) {
        switch (indexType) {
            case "btree":
                return "n"
                break
            default:
                return indexType
        }
    }

    final static def CONSTRAINTS_QUERY = '''
        select tc.table_name, tc.constraint_name, nullif(ccu.table_name, tc.table_name) as ref_table, tc.constraint_type, 
               rc.delete_rule as delete_rule, 'enabled' as status, cc.check_clause
        from information_schema.table_constraints tc
             left join information_schema.referential_constraints rc using(constraint_catalog, constraint_schema, constraint_name)
             left join information_schema.constraint_column_usage ccu using (constraint_catalog, constraint_schema, constraint_name)
             left join information_schema.check_constraints cc using (constraint_catalog, constraint_schema, constraint_name)
        where constraint_schema not in ('pg_catalog', 'information_schema')
          and constraint_name not like '%_not_null\'
        order by tc.table_name, tc.constraint_type, tc.constraint_name	
    '''

    final static def CONSTRAINTS_QUERY_BY_NAME = '''
        select tc.table_name, tc.constraint_name, nullif(ccu.table_name, tc.table_name) as ref_table, tc.constraint_type, 
               rc.delete_rule as delete_rule, 'enabled' as status, cc.check_clause
        from information_schema.table_constraints tc
             left join information_schema.referential_constraints rc using(constraint_catalog, constraint_schema, constraint_name)
             left join information_schema.constraint_column_usage ccu using (constraint_catalog, constraint_schema, constraint_name)
             left join information_schema.check_constraints cc using (constraint_catalog, constraint_schema, constraint_name)
        where constraint_schema not in ('pg_catalog', 'information_schema')
          and constraint_name not like '%_not_null\'
          and tc.table_name = ?
        order by tc.table_name, tc.constraint_type, tc.constraint_name	
	'''

    private def fillCostraints(tables, objectName) {
        def rows
        if (objectName) {
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
            if (constraint.type == 'C') {
                constraint.refTable = null
                constraint.searchCondition = it.check_clause[2..-2]
            }

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
            case "check":
                return "C"
            default:
                return constraint_type
        }
    }

    final static def CONSTRAINTS_COLUMNS_QUERY = '''
        select tc.table_name, tc.constraint_name, kcu.column_name
        from information_schema.table_constraints tc
             join information_schema.key_column_usage kcu using(constraint_catalog, constraint_schema, constraint_name)
        where constraint_schema not in ('pg_catalog', 'information_schema')
          and constraint_name not like '%_not_null\'
        order by tc.table_name, tc.constraint_type, tc.constraint_name, kcu.ordinal_position
	'''
    final static def CONSTRAINTS_COLUMNS_QUERY_BY_NAME = '''
        select tc.table_name, tc.constraint_name, kcu.column_name
        from information_schema.table_constraints tc
             join information_schema.key_column_usage kcu using(constraint_catalog, constraint_schema, constraint_name)
        where constraint_schema not in ('pg_catalog', 'information_schema')
          and constraint_name not like '%_not_null\'
          and tc.table_name = ?
        order by tc.table_name, tc.constraint_type, tc.constraint_name, kcu.ordinal_position
	'''

    private def fillCostraintsColumns(tables, objectName) {
        def rows
        if (objectName) {
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

    def getSequences(objectName) {
        def sequences = [:]
        def rows
        if (objectName) {
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
        where table_schema not in ('information_schema', 'pg_catalog')
		order by view_name
	'''
    final static def VIEWS_QUERY_BY_NAME = '''
        select table_name as view_name, view_definition as text 
        from information_schema.views
        where table_schema not in ('information_schema', 'pg_catalog')
          and table_name = lower(?)
	'''

    def getViews(objectName) {
        def views = [:]
        def rows
        if (objectName) {
            rows = sql.rows(VIEWS_QUERY_BY_NAME, [objectName])
        } else {
            rows = sql.rows(VIEWS_QUERY)
        }

        rows.each({
            def view = new View()
            view.name = it.view_name.toLowerCase()
            view.text_postgres = it.text
            views[view.name] = view
        })
        return views
    }

    final static def PROCEDURES_NAME_QUERY = '''
		select distinct n.nspname, p.proname as name
		from pg_namespace n
		inner join pg_proc p on pronamespace = n.oid
		inner join pg_type pt on (pt.oid = p.prorettype)
		where n.nspname not like 'pg_%\'
		and n.nspname not in ('information_schema','pg_catalog','pg_toast')
		order by nspname, p.proname
	'''
    final static def PROCEDURES_NAME_QUERY_BY_NAME = '''
		select distinct n.nspname, p.proname as name
		from pg_namespace n
		join pg_proc p on pronamespace = n.oid
		inner join pg_type pt on (pt.oid = p.prorettype)
		where n.nspname not like 'pg_%'
		and n.nspname not in ('information_schema','pg_catalog','pg_toast')
		and p.proname = ?
		order by nspname, p.proname
'''

    def getProcedures(objectName) {
        def procedures = getProceduresBody(objectName)
        return procedures
    }

    final static def PROCEDURES_BODY_QUERY = '''
		select pn.nspname,
			pp.proname as name,
			pg_get_functiondef(pp.oid) as text
		from pg_proc pp
			inner join pg_namespace pn on (pp.pronamespace = pn.oid)
			inner join pg_type pt on (pt.oid = pp.prorettype)
			inner join pg_language pl on (pp.prolang = pl.oid)
		where pl.lanname NOT IN ('c','internal') 
			and pn.nspname NOT IN ('pg_catalog', 'information_schema')
		order by pn.nspname, pp.proname
	'''
    final static def PROCEDURES_BODY_QUERY_BY_NAME = '''
		select pn.nspname,
			pp.proname as name,
			pg_get_functiondef(pp.oid) as text
		from pg_proc pp
			inner join pg_namespace pn on (pp.pronamespace = pn.oid)
			inner join pg_type pt on (pt.oid = pp.prorettype)
			inner join pg_language pl on (pp.prolang = pl.oid)
		where pl.lanname NOT IN ('c','internal') 
			and pn.nspname NOT IN ('pg_catalog', 'information_schema')
			and pp.proname = ?
		order by pn.nspname, pp.proname
	'''

    def getProceduresBody(objectName) {
        def procedures = [:]
        def rows

        if (objectName) {
            rows = sql.rows(PROCEDURES_BODY_QUERY_BY_NAME, [objectName])
        } else {
            rows = sql.rows(PROCEDURES_BODY_QUERY)
        }

        rows.each({
            def schema = it.nspname == 'public' ? null : it.nspname
            def name = schema ? "${schema}.${it.name.toLowerCase()}" : it.name.toLowerCase()

            Procedure proc = procedures[name]
            if(proc) {
                proc.text_postgres += ";\n\n" + it.text
            } else {
                def procedure = new Procedure(name: it.name.toLowerCase(), text_postgres: it.text, schema: schema)
                procedures[name] = procedure
            }
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

        if (objectName) {
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
            trigger.text_postgres = text
            triggers[triggerName] = trigger
        })

        return triggers
    }

    def getPackages(objectName) {
        def packages = [:]
        return packages
    }

    def cleanupSchema(Schema schema) {
        schema.userTypes.clear()
        schema.packages.clear()
    }

}
