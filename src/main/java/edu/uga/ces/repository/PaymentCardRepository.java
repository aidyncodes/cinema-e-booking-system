package edu.uga.ces.repository;

import edu.uga.ces.model.PaymentCard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentCardRepository extends JpaRepository<PaymentCard, Long> {
    List<PaymentCard> findByUserIdOrderByIdAsc(Long userId);
    long countByUserId(Long userId);
    Optional<PaymentCard> findByIdAndUserId(Long id, Long userId);
}
