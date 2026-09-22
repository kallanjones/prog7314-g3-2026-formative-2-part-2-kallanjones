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

Debug builds default to `http://10.0.2.2:5217/`, which is how the Android emulator
reaches `localhost` on the host machine. Start the API first, then run the app —
events created through `POST /api/events` appear in the app's Discover list on the
next refresh.

## Testing on a physical phone

`10.0.2.2` only means anything inside the emulator. A real phone has to reach your
laptop over the network, which takes three steps.

### 1. Make the API listen on the network, not just loopback

`--urls http://localhost:5217` binds to loopback only, so the phone cannot connect.
Bind to all interfaces instead:

```bash
dotnet run --urls http://0.0.0.0:5217
```

Docker already does this — `-p 5217:8080` publishes on all interfaces.

### 2. Point the app at your laptop's IP

Find it:

```bash
# Windows
ipconfig

# macOS / Linux
ipconfig getifaddr en0 || hostname -I
```

Look for a `192.168.x.x` or `10.x.x.x` address. Then add it to **`local.properties`**
in the repository root:

```properties
api.base.url=http://192.168.1.42:5217/
```

`local.properties` is gitignored, so your address never gets committed and each
machine keeps its own. Re-run the build for it to take effect. Leave the property out
and the build falls back to `10.0.2.2` for the emulator.

### 3. Allow the port through the firewall

Windows blocks inbound connections on new ports by default. In an **administrator**
PowerShell:

```powershell
New-NetFirewallRule -DisplayName "EventFinder API" -Direction Inbound -LocalPort 5217 -Protocol TCP -Action Allow
```

### Checking it works

Put the phone on the **same Wi-Fi** as the laptop, then open
`http://192.168.1.42:5217/swagger` in the phone's browser. If Swagger loads, the app
will connect too. If it does not, the cause is almost always step 1 or step 3.

> Debug builds permit plain HTTP so this works without a certificate. Release builds
> are HTTPS-only — see `app/src/debug/res/xml/network_security_config.xml`.

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
docker run -p 5217:8080 -e ConnectionStrings__Default="Data Source=/data/custom.db" -v eventfinder-data:/data eventfinder-api
```

## Deploying to Fly.io

[`fly.toml`](EventFinder.Api/fly.toml) is configured to build the Dockerfile, run in the
`jnb` (Johannesburg) region, and mount a volume at `/data` so the SQLite database
survives deploys.

Fly requires a card on file even on the free allowance.

### One-time setup

Install [flyctl](https://fly.io/docs/flyctl/install/), then from `api/EventFinder.Api`:

```bash
fly auth login
```

```bash
fly launch --no-deploy
```

`fly launch` reads the existing `fly.toml`. Say **no** when it offers to overwrite the
configuration or add a database — we already have both. App names are globally unique,
so if `eventfinder-api` is taken it will suggest another; put whatever you accept into
the `app = ` line of `fly.toml`.

Create the volume the config expects, in the same region:

```bash
fly volumes create eventfinder_data --size 1 --region jnb
```

### Deploy

```bash
fly deploy
```

Then check it:

```bash
fly status
curl https://<your-app>.fly.dev/api/health
```

Swagger will be at `https://<your-app>.fly.dev/swagger`.

### Point the app at it

Put the deployed URL in `gradle.properties` so release builds use it:

```properties
API_BASE_URL_RELEASE=https://<your-app>.fly.dev/
```

To make a **debug** build use the deployed API instead of a local one — handy for
testing on a phone over mobile data — set it in `local.properties` as well:

```properties
api.base.url=https://<your-app>.fly.dev/
```

### Notes

- The machine **scales to zero when idle**, so the first request after a quiet period
  takes a few seconds to wake. That is normal, and worth knowing before you record the
  demonstration video.
- A Fly volume attaches to a **single machine**. Do not scale past one instance, or each
  would get its own separate database.
- `fly logs` tails the API's output if something misbehaves.

## Deploying to Azure App Service

This is the stack named in the Planning and Design document, and Azure for Students
covers it. There are two routes; the first needs no Docker at all.

### The one rule that matters on Azure

On App Service Linux, **only `/home` persists**. Anything written elsewhere is wiped
when the app restarts or redeploys. So the SQLite database must live under `/home`:

```
ConnectionStrings__Default = Data Source=/home/data/eventfinder.db
```

The app creates that folder on startup if it does not exist.

### Route A — deploy the code (simplest)

`az webapp up` deploys **the directory you run it from**, so run it from
`api/EventFinder.Api` — the folder holding `EventFinder.Api.csproj`. From the repository
root it would try to bundle the Android app too.

Clear the local build output and dev database first, or they are uploaded as well
(`bin/` alone is around 65 MB).

PowerShell:

```powershell
cd api/EventFinder.Api
dotnet clean
Remove-Item eventfinder.db* -Force -ErrorAction SilentlyContinue
```

bash / zsh:

