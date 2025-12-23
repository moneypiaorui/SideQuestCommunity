package com.sidequest.moderation.application;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.regex.Pattern;
import java.util.List;
import java.util.ArrayList;

@Service
@ConfigurationProperties(prefix = "moderation")
public class ModerationService {
    
    private List<String> sensitiveWords = new ArrayList<>();
    private Pattern sensitivePattern;

    public void setSensitiveWords(List<String> sensitiveWords) {
        this.sensitiveWords = sensitiveWords;
        this.updatePattern();
    }

    @PostConstruct
    public void init() {
        if (sensitiveWords == null || sensitiveWords.isEmpty()) {
            sensitiveWords = List.of("badword1", "badword2"); // 兜底默认词
        }
        updatePattern();
    }

    private void updatePattern() {
        if (!sensitiveWords.isEmpty()) {
            this.sensitivePattern = Pattern.compile(
                String.join("|", sensitiveWords), 
                Pattern.CASE_INSENSITIVE
            );
        }
    }

    public boolean checkText(String text) {
        if (text == null || text.isBlank()) {
            return true;
        }
        
        if (sensitivePattern != null && sensitivePattern.matcher(text).find()) {
            return false;
        }
        
        return true;
    }
}

