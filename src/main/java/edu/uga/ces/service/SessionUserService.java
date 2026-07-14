package edu.uga.ces.service;

import edu.uga.ces.exception.AuthenticationRequiredException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

@Service
public class SessionUserService {

    public Long requireUserId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            throw new AuthenticationRequiredException();
        }

        Object userId = session.getAttribute("userId");
        if (userId instanceof Number number) {
            return number.longValue();
        }
        throw new AuthenticationRequiredException();
    }
}
