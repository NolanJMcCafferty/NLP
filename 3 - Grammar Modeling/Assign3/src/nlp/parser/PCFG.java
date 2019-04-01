package nlp.parser;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.NavigableMap;
import java.util.Random;
import java.util.TreeMap;

/**
 * A class to construct PCFGs for given files
 * Also includes methods to binarize the grammar and generate sentences from this grammar
 * 
 * @author Daniel Rosenbaum
 * @author Nolan McCafferty
 *
 */
public class PCFG {

	// Data Structures
	private HashMap<String, Double> constituentCounts; 	// number of times LHS is used
	private HashMap<String, HashMap<ArrayList<String>, Double>> ruleCounts; // number of times each total rule is used
	private HashMap<String, RandomCollection<ArrayList<String>>> grammarProbs;
	private ArrayList<GrammarRule> rules;
	private HashSet<String> terminals;
	/**
	 * PCFG class that reads in a file and creates a grammar representation
	 * 
	 * @param filename
	 */
	public PCFG(String filename){
		// initialize the data structures
		constituentCounts = new HashMap<String, Double>();
		ruleCounts = new HashMap<String, HashMap<ArrayList<String>, Double>>();
		rules = new ArrayList<GrammarRule>();
		terminals = new HashSet<String> ();
		
		// generate the ParseTree for each sentence in the file and
		// perform all the necessary counts for each sentence
		try {
			BufferedReader br = new BufferedReader(new FileReader(filename));
	
			// read through all the sentences in the corpus
			for (String sentence = br.readLine(); sentence != null; sentence = br.readLine()){
				ParseTree parseTree = new ParseTree(sentence);
				
				// for each sentence - traverse the tree and update counts
				updateCounts(parseTree);				
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Recursively iterate through the ParseTree to update counts 
	 * for each grammar rule used 
	 * 
	 * @param ParseTree
	 */
	public void updateCounts(ParseTree tree){
		// count the LHS constituent
		if (!tree.isTerminal()){
		
			String lhs = tree.getLabel();
			double lhsCount = constituentCounts.getOrDefault(lhs, 0.0);
			constituentCounts.put(lhs, lhsCount + 1.0);
			
			// add to the rule counts
			HashMap<ArrayList<String>, Double> rhsOptions = ruleCounts.getOrDefault(lhs, new HashMap<ArrayList<String>, Double>());
			
			// count the RHS rules 
			ArrayList<String> rhs = tree.getChildrenLabels();
			double rhsValue = rhsOptions.getOrDefault(rhs, 0.0);
			rhsOptions.put(rhs, rhsValue + 1.0);
			
			ruleCounts.put(lhs, rhsOptions);
			
			for (ParseTree child: tree.getChildren()){
				updateCounts(child);
			}
		}
		else {
			terminals.add(tree.getLabel());
		}
	}
	
	/**
	 * Once the file has been parsed and counts accumulated go through all the rules
	 * and create a new list with all the grammar rules and probabilities
	 */
	public ArrayList<GrammarRule> calculateProbs(){
		// go through all the rules
		for (String lhs : ruleCounts.keySet()){
			double denominator = constituentCounts.get(lhs);
			
			for (ArrayList<String> rhs : ruleCounts.get(lhs).keySet()){
				
				double numerator = ruleCounts.get(lhs).get(rhs);
				
				double probability = numerator/denominator;
				boolean isLexical = rhs.size() == 1 && terminals.contains(rhs.get(0));
				GrammarRule newRule = new GrammarRule(lhs, rhs, isLexical);
				newRule.setWeight(probability);
				
				rules.add(newRule);
			}
		}
		return rules;
	}
	
	/**
	 * Given an arrayList of either GrammarRule or Strings 
	 * create and write to a given filename 
	 * @param rules
	 * @param filename
	 */
	public void createOutfile(ArrayList<?> rules, String filename){
		try {
			FileWriter writer = new FileWriter(filename); 
			for(Object rule: rules) {
				writer.write(rule.toString() + "\n");
			}
			writer.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	/**
	 * Given an ArrayList of GrammarRules then generate a new ArrayList of GrammarRules where
	 * each rule is binary - meeting CYK form 
	 * 
	 * @param rules
	 * @return binarized rules 
	 */
	public ArrayList<GrammarRule> binarizeGrammar(ArrayList<GrammarRule> rules) {
		
		// keep track of the number of new rules
		int newRules = 0;

		// new list of rules
		ArrayList<GrammarRule> binRules = new ArrayList<GrammarRule>();
		
		// iterate through rules
		for (GrammarRule rule : rules) {
			// boolean to see if this rule has been edited already
			Boolean replacedRule = false;
			
			// check if the right hand side has more than 2 elements	
			while (rule.numRhsElements() > 2) {
				newRules++;
				ArrayList<String> oldRHS = rule.getRhs();
				ArrayList<String> newRHS = new ArrayList<String>();
				
				// get first two constituents
				ArrayList<String> firstTwoConstituents = new ArrayList<String>();
				
				// if this is the first time editing this rule
				if (!replacedRule) {
					// add first constituent to new rule constituents
					firstTwoConstituents.add(oldRHS.get(0));
					replacedRule = true;
				}
				// if the rule has already been edited
				else {
					// add X rule to new rule constituents
					firstTwoConstituents.add("X"+(newRules-1));
				}
				// add second constituent to new rule constituents
				firstTwoConstituents.add(oldRHS.get(1));

				// create new rule
				GrammarRule xRule = new GrammarRule("X"+newRules, firstTwoConstituents);
				
				// set probability == 1
				xRule.setWeight(1.0);
				binRules.add(xRule);
				
				// update the old rule
				newRHS.add("X"+newRules);
				for (int i = 2; i < oldRHS.size();i++) {
					newRHS.add(oldRHS.get(i));
				}
				
				// keep probability of old rule
				double weight = rule.getWeight();
				rule = new GrammarRule(rule.getLhs(), newRHS);
				rule.setWeight(weight);
			}
			binRules.add(rule);	
		}
		return binRules;
	}
	
	
	/**
	 * EXTRA CREDIT
	 * Attempt at implementing the binarization of the grammar reusing some intermediary rules
	 * 
	 * @param rules
	 * @return ArrayList of GrammarRules without any duplicates of intermediary rules 
 	 */
	public ArrayList<GrammarRule> binarizeSharedGrammar(ArrayList<GrammarRule> rules) {
		// initialize to one so that the rules start X1...
		int newRules = 1;

		ArrayList<GrammarRule> binSharedRules = new ArrayList<GrammarRule>();
		ArrayList<ArrayList<String>> intermediaryRHS = new ArrayList<ArrayList<String>>();
		
		for (GrammarRule rule : rules) {
			Boolean replacedRule = false;
			
			while (rule.numRhsElements() > 2) {
				ArrayList<String> oldRHS = rule.getRhs();
				ArrayList<String> newRHS = new ArrayList<String>();
				ArrayList<String> firstTwoConstituents = new ArrayList<String>();
				
				// if this is the first time editing this rule
				if (!replacedRule) {
					// add first constituent to new rule constituents
					firstTwoConstituents.add(oldRHS.get(0));
					replacedRule = true;
				}
				// if the rule has already been edited
				else {
					// add X rule to new rule constituents
					firstTwoConstituents.add("X"+(newRules-1));
				}
				// add second constituent to new rule constituents
				firstTwoConstituents.add(oldRHS.get(1));

				// create new rule
				GrammarRule xRule = new GrammarRule("X"+newRules, firstTwoConstituents);
				
				// if the new intermediary rule has not been seen
				if (!intermediaryRHS.contains(xRule.getRhs())){
					// set probability == 1
					xRule.setWeight(1.0);
					binSharedRules.add(xRule);
					
					// update the old rule
					newRHS.add("X"+newRules);
					
					// here we are using the index of the array to get which X?? rule it is 
					// so starting at X1 we need to subtract 1 to get a matching index 
					intermediaryRHS.add(newRules-1, xRule.getRhs());
					
					newRules++;
				}
				
				// else the rule has already been seen
				// so reuse the intermediary rule
				else {
					int idx = intermediaryRHS.indexOf(xRule.getRhs());
					
					// update the old rule
					newRHS.add("X"+(idx+1));
				}
				
				for (int i = 2; i < oldRHS.size();i++) {
					newRHS.add(oldRHS.get(i));
				}
				
				
				// keep probability of old rule
				double weight = rule.getWeight();
				rule = new GrammarRule(rule.getLhs(), newRHS);
				rule.setWeight(weight);
			}
			binSharedRules.add(rule);	
		}
		return binSharedRules;
	}

	/**
	 * EXTRA CREDIT
	 * 
	 * Given a grammar set, generate some sentences
	 * 
	 * @param grammar
	 * @return ArrayList of strings, where each String is a sentence
	 */
	public ArrayList<String> generateSentences(ArrayList<GrammarRule> grammar){
		// go through the new grammar create the hashmaps with probs
		grammarProbs = new HashMap<String, RandomCollection<ArrayList<String>>>();

		ArrayList<String> sentences = new ArrayList<String>();
		for (GrammarRule rule: grammar){
			RandomCollection<ArrayList<String>> newRule = grammarProbs.getOrDefault(rule.getLhs(), new RandomCollection<ArrayList<String>>());
			newRule.add(rule.getWeight(), rule.getRhs());
			grammarProbs.put(rule.getLhs(), newRule);
		}
		
		// now that the grammar is stored in the HashMap data structure probabilistically construct the tree
		// 10 sentences
		for (int i = 0; i < 10; i++){
			// start with the S 
			RandomCollection<ArrayList<String>> possRules = grammarProbs.get("S");
			// recursively go through the tree picking probabilistically
			String sentence = constructSentence(possRules, "");
			sentences.add(sentence);
		}
		
		return sentences;
	}
	
	/**
	 * EXTRA CREDIT
	 * 
	 * Given a rule, probabilistically choose the RHS and continue recursively 
	 * to create a sentence in the end 
	 * @param rules
	 * @param sentence
	 * @return the final sentence (words only)
	 */
	public String constructSentence(RandomCollection<ArrayList<String>> rules, String sentence){
		
		// get the next node 
		ArrayList<String> nextRHS = rules.next();
		
		// if the next is the terminal
		for (String newLHS: nextRHS){
			if (grammarProbs.containsKey(newLHS) && !newLHS.equals(".") && !newLHS.equals(",") && !newLHS.equals("$")){
				System.out.println(newLHS);
				sentence = constructSentence(grammarProbs.get(newLHS), sentence);
			}
			// we have reached a terminal so we add the word to our building sentence!
			else {
				sentence += newLHS + " ";
				break;
			}
		}
		return sentence;
		
	}
	
	
	public static void main(String[] args){
		
		String filename = "data/example.parsed";
		
		// initialize the class to get all the counts
		PCFG pcfg = new PCFG(filename);

		// now that everything has been counted we will calculate the probabilities
		ArrayList<GrammarRule> rules = pcfg.calculateProbs();
				
		// write the grammar to a file
		pcfg.createOutfile(rules, "output/example.pcfg");

		// binarize grammar
		ArrayList<GrammarRule> binRules = pcfg.binarizeGrammar(rules);
		ArrayList<GrammarRule> binSharedRules = pcfg.binarizeSharedGrammar(rules);

		// write binarized and shared binarization grammar to a file
		pcfg.createOutfile(binRules, "output/example.binary.pcfg");
		pcfg.createOutfile(binSharedRules, "output/example.binary.shared.pcfg");

		// generate sentences and write them to a file
		ArrayList<String> genSentences = pcfg.generateSentences(binRules);
		pcfg.createOutfile(genSentences, "output/example.random.sents");

	}


	/**
	 * This is a class in order to probabilistically choose various options in a RandomCollection
	 * 
	 * @author Daniel Rosenbaum
	 * @author Nolan McCafferty
	 *
	 * @param <E>
	 */
	private class RandomCollection<E> {
	    private final NavigableMap<Double, E> map = new TreeMap<Double, E>();
	    private final Random random;
	    private double total = 0;
	
	    public RandomCollection() {
	        this(new Random());
	    }
	
	    public RandomCollection(Random random) {
	        this.random = random;
	    }
	
	    public RandomCollection<E> add(double weight, E result) {
	        if (weight <= 0) return this;
	        total += weight;
	        map.put(total, result);
	        return this;
	    }
	
	    public E next() {
	        double value = random.nextDouble() * total;
	        return map.higherEntry(value).getValue();
	    }
	}
}
