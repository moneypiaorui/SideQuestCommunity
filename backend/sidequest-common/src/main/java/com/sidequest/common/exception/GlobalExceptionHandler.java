package com.sidequest.common.exception;

import com.sidequest.common.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    @ExceptionHandler(Exception.class)
    public Result<String> handleException(Exception e) {
        log.error("Unhandled exception: ", e);
        String message = "prod".equals(activeProfile) ? 
            messageSource.getMessage("error.internal", null, LocaleContextHolder.getLocale()) : e.getMessage();
        return Result.error(500, message);
    }

    @ExceptionHandler(BusinessException.class)
    public Result<String> handleBusinessException(BusinessException e) {
        String message = messageSource.getMessage(e.getMessageKey(), null, e.getMessageKey(), LocaleContextHolder.getLocale());
        return Result.error(e.getCode(), message);
    }
}
