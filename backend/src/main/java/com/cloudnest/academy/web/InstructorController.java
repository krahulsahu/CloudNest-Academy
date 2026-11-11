package com.cloudnest.academy.web;

import com.cloudnest.academy.security.CurrentUser;
import com.cloudnest.academy.service.InstructorService;
import com.cloudnest.academy.service.MediaService;
import com.cloudnest.academy.web.Dtos.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/instructor")
public class InstructorController {
    private final InstructorService instructor;
    private final MediaService media;

    public InstructorController(InstructorService instructor, MediaService media) {
        this.instructor = instructor;
        this.media = media;
    }

    @GetMapping("/courses")
    public List<InstructorCourse> courses() { return instructor.myCourses(CurrentUser.id()); }

    @GetMapping("/course-options")
    public List<CourseOption> options() { return instructor.options(); }

    @PostMapping("/courses")
    public CourseEdit create(@Valid @RequestBody CreateCourseReq req) { return instructor.create(CurrentUser.id(), req); }

    @GetMapping("/courses/{id}")
    public CourseEdit get(@PathVariable Long id) { return instructor.get(CurrentUser.id(), id); }

    @PutMapping("/courses/{id}")
    public CourseEdit save(@PathVariable Long id, @RequestBody CourseEdit body) { return instructor.save(CurrentUser.id(), id, body); }

    @PostMapping("/courses/{id}/submit")
    public CourseEdit submit(@PathVariable Long id) { return instructor.submit(CurrentUser.id(), id); }

    @DeleteMapping("/courses/{id}")
    public void delete(@PathVariable Long id) { instructor.delete(CurrentUser.id(), id); }

    @GetMapping("/courses/{id}/students")
    public List<StudentDto> students(@PathVariable Long id) { return instructor.students(CurrentUser.id(), id); }

    @PostMapping("/courses/{id}/announce")
    public Map<String, Integer> announce(@PathVariable Long id, @Valid @RequestBody AnnounceReq req) {
        return Map.of("sent", instructor.announce(CurrentUser.id(), id, req));
    }

    @PostMapping("/uploads")
    public UploadResponse upload(@RequestParam("file") MultipartFile file) {
        String name = media.store(file);
        return new UploadResponse("upload:" + name, file.getOriginalFilename(), file.getSize());
    }
}
