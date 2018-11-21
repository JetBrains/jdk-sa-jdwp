# sa-jdwp [![official JetBrains project](http://jb.gg/badges/official.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)
Java serviceability agent to jdwp adapter

sa-jdi was removed from jdk 9, this library should replace it and allow to attach to a process from any java debugger

## Usage
* run `java -jar sa-jdwp.jar <pid> (port)`
* wait for `Waiting for debugger on..` message
* connect java debugger to the host/port provided

## Development prerequisites
To be able to do local build, create `gradle.properties` file in the project root folder with paths to jdks installations like this:
```
JDK_16=c:\\Program Files\\Java\\jdk1.6.0_38
JDK_18=c:\\Program Files\\Java\\jdk1.8.0_181
JDK_10=c:\\Program Files\\Java\\jdk-10.0.1
```
