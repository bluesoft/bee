# Bee

A tool for managing database changes.

Note: the use with ANT is deprecated, the most recent version uses MAVEN.

## Modules

* Schema - Validation of the structures of the tables, indexes, packages, etc.
* Data - Validation of core data tables.
* Dbchange - Execute some change on tables, like "ruby migrations".

## How to Install

Download the binary distribution and unzip it. There are 2 folders: `bin` and `lib`.

Put your jdbc jar into the `lib` folder.

## Configuration

All modules use by the file "bee.properties" by default. This file contains the database connection settings. There may be several database configurations inside this file.

You can also specify an alternate configuration file using the -DdatabaseConfig = `<file>`.

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
            Configuring a database called 'delta'.
                  delta.driver=oracle.jdbc.OracleDriver
                  delta.url=jdbc:oracle:thin:@server:1111:delta
                  delta.user=delta123
                  delta.password=delta123

## Schema

The module checks the schema structure of the database, comparing with existing files in the project. These files are in the bee project, inside specific subdirectories for each type of structure.

   * tables - structure of tables, indexes and constraints.
   * triggers
   * packages
   * procedures - procedures and functions.
   * views
   * sequences.bee - file containing all sequences

### Commands

      To create a file
         bee schema:generate <database> <object>

      To create all files from a specific schema
         bee schema:generate <database>

      To validate one file
         bee schema:validate <database> <object>

      To validate all files
         bee schema:validate <database>

      To create a DDL script from local structure files. Will create a file named `bee.sql`
         bee schema:recreate <object>

## Data

This module checks the 'core' data from the database.

Core data files are in the directory bee/data, and use a CSV format.

### Commands

        To create a data file
            bee data:generate <database> <object>

        To validate all core tables
            bee data:validate <database>

        To validate one core table
            bee data:validate <database> <object>


## Dbchange

The Dbchange module performs the task of changing the database, following the concepts of ruby migrations. The update module will check which scripts to be executed and then execute them in chronological order.


Each file represents one or more changes in the database and the file name follows the format:

      code timestamp + description + .dbchange


The contents of the file consists of comments, update scripts and rollback scripts. Example:

    -- comment script

    ::up

        command to update the database;
        command to update the database;
        command to update the database;


    ::down
        
        command to reverse the database;
        command to reverse the database;
        command to reverse the database;


Note:  when there is no rollback commands section: down should be removed, including its header. This will indicate there is no way to reverse the script.

### Commands

        To create a dbchange
            bee dbchange:create <description of file>

        To verify the status of dbchanges in database (dbchanges implemented and not implemented).
            bee dbchange:status <database>

        To apply all dbchanges
            bee dbchange:up <database>

        To apply one dbchange (up)
            bee dbchange:up <database> <name of file> Example: 1311201110120100-easter_egg.dbchange

        To apply one dbchange (down)
            bee dbchange:down <database> <name of file>
            
        To mark a dbchange as implemented
        	bee dbchange:mark <database> <name of file>
        	
        To unmark a dbchange as implemented (or mark as not implemented)
        	bee dbchange:mark <database> <name of file>


## Known Issues

* The `dbschema:generate` and `dbchema:validate` only works on Oracle databases
* Windows .bat script doesn't scan for lib jars, so you need to set the CLASSPATH variable manually.