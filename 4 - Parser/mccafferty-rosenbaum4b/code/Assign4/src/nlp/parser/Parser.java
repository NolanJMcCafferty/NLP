package nlp.parser;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.HashMap;


/**
 * Parser class that takes a PCFG and can parse sentences or files accordingly 
 * 
 * @author Nolan McCafferty
 * @author Daniel Rosenbaum
 *
 */
public class Parser {

	// Data Structures for storing the PCFG
	private HashMap<String, ArrayList<GrammarRule>> lexicalRules; // map from lexical -> list of all rules
	private HashMap<String, ArrayList<GrammarRule>> unaryRules; // map from RHS -> list of all rules
	private HashSet<GrammarRule> binaryRules; // list of all rules
	
	/**
	 * Constructor
	 * Initializes the grammar so that input sentences can be parsed
	 * Stores the grammar in the corresponding data structure
	 * 
	 * @param pcfgFile - a file that has PCFG grammar
	 */
	public Parser (String pcfgFile){
		
		// initialize global variables
		lexicalRules = new HashMap<String, ArrayList<GrammarRule>>();
		unaryRules = new HashMap<String, ArrayList<GrammarRule>>();
		binaryRules = new HashSet<GrammarRule>();
		
		// read in the PCFG and store the rules accordingly
		try {
			BufferedReader br = new BufferedReader(new FileReader(pcfgFile));
			
			// read through all the lines in the pcfg
			for (String pcfgString = br.readLine(); pcfgString != null; pcfgString = br.readLine()){				
				GrammarRule newRule = new GrammarRule(pcfgString);
				
				// Lexical Rules
				if (newRule.isLexical()){
					String lexer = newRule.getRhs().get(0);
					ArrayList<GrammarRule> rules = lexicalRules.getOrDefault(lexer, new ArrayList<GrammarRule>());
					rules.add(newRule);
					lexicalRules.put(lexer, rules);
				}
				
				// Unary Rules
				else if (newRule.numRhsElements() == 1){
					ArrayList<GrammarRule> corrRules = unaryRules.getOrDefault(newRule.getRhs().get(0), new ArrayList<GrammarRule>());
					corrRules.add(newRule);
					unaryRules.put(newRule.getRhs().get(0), corrRules);
				}

				// Binary Rules
				else 
					binaryRules.add(newRule);
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	/**
	 * Given a sentence generate the parse
	 * 
	 * @param sentence
	 * @return the parsed sentence (NULL if no parse exists)
	 */
	public String parseSentence(String sentence){
		
		String parsedSentence = "NULL";
		String[] words = sentence.split(" ");
		int len = words.length;
		
		// create the CKY table
		Cell[][] table = new Cell[len+1][len+1];
		
		// go diagonal and then up each column 
		for (int j = 0; j < len; j++){
			
			// COMPLETE THE DIAGONAL ENTRY
			String word = words[j];
			
			// create the table entry
			table[j][j] = new Cell();
			
			// add each lexical rule to the table entry 
			if (lexicalRules.containsKey(word)){
				for (GrammarRule lexRule : lexicalRules.get(word)) {
					
					// create an entry for the lexical word and for the constituent
					Entry lexicalEntry = new Entry(lexRule.getRhs().get(0), lexRule.weight, null, null);
					Entry constEntry = new Entry(lexRule.getLhs(), lexRule.weight, lexicalEntry, null);
					
					// add the entries to the cell
					table[j][j].addEntryToCell(lexRule.getRhs().get(0), lexicalEntry);
					table[j][j].addEntryToCell(lexRule.getLhs(), constEntry);

					// update with possible unary rules
					table[j][j].addUnaryRules(unaryRules, constEntry, lexRule.getLhs(), lexRule.getWeight());
				}
			}
			// if we are trying to parse a sentence with a word not in our grammar return NULL
			else return parsedSentence;
		
			// COMPLETE THE COLUMN GOING UPWARD
			for (int i = j - 1; i >= 0; i--){
				
				// create the new table cell entry
				table[i][j] = new Cell();
				
				// iterate through the possible options using k 
				for (int k = i; k < j; k++){
					
					// get the two cells 
					Cell leftCell = table[i][k];
					Cell downCell = table[k+1][j];
					
					// go through all possible binary rules to see if we have a match
					for (GrammarRule binaryRule : binaryRules){
						
						// get the necessary values from this rule
						String lhs = binaryRule.getLhs();
						String rhs1 = binaryRule.getRhs().get(0);
						String rhs2 = binaryRule.getRhs().get(1);
						
						// see if this rule applies to our entries
						if (leftCell.contains(rhs1) && downCell.contains(rhs2)){
							
							// get the two children entries and the new weight
							Entry leftEntry = leftCell.getEntry(rhs1);
							Entry downEntry = downCell.getEntry(rhs2);
							double newWeight = leftEntry.weight + downEntry.weight + binaryRule.getWeight();

							// if we already have this lhs in our cell entry only update if value is better
							if (table[i][j].contains(lhs)){
								Entry entry = table[i][j].getEntry(lhs);
								if (entry.weight < newWeight){
									// update the weight and the new children
									entry.update(newWeight, leftEntry, downEntry);
									table[i][j].addUnaryRules(unaryRules, entry, lhs, newWeight);
								}
							}
							
							// if we do not have this rule then add it (and check for unary rules)
							else {
								Entry newEntry = new Entry(lhs, newWeight, leftEntry, downEntry);
								table[i][j].addEntryToCell(lhs, newEntry);
								
								// check to add any unary rules
								table[i][j].addUnaryRules(unaryRules, newEntry, lhs, newWeight);
							}
						}
					}
				}
			}
		}
		
		// start in top right
		Cell topRightCell = table[0][len-1];
		if (topRightCell.contains("S")) {
			
			// get the corresponding entry
			Entry startEntry = topRightCell.getEntry("S");
			
			// go through and create the string 
			parsedSentence = printTable(startEntry, "");
			
			// add in the probability of the parse
			parsedSentence += "\t" + startEntry.weight;
		}
		
		return parsedSentence;
	}
	
	
	/**
	 * Given an entry print it out and its children while recursively building our result
	 * 
	 * @param entry - the entry to print 
	 * @param result - the string we are recursively building
	 * @return
	 */
	public String printTable(Entry entry, String result) {
		
		// check to see if we should iterate through our children
		if (entry.leftChild != null) {

			// add the rule to the result
			result += "(" + entry.rule + " ";

			// every node has a left child
			result += printTable(entry.leftChild, "");
			
			// check if another child (unary rules do not)
			if (entry.downChild != null) 
				result += " " + printTable(entry.downChild, "");
			
			// add in the closing right paren
			result += ")";
		}
		
		// we have reached a lexical term so we print the word
		else 
			result += entry.rule;
		
		return result;
	}
	
	
	/**
	 * Read in a file and parse each sentence
	 * 
	 * @param filename
	 * @return a list of parsed sentences 
	 */
	public ArrayList<String> parseInputFile(String filename){
		// read in the PCFG and store the rules accordingly
		ArrayList<String> parsedSentences = new ArrayList<String>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(filename));
			
			// read through all the sentences in the file and parse each one
			for (String line = br.readLine(); line != null; line = br.readLine())
				parsedSentences.add(parseSentence(line));
			
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return parsedSentences;
	}
	
	/**
	 * Given an arrayList strings 
	 * create and write to a given filename
	 *  
	 * @param rules
	 * @param filename
	 */
	public void createOutfile(ArrayList<String> sentences, String filename){
		try {
			FileWriter writer = new FileWriter(filename); 
			
			// write each sentence
			for(String sentence: sentences) 
				writer.write(sentence + "\n");
			
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Main method to control the execution of the program
	 * 
	 * @param args - requires to have two entries 
	 * 		- a file for the pcfg
	 * 		- a file with the input sentences
	 */
	public static void main(String[] args){
		String pcfg = args[0];
		String input = args[1];
		
		System.out.println("Reading in the PCFG...");

		// create the cky parser
		Parser parser = new Parser(pcfg);
		
		System.out.println("Completed reading the grammar!");
		System.out.println("Parsing the sentences...");

		// parse the file
		ArrayList<String> parsedSentences = parser.parseInputFile(input);
		
		System.out.println("Completed parsing!");

		// write results to the file
		parser.createOutfile(parsedSentences, "output/test.sentences.parsed");
	}
}


