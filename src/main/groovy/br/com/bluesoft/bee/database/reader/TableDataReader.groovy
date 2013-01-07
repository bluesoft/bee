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
package br.com.bluesoft.bee.database.reader;


public class TableDataReader {

	def sql

	def TableDataReader(def sql) {
		this.sql = sql
	}

	def buildQuery(def table) {
		def query = "select "
		table.columns.each {
			query +=  it.value.name + ","
		}
		query = (query.toString())[0..-2]
		query +=" from "
		query += table.name

		def primaryKey = null
		table.constraints.each {
			if(it.value.type == 'P')
				primaryKey = it.value
		}

		if(primaryKey) {
			query += " order by "
			primaryKey.columns.each { query += it + "," }
			query = query[0..-2]
		}

		return query
	}

	def getData(def table) {
		def query = buildQuery(table)

		def columnNames = []
		table.columns.each { columnNames << it.value.name }

		def result = []

		sql.eachRow(query, {
			def rowArray = []
			def row = it
			columnNames.each {
				rowArray << (row[it] as String)
			}

			result << rowArray
		})
		
		return result
	}
}
