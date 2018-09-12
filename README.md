# sa-jpda [![official JetBrains project](http://jb.gg/badges/official.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)
Java serviceablity agent to JPDA adapter

sa-jdi was removed from jdk 9, this library should replace it and allow to attach to a process from any java debugger

## Development prerequisites
* Set up java jdk version 1.8 named `1.8`, add tools.jar and sa-jda.jar to the classpath from jdk\lib folder
* Set up java jdk version 10 named `10` (required for ClassesHelper version for jdk 10)
