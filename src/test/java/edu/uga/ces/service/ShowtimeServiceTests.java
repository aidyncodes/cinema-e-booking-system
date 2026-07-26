package edu.uga.ces.service;

import edu.uga.ces.dto.ShowtimeCreateRequest;
import edu.uga.ces.dto.ShowtimeResponse;
import edu.uga.ces.exception.MovieNotFoundException;
import edu.uga.ces.exception.ShowtimeConflictException;
import edu.uga.ces.model.Movie;
import edu.uga.ces.model.Showroom;
import edu.uga.ces.model.Showtime;
import edu.uga.ces.repository.MovieRepository;
import edu.uga.ces.repository.ShowroomRepository;
import edu.uga.ces.repository.ShowtimeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShowtimeServiceTests {

    @Mock
    private ShowtimeRepository showtimeRepository;

    @Mock
    private MovieRepository movieRepository;

    @Mock
    private ShowroomRepository showroomRepository;

    private ShowtimeService showtimeService;

    private final ShowtimeCreateRequest request = new ShowtimeCreateRequest(
            12L,
            LocalDate.of(2099, 8, 15),
            LocalTime.of(19, 30),
            3L
    );

    @BeforeEach
    void setUp() {
        showtimeService = new ShowtimeService(
                showtimeRepository, movieRepository, showroomRepository);
    }

    @Test
    void createsShowtimeWhenSlotIsAvailable() {
        Movie movie = movie(12L, "The Test Feature");
        Showroom showroom = showroom(3L, "Showroom 3");

        when(movieRepository.findById(12L)).thenReturn(Optional.of(movie));
        when(showroomRepository.findById(3L)).thenReturn(Optional.of(showroom));
        when(showtimeRepository.existsByShowroom_IdAndShowDateAndShowTime(
                3L, request.date(), request.time())).thenReturn(false);
        when(showtimeRepository.saveAndFlush(any(Showtime.class)))
                .thenAnswer(invocation -> {
                    Showtime saved = invocation.getArgument(0);
                    saved.setId(42L);
                    return saved;
                });

        ShowtimeResponse result = showtimeService.createShowtime(request);

        assertEquals(42L, result.id());
        assertEquals(12L, result.movieId());
        assertEquals("The Test Feature", result.movieTitle());
        assertEquals(3L, result.showroom().id());

        ArgumentCaptor<Showtime> captor = ArgumentCaptor.forClass(Showtime.class);
        verify(showtimeRepository).saveAndFlush(captor.capture());
        assertEquals(request.date(), captor.getValue().getShowDate());
        assertEquals(request.time(), captor.getValue().getShowTime());
    }

    @Test
    void rejectsConflictBeforeInsert() {
        when(movieRepository.findById(12L))
                .thenReturn(Optional.of(movie(12L, "The Test Feature")));
        when(showroomRepository.findById(3L))
                .thenReturn(Optional.of(showroom(3L, "Showroom 3")));
        when(showtimeRepository.existsByShowroom_IdAndShowDateAndShowTime(
                3L, request.date(), request.time())).thenReturn(true);

        assertThrows(ShowtimeConflictException.class,
                () -> showtimeService.createShowtime(request));

        verify(showtimeRepository, never()).saveAndFlush(any(Showtime.class));
    }

    @Test
    void mapsDatabaseUniqueConstraintRaceToConflict() {
        when(movieRepository.findById(12L))
                .thenReturn(Optional.of(movie(12L, "The Test Feature")));
        when(showroomRepository.findById(3L))
                .thenReturn(Optional.of(showroom(3L, "Showroom 3")));
        when(showtimeRepository.existsByShowroom_IdAndShowDateAndShowTime(
                3L, request.date(), request.time())).thenReturn(false);
        when(showtimeRepository.saveAndFlush(any(Showtime.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate slot"));

        assertThrows(ShowtimeConflictException.class,
                () -> showtimeService.createShowtime(request));
    }

    @Test
    void rejectsUnknownMovie() {
        when(movieRepository.findById(12L)).thenReturn(Optional.empty());

        assertThrows(MovieNotFoundException.class,
                () -> showtimeService.createShowtime(request));

        verify(showroomRepository, never()).findById(any());
        verify(showtimeRepository, never()).saveAndFlush(any(Showtime.class));
    }

    @Test
    void returnsRealShowtimesForMovie() {
        Movie movie = movie(12L, "The Test Feature");
        Showroom showroom = showroom(3L, "Showroom 3");
        Showtime showtime = new Showtime();
        showtime.setId(42L);
        showtime.setMovie(movie);
        showtime.setShowroom(showroom);
        showtime.setShowDate(request.date());
        showtime.setShowTime(request.time());

        when(movieRepository.existsById(12L)).thenReturn(true);
        when(showtimeRepository.findAllByMovieId(12L)).thenReturn(List.of(showtime));

        List<ShowtimeResponse> result = showtimeService.getShowtimesForMovie(12L);

        assertEquals(1, result.size());
        assertEquals(42L, result.get(0).id());
        assertEquals("The Test Feature", result.get(0).movieTitle());
        assertEquals("Showroom 3", result.get(0).showroom().name());
        assertEquals(request.date(), result.get(0).date());
        assertEquals(request.time(), result.get(0).time());
    }

    @Test
    void rejectsShowtimeLookupForUnknownMovie() {
        when(movieRepository.existsById(12L)).thenReturn(false);

        assertThrows(MovieNotFoundException.class,
                () -> showtimeService.getShowtimesForMovie(12L));

        verify(showtimeRepository, never()).findAllByMovieId(12L);
    }

    private Movie movie(Long id, String title) {
        Movie movie = new Movie();
        movie.setId(id);
        movie.setTitle(title);
        movie.setStatus("CURRENTLY_RUNNING");
        return movie;
    }

    private Showroom showroom(Long id, String name) {
        Showroom showroom = new Showroom();
        showroom.setId(id);
        showroom.setName(name);
        showroom.setRowCount(8);
        showroom.setSeatsPerRow(12);
        return showroom;
    }
}
