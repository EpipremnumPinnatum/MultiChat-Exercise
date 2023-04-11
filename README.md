# MultiChat
MultiChat is a program that allows you to chat with multiple people at once.

## Requirements
- Java 17 or higher
- Gradle 7.6 or higher

## Running the program
Run the Server and Client from an IDE or from the command line using the following commands:
```
$ gradle server:run
``` 
And in a seperate terminal:
```
$ gradle client:run
```

## Main issues
### Functional
- [Cannot connect multiple Clients](https://github.zhaw.ch/PM2-IT22tbZH-wahl-krea/uebung-hk1-verdiant-iselival-monterap/issues/31)
- [Seperate Thread for waiting on new messages](https://github.zhaw.ch/PM2-IT22tbZH-wahl-krea/uebung-hk1-verdiant-iselival-monterap/issues/23)
- [Block UI interactions when disconnected](https://github.zhaw.ch/PM2-IT22tbZH-wahl-krea/uebung-hk1-verdiant-iselival-monterap/issues/25)
- [Message field not reset when message sent](https://github.zhaw.ch/PM2-IT22tbZH-wahl-krea/uebung-hk1-verdiant-iselival-monterap/issues/32)
- [UI not updated when server shutdown](https://github.zhaw.ch/PM2-IT22tbZH-wahl-krea/uebung-hk1-verdiant-iselival-monterap/issues/40)

### Structural
- [Code duplication in connection handlers](https://github.zhaw.ch/PM2-IT22tbZH-wahl-krea/uebung-hk1-verdiant-iselival-monterap/issues/28)
- [processData method is too big](https://github.zhaw.ch/PM2-IT22tbZH-wahl-krea/uebung-hk1-verdiant-iselival-monterap/issues/15)
- [Create Configuration class](https://github.zhaw.ch/PM2-IT22tbZH-wahl-krea/uebung-hk1-verdiant-iselival-monterap/issues/8)
- [Create NetworkMessage class](https://github.zhaw.ch/PM2-IT22tbZH-wahl-krea/uebung-hk1-verdiant-iselival-monterap/issues/39)
- [Create Observer Pattern for MVC](https://github.zhaw.ch/PM2-IT22tbZH-wahl-krea/uebung-hk1-verdiant-iselival-monterap/issues/27)

## Class diagram
![Class diagram](https://github.zhaw.ch/PM2-IT22tbZH-wahl-krea/uebung-hk1-verdiant-iselival-monterap/raw/master/ClassDiagram.png)

## Future enhancements
