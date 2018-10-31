package com.honji.oa.repository;

import com.honji.oa.domain.Repair;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RepairRepository extends JpaRepository<Repair, Long> {
    Page<Repair> findByApplicantId(String userId, Pageable pageable);
}
