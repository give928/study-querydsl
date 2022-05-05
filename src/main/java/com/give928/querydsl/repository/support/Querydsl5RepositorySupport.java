package com.give928.querydsl.repository.support;

import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.JpaEntityInformationSupport;
import org.springframework.data.jpa.repository.support.Querydsl;
import org.springframework.data.querydsl.SimpleEntityPathResolver;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;
import org.springframework.util.Assert;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import java.util.List;
import java.util.function.Function;

@Repository
public abstract class Querydsl5RepositorySupport<T> {
    private final Class<T> domainClass;
    private Querydsl querydsl;
    private EntityManager entityManager;
    private JPAQueryFactory queryFactory;

    protected Querydsl5RepositorySupport(Class<T> domainClass) {
        Assert.notNull(domainClass, "Domain class must not be null!");
        this.domainClass = domainClass;
    }

    @Autowired
    public void setEntityManager(EntityManager entityManager) {
        Assert.notNull(entityManager, "EntityManager must not be null!");
        JpaEntityInformation<T, ?> entityInformation = JpaEntityInformationSupport.getEntityInformation(domainClass, entityManager);
        SimpleEntityPathResolver resolver = SimpleEntityPathResolver.INSTANCE;
        EntityPath<T> path = resolver.createPath(entityInformation.getJavaType());
        this.entityManager = entityManager;
        this.querydsl = new Querydsl(entityManager, new PathBuilder<>(path.getType(), path.getMetadata()));
        this.queryFactory = new JPAQueryFactory(entityManager);
    }

    @PostConstruct
    public void validate() {
        Assert.notNull(entityManager, "EntityManager must not be null!");
        Assert.notNull(querydsl, "Querydsl must not be null!");
        Assert.notNull(queryFactory, "QueryFactory must not be null!");
    }

    protected JPAQueryFactory getQueryFactory() {
        return queryFactory;
    }

    protected Querydsl getQuerydsl() {
        return querydsl;
    }

    protected EntityManager getEntityManager() {
        return entityManager;
    }

    protected <S extends T> JPAQuery<S> select(Expression<S> expr) {
        return getQueryFactory().select(expr);
    }

    protected <S extends T> JPAQuery<S> selectFrom(EntityPath<S> from) {
        return getQueryFactory().selectFrom(from);
    }

    protected <S extends T> Page<S> applyPagination(Pageable pageable, Function<JPAQueryFactory, JPAQuery<S>> contentQuery,
                                          Function<JPAQueryFactory, JPAQuery<Long>> countQuery) {
        JPAQuery<S> jpaContentQuery = contentQuery.apply(getQueryFactory());
        List<S> content = getQuerydsl().applyPagination(pageable, jpaContentQuery).fetch();
        JPAQuery<Long> countResult = countQuery.apply(getQueryFactory());
        return PageableExecutionUtils.getPage(content, pageable, countResult::fetchOne);
    }
}
