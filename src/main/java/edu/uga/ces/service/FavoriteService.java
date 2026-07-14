package edu.uga.ces.service;

import edu.uga.ces.dto.MovieSummary;
import edu.uga.ces.exception.MovieNotFoundException;
import edu.uga.ces.model.Favorite;
import edu.uga.ces.model.FavoriteId;
import edu.uga.ces.model.Movie;
import edu.uga.ces.repository.FavoriteRepository;
import edu.uga.ces.repository.MovieRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Favorites logic: add/remove a movie for a user, and list a user's favorites.
 * The controller resolves who the user is (from the session) and passes the id in.
 */
@Service
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final MovieRepository movieRepository;

    public FavoriteService(FavoriteRepository favoriteRepository, MovieRepository movieRepository) {
        this.favoriteRepository = favoriteRepository;
        this.movieRepository = movieRepository;
    }

    // Add a favorite. Idempotent (re-adding does nothing). Rejects unknown movies.
    public void addFavorite(Long userId, Long movieId) {
        if (!movieRepository.existsById(movieId)) {
            throw new MovieNotFoundException(movieId);
        }
        if (!favoriteRepository.existsByUserIdAndMovieId(userId, movieId)) {
            favoriteRepository.save(new Favorite(userId, movieId));
        }
    }

    // Remove a favorite. Idempotent (removing one that isn't there is a no-op).
    public void removeFavorite(Long userId, Long movieId) {
        FavoriteId key = new FavoriteId(userId, movieId);
        if (favoriteRepository.existsById(key)) {
            favoriteRepository.deleteById(key);
        }
    }

    // The user's favorite movies, newest first, in the same shape as list views.
    public List<MovieSummary> getFavorites(Long userId) {
        List<Favorite> favorites = favoriteRepository.findByUserIdOrderByCreatedAtDesc(userId);
        if (favorites.isEmpty()) {
            return List.of();
        }

        List<Long> movieIds = favorites.stream().map(Favorite::getMovieId).toList();
        Map<Long, Movie> moviesById = movieRepository.findAllById(movieIds).stream()
                .collect(Collectors.toMap(Movie::getId, Function.identity()));

        // Keep the favorites ordering; skip any movie that no longer exists.
        return movieIds.stream()
                .map(moviesById::get)
                .filter(Objects::nonNull)
                .map(this::toSummary)
                .toList();
    }

    private MovieSummary toSummary(Movie m) {
        return new MovieSummary(
                m.getId(), m.getTitle(), m.getGenre(),
                m.getRating(), m.getPosterUrl(), m.getStatus());
    }
}
