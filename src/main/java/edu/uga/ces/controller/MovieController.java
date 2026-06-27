package edu.uga.ces.controller;

import edu.uga.ces.model.Movie;
import edu.uga.ces.repository.MovieRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/movies")
public class MovieController {

    private static final String AVAILABLE_STATUS = "CURRENTLY_RUNNING";

    private final MovieRepository movieRepository;

    public MovieController(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
    }

    @GetMapping
    public List<Movie> getAvailableMovies(@RequestParam(required = false) String genre) {
        if (genre == null || genre.isBlank()) {
            return movieRepository.findByStatusOrderByTitleAsc(AVAILABLE_STATUS);
        }

        return movieRepository.findByGenreIgnoreCaseAndStatusOrderByTitleAsc(genre.trim(), AVAILABLE_STATUS);
    }

    @GetMapping("/genres")
    public List<String> getAvailableGenres() {
        return movieRepository.findDistinctGenresByStatus(AVAILABLE_STATUS);
    }
}
