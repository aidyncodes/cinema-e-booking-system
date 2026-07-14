package edu.uga.ces.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Composite primary key for Favorite (user_id + movie_id together).
 * Required by JPA's @IdClass: must be Serializable and implement equals/hashCode.
 */
public class FavoriteId implements Serializable {

    private Long userId;
    private Long movieId;

    public FavoriteId() {}

    public FavoriteId(Long userId, Long movieId) {
        this.userId = userId;
        this.movieId = movieId;
    }

    public Long getUserId() { return userId; }
    public Long getMovieId() { return movieId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FavoriteId that)) return false;
        return Objects.equals(userId, that.userId) && Objects.equals(movieId, that.movieId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, movieId);
    }
}
