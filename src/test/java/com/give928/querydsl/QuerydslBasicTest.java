package com.give928.querydsl;

import com.give928.querydsl.dto.MemberDto;
import com.give928.querydsl.dto.QMemberDto;
import com.give928.querydsl.entity.Member;
import com.give928.querydsl.entity.QMember;
import com.give928.querydsl.entity.Team;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import java.util.List;
import java.util.Objects;

import static com.give928.querydsl.entity.QMember.member;
import static com.give928.querydsl.entity.QTeam.team;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class QuerydslBasicTest {
    @Autowired
    EntityManager em;

    @PersistenceUnit
    EntityManagerFactory emf;

    JPAQueryFactory queryFactory;

    @BeforeEach
    void setUp() {
        queryFactory = new JPAQueryFactory(em);

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
    void startJPQL() {
        // given
        String qlString = "select m from Member as m where m.username = :username";
        String username = "member1";

        // when
        Member findMember = em.createQuery(qlString, Member.class)
                .setParameter("username", username)
                .getSingleResult();

        // then
        assertThat(findMember.getUsername()).isEqualTo(username);
    }

    @Test
    void startQuerydsl() {
        // given
//        QMember member = new QMember("m"); // 같은 테이블을 조인해야 하는 경우가 아니면 기본 인스턴스를 사용하자
        String username = "member1";

        // when
        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq(username)) // 파라미터 바인딩 처리
                .fetchOne();

        // then
        assertThat(findMember).isNotNull();
        assertThat(findMember.getUsername()).isEqualTo(username);
    }

    @Test
    @DisplayName("검색")
    void search() {
        // given
        String username = "member1";
        int age = 10;

        // when
        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.isNotNull()
                               .and(member.username.isNotEmpty())
                               .and(member.username.eq(username))
                               .and(member.username.ne("member2"))
                               .and(member.username.like("%member%"))
                               .and(member.username.contains("member")) // like ‘%member%’ 검색
                               .and(member.username.startsWith("member")) //like ‘member%’ 검색
                               .and(member.age.eq(age))
                               .and(member.age.in(10, 20))
                               .and(member.age.notIn(0, 1))
                               .and(member.age.between(10, 30))
                               .and(member.age.goe(10))
                               .and(member.age.gt(9))
                               .and(member.age.loe(10))
                               .and(member.age.lt(11))
                )
                .fetchOne();

        // then
        assertThat(findMember).isNotNull();
        assertThat(findMember.getUsername()).isEqualTo(username);
        assertThat(findMember.getAge()).isEqualTo(age);
    }

    @Test
    @DisplayName("결과 조회 - fetch(), fetchOne(), fetchFirst(), [deprecated]fetchResult(), [deprecated]fetchCount()")
    void fetch() {
        // List
        List<Member> findMembers = queryFactory
                .selectFrom(member)
                .fetch();

        // 단 건
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        // 처음 한 건 조회
        Member firstMember = queryFactory
                .selectFrom(member)
                .fetchFirst();

        // 페이징에서 사용 - deprecated
        QueryResults<Member> memberQueryResults = queryFactory
                .selectFrom(member)
                .fetchResults();

        // count 쿼리로 변경 - deprecated
        long count1 = queryFactory
                .selectFrom(member)
                .fetchCount();

        // count 쿼리
        Long count2 = queryFactory
//                .select(Wildcard.count) //select count(*)
                .select(member.count()) //select count(member.id)
                .from(member)
                .fetchOne();
    }

    /**
     * 회원 정렬 순서
     * 1. 회원 나이 내림차순(desc)
     * 2. 회원 이름 올림차순(asc)
     * 단 2에서 회원 이름이 없으면 마지막에 출력(nulls last)
     */
    @Test
    @DisplayName("정렬 - asc(), desc(), nullsLast() , nullsFirst()")
    void sort() {
        // given
        em.persist(Member.builder().username("member5").age(100).build());
        em.persist(Member.builder().username("member6").age(100).build());
        em.persist(Member.builder().age(100).build());

        // when
        List<Member> members = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        // then
        Member member5 = members.get(0);
        Member member6 = members.get(1);
        Member memberNull = members.get(2);
        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();
    }


    @Test
    @DisplayName("페이징 - offset(), limit()")
    void paging1() {
        // when
        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1) // 0부터 시작(zero index)
                .limit(2) // 최대 2건 조회
                .fetch();

        // then
        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("페이징 - [deprecated]getResults()")
    void paging2() {
        QueryResults<Member> queryResults = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetchResults();

        assertThat(queryResults.getOffset()).isEqualTo(1);
        assertThat(queryResults.getLimit()).isEqualTo(2);
        assertThat(queryResults.getTotal()).isEqualTo(4);
        assertThat(queryResults.getResults()).hasSize(2);
    }

    @Test
    @DisplayName("페이징 - Pageable")
    void paging3() {
        // given
        Pageable pageable = PageRequest.of(0, 2);

        // when
        List<Member> content = queryFactory
                .selectFrom(member)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
//                .select(Wildcard.count) //select count(*)
                .select(member.count())
                .from(member);

        Page<Member> page = PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);

        // then
        assertThat(page.getPageable().getOffset()).isZero();
        assertThat(page.getSize()).isEqualTo(2);
        assertThat(page.getTotalElements()).isEqualTo(4);
        assertThat(page.getNumberOfElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("집합 함수 - count(), sum(), avg(), max(), min()")
    void aggregation() {
        // when
        List<Tuple> result = queryFactory
                .select(member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min())
                .from(member)
                .fetch();

        // then
        Tuple tuple = result.get(0);
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);
    }

    @Test
    @DisplayName("집합 - group by")
    void group() {
        // when
        List<Tuple> result = queryFactory
                .select(team.name,
                        member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.id)
                .having(team.id.goe(1L))
                .fetch();

        // then
        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);
        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);
        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35);
    }

    @Test
    @DisplayName("조인 - join(), innerJoin(), leftJoin(), rightJoin()")
    void join() {
        // given

        // when
        List<Member> result = queryFactory
                .selectFrom(member)
                .leftJoin(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();

        // then
        assertThat(result)
                .extracting("username")
                .containsExactly("member1", "member2");
    }

    @Test
    @DisplayName("세타 조인 - 연관관계가 없는 필드로 조인")
    void thetaJoin() {
        // given
        em.persist(Member.builder().username("teamA").build());
        em.persist(Member.builder().username("teamB").build());

        // when
        List<Member> result = queryFactory
                .select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();

        // then
        assertThat(result)
                .extracting("username")
                .containsExactly("teamA", "teamB");
    }

    @Test
    @DisplayName("조인 on절 - 조인 대상 필터링")
    void joinOnFiltering() {
        // given

        // when
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team).on(team.name.eq("teamA"))
                .fetch();
        /*
        //
        select
            member1,
            team
        from
            Member member1
        left join
            member1.team as team with team.name = ?1
        //
        select member0_.member_id as member_i1_1_0_,
               team1_.team_id     as team_id1_2_1_,
               member0_.age       as age2_1_0_,
               member0_.team_id   as team_id4_1_0_,
               member0_.username  as username3_1_0_,
               team1_.name        as name2_2_1_
        from member member0_
        left outer join team team1_ on member0_.team_id = team1_.team_id and (team1_.name = ?)
         */

        // then
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
        assertThat(result).hasSize(4);
        assertThat(Objects.requireNonNull(result.get(0).get(team)).getName()).isEqualTo("teamA");
        assertThat(Objects.requireNonNull(result.get(1).get(team)).getName()).isEqualTo("teamA");
        assertThat(result.get(2).get(team)).isNull();
        assertThat(result.get(3).get(team)).isNull();
    }

    @Test
    @DisplayName("조인 on절 - 연관관계 없는 엔티티 외부 조인")
    void joinOnNoRelation() {
        // given
        em.persist(Member.builder().username("teamA").build());
        em.persist(Member.builder().username("teamB").build());

        // when
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(team).on(member.username.eq(team.name))
                .fetch();
        /*
        //
        select
            member1,
            team
        from
            Member member1
        left join
            Team team with member1.username = team.name
        //
        select member0_.member_id as member_i1_1_0_,
               team1_.team_id     as team_id1_2_1_,
               member0_.age       as age2_1_0_,
               member0_.team_id   as team_id4_1_0_,
               member0_.username  as username3_1_0_,
               team1_.name        as name2_2_1_
        from member member0_
        left outer join team team1_ on (member0_.username = team1_.name)
         */

        // then
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
        assertThat(result).hasSize(6);
        assertThat(result.get(0).get(team)).isNull();
        assertThat(result.get(1).get(team)).isNull();
        assertThat(result.get(2).get(team)).isNull();
        assertThat(result.get(3).get(team)).isNull();
        assertThat(Objects.requireNonNull(result.get(4).get(team)).getName()).isEqualTo("teamA");
        assertThat(Objects.requireNonNull(result.get(5).get(team)).getName()).isEqualTo("teamB");
    }

    @Test
    @DisplayName("페치 조인 미적용")
    void notUseFetchJoin() {
        // given
        em.flush();
        em.clear();

        // when
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        // then
        assert findMember != null;
        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("페치 조인 미적용").isFalse();
    }

    @Test
    @DisplayName("페치 조인 적용")
    void useFetchJoin() {
        // given
        em.flush();
        em.clear();

        // when
        Member findMember = queryFactory
                .selectFrom(member)
                .innerJoin(member.team, team).fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();

        // then
        assert findMember != null;
        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("페치 조인 적용").isTrue();
    }

    @Test
    @DisplayName("서브 쿼리")
    void subQuery() {
        // given
        QMember subMember = new QMember("subMember");

        // when
        List<Tuple> result = queryFactory
                .select(member.username,
                        member.age,
                        JPAExpressions
                                .select(subMember.age.avg())
                                .from(subMember)
                )
                .from(member)
                .where(member.age.goe(
                        JPAExpressions
                                .select(subMember.age.avg())
                                .from(subMember)
                ))
                .fetch();

        // then
        for (Tuple tuple : result) {
            System.out.print("username = " + tuple.get(member.username));
            System.out.print(", age = " + tuple.get(member.age));
            System.out.println(", avg age = " + tuple.get(JPAExpressions.select(subMember.age.avg()).from(subMember)));
        }
        assertThat(result.get(0).get(JPAExpressions.select(subMember.age.avg()).from(subMember))).isEqualTo(25.0D);
        assertThat(result.get(0).get(member.username)).isEqualTo("member3");
        assertThat(result.get(0).get(member.age)).isEqualTo(30);
        assertThat(result.get(1).get(member.username)).isEqualTo("member4");
        assertThat(result.get(1).get(member.age)).isEqualTo(40);
    }

    @Test
    @DisplayName("case 문 - 간단")
    void caseSimple() {
        // given

        // when
        List<String> result = queryFactory
                .select(member.age
                                .when(10).then("열살")
                                .when(20).then("스무살")
                                .otherwise("기타"))
                .from(member)
                .fetch();

        // then
        assertThat(result)
                .containsExactly("열살", "스무살", "기타", "기타");
    }

    @Test
    @DisplayName("case 문 - CaseBuilder")
    void caseBuilder() {
        // given

        // when
        List<String> result = queryFactory
                .select(new CaseBuilder()
                                .when(member.age.between(0, 20)).then("0~20살")
                                .when(member.age.between(21, 30)).then("21~30살")
                                .otherwise("기타"))
                .from(member)
                .fetch();

        // then
        assertThat(result)
                .containsExactly("0~20살", "0~20살", "21~30살", "기타");
    }

    @Test
    @DisplayName("case 문 - order by case")
    void orderByCase() {
        // given
        NumberExpression<Integer> rankPath = new CaseBuilder()
                .when(member.age.between(0, 20)).then(2)
                .when(member.age.between(21, 30)).then(1)
                .otherwise(3);

        // when
        List<Tuple> result = queryFactory
                .select(member.username, member.age, rankPath)
                .from(member)
                .orderBy(rankPath.desc())
                .fetch();

        // then
        for (Tuple tuple : result) {
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);
            Integer rank = tuple.get(rankPath);
            System.out.println("username = " + username + " age = " + age + " rank = " + rank);
        }
        assertThat(result.get(0).get(member.username)).isEqualTo("member4");
        assertThat(result.get(1).get(member.username)).isEqualTo("member1");
        assertThat(result.get(2).get(member.username)).isEqualTo("member2");
        assertThat(result.get(3).get(member.username)).isEqualTo("member3");
    }

    @Test
    @DisplayName("상수")
    void 상수() {
        // given

        // when
        Tuple result = queryFactory
                .select(member.username, ExpressionUtils.as(Expressions.constant("A"), "constant"))
                .from(member)
                .fetchFirst();

        // then
        assertThat(result.get(ExpressionUtils.as(Expressions.constant("A"), "constant"))).isEqualTo("A");
    }

    @Test
    @DisplayName("문자더하기")
    void 문자더하기() {
        // given

        // when
        String result = queryFactory
//                .select(member.username.concat("_").concat(member.age.stringValue())) // m1 버그인지.. cast(10 as char) 하면 결과가 '1'
                .select(member.username.concat("_")
                                .concat(Expressions.stringTemplate("convert({0}, varchar)", member.age)))
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        // then
        System.out.println("result = " + result);
        assertThat(result).isEqualTo("member1_10");
    }

    @Test
    @DisplayName("프로젝션 대상이 하나")
    void projectionOne() {
        // given

        // when
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
                .fetch();

        // then
        assertThat(result).containsExactly("member1", "member2", "member3", "member4");
    }

    @Test
    @DisplayName("프로젝션 대상이 둘 이상")
    void projectionMany() {
        // given

        // when
        List<Tuple> result = queryFactory
                .select(member.username, member.age)
                .from(member)
                .fetch();

        // then
        for (Tuple tuple : result) {
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);
            System.out.print("username=" + username);
            System.out.println(", age=" + age);
        }
        assertThat(result.get(0).get(member.username)).isEqualTo("member1");
        assertThat(result.get(1).get(member.username)).isEqualTo("member2");
        assertThat(result.get(2).get(member.username)).isEqualTo("member3");
        assertThat(result.get(3).get(member.username)).isEqualTo("member4");
    }

    @Test
    @DisplayName("순수 JPA 에서 DTO 조회")
    void jpaDto() {
        // given
        String qlString = "select new com.give928.querydsl.dto.MemberDto(m.username, m.age) from Member m";

        // when
        List<MemberDto> result = em.createQuery(qlString, MemberDto.class)
                .getResultList();

        // then
        assertThat(result).hasSize(4);
        assertThat(result.get(0).getUsername()).isEqualTo("member1");
    }

    @Test
    @DisplayName("DTO 반환 - 프로퍼티 접근 - default constructor & setter method & 컬럼명 동일")
    void returnDtoByProjectionsBean() {
        // given

        // when
        List<MemberDto> result = queryFactory
                .select(Projections.bean(MemberDto.class,
                                         member.username,
                                         member.age))
                .from(member)
                .fetch();

        // then
        assertThat(result).hasSize(4);
        assertThat(result.get(0).getUsername()).isEqualTo("member1");
    }

    @Test
    @DisplayName("DTO 반환 - 필드 직접 접근 - default constructor & 컬럼명 동일")
    void returnDtoByProjectionsFields() {
        // given

        // when
        List<MemberDto> result = queryFactory
                .select(Projections.fields(MemberDto.class,
                                           member.username,
                                           member.age))
                .from(member)
                .fetch();

        // then
        assertThat(result).hasSize(4);
        assertThat(result.get(0).getUsername()).isEqualTo("member1");
    }

    @Test
    @DisplayName("DTO 반환 - 생성자 사용 - all argument constructor")
    void returnDtoByProjectionsConstructor() {
        // given

        // when
        List<MemberDto> result = queryFactory
                .select(Projections.constructor(MemberDto.class,
                                           member.username,
                                           member.age))
                .from(member)
                .fetch();

        // then
        assertThat(result).hasSize(4);
        assertThat(result.get(0).getUsername()).isEqualTo("member1");
    }

    @Test
    @DisplayName("DTO 반환 - @QueryProjection 활용")
    void returnDtoByQueryProjection() {
        // given

        // when
        List<MemberDto> result = queryFactory
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();

        // then
        assertThat(result).hasSize(4);
        assertThat(result.get(0).getUsername()).isEqualTo("member1");
    }

    @Test
    @DisplayName("distinct")
    void distinct() {
        // given

        // when
        List<String> result = queryFactory
                .select(member.username).distinct()
                .from(member)
                .fetch();

        // then
        assertThat(result).hasSize(4);
        assertThat(result.get(0)).isEqualTo("member1");
    }

    @Test
    @DisplayName("동적 쿼리 - BooleanBuilder")
    void dynamicQueryBooleanBuilder() {
        // given
        String usernameParam = "member1";
        Integer ageParam = 10;

        BooleanBuilder booleanBuilder = getBooleanBuilder(usernameParam, ageParam);

        // when
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(booleanBuilder)
                .fetch();

        // then
        assertThat(result).hasSize(1);
    }
    private BooleanBuilder getBooleanBuilder(String usernameCond, Integer ageCond) {
        BooleanBuilder builder = new BooleanBuilder();
        if (usernameCond != null) {
            builder.and(member.username.eq(usernameCond));
        }
        if (ageCond != null) {
            builder.and(member.age.eq(ageCond));
        }
        return builder;
    }

    @Test
    @DisplayName("동적 쿼리 - where 다중 파라미터")
    void dynamicQueryWhereMultiParameters() {
        // given
        String usernameParam = "member1";
        Integer ageParam = 10;

        // when
        List<Member> result = queryFactory
                .selectFrom(member)
//                .where(usernameEq(usernameParam), ageEq(ageParam))
                .where(usernameEqAndAgeEq(usernameParam, ageParam))
                .fetch();

        // then
        assertThat(result).hasSize(1);
    }
    private BooleanExpression usernameEq(String usernameCond) {
        return usernameCond != null ? member.username.eq(usernameCond) : null;
    }
    private BooleanExpression ageEq(Integer ageCond) {
        return ageCond != null ? member.age.eq(ageCond) : null;
    }
    private BooleanExpression usernameEqAndAgeEq(String usernameCond, Integer ageCond) {
        BooleanExpression usernameExpression = usernameEq(usernameCond);
        BooleanExpression ageExpression = ageEq(ageCond);
        if (usernameExpression != null) {
            return usernameExpression.and(ageExpression);
        }
        return ageExpression;
    }

    @Test
    @DisplayName("벌크 연산 - 수정")
    void bulkUpdate() {
        // given

        // when
        long count = queryFactory
                .update(member)
                .set(member.age, member.age.add(1))
                .set(member.username, "비회원")
                .where(member.age.lt(28))
                .execute();
        /*
        //update Member member1
        set member1.username = '비회원'1
        where member1.age < 28//
        update member set username='비회원'1 where age<28;
         */

        // then
        assertThat(count).isEqualTo(2L);

        List<Member> members1 = queryFactory
                .selectFrom(member)
                .fetch();
        for (Member member1 : members1) {
            System.out.println("member1 = " + member1);
        }
        /*
        member1 = Member(id=1, username=member1, age=10)
        member1 = Member(id=2, username=member2, age=20)
        member1 = Member(id=3, username=member3, age=30)
        member1 = Member(id=4, username=member4, age=40)
         */
        // JPQL 배치와 마찬가지로, 영속성 컨텍스트에 있는 엔티티를 무시하고 실행되기 때문에
        // 배치 쿼리를 실행하고 나면 영속성 컨텍스트를 초기화 하는 것이 안전하다.
        em.flush();
        em.clear();

        List<Member> members2 = queryFactory
                .selectFrom(member)
                .fetch();
        for (Member member2 : members2) {
            System.out.println("member2 = " + member2);
        }
        /*
        member2 = Member(id=1, username=비회원, age=10)
        member2 = Member(id=2, username=비회원, age=20)
        member2 = Member(id=3, username=member3, age=30)
        member2 = Member(id=4, username=member4, age=40)
         */
    }

    @Test
    @DisplayName("벌크 연산 - 삭제")
    void bulkDelete() {
        // given

        // when
        long updateCount = queryFactory
                .delete(member)
                .where(member.age.lt(28))
                .execute();

        // then
        assertThat(updateCount).isEqualTo(2L);
    }

    @Test
    @DisplayName("SQL function 호출")
    void callSqlFunction() {
        // given

        // when
        String result = queryFactory
                .select(Expressions.stringTemplate(
                        "function('replace', {0}, {1}, {2})",
                        member.username,
                        "member",
                        "M"))
                .from(member)
                .fetchFirst();

        // then
        assertThat(result).isEqualTo("M1");
    }
}
