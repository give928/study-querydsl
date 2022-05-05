package com.give928.querydsl;

import com.give928.querydsl.entity.Hello;
import com.give928.querydsl.entity.QHello;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class QuerydslApplicationTests {
    @Autowired
    EntityManager em;

    @Test
    void contextLoads() {
        Hello hello = Hello.builder().build();
        em.persist(hello);

        JPAQueryFactory query = new JPAQueryFactory(em);
        QHello qHello = QHello.hello;

        Hello result = query.selectFrom(qHello)
                .fetchOne();

        assertThat(result).isNotNull()
                .isEqualTo(hello);
        assertThat(result.getId()).isEqualTo(hello.getId());
    }

}
