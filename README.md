<p align="right">
<img src="https://img.shields.io/maven-central/v/com.palantir.witchcraft.java.logging/witchcraft-logging-formatting" alt="Maven Central">
<a href="https://opensource.org/licenses/Apache-2.0"><img src="https://img.shields.io/badge/License-Apache%202.0-lightgrey.svg" alt="License"></a>
<a href="https://autorelease.general.dmz.palantir.tech/palantir/witchcraft-java-logging"><img src="https://img.shields.io/badge/Perform%20an-Autorelease-success.svg" alt="Autorelease"></a>
</p>

# Witchcraft Java Logging

Logging infrastructure for the Java language implementation of [witchcraft-api](https://github.com/palantir/witchcraft-api) logging functionality.

## Intellij Plugin
[![Version](https://img.shields.io/jetbrains/plugin/v/17344) ![Downloads](http://phpstorm.espend.de/badge/17344/downloads) ![Rating](https://img.shields.io/jetbrains/plugin/r/stars/17344)](https://plugins.jetbrains.com/plugin/17344-witchcraft-logging)

This repository provides an intellij plugin to parse and render witchcraft-api structured logs from IDE console output.

### Before the Intellij Plugin
![Unformatted](static/unformatted.png)

### Using the Intellij Plugin

_Dark Mode_
![Formatted Dark](static/formatted_dark.png)

_Light Mode_
![Formatted Light](static/formatted_light.png)

## Formatting Library

The `witchcraft-logging-formatting` library is used by the Intellij Plugin to parse and format structured logging, and meant
to be reused anywhere that needs to format structured output.

## Gradle Plugin

The `com.palantir.witchcraft-logging` gradle plugin applies all standard witchcraft-logging functionality, however the following plugins may be applied individually if needed:

* `com.palantir.witchcraft-logging-idea` Updates Intellij configurations so developers are prompted to install the [Intellij Plugin](#intellij-plugin).
* `com.palantir.witchcraft-logging-testreport` Filters and formats unit test report stdout and stderr for human-readable logging.test
