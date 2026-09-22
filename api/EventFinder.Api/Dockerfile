# EventFinder South Africa REST API — container image.
#
# Multi-stage: the SDK image compiles the app, then only the published output is
# copied into the much smaller ASP.NET runtime image.
#
# Build (from the repository root):
#   docker build -t eventfinder-api api/EventFinder.Api
#
# Run, keeping the SQLite file on a named volume so it survives restarts:
#   docker run -p 5217:8080 -v eventfinder-data:/data eventfinder-api

FROM mcr.microsoft.com/dotnet/sdk:8.0 AS build
WORKDIR /src

# Restore first, as its own layer, so code edits do not re-download packages.
COPY EventFinder.Api.csproj .
RUN dotnet restore

COPY . .
RUN dotnet publish -c Release -o /app/publish --no-restore

FROM mcr.microsoft.com/dotnet/aspnet:8.0 AS runtime
WORKDIR /app

COPY --from=build /app/publish .

# The SQLite database lives here. Mount a volume over it to keep the data;
# without one it is part of the container layer and is lost when the container
# is removed.
RUN mkdir -p /data
VOLUME /data
ENV ConnectionStrings__Default="Data Source=/data/eventfinder.db"

# The ASP.NET 8 runtime image serves on 8080 by default. Hosts that inject a
# $PORT variable (Render, Cloud Run) should set ASPNETCORE_HTTP_PORTS to match.
ENV ASPNETCORE_HTTP_PORTS=8080
EXPOSE 8080

ENTRYPOINT ["dotnet", "EventFinder.Api.dll"]
