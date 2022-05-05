package com.give928.querydsl;

import com.give928.querydsl.entity.Member;
import com.give928.querydsl.entity.Team;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.stream.IntStream;

@Profile("local")
@Component
@RequiredArgsConstructor
public class InitMember {
    private final InitMemberService initMemberService;

    @PostConstruct
    public void init() {
        initMemberService.init();
    }

    @Component
    static class InitMemberService {
        @PersistenceContext
        private EntityManager em;

        @Transactional
        public void init() {
            Team teamA = Team.builder().name("teamA").build();
            Team teamB = Team.builder().name("teamB").build();
            em.persist(teamA);
            em.persist(teamB);

            IntStream.rangeClosed(1, 100).forEach(i -> {
                Team team = i % 2 == 0 ? teamA : teamB;
                Member member = Member.builder().username("member" + i).age(i).team(team).build();
                em.persist(member);
            });
        }
    }
}
