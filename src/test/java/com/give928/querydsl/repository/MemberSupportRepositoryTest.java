package com.give928.querydsl.repository;

import com.give928.querydsl.dto.MemberSearchCondition;
import com.give928.querydsl.entity.Member;
import com.give928.querydsl.entity.Team;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class MemberSupportRepositoryTest {
    @Autowired
    EntityManager em;

    @Autowired
    MemberSupportRepository memberSupportRepository;

    @BeforeEach
    void setUp() {
        Team teamA = Team.builder().name("teamA").build();
        Team teamB = Team.builder().name("teamB").build();
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = Member.builder().username("member1").age(10).team(teamA).build();
        Member member2 = Member.builder().username("member2").age(20).team(teamA).build();
        Member member3 = Member.builder().username("member3").age(30).team(teamB).build();
        Member member4 = Member.builder().username("member4").age(40).team(teamB).build();
        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
    }

    @Test
    @DisplayName("Querydsl 지원 클래스 basicSelect")
    void basicSelect() {
        // given

        // when
        List<Member> members = memberSupportRepository.basicSelect();

        // then
        assertThat(members).hasSize(4);
        assertThat(members).extracting("username")
                .containsExactly("member1", "member2", "member3", "member4");
    }

    @Test
    @DisplayName("Querydsl 지원 클래스 basicSelectFrom")
    void basicSelectFrom() {
        // given

        // when
        List<Member> members = memberSupportRepository.basicSelectFrom();

        // then
        assertThat(members).hasSize(4);
        assertThat(members).extracting("username")
                .containsExactly("member1", "member2", "member3", "member4");
    }

    @Test
    @DisplayName("Querydsl 지원 클래스 applyPagination")
    void applyPagination() {
        // given
        MemberSearchCondition condition = MemberSearchCondition.builder().build();
        PageRequest pageRequest = PageRequest.of(0, 3, Sort.by(Sort.Direction.ASC, "username")); // Sort 기능이 정상 동작하지 않음
//        PageRequest pageRequest = PageRequest.of(0, 3);

        // when
        Page<Member> page = memberSupportRepository.applyPagination(condition, pageRequest);

        // then
        assertThat(page.getPageable().getOffset()).isZero();
        assertThat(page.getSize()).isEqualTo(3);
        assertThat(page.getTotalElements()).isEqualTo(4);
        assertThat(page.getNumberOfElements()).isEqualTo(3);
        assertThat(page.getContent()).extracting("username").containsExactly("member1", "member2", "member3");
    }
}
