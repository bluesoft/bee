# Bee [![Build Status](https://travis-ci.org/bluesoft/bee.svg)](https://travis-ci.org/bluesoft/bee) [![Coverage Status](https://coveralls.io/repos/bluesoft/bee/badge.svg)](https://coveralls.io/r/bluesoft/bee) [![Release](https://img.shields.io/github/release/bluesoft/bee.svg)](https://github.com/bluesoft/bee/releases)

<img src="http://bluesoft.github.io/bee/images/5186aefe.bee-logo.png" width="180px" />

Bee is a tool for managing database changes.

## How can Bee help you ?
See the [documentation](http://bluesoft.github.io/bee)

Easy install with brew:
`brew install bee`

## Completion files for the lazy:

If you are using Bash:
  Save the file `etc/bee_bash_completion` as `/etc/bash_completion.d/bee` and run `. /etc/bash_completion`.

If you are using Fish:
  Save the file `etc/bee_fish_completion` as `$HOME/.config/fish/completions/bee.fish`.

## Contributing

### Things we will merge

* Bugfixes
* Performance improvements
* Features and components which are likely to be useful to the majority of users

### Things we won't merge

* Code which introduces considerable performance degrations
* Features which are not important for most people
* Code which comes without tests
* Code which breaks existing tests

### Workflow

* Fork the repository
* Create a new branch in your fork (feature branch)
* Make sure all tests pass
* Create a pull request