```bash
cd api/EventFinder.Api
dotnet clean
rm -f eventfinder.db eventfinder.db-shm eventfinder.db-wal
```

> Windows PowerShell 5.1 has no `&&` operator — run each line separately, or join
> them with `;`.

Then:

```bash
az login
```

```bash
az webapp up --runtime "DOTNET:8" --sku F1 --name <your-app-name> --resource-group eventfinder-rg --location southafricanorth
```

`az webapp up` creates the resource group, plan and web app, then builds and deploys.
The name becomes `https://<your-app-name>.azurewebsites.net`, so it must be globally
unique.

Then point the database at the persistent share:

```bash
az webapp config appsettings set --name <your-app-name> --resource-group eventfinder-rg --settings ConnectionStrings__Default="Data Source=/home/data/eventfinder.db"
```

Redeploy later with `az webapp up` again from the same folder.

### Route B — deploy the container

Build the image in Azure Container Registry (no local Docker needed) and run it:

```bash
az acr create --name <registry-name> --resource-group eventfinder-rg --sku Basic --admin-enabled true
```

```bash
az acr build --registry <registry-name> --image eventfinder-api:latest .
```

```bash
az webapp create --name <your-app-name> --resource-group eventfinder-rg --plan <plan-name> --deployment-container-image-name <registry-name>.azurecr.io/eventfinder-api:latest
```

A container needs two extra settings — the port it listens on, and the flag that mounts
`/home` into the container:

```bash
az webapp config appsettings set --name <your-app-name> --resource-group eventfinder-rg --settings WEBSITES_PORT=8080 WEBSITES_ENABLE_APP_SERVICE_STORAGE=true ConnectionStrings__Default="Data Source=/home/data/eventfinder.db"
```

Without `WEBSITES_ENABLE_APP_SERVICE_STORAGE=true` the container gets its own throwaway
filesystem and the database resets on every restart.

### Check it

```bash
curl https://<your-app-name>.azurewebsites.net/api/health
az webapp log tail --name <your-app-name> --resource-group eventfinder-rg
```

Swagger will be at `https://<your-app-name>.azurewebsites.net/swagger`. HTTPS is
provided automatically on `azurewebsites.net`.

### Point the app at it

```properties
# gradle.properties — used by release builds
API_BASE_URL_RELEASE=https://<your-app-name>.azurewebsites.net/
```

### Notes

- **F1 (free)** has no always-on, so the first request after idle is slow — warm it up
  before recording the demonstration video. It is also capped at 60 CPU-minutes/day. If
  that bites, **B1** is small enough for the Azure for Students credit.
- Region `southafricanorth` is Johannesburg. F1 is not available in every region; if it
  is rejected, try `westeurope`.

## Deploying to Render

The option that needs **no credit card and no CLI** — useful if an Azure subscription
is not available.

Sign in to [Render](https://render.com) with your GitHub account, then choose
**New → Web Service** and select this repository.

Set these, because the API is not at the repository root:

| Field | Value |
| --- | --- |
| Language / Runtime | Docker |
| Branch | `main` |
| **Root Directory** | `api/EventFinder.Api` |
| Dockerfile Path | `./Dockerfile` |
| Instance Type | Free |
| Health Check Path | `/api/health` |

Add two environment variables:

```
ASPNETCORE_HTTP_PORTS = 8080
ConnectionStrings__Default = Data Source=/data/eventfinder.db
```

Then **Create Web Service**. The first build takes a few minutes.

> **Root Directory is the important one.** Left blank, Render builds from the
> repository root, which holds the Android app and no Dockerfile, and the build fails.
> If Render reports that it cannot detect an open port, add `PORT=8080` as well.

The API is then at `https://<service-name>.onrender.com`, with HTTPS provided
automatically. Check `https://<service-name>.onrender.com/api/health`.

[`render.yaml`](../render.yaml) in the repository root describes the same service as a
Blueprint, if you prefer that route — Render lists Blueprints separately from the New
Web Service flow.

Point the app at it:

```properties
# gradle.properties — release builds
API_BASE_URL_RELEASE=https://<service-name>.onrender.com/
```

```properties
# local.properties — to use the deployed API from a debug build on your phone
api.base.url=https://<service-name>.onrender.com/
```

### Two limits of the free plan

- **No persistent disk.** The SQLite file is recreated whenever the service
  redeploys or restarts, so events created earlier disappear. Within one sitting
  everything works normally, which is enough to demonstrate the full round-trip —
  just create the events during the demo rather than beforehand.
- **Services sleep after inactivity**, and the first request afterwards takes
  roughly a minute. **Open the URL once before recording** so the cold start is not
  mistaken for a bug.

If the data needs to survive properly, add a free hosted Postgres (Neon or Supabase)
and switch the EF Core provider to Npgsql — the schema is code-first, so it is a small
change.

## Deploying elsewhere

The container runs on any host that takes a Docker image — Render, Koyeb, Railway,
Google Cloud Run. Two things to check on whichever you pick:

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
