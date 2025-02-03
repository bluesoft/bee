# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.106] - 2025-02-03
### Added
- FAIL_ON_WARNINIG environment variable

## [1.105] - 2024-11-27
### Fixed
- comments on schema:recreate

## [1.104] - 2024-11-18
### Added
- materialized view support

## [1.103] - 2024-06-26
### Changed
- Changed schema:recreate_postgres to user copy instead of insert

## [1.102] - 2024-01-11
### Fixed
- Connection leak on DbChangeManager

## [1.101] - 2023-01-11
### Fixed
- Upgrade on version above 100

## [1.100] - 2023-01-09
### Fixed
- Commit on markAll

## [1.99] - 2022-11-18
### Added
- Transaction on each file on PostgreSQL
- DDL lock timeout on Oracle
### Changed
- Upgraded to groovy 4

## [1.98] - 2022-07-22
### Fixed
- Running dbchange without script

## [1.97] - 2022-02-21
## Fixed
- syntax error on alter table add foreign kye

## [1.96] - 2022-02-16
### Added
- on update foreign key option on postgresql

## [1.95] - 2022-02-03
### Fixed
- index - "where" attribute on oracle

## [1.94] - 2022-02-03
### Fixed
- Exception on schema:generate with postgresql extensions
### Changed
- included 'alter session set nls_timestamp_format' on schema:recreate_oracle

## [1.93] - 2022-01-24
### Fixed
- temporary tables on PostgreSQL
- text(0) datatype
- timestamp datatype
### Changed
- removed commits on schema:recreate_postgre

## [1.92] - 2022-01-12
### Added
- functional indexes on PostgreSQL
- partial indexes on PostgreSQL
### Fixed
- issues getting indexes and constratins info from PostgreSQL

## [1.91] - 2022-01-03
### Fixed
- data type with precision on schema:recreate
- order on get procedures on postgresql
- filter procedures from extensions on postgresql
- filter temporary table on postgresql

## [1.90] - 2021-12-30
### Added
- regex processing rules.json file for fields: columnDefaultOut. columnDefaultIn, checkConditionOut, checkConditionIn

## [1.89] - 2021-12-29
### Fixed
- wrong recreate procedure output

## [1.88] - 2021-12-28
### Fixed
- wrong recreate procedure output

## [1.87] - 2021-12-27
### Fixed
- oracle jdbc timestamp compliance

## [1.86]
### Fixed
- silent ignoring error when obtaining current version

## [1.85] - 2021-12-13
### Fixed
- wrong size of character columns

## [1.84] - 2021-11-18
### Fixed
- schema:recreate with char columns

## [1.83] - 2021-11-16
### Fixed
- schema:recreate with date columns
- NPE on schema:recerate procedure with no body

## [1.82] - 2021-10-28
### Fixed
- Method call for jdk 17

## [1.81] - 2021-09-30
### Added
- Virtual column for PostgreSQL
- Support for schema objects (simulates packages on PostgreSQL)

### Changed
- Fixed changelog style
- Upgraded to groovy 3
- Upgraded to gradle 6

## [1.59]
### Changed
- transforming warnings in errors 

## [1.58]
### Changed
- bug fix in oracle indexes read

## [1.43]
### Changed
- bug fixes in postgres support

## [1.42]
### Changed
- bug fix - dbchange:down executing empty down statements
- Added support for postgres (version >= 8.4)

## [1.41]
### Changed
- bug fix - schema:recreate_mysql not escaping strings in defaultValue

## [1.40]
### Changed
- bug fix - Single quotes duplicated in insert into query
- bug-fix - String fields not encapsulated with single quotes when value is number

## [1.37]
### Changed
- bug fix dbchange:markall in mysql

## [1.36]
### Changed
- bug fix schema:recreate_mysql duplicating primary key statement

## [1.35]
### Changed
- Added new function to validate if a constraint is enabled or not (Works only in Oracle)

## [1.35]
### Changed
- Added new function to validate if a constraint is enabled or not (Works only in Oracle)

## [1.34]
### Changed
- bug fix encoding problems
- bug fix metadata files generation 

## [1.33]
### Changed
- Added new function to create dbseeds (like ruby migrations).
- Updated funtion schema:recreate_oracle and schema:recreate_mysql, now creating insert DDL for core data.

## [1.32]
### Changed
- Added new function markAll to mark all archives. Good when you re-create a database with all changes applied to schema.
