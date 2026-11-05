# Release Notes

## Final POE — version 1.2.0

This release covers every functional update made to EventFinder South Africa
since the Part 2 prototype. The prototype was a single-device, offline-first app
that read events from a REST API and public feeds. This version adds the cloud
back end, synchronisation, push notifications and store-readiness required for
the final submission, and resolves the feedback from the prototype phase.

## New features added in this version

- Cloud sync (NoSQL): user-created events, favourites and RSVPs are stored in and
  read back from Cloud Firestore, scoped per user. This gives each account the
  same data across devices.
- Blob storage: event images are uploaded to Azure Blob Storage through the REST
  API, so the storage key never reaches the device. If the API is unreachable the
  image falls back to local internal storage.
- Real-time push notifications: Firebase Cloud Messaging delivers live updates to
  the shared events topic, which devices subscribe to on launch.
- Offline mode with synchronisation: changes made offline are queued in the local
  Room database and synced automatically when connectivity returns.
- Biometric authentication: sign-in with fingerprint or face unlock, enabled from
  the settings menu.

## Fixes to prototype feedback

- Single sign-on: the server now verifies the Google ID token (audience and
  expiry) instead of trusting the client, and accounts are backed by the cloud
  rather than existing only on one device.
- REST API security: the create, update and delete event endpoints now require
  authentication, so events can no longer be changed anonymously.
- REST API integration: events created in the app are now sent to the REST API
  and the cloud, not saved only on the device.
- Favourites and settings: favourites, preferences and account changes now sync
  to the cloud instead of affecting only the local device.
- User interface: colours that were hard-coded in individual screens were moved
  into a single theme palette.
- Catalogue: demo sample events are now shown only as a fallback when no real
  events are available, and are removed once real listings load, so they are no
  longer mixed in with real events.

## Other improvements

- Image loading shows a placeholder and a graceful fallback, with bounded memory
  and disk caches suited to entry-level devices.
- Weather forecasts use the correct request format and the venue's timezone.
- Event discovery and the locale read run off the main thread to prevent freezes.

## Store readiness

- Release build configured with signing, code shrinking and resource shrinking.
- Custom application icon (map pin and calendar in South African flag colours).
- Application version 1.2.0 (version code 3).

## Innovative features (from the Part 1 design) included in this version

- Community-created events with RSVP management.
- Bilingual interface (English and Afrikaans) with runtime language switching.
- Offline event caching with automatic cloud synchronisation.
- Proximity-based event discovery and sorting.
- Map view of events using OpenStreetMap.
- Weather forecast shown for each event's venue and date.
