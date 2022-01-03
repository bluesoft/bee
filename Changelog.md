# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
