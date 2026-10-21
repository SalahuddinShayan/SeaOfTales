package com.seaoftales.content.service;

import org.springframework.stereotype.Service;

@Service
public class TranslationFormatter {
    public String format(String translatedContent) {
        String[] paragraphs = translatedContent.split("(?:\\r?\\n){2,}", -1);
        StringBuilder html = new StringBuilder();
        for (String paragraph : paragraphs) {
            if (paragraph.equals("***")) {
                html.append("<p>* * *</p>\n\n");
                continue;
            }
            String escaped = escape(paragraph).replace("\r\n", "\n").replace("\n", "<br>");
            html.append("<p>").append(escaped).append("</p>\n\n");
        }
        return html.toString();
    }

    private String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
