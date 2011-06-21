package br.com.bluesoft.bee.database.reader;

import static org.junit.Assert.*
import static org.mockito.Mockito.*

import org.junit.Before
import org.junit.Test

public class OracleDatabaseReaderViewTest {

	def reader

	@Before
	void 'set up'(){
		def views = [
			[view_name:'VIEW_MENU', text:'aaa'],
			[view_name:'VIEW_USERS', text:'bbb']
		]
		def sql = [ rows: { query -> views  } ]
		reader = new OracleDatabaseReader(sql)
	}

	@Test
	void 'it should fill the views'() {
		def sequences = reader.getViews(null)
		assertEquals 2, sequences.size()
	}

	@Test
	void 'it should fill the view name'() {
		def sequences = reader.getViews(null)
		assertEquals 'view_menu', sequences['view_menu'].name
		assertEquals 'view_users', sequences['view_users'].name
	}
}
