## 2011-07-01  Daniel Carneiro <daniel@bluesoft.com.br>

* Initial Release

1.32 
Added new function markAll to mark all archives. Good when you re-create a database with all changes applied to schema.
1.33
Added new function to create dbseeds (like ruby migrations).
Updated funtion schema:recreate_oracle and schema:recreate_mysql, now creating insert DDL for core data.
1.34
bug fix encoding problems
bug fix metadata files generation 
1.35
Added new function to validate if a constraint is enabled or not (Works only in Oracle)
1.36
bug fix schema:recreate_mysql duplicating primary key statement
1.37
bug fix dbchange:markall in mysql
1.40
bug fix - Single quotes duplicated in insert into query
bug-fix - String fields not encapsulated with single quotes when value is number
1.41
bug fix - schema:recreate_mysql not escaping strings in defaultValue
1.42
bug fix - dbchange:down executing empty down statements
Added support for postgres (version >= 8.4)
1.43
bug fixes in postgres support
1.58
bug fix in oracle indexes read
1.59
transforming warnings in errors 