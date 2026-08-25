package com.blackout.repository;

import com.blackout.entity.DeadDrop;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Spring Data JPA repository for buried dead drops. Query methods are derived from
 * method names - no SQL required.
 */
public interface DeadDropRepository extends JpaRepository<DeadDrop, Long> {

    /** Newest first - the wiretap feed reads in reverse-chronological order. */
    List<DeadDrop> findAllByOrderByTimestampDesc();
}
