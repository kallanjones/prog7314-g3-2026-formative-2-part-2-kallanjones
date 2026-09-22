using System.ComponentModel.DataAnnotations;
using EventFinder.Api.Data;
using EventFinder.Api.Models;
using Microsoft.Data.Sqlite;
using Microsoft.EntityFrameworkCore;

// EventFinder South Africa REST API.
//
// A minimal ASP.NET Core Web API backed by EF Core + SQLite, as specified in
// section 5 of the Planning and Design document. Minimal APIs are used instead
// of controllers to keep the whole surface readable in one file.

var builder = WebApplication.CreateBuilder(args);

// SQLite lives beside the app so no database server has to be provisioned.
var connectionString = builder.Configuration.GetConnectionString("Default")
    ?? "Data Source=eventfinder.db";

builder.Services.AddDbContext<EventFinderDbContext>(options =>
    options.UseSqlite(connectionString));

builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen();

// The Android client calls this from a device, so allow cross-origin callers.
builder.Services.AddCors(options =>
    options.AddDefaultPolicy(policy =>
        policy.AllowAnyOrigin().AllowAnyMethod().AllowAnyHeader()));

var app = builder.Build();

// SQLite will not create missing directories, and hosts put the writable disk in
// different places — /data on Fly and Docker, /home/data on Azure App Service.
// Create the folder first so any of them works without a manual step.
var dataSource = new SqliteConnectionStringBuilder(connectionString).DataSource;
if (!string.IsNullOrWhiteSpace(dataSource))
{
    var directory = Path.GetDirectoryName(Path.GetFullPath(dataSource));
    if (!string.IsNullOrEmpty(directory))
    {
        Directory.CreateDirectory(directory);
    }
}

// Create the database on first run so a fresh deployment needs no manual step.
using (var scope = app.Services.CreateScope())
{
    var db = scope.ServiceProvider.GetRequiredService<EventFinderDbContext>();
    db.Database.EnsureCreated();
}

app.UseCors();
app.UseSwagger();
app.UseSwaggerUI();

// ---------------------------------------------------------------------------
// Health
// ---------------------------------------------------------------------------

app.MapGet("/", () => Results.Redirect("/swagger"))
    .ExcludeFromDescription();

app.MapGet("/api/health", () =>
        Results.Ok(ApiResponse<object>.Ok(new { status = "healthy", utc = DateTimeOffset.UtcNow })))
    .WithName("GetHealth");

// ---------------------------------------------------------------------------
// Events
// ---------------------------------------------------------------------------

// GET /api/events — newest-first list, optionally filtered by category or text.
app.MapGet("/api/events", async (
        EventFinderDbContext db,
        string? category,
        string? q,
        int page = 1,
        int pageSize = 50) =>
    {
        if (page < 1) page = 1;
        pageSize = Math.Clamp(pageSize, 1, 200);

        var query = db.Events.AsQueryable();

        if (!string.IsNullOrWhiteSpace(category) && !category.Equals("ALL", StringComparison.OrdinalIgnoreCase))
        {
            query = query.Where(e => e.Category == category.ToUpperInvariant());
        }

        if (!string.IsNullOrWhiteSpace(q))
        {
            var term = q.Trim();
            query = query.Where(e =>
                EF.Functions.Like(e.Title, $"%{term}%") ||
                EF.Functions.Like(e.VenueName, $"%{term}%"));
        }

        var events = await query
            .OrderBy(e => e.StartDate)
            .Skip((page - 1) * pageSize)
            .Take(pageSize)
            .ToListAsync();

        return Results.Ok(ApiResponse<List<EventEntity>>.Ok(events));
    })
    .WithName("GetEvents");

// GET /api/events/{id}
app.MapGet("/api/events/{id}", async (EventFinderDbContext db, string id) =>
    {
        var found = await db.Events.FindAsync(id);

        return found is null
            ? Results.NotFound(ApiResponse<EventEntity>.Fail($"No event with id '{id}'."))
            : Results.Ok(ApiResponse<EventEntity>.Ok(found));
    })
    .WithName("GetEventById");

// POST /api/events
app.MapPost("/api/events", async (EventFinderDbContext db, EventRequest request) =>
    {
        if (Validate(request) is { Count: > 0 } errors)
        {
            return Results.BadRequest(ApiResponse<EventEntity>.Fail("Validation failed.", errors));
        }

        var entity = new EventEntity();
        Apply(request, entity);

        db.Events.Add(entity);
        await db.SaveChangesAsync();

        return Results.Created(
            $"/api/events/{entity.Id}",
            ApiResponse<EventEntity>.Ok(entity, "Event created."));
    })
    .WithName("CreateEvent");

// PUT /api/events/{id}
app.MapPut("/api/events/{id}", async (EventFinderDbContext db, string id, EventRequest request) =>
    {
        var existing = await db.Events.FindAsync(id);
        if (existing is null)
        {
            return Results.NotFound(ApiResponse<EventEntity>.Fail($"No event with id '{id}'."));
        }

        if (Validate(request) is { Count: > 0 } errors)
        {
            return Results.BadRequest(ApiResponse<EventEntity>.Fail("Validation failed.", errors));
        }

        Apply(request, existing);
        existing.UpdatedAt = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();

        await db.SaveChangesAsync();

        return Results.Ok(ApiResponse<EventEntity>.Ok(existing, "Event updated."));
    })
    .WithName("UpdateEvent");

// DELETE /api/events/{id}
app.MapDelete("/api/events/{id}", async (EventFinderDbContext db, string id) =>
    {
        var existing = await db.Events.FindAsync(id);
        if (existing is null)
        {
            return Results.NotFound(ApiResponse<string>.Fail($"No event with id '{id}'."));
        }

        db.Events.Remove(existing);
        await db.SaveChangesAsync();

        return Results.Ok(ApiResponse<string>.Ok(id, "Event deleted."));
    })
    .WithName("DeleteEvent");

app.Run();

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

/// <summary>Runs data-annotation validation plus the date-order rule.</summary>
static List<string> Validate(EventRequest request)
{
    var context = new ValidationContext(request);
    var results = new List<ValidationResult>();
    Validator.TryValidateObject(request, context, results, validateAllProperties: true);

    var errors = results
        .Select(r => r.ErrorMessage ?? "Invalid value.")
        .ToList();

    if (request.EndDate < request.StartDate)
    {
        errors.Add("endDate must not be before startDate.");
    }

    return errors;
}

/// <summary>Copies a request onto an entity, leaving Id and CreatedAt alone.</summary>
static void Apply(EventRequest request, EventEntity entity)
{
    entity.Title = request.Title.Trim();
    entity.Description = request.Description.Trim();
    entity.Category = string.IsNullOrWhiteSpace(request.Category)
        ? "OTHER"
        : request.Category.Trim().ToUpperInvariant();
    entity.StartDate = request.StartDate;
    entity.EndDate = request.EndDate;
    entity.VenueName = request.VenueName.Trim();
    entity.Address = request.Address.Trim();
    entity.Latitude = request.Latitude;
    entity.Longitude = request.Longitude;
    entity.ImageUrl = request.ImageUrl;
    entity.IsPublic = request.IsPublic;
    entity.OrganizerId = request.OrganizerId.Trim();
    entity.OrganizerName = request.OrganizerName.Trim();
    entity.AttendeeCount = request.AttendeeCount;
}

/// <summary>Exposed so the integration tests can drive the same host.</summary>
public partial class Program;
