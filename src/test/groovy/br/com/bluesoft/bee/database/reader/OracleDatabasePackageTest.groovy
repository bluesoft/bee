package br.com.bluesoft.bee.database.reader;

import static org.junit.Assert.*

import org.junit.Before
import org.junit.Test

public class OracleDatabasePackageTest {
	def reader

	@Before
	void 'set up'(){
		def packages = [
			[name:'PACKAGE1', type:'PACKAGE', text:'line1\n'],
			[name:'PACKAGE1', type:'PACKAGE', text:'line2\n'],
			[name:'PACKAGE1', type:'PACKAGE', text:'line3\n'],
			[name:'PACKAGE1', type:'PACKAGE BODY', text:'line pack1\n'],
			[name:'PACKAGE1', type:'PACKAGE BODY', text:'line pack2\n'],
			[name:'PACKAGE1', type:'PACKAGE BODY', text:'line pack3\n'],
			[name:'PACKAGE2', type:'PACKAGE', text:'line1\n'],
			[name:'PACKAGE2', type:'PACKAGE', text:'line2\n'],
			[name:'PACKAGE2', type:'PACKAGE', text:'line3\n'],
			[name:'PACKAGE2', type:'PACKAGE BODY', text:'line pack1\n'],
			[name:'PACKAGE2', type:'PACKAGE BODY', text:'line pack2\n'],
			[name:'PACKAGE2', type:'PACKAGE BODY', text:'line pack3\n'],
		]
		final def sql = [ rows: { query -> return packages } ]
		reader = new OracleDatabaseReader(sql)
	}

	@Test
	void 'it should fill the packages'() {
		final def packages = reader.getPackages()
		assertEquals 2, packages.size()
	}

	@Test
	void 'it should fill the package name, text and body'() {
		final def packages = reader.getPackages()
		def pack = packages['package1']
		assertEquals('package1', pack.name)
		assertEquals('line1\nline2\nline3\n', pack.text)
		assertEquals('line pack1\nline pack2\nline pack3\n', pack.body)
	}
}
