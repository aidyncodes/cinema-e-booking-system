package edu.uga.ces.repository;

import edu.uga.ces.model.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** Provides the row lock needed to enforce the three-card limit concurrently. */
@Repository
public class UserLockRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public Optional<User> findByIdForUpdate(Long userId) {
        return Optional.ofNullable(entityManager.find(User.class, userId, LockModeType.PESSIMISTIC_WRITE));
    }
}
