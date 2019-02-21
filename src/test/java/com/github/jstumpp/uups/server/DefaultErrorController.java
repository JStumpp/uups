package com.github.jstumpp.uups.server;

import com.github.jstumpp.uups.Uups;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ErrorAttributes;
import org.springframework.boot.autoconfigure.web.ErrorController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

@RestController
public class DefaultErrorController implements ErrorController {

    private static final String PATH = "/error";
    @Autowired
    private ErrorAttributes errorAttributes;

    @RequestMapping(value = PATH)
    public String error(HttpServletRequest request, HttpServletResponse response) {
        RequestAttributes requestAttributes = new ServletRequestAttributes(request);
        Throwable cause = errorAttributes.getError(requestAttributes);
        Map errorAtt = errorAttributes.getErrorAttributes(requestAttributes, false);
        return Uups.renderHtml(cause, response.getStatus(), request.getMethod(), errorAtt.get("path").toString(), request);
    }

    @Override
    public String getErrorPath() {
        return PATH;
    }
}