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

## Deploying

Publish a self-contained build and upload it to any host that runs .NET 8:

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
