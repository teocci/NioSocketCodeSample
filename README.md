## NIO Socket Code Sample

This is a very simple and straight-forward implementation of a Java application using socket programming for scalable systems.

### Disclaimer

This repository contains sample code intended to demonstrate the capabilities of using non-blocking asynchronous channels in the Java socket model. It is not intended to be used as-is in applications as a library dependency, and will not be maintained as such. Bug fix contributions are welcome, but issues and feature requests will not be addressed.

### Specifications

NIO Socket Code Sample has been implemented using the `AsynchronousServerSocketChannel` class, which provides a non-blocking asynchronous channel for stream-oriented listening sockets. 

To use it, first execute its static `open()` method and then `bind()` it to a specific **port**. Next, you'll execute its `accept()` method, passing to it a class that implements the `CompletionHandler` interface. Most often, you'll find that handler created as an *anonymous inner class*.

From this `AsynchronousServerSocketChannel` object, you invoke `accept()` to tell it to start listening for connections, passing to it a custom `CompletionHandler` instance. When we invoke `accept()`, it returns immediately. Note that this is different from the traditional blocking approach; whereas the `accept()` method **blocked until a client connected to it**, the `AsynchronousServerSocketChannel` `accept()` method handles it for you.


### Contributing
If you would like to contribute code, you can do so through GitHub by forking the repository and sending a pull request.
When submitting code, please make every effort to follow existing conventions and style in order to keep the code as readable as possible.

### Pre-requisites

* Java JDK 8

## License and third party libraries

The code supplied here is covered under the MIT Open Source License..