import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.*;
import java.util.*;

public class SpellCheck {

    //TODO: add config file.
    public static int maxEditDistance = 2;

    public static HashMap<String, Object> dictionary = Maps.newHashMap();
    public static List<String> wordList = Lists.newArrayList();

    public static class Suggestion implements Serializable {
        private static final long serialVersionUID = 8762405795571200021L;
        String CorrectedString;
        Integer editDistance;
        Integer count;

        @Override
        public String toString() {
            return new ToStringBuilder(this)
                    .append("CorrectedString", CorrectedString)
                    .append("editDistance", editDistance)
                    .append("count", count)
                    .toString();
        }

        public Suggestion() {
        }

        public Suggestion(String correctedString, Integer editDistance, Integer count) {
            CorrectedString = correctedString;
            this.editDistance = editDistance;
            this.count = count;
        }
    }

    public static class CorrectedResults implements Serializable {
        private static final long serialVersionUID = 4161064849627391911L;
        List<Suggestion> suggestions;
        String actualString;

        public CorrectedResults(List<Suggestion> suggestions, String actualString) {
            this.suggestions = suggestions;
            this.actualString = actualString;
        }

        public CorrectedResults(String actualString) {
            this.actualString = actualString;
        }

        public CorrectedResults() {
        }

        @Override
        public String toString() {
            return new ToStringBuilder(this)
                    .append("suggestions", suggestions)
                    .append("actualString", actualString)
                    .toString();
        }
    }

    public static class DictionaryItem {
        public int count;
        public List<Integer> suggestions = new ArrayList<>();

        DictionaryItem(int count, List<Integer> suggestions) {
            this.count = count;
            this.suggestions = suggestions;
        }

        DictionaryItem(int count) {
            this.count = count;
        }

        DictionaryItem(List<Integer> suggestions) {
            this.suggestions = suggestions;
        }

        @Override
        public String toString() {
            return new org.apache.commons.lang3.builder.ToStringBuilder(this)
                    .append("count", count)
                    .append("suggestions", suggestions)
                    .toString();
        }
    }

    public static Set<String> DeleteWords(String str, int distance, Set<String> results) {
        if (distance < maxEditDistance && str.length() > 1) {
            for (int i = 0; i < str.length(); i++) {
                String newLine = str.substring(0, i) + str.substring(i + 1);
                if (results.add(newLine)) {
                    DeleteWords(newLine, distance + 1, results);
                }
            }
        }
        return results;
    }

    public static void updateLowestDistanceValue(DictionaryItem dictionaryItem, String line, int wordListIndex, String deleteWord) {
        if (wordListIndex < wordList.size() && (dictionaryItem.suggestions.size() > 0)
                && (wordList.get(dictionaryItem.suggestions.get(0)).length()
                - deleteWord.length() >= line.length() - deleteWord.length())) {
            dictionaryItem.suggestions.clear();
            dictionaryItem.suggestions.add(wordListIndex);
        } else if ((dictionaryItem.suggestions.size() == 0)) {
            dictionaryItem.suggestions.add(wordListIndex);
        }
    }

    public static void AddToDictionary(String line) {
        Object dictValue;
        DictionaryItem dictionaryItem;
        if ((dictValue = dictionary.get(line)) != null) {
            if (dictValue instanceof Integer) {
                dictionaryItem = new DictionaryItem(Lists.newArrayList((Integer) dictValue));
                dictionary.put(line, dictionaryItem);
            } else {
                dictionaryItem = (DictionaryItem) dictValue;
            }
            if (dictionaryItem.count < Integer.MAX_VALUE) {
                dictionaryItem.count++;
            }
        } else {
            dictionaryItem = new DictionaryItem(1);
            dictionary.put(line, dictionaryItem);
        }

        //form delete words
        if (dictionaryItem.count == 1) {
            wordList.add(line);
            int wordListIndex = wordList.size() - 1;
            //create delete words based on maxEditDistance;
            for (String deleteWord : DeleteWords(line, 0, Sets.<String>newHashSet())) {
                if ((dictValue = dictionary.get(deleteWord)) != null) {
                    if (dictValue instanceof Integer) {
                        dictionaryItem = new DictionaryItem(Lists.newArrayList((Integer) dictValue));
                        dictionary.put(deleteWord, dictionaryItem);
                    } else {
                        dictionaryItem = (DictionaryItem) dictValue;
                    }
                    if (!dictionaryItem.suggestions.contains(wordListIndex)) {
                        updateLowestDistanceValue(dictionaryItem, line, wordListIndex, deleteWord);
                    }
                } else {
                    dictionary.put(deleteWord, wordListIndex);
                }
            }
        }
    }

