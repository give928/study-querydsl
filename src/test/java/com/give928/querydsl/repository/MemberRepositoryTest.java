package com.give928.querydsl.repository;

import com.give928.querydsl.dto.MemberSearchCondition;
import com.give928.querydsl.dto.MemberTeamDto;
import com.give928.querydsl.entity.Member;
import com.give928.querydsl.entity.QMember;
import com.give928.querydsl.entity.Team;
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
class MemberRepositoryTest {
    @Autowired
    EntityManager em;

    @Autowired
    MemberRepository memberRepository;

    @Test
    @DisplayName("Querydsl 테스트")
    void springDataJpaQuerydsl() {
        // given
        Member member = Member.builder().username("member1").age(10).build();
        memberRepository.save(member);

        // when
        Member findMember = memberRepository.findById(member.getId()).orElseThrow(IllegalStateException::new);
        List<Member> result1 = memberRepository.findAll();
        List<Member> result2 = memberRepository.findByUsername("member1");

        // then
        assertThat(findMember).isEqualTo(member);
        assertThat(result1).containsExactly(member);
        assertThat(result2).containsExactly(member);
    }

    @Test
    @DisplayName("사용자 정의 리포지토리 테스트")
    void springDataJpaQuerydslCustomRepository() {
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
        List<MemberTeamDto> result = memberRepository.search(condition);

        // then
        assertThat(result).extracting("username").containsExactly("member2");
    }

    @Test
    @DisplayName("단순한 페이징, fetchResults() 사용")
    void searchPageSimple() {
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

        MemberSearchCondition condition = MemberSearchCondition.builder().build();
        PageRequest pageRequest = PageRequest.of(0, 3);

        // when
        Page<MemberTeamDto> page = memberRepository.searchPageSimple(condition, pageRequest);

        // then
        assertThat(page.getPageable().getOffset()).isZero();
        assertThat(page.getSize()).isEqualTo(3);
        assertThat(page.getTotalElements()).isEqualTo(4);
        assertThat(page.getNumberOfElements()).isEqualTo(3);
        assertThat(page.getContent()).extracting("username").containsExactly("member1", "member2", "member3");
    }

    @Test
    @DisplayName("복잡한 페이징, 데이터 조회 쿼리와 전체 카운트 쿼리를 분리")
    void searchPageComplex() {
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

        MemberSearchCondition condition = MemberSearchCondition.builder().build();
        PageRequest pageRequest = PageRequest.of(0, 3, Sort.by(Sort.Direction.DESC, "username"));

        // when
        Page<MemberTeamDto> page = memberRepository.searchPageComplex(condition, pageRequest);

        // then
        assertThat(page.getPageable().getOffset()).isZero();
        assertThat(page.getSize()).isEqualTo(3);
        assertThat(page.getTotalElements()).isEqualTo(4);
        assertThat(page.getNumberOfElements()).isEqualTo(3);
        assertThat(page.getContent()).extracting("username").containsExactly("member1", "member2", "member3");
    }

    @Test
    @DisplayName("페이징 count 쿼리가 생략 가능한 경우 실행하지 않는다.")
    void noCountQuery() {
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

        MemberSearchCondition condition = MemberSearchCondition.builder().build();
        PageRequest pageRequest = PageRequest.of(1, 3, Sort.by(Sort.Direction.DESC, "username"));

        // when
        Page<MemberTeamDto> page = memberRepository.searchPageComplex(condition, pageRequest);

        // then
        assertThat(page.getPageable().getOffset()).isEqualTo(3);
        assertThat(page.getSize()).isEqualTo(3);
        assertThat(page.getTotalElements()).isEqualTo(4);
        assertThat(page.getNumberOfElements()).isEqualTo(1);
        assertThat(page.getContent()).extracting("username").containsExactly("member4");
    }

    @Test
    @DisplayName("스프링 데이터 JPA가 제공하는 Querydsl 기능 - QuerydslPredicateExecutor")
    void querydslPredicateExecutor() {
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

        // when
        Iterable<Member> result = memberRepository.findAll(
                QMember.member.age.between(10, 40).and(QMember.member.username.eq("member1"))
        );

        // then
        for (Member member : result) {
            assertThat(member).isNotNull();
            System.out.println("member = " + member);
        }
    }

    @Test
    @DisplayName("스프링 데이터 JPA가 제공하는 Querydsl 기능 - QuerydslRepositorySupport")
    void querydslRepositorySupport() {
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

        MemberSearchCondition condition = MemberSearchCondition.builder().build();
//        PageRequest pageRequest = PageRequest.of(0, 3, Sort.by(Sort.Direction.DESC, "username")); // Sort 기능이 정상 동작하지 않음
        PageRequest pageRequest = PageRequest.of(0, 3);

        // when
        Page<MemberTeamDto> page = memberRepository.searchPageByQuerydslRepositorySupport(condition, pageRequest);

        // then
        assertThat(page.getPageable().getOffset()).isZero();
        assertThat(page.getSize()).isEqualTo(3);
        assertThat(page.getTotalElements()).isEqualTo(4);
        assertThat(page.getNumberOfElements()).isEqualTo(3);
        assertThat(page.getContent()).extracting("username").containsExactly("member1", "member2", "member3");
    }
}
