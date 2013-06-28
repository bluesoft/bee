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