    public static void CreateDictionary(String line) {
        String[] splits = line.split("\\s");

        for (int i = 0; i < splits.length; i++) {
            AddToDictionary(splits[i]);
        }
    }

    public static Integer min(int a, int b, int c, int d) {
        return Math.min(a, Math.min(b, Math.min(c, d)));
    }

    public static Integer min(int a, int b, int c) {
        return Math.min(a, Math.min(b, c));
    }

    public static Integer DamerauLevenshteinDistance(String str1, String str2) {
        //if null throw error?
        if (str1 == null && str2 == null) {
            return 0;
        } else if (str1 == null) {
            return str2.length();
        } else if (str2 == null) {
            return str1.length();
        }
        int length1 = str1.length();
        int length2 = str2.length();
        int dp[][] = new int[length1 + 1][length2 + 1];
        for (int i = 0; i <= length1; i++) {
            for (int j = 0; j <= length2; j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else {
                    if (i > 1 && j > 2 && str1.charAt(i - 1) == str2.charAt(j - 2) && str1.charAt(i - 2) == str2.charAt(j - 1)) {
                        dp[i][j] = min(dp[i - 1][j] + 1, dp[i][j - 1] + 1, dp[i - 1][j - 1] + 1, dp[i - 2][j - 2] + 1);
                    } else {
                        if (str1.charAt(i - 1) == str2.charAt(j - 1)) {
                            dp[i][j] = dp[i - 1][j - 1];
                        } else {
                            dp[i][j] = min(dp[i - 1][j] + 1, dp[i][j - 1] + 1, dp[i - 1][j - 1] + 1);
                        }
                    }
                }
            }
        }
        return dp[length1][length2];
    }

