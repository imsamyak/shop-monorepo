# server

```
platform/    the backend: Spring Boot services and modules (Maven)
chatbot/     the shopping assistant (Node, Express)           (not added yet)
```

Each folder is its own process (or set of processes) with its own build. A service that other code calls gets a typed
client under `client/service-sdk/<service>`.
