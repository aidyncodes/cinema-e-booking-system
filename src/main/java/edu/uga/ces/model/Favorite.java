package edu.uga.ces.model;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * One "user favorited a movie" row in the favorites join table.
 * Uses a composite key (user_id + movie_id) via @IdClass, so there is no
 * surrogate id column here, unlike Movie/User.
 */
@Entity
@Table(name = "favorites")
@IdClass(FavoriteId.class)
public class Favorite {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Id
    @Column(name = "movie_id")
    private Long movieId;

    // DB-managed (DEFAULT CURRENT_TIMESTAMP); the app never writes it.
    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    public Favorite() {}

    public Favorite(Long userId, Long movieId) {
        this.userId = userId;
        this.movieId = movieId;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getMovieId() { return movieId; }
    public void setMovieId(Long movieId) { this.movieId = movieId; }

    public Instant getCreatedAt() { return createdAt; }
}
