using System.ComponentModel.DataAnnotations;

namespace EventFinder.Api.Models;

/// <summary>
/// The response envelope described in section 5.1 of the Planning and Design
/// document. Every endpoint returns this shape so the Android client can handle
/// outcomes the same way regardless of which endpoint it called.
/// </summary>
public record ApiResponse<T>(bool Success, T? Data, string? Message = null, IEnumerable<string>? Errors = null)
{
    public static ApiResponse<T> Ok(T data, string? message = null) => new(true, data, message);

    public static ApiResponse<T> Fail(string message, IEnumerable<string>? errors = null) =>
        new(false, default, message, errors);
}

/// <summary>Body accepted when creating or updating an event.</summary>
public class EventRequest
{
    [Required]
    [MaxLength(200)]
    public string Title { get; set; } = string.Empty;

    [MaxLength(4000)]
    public string Description { get; set; } = string.Empty;

    [MaxLength(40)]
    public string Category { get; set; } = "OTHER";

    [Range(1, long.MaxValue, ErrorMessage = "startDate must be a positive epoch-millisecond value.")]
    public long StartDate { get; set; }

    [Range(1, long.MaxValue, ErrorMessage = "endDate must be a positive epoch-millisecond value.")]
    public long EndDate { get; set; }

    [MaxLength(200)]
    public string VenueName { get; set; } = string.Empty;

    [MaxLength(500)]
    public string Address { get; set; } = string.Empty;

    [Range(-90, 90)]
    public double? Latitude { get; set; }

    [Range(-180, 180)]
    public double? Longitude { get; set; }

    [MaxLength(1000)]
    public string? ImageUrl { get; set; }

    public bool IsPublic { get; set; } = true;

    [MaxLength(100)]
    public string OrganizerId { get; set; } = string.Empty;

    [MaxLength(200)]
    public string OrganizerName { get; set; } = string.Empty;

    public int AttendeeCount { get; set; }
}
