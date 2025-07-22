
# gRPC Client-Server with Comprehensive Observability

## Table of Contents
* [1-Introduction](#introduction)
* [2-Architecture](#architecture)
* [3-Observability Features](#observability-features)
    * [Distributed Tracing with OpenTelemetry and Jaeger](#distributed-tracing-with-opentelemetry-and-jaeger)
    * [Metrics with OpenTelemetry and Prometheus](#metrics-with-opentelemetry-and-prometheus)
    * [Context Propagation](#context-propagation)
* [4-Project Structure](#project-structure)
* [5-Prerequisites](#prerequisites)
* [6-Getting Started](#getting-started)
    * [1. Clone the Repository](#1-clone-the-repository)
    * [2. Build Docker Images](#2-build-docker-images)
    * [3. Run with Docker Compose](#3-run-with-docker-compose)
* [7-Usage](#usage)
* [8-Viewing Observability Data](#viewing-observability-data)
    * [Jaeger UI (Traces)](#jaeger-ui-traces)
    * [Prometheus UI (Metrics)](#prometheus-ui-metrics)


---

## Introduction

This project demonstrates a simple gRPC client-server communication model built in Java, enhanced with robust observability features using OpenTelemetry, Jaeger, and Prometheus, all orchestrated with Docker Compose.

The server manages user data, storing and retrieving it from a PostgreSQL database using JDBC. Both client and server services are instrumented to emit distributed traces and application metrics, providing deep insights into request flows and system health.

## Architecture

The application consists of several interconnected components, deployed as individual services within a Docker network:

```

\+----------------+      +------------------+      +-------------+
|  grpc-client   |\<----\>|   grpc-server    |\<----\>|  PostgreSQL |
| (Java App)     |      |  (Java App)      |      |   (DB)      |
\+----------------+      +------------------+      +-------------+
|                        |
|  OTLP Traces & Metrics |
V                        V
\+------------------------------------------------+
|           otel-collector (OpenTelemetry)       |
| (Receives, Processes, and Forwards Data)       |
\+------------------------------------------------+
|       |
| Traces| Metrics
V       V
\+-----------+ +-------------+
|   Jaeger  | |  Prometheus |
| (Tracing) | |   (Metrics) |
\+-----------+ +-------------+

```

* **`grpc-client`**: Initiates gRPC calls to the `grpc-server`. Instrumented with OpenTelemetry to generate traces and metrics.
* **`grpc-server`**: Responds to gRPC requests, interacts with PostgreSQL, and generates its own traces and metrics.
* **`PostgreSQL`**: The relational database used by the `grpc-server` to store user data.
* **`otel-collector`**: The OpenTelemetry Collector acts as an intermediary. It receives OTLP (OpenTelemetry Protocol) traces and metrics from the `grpc-client` and `grpc-server`, processes them (e.g., batching), and then exports them to Jaeger (for traces) and Prometheus (for metrics).
* **`Jaeger`**: A distributed tracing system. It receives traces from the `otel-collector` and provides a UI for visualizing end-to-end request flows across services.
* **`Prometheus`**: A monitoring system with a time-series database. It scrapes metrics directly from the `otel-collector` (which re-exposes application metrics) and provides a UI for querying and visualizing this data.

## Observability Features

### Distributed Tracing with OpenTelemetry and Jaeger

Both the `grpc-client` and `grpc-server` are instrumented using the OpenTelemetry Java SDK. This enables:

* **Span Generation**: Each operation (e.g., a gRPC call, a database query) automatically generates a "span," representing a unit of work.
* **Trace Context Propagation**: OpenTelemetry automatically propagates trace context (like `traceId` and `spanId`) across gRPC calls. This links all related spans from the client, through the server, and even to the database calls, forming a complete end-to-end "trace" that can be visualized in Jaeger. This is crucial for understanding distributed transactions and pinpointing performance bottlenecks.
* **Jaeger Visualization**: The `otel-collector` forwards these traces to Jaeger, where you can view detailed timelines of requests, understand dependencies, and identify latency issues.

### Metrics with OpenTelemetry and Prometheus

OpenTelemetry is also used to capture application metrics (e.g., request counts, latencies). These metrics are:

* **Exported to Collector**: Sent from the Java applications to the `otel-collector` via OTLP.
* **Exposed by Collector**: The `otel-collector` is configured to re-expose these collected metrics (along with its own internal metrics) in a Prometheus-compatible format.
* **Scraped by Prometheus**: Prometheus periodically pulls (scrapes) these metrics from the `otel-collector`'s exposed endpoints.
* **Analysis**: In the Prometheus UI, you can query and visualize these metrics, allowing you to monitor the health and performance of your gRPC services.

### Context Propagation

A key aspect of distributed tracing, context propagation ensures that the unique `traceId` and `spanId` are carried across network boundaries (e.g., from `grpc-client` to `grpc-server`). OpenTelemetry handles this automatically for gRPC, ensuring that all operations related to a single user request, even if they span multiple services, are grouped under one coherent trace in Jaeger.

## Project Structure

The project has a clear and modular structure:

```

.
├── grpc-client/             \# Contains the gRPC client Java application
│   ├── src/                 \# Java source code for the client
│   ├── Dockerfile           \# Dockerfile for building the client image
│   └── pom.xml              \# Maven configuration for client dependencies
├── grpc-server/             \# Contains the gRPC server Java application
│   ├── src/                 \# Java source code for the server
│   ├── Dockerfile           \# Dockerfile for building the server image
│   └── pom.xml              \# Maven configuration for server dependencies
├── otel-collector-config.yaml \# Configuration for the OpenTelemetry Collector
├── prometheus.yml           \# Configuration for Prometheus to scrape metrics
└── docker-compose.yml       \# Orchestrates all services for easy deployment

````

## Prerequisites

Before you begin, ensure you have the following installed:

* **Docker Desktop**: Includes Docker Engine and Docker Compose.
    * [Install Docker Desktop](https://www.docker.com/products/docker-desktop/)
* **(Optional) Java Development Kit (JDK) 11+**: If you want to modify or build the Java applications outside of Docker.

## Getting Started

Follow these steps to get the entire gRPC client-server system with observability up and running using Docker Compose.

### 1. Clone the Repository

First, clone this repository to your local machine:

```
git clone <your-repository-url>
cd <your-repository-directory>
````

### 2\. Build Docker Images

Navigate to the root directory of the project (where `docker-compose.yml` is located) and build all necessary Docker images:

```
docker compose build --no-cache
```

This step compiles your Java applications and packages them into Docker images.

### 3\. Run with Docker Compose

Once the images are built, you can start all services using Docker Compose:

```
docker compose up -d
```

This command will:

* Start the `jaeger` service.
* Start the `otel-collector` service.
* Start the `prometheus` service.
* Start the `postgres` service (if included in your `docker-compose.yml`).
* Start your `grpc-server` and `grpc-client` services.

You can check the status of your running containers:

```
docker compose ps
```

You can view the logs of any service (e.g., `grpc-server` or `otel-collector`) using:

```
docker compose logs -f grpc-server
docker compose logs -f otel-collector
```

## Usage

Once all services are up and running, your `grpc-client` should automatically start making requests to the `grpc-server`.

* **Client Behavior**: The `grpc-client` is designed to periodically send requests to the `grpc-server` to trigger user data operations (e.g., `getUserData`, `getOrCreateUser`).

* **Server Behavior**: The `grpc-server` will log incoming requests and interact with the PostgreSQL database.

Monitor the logs of `grpc-client` and `grpc-server` to see the interactions:

```
docker compose logs grpc-client
docker compose logs grpc-server
```

## Viewing Observability Data

Now that your services are running and emitting data, you can explore the observability UIs.

### Jaeger UI (Traces)

Access the Jaeger UI to visualize the distributed traces:

* **URL**: [http://localhost:16686](https://www.google.com/search?q=http://localhost:16686)

In the Jaeger UI:

1.  In the "Service" dropdown, select `grpc-server` or `grpc-client` (or whatever service name you configured in your OpenTelemetry setup for each app).
2.  Click "Find Traces".
3.  You should see a list of traces. Click on any trace to see its detailed timeline, showing how requests flow through the `grpc-client`, `grpc-server`, and any internal database calls.

### Prometheus UI (Metrics)

Access the Prometheus UI to explore the collected metrics:

* **URL**: [http://localhost:9090](https://www.google.com/search?q=http://localhost:9090)

In the Prometheus UI:

1.  Go to the "Status" menu -\> "Targets".
2.  Verify that `otel-collector-app-metrics` and `otel-collector-self-metrics` (or similar names based on your `prometheus.yml`) are listed as `UP`. This confirms Prometheus is successfully scraping metrics from the OpenTelemetry Collector.
3.  Go to the "Graph" page. You can now query for metrics emitted by your services, e.g., `process_cpu_usage` (from the collector's own metrics) or specific application metrics you've defined within your gRPC apps.


-----

