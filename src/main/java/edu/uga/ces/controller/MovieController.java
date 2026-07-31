package edu.uga.ces.controller;

import edu.uga.ces.dto.MovieDetail;
import edu.uga.ces.dto.MovieSummary;
import edu.uga.ces.dto.ShowtimeResponse;
import edu.uga.ces.service.MovieService;
import edu.uga.ces.service.ShowtimeService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/movies")
public class MovieController {

    private final MovieService movieService;
    private final ShowtimeService showtimeService;

    public MovieController(MovieService movieService, ShowtimeService showtimeService) {
        this.movieService = movieService;
        this.showtimeService = showtimeService;
    }

    // Home page. genre and date are both optional filters and combine when
    // both are present (date accepts "today", "tomorrow", or "weekend").
    @GetMapping
    public List<MovieSummary> getCurrentlyRunning(@RequestParam(required = false) String genre,
                                                   @RequestParam(required = false) String date) {
        return movieService.getCurrentlyRunning(genre, date);
    }

    // home page: coming soon section
    @GetMapping("/coming-soon")
    public List<MovieSummary> getComingSoon() {
        return movieService.getComingSoon();
    }

    // genre list for filter dropdown
    @GetMapping("/genres")
    public List<String> getGenres() {
        return movieService.getCurrentlyRunningGenres();
    }

    // search by title, returns [] if nothing matches
    @GetMapping("/search")
    public List<MovieSummary> search(@RequestParam String title) {
        return movieService.searchByTitle(title);
    }

    // movie detail page (full info incl. parsed showtimes + trailer).
    @GetMapping("/{id}")
    public MovieDetail getMovieById(@PathVariable Long id) {
        return movieService.getMovieById(id);
    }

    @GetMapping("/{id}/showtimes")
    public List<ShowtimeResponse> getShowtimes(@PathVariable Long id) {
        return showtimeService.getShowtimesForMovie(id);
    }
}