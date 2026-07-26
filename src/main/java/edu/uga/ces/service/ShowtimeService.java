package edu.uga.ces.service;

import edu.uga.ces.dto.ShowroomResponse;
import edu.uga.ces.dto.ShowtimeCreateRequest;
import edu.uga.ces.dto.ShowtimeMovieOption;
import edu.uga.ces.dto.ShowtimeResponse;
import edu.uga.ces.exception.MovieNotFoundException;
import edu.uga.ces.exception.ShowroomNotFoundException;
import edu.uga.ces.exception.ShowtimeConflictException;
import edu.uga.ces.model.Movie;
import edu.uga.ces.model.Showroom;
import edu.uga.ces.model.Showtime;
import edu.uga.ces.repository.MovieRepository;
import edu.uga.ces.repository.ShowroomRepository;
import edu.uga.ces.repository.ShowtimeRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;

@Service
public class ShowtimeService {

    private final ShowtimeRepository showtimeRepository;
    private final MovieRepository movieRepository;
    private final ShowroomRepository showroomRepository;

    public ShowtimeService(ShowtimeRepository showtimeRepository,
                           MovieRepository movieRepository,
                           ShowroomRepository showroomRepository) {
        this.showtimeRepository = showtimeRepository;
        this.movieRepository = movieRepository;
        this.showroomRepository = showroomRepository;
    }

    @Transactional
    public ShowtimeResponse createShowtime(ShowtimeCreateRequest request) {
        // The MySQL TIME column stores whole seconds. Normalizing here keeps the
        // service-level conflict query identical to the value enforced by MySQL.
        LocalTime showTime = request.time().withNano(0);

        Movie movie = movieRepository.findById(request.movieId())
                .orElseThrow(() -> new MovieNotFoundException(request.movieId()));
        Showroom showroom = showroomRepository.findById(request.showroomId())
                .orElseThrow(() -> new ShowroomNotFoundException(request.showroomId()));

        if (hasConflict(request, showTime)) {
            throw conflict(request, showTime);
        }

        Showtime showtime = new Showtime();
        showtime.setMovie(movie);
        showtime.setShowroom(showroom);
        showtime.setShowDate(request.date());
        showtime.setShowTime(showTime);

        try {
            // saveAndFlush makes the database unique constraint run before this
            // method returns, closing the race between the conflict check and insert.
            return toResponse(showtimeRepository.saveAndFlush(showtime));
        } catch (DataIntegrityViolationException ex) {
            throw conflict(request, showTime);
        }
    }

    @Transactional(readOnly = true)
    public List<ShowtimeResponse> getAllShowtimes() {
        return showtimeRepository.findAllForAdmin().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ShowtimeMovieOption> getMovieOptions() {
        return movieRepository.findAll().stream()
                .sorted((left, right) -> left.getTitle().compareToIgnoreCase(right.getTitle()))
                .map(movie -> new ShowtimeMovieOption(
                        movie.getId(), movie.getTitle(), movie.getStatus()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ShowroomResponse> getShowroomOptions() {
        return showroomRepository.findAllByOrderByNameAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    private boolean hasConflict(ShowtimeCreateRequest request, LocalTime showTime) {
        return showtimeRepository.existsByShowroom_IdAndShowDateAndShowTime(
                request.showroomId(), request.date(), showTime);
    }

    private ShowtimeConflictException conflict(ShowtimeCreateRequest request, LocalTime showTime) {
        return new ShowtimeConflictException(
                request.showroomId(), request.date(), showTime);
    }

    private ShowtimeResponse toResponse(Showtime showtime) {
        return new ShowtimeResponse(
                showtime.getId(),
                showtime.getMovie().getId(),
                showtime.getMovie().getTitle(),
                showtime.getShowDate(),
                showtime.getShowTime(),
                toResponse(showtime.getShowroom())
        );
    }

    private ShowroomResponse toResponse(Showroom showroom) {
        return new ShowroomResponse(
                showroom.getId(),
                showroom.getName(),
                showroom.getRowCount(),
                showroom.getSeatsPerRow()
        );
    }
}
