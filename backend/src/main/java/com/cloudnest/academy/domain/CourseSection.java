package com.cloudnest.academy.domain;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "course_sections")
public class CourseSection {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @ManyToOne(optional = false) public Course course;
    @Column(nullable = false) public String title;
    public int position;

    @OneToMany(mappedBy = "section", cascade = CascadeType.ALL)
    @OrderBy("position")
    public List<Lesson> lessons = new ArrayList<>();
}
