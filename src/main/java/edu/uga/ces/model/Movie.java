package edu.uga.ces.model;

import jakarta.persistence.*;

/**
 * Movie entity, one Java object per row in the movies table.
 * JPA reads/writes this automatically. we never write SQL by hand for basic reads.
 * Column names that differ from the field name are mapped with @Column.
 */
@Entity
@Table(name = "movies")
public class Movie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String genre;

    @Column(nullable = false)
    private String status;

    private String rating;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "poster_url")
    private String posterUrl;

    @Column(name = "trailer_url")
    private String trailerUrl;

    // Stored in the DB as a JSON-array string, ["2:00 PM","5:00 PM"].
    // We parse it into a List<String> in the service before sending out
    @Column(columnDefinition = "TEXT")
    private String showtimes;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRating() { return rating; }
    public void setRating(String rating) { this.rating = rating; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPosterUrl() { return posterUrl; }
    public void setPosterUrl(String posterUrl) { this.posterUrl = posterUrl; }

    public String getTrailerUrl() { return trailerUrl; }
    public void setTrailerUrl(String trailerUrl) { this.trailerUrl = trailerUrl; }

    public String getShowtimes() { return showtimes; }
    public void setShowtimes(String showtimes) { this.showtimes = showtimes; }
}