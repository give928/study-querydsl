package com.give928.querydsl.repository;

import com.give928.querydsl.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

import java.util.List;

public interface MemberRepository extends JpaRepository<Member, Long>, MemberRepositoryCustom,
        QuerydslPredicateExecutor<Member>, MemberRepositoryQuerydslSupport {
    List<Member> findByUsername(String username);
}
