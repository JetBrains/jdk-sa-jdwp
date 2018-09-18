# sa-jdwp [![official JetBrains project](http://jb.gg/badges/official.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)
Java serviceability agent to jdwp adapter

sa-jdi was removed from jdk 9, this library should replace it and allow to attach to a process from any java debugger

## Usage
* run `java -jar sa-jdwp.jar <pid> (port)`
* wait for `Waiting for debugger on..` message
* connect java debugger to the host/port provided

## Development prerequisites
* Set up env variable `JDK_16`, `JDK_18` and `JDK_10` pointing to the correcponding jdk installations
