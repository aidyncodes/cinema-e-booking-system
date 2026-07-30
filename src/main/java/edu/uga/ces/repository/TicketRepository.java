package edu.uga.ces.repository;

import edu.uga.ces.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    // All tickets for a batch of orders, so history can attach them in one query.
    List<Ticket> findByOrderIdInOrderBySeatLabelAsc(Collection<Long> orderIds);
}
