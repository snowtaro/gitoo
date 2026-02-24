package com.example.gitoo.game.service;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Component
public class DictionaryLoader {

    private final Set<String> dictionary = new HashSet<>();

    @PostConstruct
    public void load() {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:dict/*.xml");

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            for (Resource resource : resources) {
                Document doc = builder.parse(resource.getInputStream());
                NodeList items = doc.getElementsByTagName("word");

                for (int i = 0; i < items.getLength(); i++) {
                    String raw = items.item(i).getTextContent().trim();
                    // "가01", "가02" → "가" (뒤에 붙은 숫자 제거)
                    String word = raw.replaceAll("[0-9]+$", "").trim();
                    if (!word.isEmpty()) {
                        dictionary.add(word);
                    }
                }
            }

            log.info("✅ 사전 로드 완료: {}개 단어", dictionary.size());

        } catch (Exception e) {
            log.error("❌ 사전 로드 실패: {}", e.getMessage());
        }
    }

    public boolean contains(String word) {
        return dictionary.contains(word);
    }
}