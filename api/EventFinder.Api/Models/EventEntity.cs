using System.ComponentModel.DataAnnotations;

namespace EventFinder.Api.Models;

/// <summary>
/// An event stored by the API. Mirrors the Event schema in section 5.3 of the
/// Planning and Design document.
/// </summary>
public class EventEntity
{
    [Key]
    public string Id { get; set; } = Guid.NewGuid().ToString();

    [Required]
    [MaxLength(200)]
    public string Title { get; set; } = string.Empty;

    [MaxLength(4000)]
    public string Description { get; set; } = string.Empty;

    /// <summary>MUSIC, SPORTS, FOOD, COMMUNITY, ARTS, BUSINESS or OTHER.</summary>
    [MaxLength(40)]
    public string Category { get; set; } = "OTHER";

    /// <summary>Unix epoch milliseconds, matching the Android client.</summary>
    public long StartDate { get; set; }

    public long EndDate { get; set; }

    [MaxLength(200)]
    public string VenueName { get; set; } = string.Empty;

    [MaxLength(500)]
    public string Address { get; set; } = string.Empty;

    public double? Latitude { get; set; }

    public double? Longitude { get; set; }

    [MaxLength(1000)]
    public string? ImageUrl { get; set; }

    public bool IsPublic { get; set; } = true;

    [MaxLength(100)]
    public string OrganizerId { get; set; } = string.Empty;

    [MaxLength(200)]
    public string OrganizerName { get; set; } = string.Empty;

    public int AttendeeCount { get; set; }

    public long CreatedAt { get; set; } = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();

    public long UpdatedAt { get; set; } = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
}
