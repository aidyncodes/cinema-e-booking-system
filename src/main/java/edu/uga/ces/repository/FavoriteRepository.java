package edu.uga.ces.repository;

import edu.uga.ces.model.Favorite;
import edu.uga.ces.model.FavoriteId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FavoriteRepository extends JpaRepository<Favorite, FavoriteId> {

    boolean existsByUserIdAndMovieId(Long userId, Long movieId);

    List<Favorite> findByUserIdOrderByCreatedAtDesc(Long userId);
}
