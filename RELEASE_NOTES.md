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
- Real-time push notifications: when anyone creates a public event, the REST API
  sends a Firebase Cloud Messaging message to the shared events topic, which every
  device subscribes to on launch. The notification text is built on the device so
  it appears in the user's language, and it can be switched off in Settings.
- Offline mode with synchronisation: events, favourites and RSVPs are kept in the
  local Room database, so they can be viewed and changed without a connection.
  Changes made offline are written locally first and pushed to Cloud Firestore
  automatically when the connection returns.
- Biometric authentication: sign-in with fingerprint or face unlock, enabled from
  the settings menu. The prompt is tied to an Android Keystore key and fails
  closed if the key is invalidated.
- Ratings and reviews: verified attendees can rate and review events, with
  filters for rating, verified reviews, recent reviews and photos.
- Content ratings and reporting: attendees can rate an event's content (family,
  adult, 18+) and report unsuitable listings or scams, with moderation tools to
  review and resolve reports.
- Tickets: an RSVP issues a ticket with a QR code that works offline, and
  organisers can check attendees in with a scanner and export the list as CSV.
- Group planning: friends can plan to attend together, pick a meeting point and
  invite others.
- Map routes: driving, walking and cycling routes to an event from the map view.
- Organiser profiles, personal event collections, and accessibility and sensory
  preferences (wheelchair access, noise, strobe lights) that filter the feed.

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
- Every screen is now translated: about 370 strings that were written directly in
  the code were moved into the string files with Afrikaans versions.
- Real events appear on a fresh install after about 17 seconds instead of about
  50, because events are saved before their cover photos are looked up.

## Bug fixes

- The app no longer crashes when updating from an older version: two database
  tables are rebuilt to the shape the app expects.
- Refreshing the event feeds no longer deletes reviews saved on the device.
- A fresh install no longer posts a burst of "new events" notifications.
- Blocking a second user no longer unblocks the first.
- The moderation list no longer shows "just now ago", and the group plan card no
  longer shows a made-up "2 maybe" count.

## Store readiness

- Release build configured with signing, code shrinking and resource shrinking.
- Custom application icon (map pin and calendar in South African flag colours).
- Targets Android 16 (API 36), as Google Play requires for new apps.
- Play submission pack in `play/`: feature graphic, 512 px icon, phone
  screenshots, store listing text, content rating and data safety answers, and a
  published privacy policy.
- Application version 1.2.0 (version code 4).

## Innovative features (from the Part 1 design) included in this version

- Community-created events with RSVP management.
- Bilingual interface (English and Afrikaans) across every screen, with runtime
  language switching.
- Offline event caching with automatic cloud synchronisation.
- Proximity-based event discovery and sorting.
- Map view of events using OpenStreetMap.
- Weather forecast shown for each event's venue and date.
