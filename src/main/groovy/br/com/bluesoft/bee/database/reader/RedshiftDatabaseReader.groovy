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

class RedshiftDatabaseReader implements DatabaseReader {

	def sql

	RedshiftDatabaseReader (def sql) {
		this.sql = sql
	}

	Schema getSchema(objectName = null) {
		def schema = new Schema()
		schema.tables = getTables(objectName)
		schema.views = getViews(objectName)
		return schema
	}


	def getTables(objectName) {
		def tables = fillTables(objectName)
		fillColumns(tables, objectName)
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
		ic.numeric_scale as data_scale, ic.column_default as data_default,
		encoding,
		distkey,
		sortkey
		from information_schema.columns ic
		inner join information_schema.tables it on it.table_name = ic.table_name
		inner join pg_table_def ptd on ptd.tablename = it.table_name and ptd."column" = ic.column_name
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
		ic.numeric_scale as data_scale, ic.column_default as data_default,
		encoding,
		distkey,
		sortkey
		from information_schema.columns ic
		inner join information_schema.tables it on it.table_name = ic.table_name
		inner join pg_table_def ptd on ptd.tablename = it.table_name and ptd."column" = ic.column_name
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
			column.type = it.data_type.toLowerCase()
			column.size = it.data_size
			column.scale = it.data_scale == null ? 0 : it.data_scale
			column.nullable = it.nullable == 'N' ? false : true
			column.encoding = it.encoding
			column.distkey = it.distkey
			column.sortkey = it.sortkey
			def defaultValue = it.data_default
			if(defaultValue) {
				column.defaultValue = defaultValue?.trim()?.toUpperCase() == 'NULL' ? null : defaultValue?.trim()
			}
			table.columns[column.name] = column
		})
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
}
