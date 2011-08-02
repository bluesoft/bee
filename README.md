# Bee

A tool for managing database changes.

## How to Install

Download the binary distribution and unzip it. There are 2 folders: `bin` and `lib`.

Put your jdbc jar into the `lib` folder.

## Usage

## Known Issues

* The `dbschema:generate` and `dbchema:validate` only works on Oracle databases
* Windows .bat script doesn't scan for lib jars, so you need to set the CLASSPATH variable manually.