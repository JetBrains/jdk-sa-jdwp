# sa-jdwp [![official JetBrains project](https://jb.gg/badges/official.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub) [![Download](https://api.bintray.com/packages/jetbrains/intellij-third-party-dependencies/sa-jdwp/images/download.svg) ](https://bintray.com/jetbrains/intellij-third-party-dependencies/sa-jdwp/_latestVersion)
Java serviceability agent to jdwp adapter

sa-jdi was removed from jdk 9, this library should replace it and allow to attach to a process from any java debugger

checked with jdk version 6 to 25

## Usage
* run `java -jar sa-jdwp.jar <pid> (port)`
* wait for `Waiting for debugger on..` message
* connect java debugger to the host/port provided

## Development prerequisites
You'll need jdks version 6, 8, 10 and 13.
To be able to do local build, create `gradle.properties` file in the project root folder with paths to jdks installations like this:
```
JDK_1_6=c:\\Program Files\\Java\\jdk1.6.0_38
JDK_1_8=c:\\Program Files\\Java\\jdk1.8.0_181
JDK_10=c:\\Program Files\\Java\\jdk-10.0.1
JDK_13=c:\\Program Files\\Java\\jdk-13
```
