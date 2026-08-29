package io.github.trilhalog.sample.security;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

@Aspect
@Component
public class SanitizeHtmlAspect {

    @Around("@annotation(SanitizeHtml)")
    public Object sanitize(ProceedingJoinPoint pjp) throws Throwable {
        Object result = pjp.proceed();
        if (result instanceof String s) {
            return HtmlUtils.htmlEscape(s);
        }
        return result;
    }
}
