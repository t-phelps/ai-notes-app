package com.tphelps.backend.service.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public class TextUtils {

    private static final Set<String> NLTK_STOPWORDS = new HashSet<>(Arrays.asList(
            "i","me","my","myself","we","our","ours","ourselves",
            "you","your","yours","yourself","yourselves",
            "he","him","his","himself","she","her","hers","herself",
            "it","its","itself","they","them","their","theirs","themselves",
            "what","which","who","whom","this","that","these","those",
            "am","is","are","was","were","be","been","being",
            "have","has","had","having","do","does","did","doing",
            "a","an","the","and","but","if","or","because","as","until","while",
            "of","at","by","for","with","about","against","between","into","through",
            "during","before","after","above","below","to","from","up","down",
            "in","out","on","off","over","under",
            "again","further","then","once","here","there","when","where","why","how",
            "all","any","both","each","few","more","most","other","some","such",
            "no","nor","not","only","own","same","so","than","too","very",
            "s","t","can","will","just","don","should","now"
    ));


    public static Set<String> tokenizeAndFilter(String input){
        if(input == null || input.isEmpty()){
            return Set.of();
        }

        return Arrays.stream(input
                    .toLowerCase()
                    .replaceAll("[^a-zA-Z0-9]", "") // removes punctuation
                    .split("\\s+")) // split on whitespace
                .filter(str -> !NLTK_STOPWORDS.contains(str))
                .collect(Collectors.toSet());
    }
    }

