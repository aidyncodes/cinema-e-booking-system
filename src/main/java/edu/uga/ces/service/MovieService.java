package edu.uga.ces.service;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;
import edu.uga.ces.dto.MovieDetail;
import edu.uga.ces.dto.MovieSummary;
import edu.uga.ces.exception.MovieNotFoundException;
import edu.uga.ces.model.Movie;
import edu.uga.ces.repository.MovieRepository;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * controller asks service for data; the service uses the repo to hit the DB, 
 * then converts raw Movie entities into the right DTO shapes (in our contract)
 */
@Service
public class MovieService {

    private static final String CURRENTLY_RUNNING = "CURRENTLY_RUNNING";
    private static final String COMING_SOON = "COMING_SOON";

    private final MovieRepository movieRepository;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    public MovieService(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
    }

    public List<MovieSummary> getCurrentlyRunning() {
        return movieRepository.findByStatusOrderByTitleAsc(CURRENTLY_RUNNING)
                .stream().map(this::toSummary).toList();
    }

    public List<MovieSummary> getCurrentlyRunningByGenre(String genre) {
        return movieRepository
                .findByGenreIgnoreCaseAndStatusOrderByTitleAsc(genre.trim(), CURRENTLY_RUNNING)
                .stream().map(this::toSummary).toList();
    }

    public List<MovieSummary> getComingSoon() {
        return movieRepository.findByStatusOrderByTitleAsc(COMING_SOON)
                .stream().map(this::toSummary).toList();
    }

    public List<MovieSummary> searchByTitle(String title) {
        return movieRepository.findByTitleContainingIgnoreCaseOrderByTitleAsc(title.trim())
                .stream().map(this::toSummary).toList();
    }

    public List<String> getCurrentlyRunningGenres() {
        return movieRepository.findDistinctGenresByStatus(CURRENTLY_RUNNING);
    }

    public MovieDetail getMovieById(Long id) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new MovieNotFoundException(id));
        return toDetail(movie);
    }

    // mapping helpers, entity to DTO

    private MovieSummary toSummary(Movie m) {
        return new MovieSummary(
                m.getId(), m.getTitle(), m.getGenre(),
                m.getRating(), m.getPosterUrl(), m.getStatus());
    }

    private MovieDetail toDetail(Movie m) {
        return new MovieDetail(
                m.getId(), m.getTitle(), m.getGenre(), m.getRating(),
                m.getDescription(), m.getPosterUrl(), m.getTrailerUrl(),
                m.getStatus(), parseShowtimes(m.getShowtimes()));
    }

    // turn stored JSON array str into list<string>
    private List<String> parseShowtimes(String raw) {
        if (raw == null || raw.isBlank()) return Collections.emptyList();
        try {
            return jsonMapper.readValue(raw, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return Collections.emptyList(); // bad data shouldn't crash the page
        }
    }
}