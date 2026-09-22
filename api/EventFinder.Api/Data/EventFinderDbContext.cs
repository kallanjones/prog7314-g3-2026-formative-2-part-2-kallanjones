using EventFinder.Api.Models;
using Microsoft.EntityFrameworkCore;

namespace EventFinder.Api.Data;

/// <summary>
/// EF Core code-first context. SQLite keeps the database to a single file so the
/// API can be hosted on any free tier without provisioning a database server.
/// </summary>
public class EventFinderDbContext(DbContextOptions<EventFinderDbContext> options) : DbContext(options)
{
    public DbSet<EventEntity> Events => Set<EventEntity>();

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        base.OnModelCreating(modelBuilder);

        // The app lists events by date, so index the column it sorts on.
        modelBuilder.Entity<EventEntity>()
            .HasIndex(e => e.StartDate);
    }
}
