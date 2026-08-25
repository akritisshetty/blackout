package com.blackout.repository;

import com.blackout.entity.Agent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for agent dossiers. Query methods are derived from
 * method names - no SQL required.
 */
public interface AgentRepository extends JpaRepository<Agent, Long> {

    Optional<Agent> findByCodenameIgnoreCase(String codename);

    boolean existsByCodenameIgnoreCase(String codename);

    /** Leaderboard order - top scorers first, ties broken by earliest enlistment. */
    List<Agent> findAllByOrderByScoreDescCreatedAtAsc();
}
