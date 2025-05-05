# CockroachDB Dynamic Certs Client

A utility container that dynamically generates certificates for secure CockroachDB deployments. This application combines a Spring Boot service with the CockroachDB binary to create:

- A Certificate Authority (CA)
- Client certificates for specified users
- Node certificates with configurable alternative names

## Features

- Dynamically generates all necessary certificates for CockroachDB secure mode
- Supports custom client usernames
- Configurable node alternative names
- Health endpoint for monitoring
- Based on official CockroachDB and Eclipse Temurin JDK 21 images

## Environment Variables

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| NODE_ALTERNATIVE_NAMES | Yes | - | Space-separated list of alternative names for the node certificate |
| CLIENT_USERNAME | No | root | Username for client certificate generation |

## Building the Image
```bash
docker build --no-cache -t timveil/cockroachdb-dynamic-certs:latest .
```

For specific platforms:
```bash
docker build --no-cache --platform linux/arm64 -t timveil/cockroachdb-dynamic-certs:latest .
```

## Publishing the Image
```bash
docker push timveil/cockroachdb-dynamic-certs:latest
```

## Running the Image

Basic usage:
```bash
docker run -p 9999:9999 \
    --env NODE_ALTERNATIVE_NAMES=localhost \
    -it timveil/cockroachdb-dynamic-certs:latest
```

With custom client username:
```bash
docker run -p 9999:9999 \
    --env NODE_ALTERNATIVE_NAMES="localhost node1.example.com" \
    --env CLIENT_USERNAME=myuser \
    -it timveil/cockroachdb-dynamic-certs:latest
```

## Health Monitoring

The application exposes a health endpoint at:
```
http://localhost:9999/actuator/health
```

This can be used to monitor the application's status in container orchestration environments.

## Certificate Locations

Inside the container, certificates are generated in:
- `/.cockroach-certs` - Contains all generated certificates
- `/.cockroach-key` - Contains the CA key
