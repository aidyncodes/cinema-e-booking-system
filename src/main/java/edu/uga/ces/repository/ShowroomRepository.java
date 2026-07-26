package edu.uga.ces.repository;

import edu.uga.ces.model.Showroom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShowroomRepository extends JpaRepository<Showroom, Long> {

    List<Showroom> findAllByOrderByNameAsc();
}
