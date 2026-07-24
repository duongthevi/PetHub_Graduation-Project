package com.dtvi.Pet.Ecommerce.security;

import com.dtvi.Pet.Ecommerce.dto.Response;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    public static final String ERROR_CODE_ATTRIBUTE = "authentication_error_code";
    public static final String ERROR_MESSAGE_ATTRIBUTE = "authentication_error_message";

    private final ObjectMapper objectMapper;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authenticationException
    ) throws IOException, ServletException {
        String code = getAttribute(request, ERROR_CODE_ATTRIBUTE, "UNAUTHORIZED");
        String message = getAttribute(
                request,
                ERROR_MESSAGE_ATTRIBUTE,
                "Authentication is required to access this resource"
        );

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(
                response.getOutputStream(),
                Response.builder()
                        .status(HttpServletResponse.SC_UNAUTHORIZED)
                        .code(code)
                        .message(message)
                        .build()
        );
    }

    private String getAttribute(HttpServletRequest request, String name, String defaultValue) {
        Object value = request.getAttribute(name);
        return value instanceof String stringValue ? stringValue : defaultValue;
    }
}
