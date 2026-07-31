package edu.uga.ces.service;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;
import edu.uga.ces.dto.MovieCreateRequest;
import edu.uga.ces.dto.MovieDetail;
import edu.uga.ces.dto.MovieSummary;
import edu.uga.ces.exception.MovieNotFoundException;
import edu.uga.ces.model.Movie;
import edu.uga.ces.repository.MovieRepository;
import edu.uga.ces.repository.ShowtimeRepository;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * controller asks service for data; the service uses the repo to hit the DB, 
 * then converts raw Movie entities into the right DTO shapes (in our contract)
 */
@Service
public class MovieService {

    private static final String CURRENTLY_RUNNING = "CURRENTLY_RUNNING";
    private static final String COMING_SOON = "COMING_SOON";

    private final MovieRepository movieRepository;
    private final ShowtimeRepository showtimeRepository;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    public MovieService(MovieRepository movieRepository, ShowtimeRepository showtimeRepository) {
        this.movieRepository = movieRepository;
        this.showtimeRepository = showtimeRepository;
    }

    // Home page. genre and date are both optional and combine when both are
    // given. date accepts "today", "tomorrow", or "weekend" (matching the
    // values the homepage's "Show date" dropdown sends); anything else, blank,
    // or missing skips the date filter entirely.
    public List<MovieSummary> getCurrentlyRunning(String genre, String date) {
        List<Movie> movies = (genre == null || genre.isBlank())
                ? movieRepository.findByStatusOrderByTitleAsc(CURRENTLY_RUNNING)
                : movieRepository.findByGenreIgnoreCaseAndStatusOrderByTitleAsc(genre.trim(), CURRENTLY_RUNNING);

        if (date != null && !date.isBlank()) {
            Set<Long> showingOnDate = new HashSet<>(
                    showtimeRepository.findDistinctMovieIdsByShowDateIn(datesForFilter(date)));
            movies = movies.stream().filter(movie -> showingOnDate.contains(movie.getId())).toList();
        }

        return movies.stream().map(this::toSummary).toList();
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

    // Admin "Add Movie". No custom repository method needed - JpaRepository
    // already gives us .save() for a plain insert.
    public MovieDetail createMovie(MovieCreateRequest request) {
        Movie movie = new Movie();
        movie.setTitle(request.title().trim());
        movie.setGenre(request.genre().trim());
        movie.setStatus(request.status());
        movie.setRating(request.rating());
        movie.setDescription(request.description());
        movie.setPosterUrl(request.posterUrl());
        movie.setTrailerUrl(request.trailerUrl());
        movie.setShowtimes(writeShowtimes(request.showtimes()));

        movie = movieRepository.save(movie);
        return toDetail(movie);
    }

    // "today" / "tomorrow" are single dates; "weekend" is the upcoming
    // Saturday+Sunday, inclusive of today if today already is one of them.
    // Anything unrecognized matches nothing rather than silently matching
    // everything, so a typo'd filter value shows an empty result, not the
    // unfiltered list.
    private List<LocalDate> datesForFilter(String date) {
        LocalDate today = LocalDate.now();
        return switch (date.trim().toLowerCase()) {
            case "today" -> List.of(today);
            case "tomorrow" -> List.of(today.plusDays(1));
            case "weekend" -> weekendDates(today);
            default -> List.of();
        };
    }

    private List<LocalDate> weekendDates(LocalDate today) {
        LocalDate saturday = switch (today.getDayOfWeek()) {
            case SATURDAY -> today;
            case SUNDAY -> today.minusDays(1);
            default -> today.with(TemporalAdjusters.next(DayOfWeek.SATURDAY));
        };
        return List.of(saturday, saturday.plusDays(1));
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

    // the other direction of parseShowtimes, for saving a new movie
    private String writeShowtimes(List<String> showtimes) {
        if (showtimes == null || showtimes.isEmpty()) return "[]";
        return jsonMapper.writeValueAsString(showtimes);
    }
}