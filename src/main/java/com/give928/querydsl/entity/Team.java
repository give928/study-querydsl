package com.give928.querydsl.entity;

import lombok.*;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@ToString(of = {"id", "name"})
public class Team {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "team_id", nullable = false)
    private Long id;

    private String name;

    @OneToMany(mappedBy = "team")
    List<Member> members;

    @Builder
    public Team(Long id, String name, List<Member> members) {
        this.id = id;
        this.name = name;
        this.members = members;
        if (this.members == null) {
            this.members = new ArrayList<>();
        }
    }
}
