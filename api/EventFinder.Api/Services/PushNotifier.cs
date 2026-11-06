using EventFinder.Api.Data;
using FirebaseAdmin;
using FirebaseAdmin.Messaging;

namespace EventFinder.Api.Services;

/// <summary>Sends real-time push notifications to installed apps.</summary>
public interface IPushNotifier
{
    /// <summary>Tells every app install that a public event was just created.</summary>
    Task NotifyNewEventAsync(EventEntity ev, CancellationToken ct = default);
}

/// <summary>
/// Firebase Cloud Messaging implementation. Sends a data message to the
/// "new_events" topic, which the app subscribes to on launch. Only ids and the
/// event title are sent; the app builds the notification text from its own
/// string resources so it appears in the user's language.
///
/// Without Firebase credentials (local runs, tests) nothing is sent. A failed
/// push is logged and never fails the request that created the event.
/// </summary>
public class FcmPushNotifier : IPushNotifier
{
    public const string NewEventsTopic = "new_events";

    private readonly ILogger<FcmPushNotifier> _logger;

    public FcmPushNotifier(ILogger<FcmPushNotifier> logger) => _logger = logger;

    public async Task NotifyNewEventAsync(EventEntity ev, CancellationToken ct = default)
    {
        if (FirebaseApp.DefaultInstance is null)
        {
            _logger.LogInformation("Firebase not configured; skipping new-event push for {EventId}", ev.Id);
            return;
        }

        var message = new Message
        {
            Topic = NewEventsTopic,
            Data = new Dictionary<string, string>
            {
                ["type"] = "new_event",
                // The app stores backend events as "api:{id}".
                ["eventId"] = $"api:{ev.Id}",
                ["eventTitle"] = ev.Title,
                ["organizerId"] = ev.OrganizerId.ToString(),
            },
            Android = new AndroidConfig { Priority = Priority.High },
        };

        try
        {
            var messageId = await FirebaseMessaging.DefaultInstance.SendAsync(message, ct);
            _logger.LogInformation("Sent new-event push {MessageId} for {EventId}", messageId, ev.Id);
        }
        catch (Exception e)
        {
            _logger.LogWarning(e, "New-event push failed for {EventId}", ev.Id);
        }
    }
}
