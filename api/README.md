# EventFinder South Africa — REST API

The custom REST API for the EventFinder South Africa Android app, built with
**ASP.NET Core 8 Minimal APIs** and **Entity Framework Core (code-first)** over
**SQLite**.

This is the API referred to in section 5 of the Planning and Design document.
SQLite is used instead of Azure SQL so the service can be hosted on any free
tier without provisioning a separate database server — the schema and endpoints
are otherwise as specified.

## Endpoints

All endpoints return the same response envelope:

```json
{ "success": true, "data": { }, "message": null, "errors": null }
```

| Method | Route | Purpose |
| --- | --- | --- |
| `GET` | `/api/health` | Liveness probe |
| `GET` | `/api/events` | List events (`?category=`, `?q=`, `?page=`, `?pageSize=`) |
| `GET` | `/api/events/{id}` | Fetch one event |
| `POST` | `/api/events` | Create an event |
| `PUT` | `/api/events/{id}` | Update an event |
| `DELETE` | `/api/events/{id}` | Delete an event |

Interactive Swagger documentation is served at `/swagger`.

### Status codes

| Code | When |
| --- | --- |
| `200` | Request succeeded |
| `201` | Event created (with a `Location` header) |
| `400` | Validation failed — `errors` lists the field messages |
| `404` | No event with that id |

## Running locally

```bash
cd api/EventFinder.Api
dotnet run --urls http://localhost:5217
```

Then open <http://localhost:5217/swagger>.

The SQLite file `eventfinder.db` is created automatically on first run.

### Connecting the Android app

Debug builds already point at `http://10.0.2.2:5217/`, which is how the Android
emulator reaches `localhost` on the host machine. Start the API first, then run
the app — events created through `POST /api/events` appear in the app's Discover
list on the next refresh.

The base URL is set per build type in `app/build.gradle.kts` (`API_BASE_URL`).

## Running in Docker

From the repository root:

```bash
docker build -t eventfinder-api api/EventFinder.Api
```

```bash
docker run -p 5217:8080 -v eventfinder-data:/data eventfinder-api
```

The API is then on <http://localhost:5217/swagger>, and the emulator reaches it at
`http://10.0.2.2:5217/` — the same address debug builds already use, so no app change
is needed.

### Why the volume

SQLite is a file. Without `-v eventfinder-data:/data` that file lives in the container
layer and is destroyed when the container is removed, taking every event with it. The
named volume keeps it on the host.

The image sets `ConnectionStrings__Default` to `/data/eventfinder.db`; override it to put
the database somewhere else:

```bash
docker run -p 5217:8080 \
  -e ConnectionStrings__Default="Data Source=/data/custom.db" \
  -v eventfinder-data:/data eventfinder-api
```

## Deploying

The container runs on any host that takes a Docker image — Render, Koyeb, Fly.io,
Railway, Google Cloud Run. Two things to check on whichever you pick:

1. **Port.** The image listens on `8080`. Hosts that inject a `$PORT` variable need
   `ASPNETCORE_HTTP_PORTS` set to the same value.
2. **Disk.** If the host's filesystem is ephemeral (most free tiers are), attach a
   persistent volume mounted at `/data`, or the database resets on every deploy.

Without Docker, publish a plain build and upload it to any host that runs .NET 8:

```bash
cd api/EventFinder.Api
dotnet publish -c Release -o publish
```

Set `API_BASE_URL` in the `release` block of `app/build.gradle.kts` to the
deployed HTTPS URL before building the release APK.

> **Note:** the release build is HTTPS-only. Plain HTTP is permitted for debug
> builds only, and only to `10.0.2.2` / `localhost` — see
> `app/src/debug/res/xml/network_security_config.xml`.

## Tests

```bash
cd api/EventFinder.Api.Tests
dotnet test
```
