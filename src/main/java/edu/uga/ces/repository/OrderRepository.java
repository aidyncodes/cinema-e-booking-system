package edu.uga.ces.repository;

import edu.uga.ces.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    // A user's orders for the history page, most recently placed first.
    // Id is the tiebreaker so any not-yet-placed orders stay newest-first too.
    List<Order> findByUserIdOrderByPlacedAtDescIdDesc(Long userId);
}
