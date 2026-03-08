package net.rodald;

import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

public class Main {
    private static final Map<String, List<Integer>> MANUAL_CHECKS = Map.of(
//            "dicitur ohne NCI", List.of(
//                    94, 129, 134, 484, 928, 1253, 1285, 1377, 1398, 1435,
//                    1613, 1661, 1742, 1754, 1853, 1937, 1995
//            )
    );

    private static final List<String> REGEX_KEYWORDS = List.of(
            "\\bita quod\\b",
            "\\bquia\\b",
//            "\\b\\w+(ndo|ndorum|ndis|nda)\\b",
            "\\but\\b\\s+(?:\\w+\\s+){0,3}non\\s+\\w+",
            "\\bdicitur\\b",
            "\\bsi\\b",
            "\\banima",

//            "(?<=\\s)dum\\b",
            "\\b(esurie)\\w*\\b",
            "\\b(siti)\\w*\\b"
//            "\\b(adipisc|aggredi|amplect|alloqu|ancill|conversatur|egreditur|egrediuntur|nascitur|nascuntur|moriuntur|sequuntur|supergreditur|supergrediuntur|versatur|operatur|patitur|laetante|moriturus est|laetatur|loquitur)\\w*\\b",
//            "\\b(cum)\\b[^.;?\\n]*?\\b(?!\\w*(ntas|quomodo|dei|tas|opus|do|illis|ntis|bus|sis|li|nto|uo|ms|\\bet|ni|iam|ibi|pes|omo|eam|his|illam|deus|pso)\\b)\\w+(o|eo|s|t|tis|nt|am|i|or|tur|mur|mini)\\b"
    );

    public static void main(String[] args) throws Exception {
        String text = Files.readString(Path.of("causae_et_curaeChapterBased.txt"));
        String[] paragraphs = text.split("\\n\\s*\\n");
        String[] lines = text.split("\\n");

        Map<Integer, Double> scores = new HashMap<>();

        for (String regex : REGEX_KEYWORDS) {
            int paragraphsWithTerm = 0;
            for (String paragraph : paragraphs) {
                if (Pattern.compile(regex).matcher(paragraph).find()) {
                    paragraphsWithTerm++;
                }
            }
            
            if (paragraphsWithTerm == 0) continue;

            System.out.println("Term '" + regex + "' appears in " + paragraphsWithTerm + " out of " + paragraphs.length + " paragraphs");

            double weight = Math.log((double) paragraphs.length / paragraphsWithTerm);
            
            System.out.println("Weight for '" + regex + "': " + weight);

            for (int i = 0; i < paragraphs.length; i++) {
                if (Pattern.compile(regex).matcher(paragraphs[i]).find()) {
                    scores.merge(i, weight, Double::sum);
                }
            }
        }

        for (List<Integer> lineNumbers : MANUAL_CHECKS.values()) {
            double manualWeight = Math.log((double) paragraphs.length / lineNumbers.size());
            
            for (int lineNum : lineNumbers) {
                if (lineNum <= 0 || lineNum > lines.length) continue;
                int paragraphIndex = findParagraphForLine(lines, text, lineNum - 1);
                if (paragraphIndex >= 0) {
                    scores.merge(paragraphIndex, manualWeight / lineNumbers.size(), Double::sum);
                }
            }
        }

        if (scores.isEmpty()) {
            System.out.println("No matches found!");
            return;
        }

        double totalWeight = scores.values().stream().mapToDouble(Double::doubleValue).sum();

        scores.entrySet().stream()
                .map(e -> Map.entry(e.getKey(), e.getValue() / totalWeight))
                .sorted(Map.Entry.<Integer, Double>comparingByValue().reversed())
                .limit(10)
                .forEach(e -> {
                    double percent = e.getValue() * 100;
                    System.out.printf("Paragraph %d Chance: %.2f%%\n", e.getKey(), percent);
                    System.out.println(paragraphs[e.getKey()].substring(0, Math.min(100, paragraphs[e.getKey()].length())) + "...");
                    System.out.println("----------------------");
                });
    }

    static int findParagraphForLine(String[] lines, String originalText, int lineIndex) {
        if (lineIndex >= lines.length) return -1;
        
        String targetLine = lines[lineIndex].trim();
        if (targetLine.isEmpty()) return -1;
        
        String[] paragraphs = originalText.split("\\n\\s*\\n");
        
        for (int i = 0; i < paragraphs.length; i++) {
            if (paragraphs[i].contains(targetLine)) {
                return i;
            }
        }
        
        return -1;
    }
}