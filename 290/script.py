import sys
from nltk.tokenize import sent_tokenize, word_tokenize


def main():
	
	print(sent_tokenize(sys.argv[1]))

main()
