# Assignment 5

# Nolan McCafferty
# Daniel Rosenbaum 

# This program takes input and a list of words with specific similarity measures and 
# returns the top 10 most similar words 

import sys
import re
import numpy as np
from numpy.linalg import norm
from numpy import true_divide
import math

# Initialization and Variables
total_words = {} # unique words to unique index
stoplist = {} # list of stoplist (use hashing to get O(1) lookup)
num_words = 0 # number of all word occurences 
word_counts = {} # unique word to number of occurences (term freq)
df = {} # unique word to # of documents its in
context_vectors = {} # 
sentences = [] # list of all editted sentences
# context vectors with correct weighting
tf_idf_vectors = {} 
pmi_vectors = {}

# Helper class to hold the context vectors for each word
class ContextVector:
	# create a context vector for each word initialized to zeros
	def __init__(self, size):
		self.c_vector = np.zeros(size)

	# increase count for a specific index
	def add(self, index):
		self.c_vector[index] += 1

## Helper Functions ##

# add the TF-IDF weighting to the context vector
def add_TFIDF(word, context_word, context_idx):
	
	# get the idf value and update vector with tf*idf
	idf = math.log(len(sentences)/df[context_word])
	tf_idf_vectors[word].c_vector[context_idx] = context_vectors[word].c_vector[context_idx] * idf

# add the PMI weighting to the context vector
def add_PMI(p_x, word, context_word, context_idx):
	
	# calculate the other individual parts
	p_x_y = context_vectors[word].c_vector[context_idx]/num_words
	p_y = word_counts[context_word]/num_words

	# check to make sure we are not doing log(0)
	pmi = math.log(p_x_y / (p_x * p_y)) if p_x_y > 0 else 0

	pmi_vectors[word].c_vector[context_idx] = pmi

# get the word in the context of a word, given a sentence
def get_context(sentence, word, idx):
	context = []

	# add words to the left
	if idx > 0:
		context.append(sentence[idx - 1])
		if idx > 1:
			context.append(sentence[idx - 2])

	# add words to the right
	if idx < len(sentence) - 1:
		context.append(sentence[idx + 1])
		if idx < len(sentence) - 2:
			context.append(sentence[idx + 2])

	# return list of context words
	return context

# stoplist - or more precisely stopset...
# we split the line in order to remove \n character
stoplist = {line.split()[0] for line in open(sys.argv[1], "r")}

# clean input sentences and calculate statistic measures
for line in open(sys.argv[2], "r"):
	sentence = []
	# split on spaces and only add unique alpha lower case words
	for idx, word in enumerate(line.lower().split()):
		if word not in stoplist and re.match("^[a-z]+$", word):
			num_words += 1 # keep track of all words
			
			# each unique word given a unique index
			if word not in total_words:
				total_words[word] = len(total_words)

			# update the counts for each word
			word_counts[word] = word_counts[word] + 1 if word in word_counts else 1

			# update the document counts for each word
			# only add the word if it is not in the sentence previously
			if word not in sentence:
				df[word] = df[word] + 1 if word in df else 1

			sentence.append(word)

	# add editted sentence to list
	sentences.append(sentence)

# create context vectors for each word in each sentence
for sentence in sentences:
	for i, word in enumerate(sentence):
		# create a new context vector if not initialized
		if word not in context_vectors:
			context_vectors[word] = ContextVector(len(total_words))
			
		# add words to the left
		if i > 0:
			context_vectors[word].add(total_words[sentence[i-1]])
			if i > 1:
				context_vectors[word].add(total_words[sentence[i-2]])
		
		# add words to the right
		if i < len(sentence) - 1:
			context_vectors[word].add(total_words[sentence[i+1]])
			if i < len(sentence) - 2:
				context_vectors[word].add(total_words[sentence[i+2]])


# create tf-idf vectors and pmi vectors
for sentence in sentences:
	for i, word in enumerate(sentence):

		# calculate the probability for the word for PMI weight
		p_x = word_counts[word]/num_words

		# get the contextual words
		context = get_context(sentence, word, i)

		# create a new empty context vector for a new word
		if word not in tf_idf_vectors:
			tf_idf_vectors[word] = ContextVector(len(total_words))
			pmi_vectors[word] = ContextVector(len(total_words))
		
		# add correct weighting to the corresponding vector for each context word
		# only need to deal with context words since all other will be 0
		for context_word in context:

			# get the index to update in the context vector
			context_idx = total_words[context_word]

			add_TFIDF(word, context_word, context_idx)
			add_PMI(p_x, word, context_word, context_idx)


# put all the weighted vectors searchable by key 
# we only need one copy of each
weighted_vectors = {}
weighted_vectors['TF'] = context_vectors
weighted_vectors['TF-IDF'] = tf_idf_vectors
weighted_vectors['PMI'] = pmi_vectors


# function to find the 10 most similar words
def find_most_similar(word, weighting, sim_measure, vector):
	query_vector = vector[word].c_vector
	norm_query_vec = true_divide(query_vector, norm(query_vector))
	similarities = {}

	for w in total_words:
		# use only words seen more than twice and do not compare the same word
		if word_counts[w] >= 3 and w != word:
			compare_vector = vector[w].c_vector
			norm_compare_vec = true_divide(compare_vector, norm(compare_vector))
			
			# calculate correct similarity measure
			if sim_measure == 'L1':
				sim = np.sum(np.absolute(np.subtract(norm_query_vec, norm_compare_vec)))
			elif sim_measure == 'EUCLIDEAN':
				sim = math.sqrt(np.sum(np.square(np.subtract(norm_query_vec, norm_compare_vec))))
			elif sim_measure == "COSINE":
				sim = np.dot(norm_query_vec, norm_compare_vec)
			else:
				sim = 0
				print("Invalid sim-measure given: Expected L1, EUCLIDEAN, COSINE")

			similarities[w] = sim

	# get top 10 results
	top_10 = sorted(similarities, key=similarities.get, reverse=True)[0:min(10, len(similarities))]
	
	# print output
	print('SIM:', word, weighting, sim_measure)
	for term in top_10:
		print(term, similarities[term])


# output 
print(len(total_words), "unique words")
print(num_words, "word occurences")
print(len(sentences), "sentences/lines/documents\n")

for line in open(sys.argv[3], "r"):
	query = line.split() 
	find_most_similar(query[0], query[1], query[2], weighted_vectors[query[1]])


