package nlp.lm;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

/**
 * An abstract class for bigram language models
 * 
 * @author Nolan McCafferty
 * @author Daniel Rosenbaum
 *
 */
public abstract class LMBase implements LMModel {
	
	// Final Variables to be used throughout the class
	protected final String unk = "<unk>";
	protected final String start = "<s>";
	protected final String end = "</s>";
	
	// counts the number of times each word has been seen
	protected HashSet<String> allWords;
	protected HashSet<String> vocabulary;
	
	
	// NUMBER OF TIMES WORD WAS SEEN IN TOTAL
	// these values will be 1 less than total since first occurence turns to <unk>
	// this data structure could probably be combined with something above
	protected HashMap<String, Double> unigramCounts;

	// counts the number of times the word is seen as the first word in a bigram
	protected HashMap<String, Double> bigramTotals;

	// holds the number of times this bigram was seen
	protected HashMap<String, HashMap<String, Double>> bigramCounts;
	
	// total number of words
	protected double totalNumWords;
	
	/**
	 * Train the model
	 * Calculate counts!
	 *
	 */
	public void trainModel(String filename){
		// Initialization
		allWords = new HashSet<String>();
		vocabulary = new HashSet<String>();
		unigramCounts = new HashMap<String, Double>();
		bigramTotals = new HashMap<String, Double>();
		bigramCounts = new HashMap<String, HashMap<String, Double>>();
		
		// add special tokens <s>, </s> and <unk> to our list of words
		vocabulary.add(unk);
		vocabulary.add(start);
		vocabulary.add(end);
		
		unigramCounts.put(unk, 0.0);
		unigramCounts.put(start, 0.0);
		unigramCounts.put(end, 0.0);
		
		// Read in the file and begin training
		try {
			BufferedReader br = new BufferedReader(new FileReader(filename));
			
			// read through all the sentences in the corpus
			for (String sentence = br.readLine(); sentence != null; sentence = br.readLine()){

				// add start and end tokens to sentence
				sentence = start + " " + sentence + " " + end;
				
				// get all the words in the sentence
				String[] words = sentence.split(" ");
				
				totalNumWords += words.length;
				unigramCounts.put(start, unigramCounts.get(start) + 1.0);
				unigramCounts.put(end, unigramCounts.get(end) + 1.0);

				// go through all the words (not including start and end)
				for (int i = 1; i < words.length - 1; i++){
					String word = words[i];
					
					// add the new words to our vocabulary
					if (!allWords.contains(word)){
						allWords.add(word);
						
						if (i > 0 && i < words.length -1){
							words[i] = unk;
							unigramCounts.put(unk, unigramCounts.get(unk)+1);
						}
						unigramCounts.put(word, 0.0);
					}
					// this means we have seen it once already so now add it to the vocab
					else {
						vocabulary.add(word);
						unigramCounts.put(word, unigramCounts.get(word) + 1);
					}
				}	
				
				// calculate the bigrams for the given sentence 
				for (int i = 0; i < words.length - 1; i++){
					String firstWord = words[i];
					String secondWord = words[i+1];
					
					// the first word has already been seen
					if (bigramCounts.containsKey(firstWord)){
						HashMap<String, Double> firstWordVals = bigramCounts.get(firstWord);
						
						// bigram has already been seen
						if (firstWordVals.containsKey(secondWord)){
							double bigramVal = firstWordVals.get(secondWord);
							firstWordVals.put(secondWord, bigramVal + 1);
						}
						// second word has not been seen 
						else {
							firstWordVals.put(secondWord, 1.0);
						}	
						
						// update the bigram counts
						bigramCounts.put(firstWord, firstWordVals);
					}
					
					// we have not seen the first word so create a new map as the value
					else {
						HashMap<String, Double> firstWordNewVals = new HashMap<String, Double>();
						firstWordNewVals.put(secondWord, 1.0);
						bigramCounts.put(firstWord, firstWordNewVals);
						
						// initialize this count to 0 since we update in the end 
						bigramTotals.put(firstWord, 0.0);
					}
					
					// update total count for the bigrams
					bigramTotals.put(firstWord, bigramTotals.get(firstWord) + 1);
				}
			}
			
			// Training is complete -> close the file
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	/**
	 * Given a sentence (list of words) calculate its logProb
	 * @param sentWords
	 * @return logarithmic probability
	 */
	@Override
	public double logProb(ArrayList<String> sentWords) {
		double logProb = 0;
		
		// sum of the logs (base 10) of the probabilities of the bigrams
		for (int i = 0; i < sentWords.size() - 1; i ++){
			logProb += Math.log10(getBigramProb(sentWords.get(i), sentWords.get(i+1)));
		}
		
		return logProb;
	}
	
	
	/**
	 * Given a filename (a corpus) calculate its perplexity
	 * @param filename
	 * @return perplexity value
	 */
	@Override
	public double getPerplexity(String filename) {
		System.out.println("Calculating Perplexity...");
		double perplexity = 0;
		
		// Read in the file and calculate perplexity
		try {
			BufferedReader br = new BufferedReader(new FileReader(filename));
			
			// initialize counts and sentence
			double numWords = 0;
			double totalLogProb = 0;
			
			for (String sentence = br.readLine(); sentence != null; sentence = br.readLine()){
				// convert the sentence into ArrayList<String> of words 
				ArrayList<String> sentWords = sentenceToWords(sentence);
				
				// calculate logProb for the given sentence				
				totalLogProb += logProb(sentWords);
				numWords += sentWords.size() - 1;
			}
			System.out.println("LogProb: " + totalLogProb);
			// calculate perplexity
			perplexity = Math.pow(10, -totalLogProb/numWords);
			
			// close the file
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return perplexity;
	}
	
	/**
	 * Convert a single string of a sentence into an arrayList of the words with respect to the following: 
	 * add the <s> and </s> characters 
	 * replace any unseen words with <unk>
	 * @param sentence
	 * @return list of words
	 */
	public ArrayList<String> sentenceToWords (String sentence){
		
		// add start and end characters to the sentence
		sentence = start + " " + sentence + " " + end;
		
		// convert the sentence into ArrayList<String> of words 
		ArrayList<String> sentWords = new ArrayList<String>(Arrays.asList(sentence.split(" ")));
		
		// make sure each word was either seen in training or replace it with unk
		// not checking the first or last word which is <s> and </s>
		for (int i = 1; i < sentWords.size() - 1; i++){
			String word = sentWords.get(i);
			if (!vocabulary.contains(word)){
				sentWords.set(i, unk);
			}
		}
		//System.out.println(sentWords);
		return sentWords;
	}
	
	/**
	 * Generate greedy sentence
	 * Takes the most likely word given the word before
	 */
	public String generateGreedySentence() {
		String word = "<s>";
		String sentence = "";
		while (!word.equals("</s>")) {
			double maxProb = 0.0;
			String next = "";
			for (String possibleWord : bigramCounts.get(word).keySet()) {
				Double prob = getBigramProb(word, possibleWord);
				if (prob > maxProb && possibleWord != unk && !possibleWord.equals("%NUMBER%") && !sentence.contains(possibleWord)) {
					next = possibleWord;
					maxProb = prob;
				}			
			}
			word = next;
			if (sentence.equals("")) {
				sentence = word;
			} else if (!word.equals("</s>")) {
				sentence = sentence + " " + word;
			}
		}
		return sentence;
	}
	
	/**
	 * Generate sentence by sampling from a conditional
	 * probability distribution 
	 * Words with a higher probability will have a 
	 * higher chance of being chosen
	 */
	public String generateSamplingSentence() {
		String word = "<s>";
		String sentence = "";
		String[] possibleWords = new String[vocabulary.size()];
		Double[] possibleProbs = new Double[vocabulary.size()];
		int h = 0;
		while (!word.equals(".") && h < 10) {
			int i = 0;
			for (String possibleWord : vocabulary) {
				possibleWords[i] = possibleWord;
				possibleProbs[i] = getBigramProb(word, possibleWord);
				i++;
			}
			Double[] sortProbs = possibleProbs.clone();
			for (int j = 1; j < sortProbs.length; j++) {
				sortProbs[j] += sortProbs[j - 1];
			}
			String nextWord = unk;
			while ((nextWord.equals(unk) || nextWord.equals("%NUMBER%") || nextWord.equals(start) || nextWord.equals(end)) 
					|| (nextWord.equals(".") && h < 3)) {
				double p = Math.random();
				int q = Arrays.binarySearch(sortProbs, p);
				q = q >= 0 ? q : (-q-1);
				nextWord = possibleWords[q];
			}
			word = nextWord;
			if (sentence.equals("")) {
				sentence = word;
			} else if (!word.equals("</s>")){
				sentence = sentence + " " + word;
			}
			h++;
		}
		return sentence;
	}
	
}
