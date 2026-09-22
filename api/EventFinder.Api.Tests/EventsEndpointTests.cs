using System.Net;
using System.Net.Http.Json;
using EventFinder.Api.Data;
using EventFinder.Api.Models;
using Microsoft.AspNetCore.Mvc.Testing;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.DependencyInjection.Extensions;

namespace EventFinder.Api.Tests;

/// <summary>
/// Drives the real API host over HTTP, with the SQLite database swapped for a
/// throwaway file per test run so the tests never touch the development data.
/// </summary>
public class EventsEndpointTests : IClassFixture<ApiFactory>
{
    private readonly HttpClient _client;

    public EventsEndpointTests(ApiFactory factory) => _client = factory.CreateClient();

    private static EventRequest ValidRequest(string title = "Cape Town Jazz Festival")
    {
        var start = DateTimeOffset.UtcNow.AddDays(7).ToUnixTimeMilliseconds();

        return new EventRequest
        {
            Title = title,
            Description = "An evening of live jazz.",
            Category = "music",
            StartDate = start,
            EndDate = start + 10_800_000,
            VenueName = "Artscape Theatre",
            Address = "DF Malan St, Cape Town",
            Latitude = -33.9186,
            Longitude = 18.4285,
            OrganizerId = "user-1",
            OrganizerName = "Cape Town Events",
            AttendeeCount = 120
        };
    }

    [Fact]
    public async Task Health_reports_healthy()
    {
        var response = await _client.GetFromJsonAsync<ApiResponse<HealthPayload>>("/api/health");

        Assert.NotNull(response);
        Assert.True(response!.Success);
        Assert.Equal("healthy", response.Data!.Status);
    }

    [Fact]
    public async Task Creating_an_event_returns_201_and_makes_it_readable()
    {
        var created = await _client.PostAsJsonAsync("/api/events", ValidRequest("Readable event"));
        Assert.Equal(HttpStatusCode.Created, created.StatusCode);

        var body = await created.Content.ReadFromJsonAsync<ApiResponse<EventEntity>>();
        Assert.True(body!.Success);
        Assert.False(string.IsNullOrWhiteSpace(body.Data!.Id));

        var fetched = await _client.GetFromJsonAsync<ApiResponse<EventEntity>>($"/api/events/{body.Data.Id}");
        Assert.Equal("Readable event", fetched!.Data!.Title);

        // Category is normalised to upper case on write.
        Assert.Equal("MUSIC", fetched.Data.Category);
    }

    [Fact]
    public async Task Creating_an_event_without_a_title_returns_400_with_a_field_message()
    {
        var request = ValidRequest();
        request.Title = string.Empty;

        var response = await _client.PostAsJsonAsync("/api/events", request);

        Assert.Equal(HttpStatusCode.BadRequest, response.StatusCode);

        var body = await response.Content.ReadFromJsonAsync<ApiResponse<EventEntity>>();
        Assert.False(body!.Success);
        Assert.Contains(body.Errors!, e => e.Contains("Title", StringComparison.OrdinalIgnoreCase));
    }

    [Fact]
    public async Task Creating_an_event_that_ends_before_it_starts_returns_400()
    {
        var request = ValidRequest();
        request.EndDate = request.StartDate - 1;

        var response = await _client.PostAsJsonAsync("/api/events", request);

        Assert.Equal(HttpStatusCode.BadRequest, response.StatusCode);

        var body = await response.Content.ReadFromJsonAsync<ApiResponse<EventEntity>>();
        Assert.Contains(body!.Errors!, e => e.Contains("endDate", StringComparison.OrdinalIgnoreCase));
    }

    [Fact]
    public async Task Unknown_event_returns_404()
    {
        var response = await _client.GetAsync("/api/events/not-a-real-id");

        Assert.Equal(HttpStatusCode.NotFound, response.StatusCode);
    }

    [Fact]
    public async Task Updating_an_event_changes_the_stored_values()
    {
        var created = await _client.PostAsJsonAsync("/api/events", ValidRequest("Before update"));
        var id = (await created.Content.ReadFromJsonAsync<ApiResponse<EventEntity>>())!.Data!.Id;

        var update = ValidRequest("After update");
        update.AttendeeCount = 250;

        var response = await _client.PutAsJsonAsync($"/api/events/{id}", update);
        Assert.Equal(HttpStatusCode.OK, response.StatusCode);

        var body = await response.Content.ReadFromJsonAsync<ApiResponse<EventEntity>>();
        Assert.Equal("After update", body!.Data!.Title);
        Assert.Equal(250, body.Data.AttendeeCount);
    }

    [Fact]
    public async Task Deleting_an_event_removes_it()
    {
        var created = await _client.PostAsJsonAsync("/api/events", ValidRequest("To be deleted"));
        var id = (await created.Content.ReadFromJsonAsync<ApiResponse<EventEntity>>())!.Data!.Id;

        var deleted = await _client.DeleteAsync($"/api/events/{id}");
        Assert.Equal(HttpStatusCode.OK, deleted.StatusCode);

        var fetched = await _client.GetAsync($"/api/events/{id}");
        Assert.Equal(HttpStatusCode.NotFound, fetched.StatusCode);
    }

    [Fact]
    public async Task Search_matches_on_title()
    {
        await _client.PostAsJsonAsync("/api/events", ValidRequest("Soweto Marathon"));

        var response = await _client.GetFromJsonAsync<ApiResponse<List<EventEntity>>>("/api/events?q=soweto");

        Assert.NotNull(response);
        Assert.Contains(response!.Data!, e => e.Title == "Soweto Marathon");
    }

    /// <summary>Health payload, deserialised with the casing the API emits.</summary>
    public record HealthPayload(string Status, DateTimeOffset Utc);
}

/// <summary>Boots the API with its own temporary SQLite database.</summary>
public class ApiFactory : WebApplicationFactory<Program>, IDisposable
{
    private readonly string _dbPath = Path.Combine(Path.GetTempPath(), $"eventfinder-tests-{Guid.NewGuid()}.db");

    protected override void ConfigureWebHost(Microsoft.AspNetCore.Hosting.IWebHostBuilder builder)
    {
        builder.ConfigureServices(services =>
        {
            services.RemoveAll<DbContextOptions<EventFinderDbContext>>();
            services.AddDbContext<EventFinderDbContext>(options =>
                options.UseSqlite($"Data Source={_dbPath}"));
        });
    }

    protected override void Dispose(bool disposing)
    {
        base.Dispose(disposing);

        if (!disposing)
        {
            return;
        }

        // SQLite pools its connections, so the file stays locked until the pool
        // is cleared. A leftover temp file is harmless, so never fail the run.
        Microsoft.Data.Sqlite.SqliteConnection.ClearAllPools();

        try
        {
            if (File.Exists(_dbPath))
            {
                File.Delete(_dbPath);
            }
        }
        catch (IOException)
        {
            // The OS will clean the temp directory up later.
        }
    }
}
