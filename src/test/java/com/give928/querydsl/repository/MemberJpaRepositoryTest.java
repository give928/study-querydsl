package com.give928.querydsl.repository;

import com.give928.querydsl.dto.MemberSearchCondition;
import com.give928.querydsl.dto.MemberTeamDto;
import com.give928.querydsl.entity.Member;
import com.give928.querydsl.entity.Team;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class MemberJpaRepositoryTest {
    @Autowired
    EntityManager em;

    @Autowired
    MemberJpaRepository memberJpaRepository;

    @Test
    @DisplayName("JPA 테스트")
    void basicTest() {
        // given
        Member member = Member.builder().username("member1").age(10).build();
        memberJpaRepository.save(member);

        // when
        Member findMember = memberJpaRepository.findById(member.getId()).orElseThrow(IllegalStateException::new);
        List<Member> result1 = memberJpaRepository.findAll();
        List<Member> result2 = memberJpaRepository.findByUsername("member1");

        // then
        assertThat(findMember).isEqualTo(member);
        assertThat(result1).containsExactly(member);
        assertThat(result2).containsExactly(member);
    }

    @Test
    @DisplayName("Querydsl 테스트")
    void basicQuerydslTest() {
        // given
        Member member = Member.builder().username("member1").age(10).build();
        memberJpaRepository.save(member);

        // when
        Member findMember = memberJpaRepository.findById(member.getId()).orElseThrow(IllegalStateException::new);
        List<Member> result1 = memberJpaRepository.findAllQuerydsl();
        List<Member> result2 = memberJpaRepository.findByUsernameQuerydsl("member1");

        // then
        assertThat(findMember).isEqualTo(member);
        assertThat(result1).containsExactly(member);
        assertThat(result2).containsExactly(member);
    }

    @Test
    @DisplayName("동적쿼리 - Builder 사용")
    void searchByBuilder() {
        // given
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

        MemberSearchCondition condition = MemberSearchCondition.builder()
                .ageGoe(35)
                .ageLoe(40)
                .teamName("teamB")
                .build();

        // when
        List<MemberTeamDto> result = memberJpaRepository.searchByBuilder(condition);

        // then
        assertThat(result).extracting("username").containsExactly("member4");
    }

    @Test
    @DisplayName(" 동적 쿼리와 성능 최적화 조회 - Where절 파라미터 사용")
    void search1() {
        // given
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

        MemberSearchCondition condition = MemberSearchCondition.builder()
                .ageGoe(35)
                .ageLoe(40)
                .teamName("teamB")
                .build();

        // when
        List<MemberTeamDto> result = memberJpaRepository.search(condition);

        // then
        assertThat(result).extracting("username").containsExactly("member4");
    }

    @Test
    @DisplayName(" 동적 쿼리와 성능 최적화 조회 - Where절 파라미터 사용")
    void search2() {
        // given
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

        MemberSearchCondition condition = MemberSearchCondition.builder()
                .ageGoe(19)
                .ageLoe(20)
                .build();

        // when
        List<Member> result = memberJpaRepository.findMember(condition);

        // then
        assertThat(result).extracting("username").containsExactly("member2");
    }
}