    public static CorrectedResults lookUp(String word) {

        CorrectedResults correctedResults = new CorrectedResults(word);
        List<String> candidates = Lists.newArrayList(word);
        Set<String> uniqueCandidates = Sets.newHashSet();
        List<Suggestion> suggestions = Lists.newArrayList();

        if (CollectionUtils.isNotEmpty(candidates)) {
            while (candidates.size() > 0) {
                String candidate = candidates.remove(0);
                if (uniqueCandidates.add(candidate)) {
                    Object dictValue = dictionary.get(candidate);
                    if (dictValue != null) {
                        DictionaryItem dictionaryItem;
                        //delete word
                        if (dictValue instanceof Integer) {
                            dictionaryItem = new DictionaryItem(Lists.newArrayList((Integer) dictValue));
                        } else {
                            dictionaryItem = (DictionaryItem) dictValue;
                        }
                        //dictionary word.
                        if (dictionaryItem.count > 0) {
                            Suggestion suggestion = new Suggestion(candidate, word.length() - candidate.length(), dictionaryItem.count);
                            suggestions.add(suggestion);
                        } else {
                            if (CollectionUtils.isNotEmpty(dictionaryItem.suggestions)) {
                                for (Integer index : dictionaryItem.suggestions) {
                                    String str = wordList.get(index);
                                    int distance = 0;
                                    if (!Objects.equals(str, word)) {
                                        if (str.length() == candidate.length()) {
                                            distance = word.length() - candidate.length();
                                        } else if (word.length() == candidate.length()) {
                                            distance = str.length() - candidate.length();
                                        } else { //Damerau Levenshtein Distance
                                            int a = 0;
                                            int b = 0;
                                            while (a >= 0 && a < str.length() && a < word.length() && str.charAt(a) == word.charAt(a)) {
                                                a++;
                                            }
                                            while (b < str.length() - a && b < word.length() - a && str.charAt(str.length() - b - 1) == word.charAt(word.length() - b - 1)) {
                                                b++;
                                            }
                                            if (a > 0 || b > 0) {
                                                distance = DamerauLevenshteinDistance(str.substring(a, str.length() - b), word.substring(a, word.length() - b));
                                            } else {
                                                distance = DamerauLevenshteinDistance(str, word);
                                            }
                                        }
                                    }
                                    if (distance <= maxEditDistance) {
                                        Object dictValue1 = dictionary.get(str);
                                        if (dictValue1 != null) {
                                            if (dictValue1 instanceof DictionaryItem) {
                                                Suggestion suggestion = new Suggestion(str, distance, ((DictionaryItem) dictValue1).count);
                                                suggestions.add(suggestion);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (word.length() - candidate.length() < maxEditDistance) {
                        if (!(Objects.equals(word, candidate) && suggestions.size() > 0)) {
                            for (int i = 0; i < candidate.length(); i++) {
                                String deleteStr = candidate.substring(0, i) + candidate.substring(i + 1);
                                candidates.add(deleteStr);
                            }
                        }
                    }
                }
            }
            correctedResults.suggestions = suggestions;
            sortSuggestions(suggestions);
        }
        return correctedResults;
    }

    public static CorrectedResults lookUpCompound(String word) {
        String[] words = word.split("\\s+");
        CorrectedResults correctedResults = new CorrectedResults(word);
        List<Suggestion> results = Lists.newArrayList();

        List<Suggestion> bestSuggestions = Lists.newArrayList();

        List<Suggestion> suggestionsPrev = Lists.newArrayList();
        List<Suggestion> suggestions = Lists.newArrayList();

        boolean flag = false;
        for (int i = 0; i < words.length; i++) {
            String str = words[i];
            suggestionsPrev = Lists.newArrayList();
            for (Suggestion suggestion : suggestions) {
                suggestionsPrev.add(SerializationUtils.clone(suggestion));
            }

            CorrectedResults correctedResults1 = lookUp(str);
            if (correctedResults1 != null && CollectionUtils.isNotEmpty(correctedResults1.suggestions)) {
                suggestions = correctedResults1.suggestions;
            }

            if (i > 0 && !flag) {
                CorrectedResults correctedCompResults = lookUp(words[i - 1] + words[i]);
                if (correctedCompResults != null && CollectionUtils.isNotEmpty(correctedCompResults.suggestions)) {
                    Suggestion combBest = correctedCompResults.suggestions.get(0);
                    Suggestion best1 = suggestionsPrev.get(suggestionsPrev.size() - 1);
                    Suggestion best2 = new Suggestion();
                    if (CollectionUtils.isNotEmpty(suggestions)) {
                        best2 = suggestions.get(0);
                    } else {
                        best2.editDistance = maxEditDistance + 1;
                        best2.CorrectedString = words[i];
                        best2.count = 0;
                    }
                    if (combBest.editDistance + 1 < DamerauLevenshteinDistance(words[i - 1] + " " + words[i], best1.CorrectedString + " " + best2.CorrectedString)) {
                        flag = true;
                        combBest.editDistance++;
                        bestSuggestions.set(bestSuggestions.size() - 1, combBest);
                        break;
                    }
                }
            }
            flag = false;
            if (CollectionUtils.isNotEmpty(suggestions) && (suggestions.get(0).editDistance == 0 || str.length() == 1)) {
                bestSuggestions.add(suggestions.get(0));
            } else {
                List<Suggestion> splitSuggestions = Lists.newArrayList();
                if (CollectionUtils.isNotEmpty(suggestions)) {
                    splitSuggestions.add(suggestions.get(0));
                }
                if (str.length() > 1) {
                    for (int j = 1; j < str.length(); j++) {
                        String str1 = str.substring(0, j);
                        String str2 = str.substring(j);
                        CorrectedResults correctedSplitResults1 = lookUp(str1);

                        if (correctedSplitResults1 != null && correctedSplitResults1.suggestions.size() > 0) {

                            if (CollectionUtils.isNotEmpty(suggestions) && correctedSplitResults1.suggestions.get(0).equals(suggestions.get(0))) {
                                break;
                            }
                            CorrectedResults correctedSplitResults2 = lookUp(str2);
                            if (correctedSplitResults2 != null && correctedSplitResults2.suggestions.size() > 0) {
                                if (CollectionUtils.isNotEmpty(suggestions) && correctedSplitResults1.suggestions.get(0).equals(suggestions.get(0))) {
                                    break;
                                }

                                String term = correctedSplitResults1.suggestions.get(0).CorrectedString + " " + correctedSplitResults2.suggestions.get(0).CorrectedString;
                                int distance = DamerauLevenshteinDistance(str, term);
                                int count = Math.min(correctedSplitResults1.suggestions.get(0).count, correctedSplitResults2.suggestions.get(0).count);
                                splitSuggestions.add(new Suggestion(term, distance, count));
                                if (distance == 1) {
                                    break;
                                }
                            }
                        }
                    }
                } else {
                    splitSuggestions.add(new Suggestion(str, maxEditDistance + 1, 0));
                }
                sortSuggestions(splitSuggestions);
                if (CollectionUtils.isEmpty(splitSuggestions)) {
                    splitSuggestions.add(new Suggestion(str, maxEditDistance + 1, 0));
                }
                bestSuggestions.add(splitSuggestions.get(0));
            }
        }

        System.out.println(bestSuggestions);
        Suggestion suggestion = new Suggestion("", Integer.MAX_VALUE, Integer.MAX_VALUE);
        for (Suggestion sugges : bestSuggestions) {
            if (!suggestion.CorrectedString.equals(StringUtils.EMPTY)) {
                suggestion.CorrectedString += " ";
            }
            suggestion.CorrectedString += sugges.CorrectedString;
            suggestion.count = Math.min(suggestion.count, sugges.count);
        }
        System.out.println(word);
        System.out.println(suggestion.CorrectedString);
        suggestion.editDistance = DamerauLevenshteinDistance(word, suggestion.CorrectedString);
        correctedResults.suggestions = Lists.newArrayList(suggestion);
        return correctedResults;
    }

    public static void sortSuggestions(List<Suggestion> splitSuggestions) {
        if (CollectionUtils.isNotEmpty(splitSuggestions)) {
            Collections.sort(splitSuggestions, new Comparator<Suggestion>() {
                @Override
                public int compare(Suggestion o1, Suggestion o2) {
                    if (Objects.equals(o1.editDistance, o2.editDistance)) {
                        return -(o1.count - o2.count);
                    } else {
                        return (o1.editDistance - o2.editDistance);
                    }
                }
            });
        }
    }


    public static void CorrectString() {
        String word;
        System.out.println("enter string");
        try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
            while ((word = br.readLine()) != null) {
                if (StringUtils.isBlank(word)) break;
                System.out.println(word);
                CorrectedResults correctedResults;
                if (word.split("\\s+").length > 1) {
                    correctedResults = lookUpCompound(word);
                } else {
                    correctedResults = lookUpCompound(word);
                }
                System.out.println(correctedResults);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        InputStream is = classloader.getResourceAsStream("myfile.txt");
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = br.readLine()) != null) {
                CreateDictionary(line);
            }
            System.out.println(wordList);
            System.out.println(dictionary);
            CorrectString();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}