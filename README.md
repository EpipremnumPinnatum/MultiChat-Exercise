# MultiChat
MultiChat is a server/client communication application that allows you to chat with multiple people at once.
It was made as a group project as part of the bachelor's degree in IT at the ZHAW in Zurich. We were provided
with the base code that had multiple issues. The issues were classified and documented as part of the exercise.


The program also has a search feature with which one can search through the text that appears in the chat window.
It was not clear what the requested implementation was. We saw it as a feature: The search function
includes the search for usernames, so we left it as is. In this way we can search for all the messages belonging to a 
client by filtering with his username.


## Requirements
- Java 17 or higher
- Gradle 7.6 or higher

## Running the program
Run the Server and Client from an IDE or from the command line using the following commands:
```
gradle server:run
``` 
And in a separate terminal window:
```
gradle client:run
```

## Issues
The issues have been split into two main categories:

- [Structural Issues](https://github.zhaw.ch/PM2-IT22tbZH-wahl-krea/uebung-hk1-verdiant-iselival-monterap/issues?q=is%3Aissue+label%3Astructural) - All issues that have to do with architecture, clean-code, JavaDoc, etc.
- [Functional Issues](https://github.zhaw.ch/PM2-IT22tbZH-wahl-krea/uebung-hk1-verdiant-iselival-monterap/issues?q=is%3Aissue+label%3Afunctional) - Issues related to functionality that doesn't work as specified.

View [All Open Issues](https://github.zhaw.ch/PM2-IT22tbZH-wahl-krea/uebung-hk1-verdiant-iselival-monterap/issues?q=is%3Aissue+is%3Aopen)

The following important structural/functional issues have been found:

### Structural Issues

- [Code duplication in connection handlers](https://github.zhaw.ch/PM2-IT22tbZH-wahl-krea/uebung-hk1-verdiant-iselival-monterap/issues/28)
- [processData method is too big](https://github.zhaw.ch/PM2-IT22tbZH-wahl-krea/uebung-hk1-verdiant-iselival-monterap/issues/15)
- [Create Configuration class](https://github.zhaw.ch/PM2-IT22tbZH-wahl-krea/uebung-hk1-verdiant-iselival-monterap/issues/8)
- [Create NetworkMessage class](https://github.zhaw.ch/PM2-IT22tbZH-wahl-krea/uebung-hk1-verdiant-iselival-monterap/issues/39)
- [Create Observer Pattern for MVC](https://github.zhaw.ch/PM2-IT22tbZH-wahl-krea/uebung-hk1-verdiant-iselival-monterap/issues/27)

### Functional Issues

- [Cannot connect multiple Clients](https://github.zhaw.ch/PM2-IT22tbZH-wahl-krea/uebung-hk1-verdiant-iselival-monterap/issues/31)
- [Seperate Thread for waiting on new messages](https://github.zhaw.ch/PM2-IT22tbZH-wahl-krea/uebung-hk1-verdiant-iselival-monterap/issues/23)
- [Block UI interactions when disconnected](https://github.zhaw.ch/PM2-IT22tbZH-wahl-krea/uebung-hk1-verdiant-iselival-monterap/issues/25)
- [Message field not reset when message sent](https://github.zhaw.ch/PM2-IT22tbZH-wahl-krea/uebung-hk1-verdiant-iselival-monterap/issues/32)
- [UI not updated when server shutdown](https://github.zhaw.ch/PM2-IT22tbZH-wahl-krea/uebung-hk1-verdiant-iselival-monterap/issues/40)

## Class diagram
![Class diagram](assets/ClassDiagram.png)

### Client
The Client implements the Model-View-Controller pattern, which allows for a better separation of concerns.
The Model is represented by ```ClientConnectionHandler```, which is responsible for all incoming and outgoing messages from the client to the server.
```Client``` is the class responsible for starting the JavaFX GUI. The View itself is in ```ChatWindow.fxml```. For the Controller we have ```ChatWindowController```, which is responsible for handling all user interactions with the GUI and acts as a bridge between the Model and View.

The client is multithreaded, as it has to wait for incoming messages from the server and for user input at the same time. This is achieved by using a ```ExecutorService```, which is responsible for waiting for incoming messages.

### Protocol
```ConnectionHandler``` provides the logic for sending and receiving messages. It is used by both the Client and the Server, in ```ClientConnectionHandler``` and ```ServerConnectionHandler``` respectively. The protocol is defined in ```NetworkHandler```, which is used by ```ConnectionHandler``` to parse incoming messages and to create outgoing messages.

```NetworkMessage``` is an object to efficiently store message data between client and server. It allows for easy access to each parameter of a message (sender, receiver, data type, message) and serialization to a string.

To eliminate code duplication, ```Configuration``` holds all enums and constants used by the client and server.

### Server
Like the client, the server is also multithreaded. All connections are listening for new messages in a separate thread, which is managed by a ```ExecutorService```.

```ServerConnectionHandler``` is responsible for all incoming and outgoing messages from the server to the client. It also manages the list of all clients connected to the server.
```Server``` is the class responsible for starting the server.

When the ```Server``` is interrupted, it shuts down all connections and stops listening for new connections, while also signaling all clients that the server is shutting down.

## Future enhancements
- implement channels (e.g. in Teams or Slack), so that clients can communicate on a specific topic. This would require the creation of a new class ```Channel``` and extending the ```NetworkMessage``` class.
- create a login system, so that clients can identify themselves. With the use of a database
- other use for a database is to store previous messages
- implement the ability to send emoticons, images or files. This could be done by extending the ```NetworkMessage``` class.

## Authors
- verdiant (Michael Verdile)
- iselival (Valentin Iseli)
- monterap (Raphael Monteiro)
