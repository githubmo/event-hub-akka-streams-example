# EventHub with Akka Stream example

This example uses Akka Streams in conjunction with EventHub library offered by [azure-sdk-for-java](https://github.com/Azure/azure-sdk-for-java).

Azure SDK for java offers asynchronous sets of APIs for their Azure services that are built upon [Project Reactor's Reactive Core](https://projectreactor.io).

This means that we have a set of fully compliant [reactive streams](https://www.reactive-streams.org) set of [APIs](https://github.com/reactive-streams/reactive-streams-jvm)
that are fully compatible with [Akka Stream](https://doc.akka.io/docs/akka/current/stream/reactive-streams-interop.html).