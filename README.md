# sa-jdwp [![official JetBrains project](http://jb.gg/badges/official.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)
Java serviceablity agent to jdwp adapter

sa-jdi was removed from jdk 9, this library should replace it and allow to attach to a process from any java debugger

## Usage
* run `java -jar sa-jdwp.jar <pid> (port)`
* wait for `Waiting for debugger on..` message
* connect java debugger to the host/port provided

## Development prerequisites
* Set up java jdk version 1.6 named `1.6`, add tools.jar and sa-jda.jar to the classpath from jdk\lib folder
* Set up java jdk version 1.8 named `1.8`, add tools.jar and sa-jda.jar to the classpath from jdk\lib folder
* Set up java jdk version 10 named `10`
