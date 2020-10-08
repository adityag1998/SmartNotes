package com.samsung.smartnotes;

import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TfidfCalculation {

    public static final String TAG = "TfidfCalculation";

    static List<String> stopWords = Arrays.asList("a","about","above","after","again","against","ain","all","am","an","and","any","are","aren",
            "aren't","as","at","be","because","been","before","being","below","between","both","but","by","can","couldn","couldn't","d",
            "did","didn","didn't","do","does","doesn","doesn't","doing","don","don't","down","during","each","few","for","from",
            "further","had","hadn","hadn't","has","hasn","hasn't","have","haven","haven't","having","he","her","here","hers","herself",
            "him","himself","his","how","i","if","in","into","is","isn","isn't","it","it's","its","itself","just","ll","m","ma","me",
            "mightn","mightn't","more","most","mustn","mustn't","my","myself","needn","needn't","no","nor","not","now","o","of","off",
            "on","once","only","or","other","our","ours","ourselves","out","over","own","re","s","same","shan","shan't","she","she's",
            "should","should've","shouldn","shouldn't","so","some","such","t","than","that","that'll","the","their","theirs","them",
            "themselves","then","there","these","they","this","those","through","to","too","under","until","up","ve","very","was","wasn",
            "wasn't","we","were","weren","weren't","what","when","where","which","while","who","whom","why","will","with","won","won't",
            "wouldn","wouldn't","y","you","you'd","you'll","you're","you've","your","yours","yourself","yourselves","could","he'd",
            "he'll","he's","here's","how's","i'd","i'll","i'm","i've","let's","ought","she'd","she'll","that's","there's","they'd",
            "they'll","they're","they've","we'd","we'll","we're","we've","what's","when's","where's","who's","why's","would");

    static String preProcessText(String input) {
        String newStr = input.replaceAll("[,.:;\"*]", " ");
        newStr = newStr.replaceAll("\\p{P}"," ");
        newStr = newStr.replaceAll("\t"," ");
        newStr = newStr.replaceAll("\n", " ");
        return newStr;
    }

    //Returns if input contains numbers or not
    static boolean isDigit(String input)
    {
        String regex = "(.)*(\\d)(.)*";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);

        boolean isMatched = matcher.matches();
        if (isMatched) {
            return true;
        }
        return false;
    }

    public static ArrayList<String> getTopTwoTerms(MainActivity.Note note) {
        ArrayList<String> topTwo= new ArrayList<String>();
        HashMap<String,Double> map = note.getTermTfidfMap();

        if( map== null || map.size() == 0 ) return topTwo;

        double max = -1;
        double secondMax = -1;
        String maxTerm = null;
        String secondMaxTerm = null;
        for(String term : map.keySet()) {
            if(map.get(term) > max) {
                max = map.get(term);
                maxTerm = term;
            }
        }
        topTwo.add(maxTerm);
        if(map.size() == 1) return topTwo;

        for(String term : map.keySet()) {
            if((map.get(term) > secondMax) && (map.get(term) <= max) && (!term.equals(maxTerm))) {
                secondMax = map.get(term);
                secondMaxTerm = term;
            }
        }
        topTwo.add(secondMaxTerm);
        return topTwo;
    }

    public static void updateTopTwoTerms(MainActivity.Note note) {
        note.setTopTwoTerms(getTopTwoTerms(note));
    }

    public static void updateAllTopTwoTerms() {
        for (MainActivity.Note note : MainActivity.notesList) {
            updateTopTwoTerms(note);
        }
    }

    public static void updateTermMaps(MainActivity.Note note) {
        HashMap<String,Integer> wordCount = new HashMap<String,Integer>();
        HashMap<String,Double> termFreq = new HashMap<String,Double>();

        // pre-process note string
        String text = note.getText().toLowerCase();
        text = preProcessText(text);
        String[] words = text.split(" ");

        // if words are not there, exit
        if (words.length == 0) {
            note.isTermMapsUpdated = true;
            return;
        }

        // update termCountMap
        for(String term : words) {
            if(term.length()==0 || isDigit(term)) continue;
            if(stopWords.contains(term)) continue;
            if(wordCount.containsKey(term)) {
                wordCount.put(term, wordCount.get(term) + 1);
            } else {
                wordCount.put(term, 1);
            }
        }
        // sorting the hashMap
        Map<String, Integer> treeMap = new TreeMap<>(wordCount);
        wordCount = new HashMap<String, Integer>(treeMap);


        // update termFreqMap
        double sum = 0;
        for (Integer val : wordCount.values()) {
            sum += val;
        }

        Iterator it = wordCount.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            double tf = ((Integer)pair.getValue())/sum;
            termFreq.put(pair.getKey().toString(), tf);
        }

        // update Map value in note
        note.setTermCounts(wordCount);
        note.setTermFreqMap(termFreq);
        note.isTermMapsUpdated = true;
    }

    public static HashMap<String, Double> calculateInverseDocFrequency(MainActivity.Note note) {
        HashMap<String, Double> idfMap = new HashMap<>();
        int size = MainActivity.notesList.size();  // Considering even empty notes will affect the idf value.
        int termCount;
        double idf;
        int totalCorpusSize = 0;

        for (MainActivity.Note eachNote : MainActivity.notesList) {
            if (!eachNote.isTermMapsUpdated) {
                updateTermMaps(eachNote);
            }
            totalCorpusSize += eachNote.getTermCounts().size();
        }

        for(String term : note.getTermCounts().keySet()) {
            termCount = 0;
            for (MainActivity.Note eachNote : MainActivity.notesList) {
                if (!eachNote.isTermMapsUpdated) {
                    updateTermMaps(eachNote);
                }

                if (eachNote.getTermCounts().containsKey(term)) termCount++;
            }

            double df = termCount/(double)size;
            Log.d(TAG, "Term : " + term + " - " + termCount + "/" + size + ", Df = " + df);
            idf = Math.log(totalCorpusSize/(double)(1 + df));
            idfMap.put(term, idf);
        }

        return idfMap;
    }

    public static void updateNoteTfidf(MainActivity.Note note) {

        // calculate term freq
        if(!note.isTermMapsUpdated) {
            updateTermMaps(note);
        }
        if(note.getTermFreqMap().size() == 0) {
            note.isTfidfUpdated = true;
            return;
        }

        // calculate inverse doc freq
        HashMap<String,Double> idfMap = calculateInverseDocFrequency(note);
        Log.d(TAG, "idfMap : " + idfMap.toString());

        // calculate and update TFIDF values for note
        HashMap<String,Double> tfidfMap = new HashMap<>();
        for(String term : idfMap.keySet()) {
            if (!note.getTermFreqMap().containsKey(term)) {
                Log.d(TAG, term + " : not present in termFreqMap of note.");
                return;
            }

            try {
                tfidfMap.put(term, idfMap.get(term) * note.getTermFreqMap().get(term));
            } catch(NullPointerException e) {
                Log.d(TAG, "Null Pointer Exception in fetching Map : " + e);
            }
        }
        note.setTermTfidfMap(tfidfMap);
        note.isTfidfUpdated = true;
        updateTopTwoTerms(note);
    }

    // this function will not provide correct results as a single word change will cause change in all idf values.
    public static void updateAllTfidf() {
        for(MainActivity.Note note : MainActivity.notesList) {
            if(!note.isTfidfUpdated) {
                updateNoteTfidf(note);
            }
        }
    }

    public static void recalculateAllTfidf() {
        for(MainActivity.Note note : MainActivity.notesList) {
            if(!note.isTermMapsUpdated)
                updateTermMaps(note);
        }
        for(MainActivity.Note note : MainActivity.notesList) {
            updateNoteTfidf(note);
        }
    }
}
