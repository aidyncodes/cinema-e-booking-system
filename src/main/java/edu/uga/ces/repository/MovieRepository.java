package edu.uga.ces.repository;

import edu.uga.ces.model.Movie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MovieRepository extends JpaRepository<Movie, Long> {
    List<Movie> findByStatusOrderByTitleAsc(String status);

    List<Movie> findByGenreIgnoreCaseAndStatusOrderByTitleAsc(String genre, String status);

    @Query("select distinct m.genre from Movie m where m.status = :status order by m.genre")
    List<String> findDistinctGenresByStatus(@Param("status") String status);
}
