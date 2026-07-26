package edu.uga.ces.repository;

import edu.uga.ces.model.Showtime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface ShowtimeRepository extends JpaRepository<Showtime, Long> {

    boolean existsByShowroom_IdAndShowDateAndShowTime(
            Long showroomId, LocalDate showDate, LocalTime showTime);

    @Query("""
            select s
            from Showtime s
            join fetch s.movie
            join fetch s.showroom
            order by s.showDate, s.showTime, s.showroom.name
            """)
    List<Showtime> findAllForAdmin();

    @Query("""
            select s
            from Showtime s
            join fetch s.movie
            join fetch s.showroom
            where s.movie.id = :movieId
            order by s.showDate, s.showTime, s.showroom.name
            """)
    List<Showtime> findAllByMovieId(@Param("movieId") Long movieId);
}
