package edu.uga.ces.controller;

import edu.uga.ces.dto.ShowroomResponse;
import edu.uga.ces.dto.ShowtimeCreateRequest;
import edu.uga.ces.dto.ShowtimeMovieOption;
import edu.uga.ces.dto.ShowtimeResponse;
import edu.uga.ces.exception.AdminAccessRequiredException;
import edu.uga.ces.service.SessionUserService;
import edu.uga.ces.service.ShowtimeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/showtimes")
public class AdminShowtimeController {

    private final ShowtimeService showtimeService;
    private final SessionUserService sessionUserService;

    public AdminShowtimeController(ShowtimeService showtimeService,
                                   SessionUserService sessionUserService) {
        this.showtimeService = showtimeService;
        this.sessionUserService = sessionUserService;
    }

    @PostMapping
    public ResponseEntity<ShowtimeResponse> addShowtime(
            @Valid @RequestBody ShowtimeCreateRequest request,
            HttpServletRequest httpRequest) {
        requireAdmin(httpRequest);
        ShowtimeResponse created = showtimeService.createShowtime(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public List<ShowtimeResponse> getShowtimes(HttpServletRequest httpRequest) {
        requireAdmin(httpRequest);
        return showtimeService.getAllShowtimes();
    }

    @GetMapping("/movies")
    public List<ShowtimeMovieOption> getMovieOptions(HttpServletRequest httpRequest) {
        requireAdmin(httpRequest);
        return showtimeService.getMovieOptions();
    }

    @GetMapping("/showrooms")
    public List<ShowroomResponse> getShowroomOptions(HttpServletRequest httpRequest) {
        requireAdmin(httpRequest);
        return showtimeService.getShowroomOptions();
    }

    private void requireAdmin(HttpServletRequest httpRequest) {
        sessionUserService.requireUserId(httpRequest);

        HttpSession session = httpRequest.getSession(false);
        if (!"ADMIN".equals(session.getAttribute("role"))) {
            throw new AdminAccessRequiredException();
        }
    }
}
