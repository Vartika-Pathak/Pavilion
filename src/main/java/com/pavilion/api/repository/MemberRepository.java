package com.pavilion.api.repository;

import com.pavilion.api.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MemberRepository extends JpaRepository<Member, Long> {
    List<Member> findAllByOrderByJoinedAtAsc();
}
