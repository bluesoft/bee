# Bee

A tool for managing database changes.
Today is embedded deployment tasks, along with validation tests.

Note: deprecated code is used by ANT, should be operated only version of MAVEN.

## Modules

* Schema - Validation of the structures of the tables, indexes, packages, etc.
* Data - Validation of core data tables.
* Dbchange - Making the change tables in the style of ruby migrations.

## How to Install

Download the binary distribution and unzip it. There are 2 folders: `bin` and `lib`.

Put your jdbc jar into the `lib` folder.

## Configuration

All modules used by the bee bee.properties default host the file containing the settings for connecting to the database. There may be various configurations of the same database file.

You can also specify an alternate configuration file using the-DdatabaseConfig = `<file>`.

     The file structure is as follows: 

          <conf>.driver=<driver>

          <conf>.url=<url>

          <conf>.user=<user>

          <conf>.password=<password>

          <conf> - configuration name to be used.
          <driver> - jdbc driver class.
          <url> - url of the database connection.
          <user> - user connection.
          <password> - connection password.

      Example:
            Configuring a delta.
                  delta.driver=oracle.jdbc.OracleDriver.
                  delta.url=jdbc:oracle:thin:@server:1111:delta.
                  delta.user=delta123.
                  delta.password=delta123.

Legend

  `<configuration>` -> name of the database.
  `<object>` -> name of the table.

## Schema

The module checks the schema structure of the database, comparing with existing files in the project. These files are in the bee project, inside specific subdirectories for each type of structure.

   * tables  -  structure of tables, indexes and constraints.
   * triggers  -  code of triggers.
   * packages  -  code packages and packages bodies.
   * procedures  -  procedures and functions.
   * views
   * sequences.bee  -  file containing all sequences.

### Commands

 To create a file .bee
   bee.sh schema:generate <configuration>
   bee.sh schema:generate <configuration> <object>

 To validate a file .bee
   bee.sh schema:validate <configuration>
   bee.sh schema:validate <configuration> <object>


## Data

The module checks the date the table core from database. The tables core are those reference and are not changed by the user.

Data files are in the directory bee/data, and are in CSV format.

### Commands

  To create a file data.
     bee.sh data:generate <configuration> <object>

  To validate all tables core of database.
     bee.sh data:validate <configuration>

  To validate one table core.
     bee.sh data:validate <configuration> <object>


## Dbchange

The Dbchange module performs the task of changing the database, following the concepts of ruby migrations. At the time of execution of the update module should check which scripts will be executed and then execute them in chronological order.


* Each file represents a file dbchange or more changes in the database and the file name follows the format:

    codigo timestamp + description + .dbchange


* The contents of the file consists of comments, update script and rollback script. example:

    -- comment script

    ::up

        command to update the database;
        command to update the database;
        command to update the database;


    ::down
        
        command to reverse the database;
        command to reverse the database;
        command to reverse the database;


Note:  when there is no rollback commands section: down should be removed, including its header. This will indicate to the bee there is no way to reverse the script.

### Commands

  To create dbchange.
     bee.sh dbchange:create <description of file>

  To verify status of dbchanges in database(dbchanges implemented and not implemented).
     bee.sh dbchange:status <configuration>

  To implement all dbchanges.  
     bee.sh dbchange:up <configuration>

  To implement one dbchanges(up)
     bee.sh dbchange:up <configuration> <name of file> Example: 1311201110120100-easter_egg.dbchange

  To implement one dbchanges(down)
     bee.sh dbchange:down <configuration> <name of file>


## Known Issues

* The `dbschema:generate` and `dbchema:validate` only works on Oracle databases
* Windows .bat script doesn't scan for lib jars, so you need to set the CLASSPATH variable manually.