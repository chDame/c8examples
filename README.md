# Camunda 8 Examples

A collection of practical examples built on top of [Camunda 8](https://camunda.com/) that demonstrate real-world integration patterns using a **Spring Boot** backend and **Angular** frontends.

---

## Architecture

```
┌─────────────────────────────────────────┐
│  Spring Boot 3 (port 8081)              │
│                                         │
│  ┌─────────────┐  ┌──────────────────┐  │
│  │  Chatbot    │  │  Synchronous API │  │
│  │  Example    │  │  Wrapper Example │  │
│  └──────┬──────┘  └────────┬─────────┘  │
│         │                  │            │
│         └────────┬─────────┘            │
│                  │                      │
│           CamundaClient                 │
└──────────────────┼──────────────────────┘
                   │  gRPC / REST
           ┌───────▼────────┐
           │  Camunda 8     │
           │  (Zeebe + AI)  │
           └────────────────┘

Angular frontend (served as static assets from Spring Boot):
  /chatbot  →  Chatbot UI
```

- The **backend** is a single Spring Boot application that hosts both examples.
- The **Angular** frontend for the chatbot is built and served as static files directly from Spring Boot.
- An embedded **H2** database (file-based) is used for session persistence — no additional database setup is required.
- All BPMN and DMN process definitions are automatically deployed to Camunda 8 on application startup via the `@Deployment` annotation.

---

## Prerequisites

- Java 21+
- Maven 3.9+
- Node.js 18+ and npm 9+
- A running **Camunda 8** cluster (SaaS or self-managed)

---

## Configuration

The application reads its configuration from environment variables. Set the following before running:

| Variable | Description |
|---|---|
| `CLIENT_CLOUD_CLUSTER_ID` | Your Camunda cluster ID |
| `CLIENT_CLOUD_REGION` | The region your cluster is in (e.g. `bru-2`) |
| `CAMUNDA_DOMAIN` | The base domain (e.g. `camunda.io`) |
| `CLIENT_CLOUD_CLIENT_ID` | OAuth client ID for Zeebe access |
| `CLIENT_CLOUD_CLIENT_SECRET` | OAuth client secret for Zeebe access |
| `PORT` | HTTP port the server listens on (default: `8081`) |

These variables are referenced in `src/main/resources/application.yaml`:

```yaml
camunda:
  client:
    cluster-id: ${CLIENT_CLOUD_CLUSTER_ID}
    region: ${CLIENT_CLOUD_REGION}
    domain: ${CAMUNDA_DOMAIN}
    auth:
      client-id: ${CLIENT_CLOUD_CLIENT_ID}
      client-secret: ${CLIENT_CLOUD_CLIENT_SECRET}
```

Example (bash):
```bash
export CLIENT_CLOUD_CLUSTER_ID=my-cluster-id
export CLIENT_CLOUD_REGION=bru-2
export CAMUNDA_DOMAIN=camunda.io
export CLIENT_CLOUD_CLIENT_ID=my-client-id
export CLIENT_CLOUD_CLIENT_SECRET=my-client-secret
```

---

## Building and Running

The project uses a `Makefile` to orchestrate the build. The main targets are:

| Target | Description |
|---|---|
| `make all` | Build the Angular frontend and start the Spring Boot server |
| `make buildall` | Build the frontend and package the application as a JAR |
| `make buildfront` | Build the Angular app and copy it to Spring Boot's static resources |
| `make run` | Start the Spring Boot application (`mvn spring-boot:run`) |
| `make runfront` | Run the Angular development server independently |
| `make npminstall` | Install npm dependencies for the Angular project |

**Quick start** (build everything and run):
```bash
make all
```

The first run will install npm dependencies automatically. After that, the application is available at [http://localhost:8081](http://localhost:8081).

---

## Examples

### 1. Chatbot

**URL:** [http://localhost:8081/chatbot](http://localhost:8081/chatbot)

A conversational AI banking assistant that can handle a wide range of customer requests (account queries, jokes, general questions) as well as specific banking operations like loan applications. The chatbot demonstrates how to **restrict certain tools to specific users** using a DMN decision table:

- **Joker** cannot request a loan.
- **Batman** cannot request a joke.

To try this out, log in with the username `joker` or `batman` and ask about loans or jokes respectively.

#### How it works

The chatbot is built on top of a Camunda 8 agentic AI process defined in `modeler/chatbot.bpmn`. Each conversation maps 1-to-1 to a running process instance.

**Flow:**

```
Browser                  Spring Boot                  Camunda 8
  │                           │                           │
  │── POST /api/chatbot ──────►│                           │
  │                           │── startInstance() ───────►│
  │◄── { sessionId } ─────────│◄── processInstanceKey ────│
  │                           │                           │
  │── GET /api/chatbot/        │                           │
  │   chat-sse/{sessionId} ──►│ (SSE connection open)     │
  │                           │                           │
  │                           │◄── job: chatbotThinking ──│
  │◄── SSE: thinking msg ─────│                           │
  │                           │                           │
  │                           │◄── job: chatbotReply ─────│
  │◄── SSE: reply + jobKey ───│                           │
  │                           │                           │
  │── POST /api/chatbot/       │                           │
  │   userInput/{sessionId} ──►│── completeJob(jobKey) ──►│
  │   body: { jobKey, msg }    │                           │
  │                           │                     (process resumes)
```

**Key concepts:**

- **`sessionId`** — A UUID generated when a new chat starts. It is passed as a process variable into the BPMN process and included in every message sent to the frontend. It is the single link between the HTTP session, the SSE stream, and the running process instance.

- **Server-Sent Events (SSE)** — The backend uses Spring's `SseEmitter` to push messages to the browser as the AI process progresses. This avoids polling and gives a real-time streaming feel. The frontend opens an `EventSource` connection immediately after starting a new chat and listens for messages.

- **Hospital pattern** — If the SSE connection is not yet established when the process sends a message (e.g. due to network timing), messages are buffered in memory and flushed as soon as the client connects.

- **`jobKey`** — When the process needs user input (e.g. asking a follow-up question), the job is *not* auto-completed. Instead, the `jobKey` is sent to the frontend inside the SSE message. The frontend stores the key and sends it back with the user's next message. The backend then calls `completeJob(jobKey)` to resume the process.

- **Access control via DMN** — The `Policy.dmn` decision table is evaluated inside the process before calling certain tools. If the decision returns `"rejected"`, the tool call is skipped and the user receives an appropriate message.

#### Code structure

| Layer | Path | Description |
|---|---|---|
| Controller | `chatbot/controller/ChatController.java` | REST and SSE endpoints |
| Facade | `chatbot/facade/ChatbotFacade.java` | Orchestrates sessions, SSE emitters, hospital pattern |
| Workers | `chatbot/worker/ChatWorkers.java` | `@JobWorker` handlers for `chatbotThinking` and `chatbotReply` |
| Service | `chatbot/service/ZeebeService.java` | Wraps `CamundaClient` for process and job operations |
| Persistence | `chatbot/service/ChatSessionService.java` | Saves/loads chat history via JPA (H2) |
| Frontend | `src/main/chatbot/` | Angular 17 standalone application |
| Process | `modeler/chatbot.bpmn` | Main AI agent process |
| Policy | `modeler/Policy.dmn` | User permission decision table |

---

### 2. Synchronous API Wrapper

**Swagger UI:** [http://localhost:8081/swagger-ui/index.html](http://localhost:8081/swagger-ui/index.html)

Endpoint to try: `POST /synchronous/simulateSynchronousCall`

#### Why you (almost) should not do this

Modern API design strongly favours **asynchronous patterns**. When a client sends a request and the server must coordinate multiple steps (calling external services, waiting on business logic, retrying on failure), keeping the HTTP connection open while all that happens creates several problems:

- Threads are held for the duration of the operation, limiting scalability.
- Load balancers and proxies impose timeout limits that may be shorter than the process.
- There is no built-in way to resume if the server restarts mid-flight.
- Clients need complex retry logic for transient failures.

Asynchronous patterns (webhooks, callbacks, polling, or event streaming as shown in the chatbot example) decouple the request lifecycle from the processing lifecycle and are far more resilient.

**This example exists purely to show that it is technically possible to wrap a Camunda 8 process with a synchronous HTTP facade — not as a recommendation to do so in production.**

#### How it works

The key idea is to bridge the gap between a synchronous HTTP call and an asynchronous process execution using a `CompletableFuture`.

```
HTTP Client          Spring Boot                   Camunda 8
     │                    │                            │
     │── POST /sync ──────►│                            │
     │                    │── startInstance() ─────────►│
     │                    │◄── processInstanceKey ──────│
     │                    │                            │
     │                    │  register(key) → future     │
     │   (waiting…)        │                            │
     │                    │                   (process runs)
     │                    │                            │
     │                    │◄── job: catchProcessEnd ───│
     │                    │  complete(key, variables)   │
     │                    │   → future.complete()       │
     │                    │                            │
     │◄── HTTP 200 ────────│                            │
```

**Components:**

- **`SynchronousController`** — Starts the BPMN process, registers a `CompletableFuture` keyed on the `processInstanceKey`, then returns that future to Spring MVC (which suspends the HTTP thread without blocking it). A 30-second timeout is applied; if the process does not complete in time, the endpoint returns HTTP 504.

- **`PendingRequestRegistry`** — A `ConcurrentHashMap<Long, CompletableFuture<…>>` that maps each `processInstanceKey` to its waiting future. Thread-safe by design.

- **`ResponseWorker`** — A `@JobWorker` that subscribes to the `catchProcessEnd` execution listener defined on the BPMN end event. When the process finishes, this worker fires, looks up the future by `processInstanceKey`, and calls `future.complete(variables)` — which unblocks the HTTP response.

- **`fakeSynchronousProcess.bpmn`** — A minimal process that contains parallel tasks to simulate work, ending with an execution listener of type `catchProcessEnd`. The process variables accumulated during execution are returned to the HTTP caller as the response body.

#### Code structure

| Component | Path | Description |
|---|---|---|
| Controller | `synchronous/SynchronousController.java` | HTTP endpoint, starts process, awaits future |
| Registry | `synchronous/PendingRequestRegistry.java` | Maps `processInstanceKey` → `CompletableFuture` |
| Worker | `synchronous/ResponseWorker.java` | Completes the future when the process ends |
| Process | `modeler/fakeSynchronousProcess.bpmn` | Demo BPMN with `catchProcessEnd` listener |
