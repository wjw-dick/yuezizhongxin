package com.yzz.common.sensitive;

import com.yzz.common.api.BusinessException;
import com.yzz.common.api.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 敏感词检测。命中即拦截，不允许发布。
 *
 * <p>词库分两部分：法定禁用语（绝对化表述、疗效承诺）内置兜底，业务词库由配置项补充。
 * 生产环境应改为从内容 CMS 的词库表加载并热更新。</p>
 */
@Component
public class SensitiveWordFilter {

    private static final List<String> LEGAL_WORDS = List.of(
            "包治", "包好", "保证恢复", "疗效", "治愈", "根治", "药到病除",
            "最好", "第一品牌", "国家级", "永久有效");

    private final Set<String> words = new LinkedHashSet<>(LEGAL_WORDS);

    public SensitiveWordFilter(@Value("${yzz.sensitive.extra-words:}") String extraWords) {
        if (extraWords != null && !extraWords.isBlank()) {
            Arrays.stream(extraWords.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .forEach(words::add);
        }
    }

    /** 返回命中的敏感词列表，未命中返回空列表。 */
    public List<String> match(String text) {
        List<String> hits = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return hits;
        }
        for (String word : words) {
            if (text.contains(word)) {
                hits.add(word);
            }
        }
        return hits;
    }

    /** 内容发布前调用，命中即抛出业务异常并提示修改。 */
    public void assertClean(String text) {
        List<String> hits = match(text);
        if (!hits.isEmpty()) {
            throw BusinessException.of(ErrorCode.SENSITIVE_WORD,
                    "内容命中敏感词：" + String.join("、", hits) + "，请修改后重新提交");
        }
    }
}
