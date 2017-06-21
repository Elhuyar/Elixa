#!/bin/sh

# the folowing commented commands show how to use especific models rather than those built-in
#java -jar target/elixa-0.9.2.jar tag-gp -f tabNotagged -p elixa-models-0.9/en-twt.cfg -l en -m elixa-models-0.9/en-twt.model -cn 3 < src/main/resources/examples/example_en.tsv 
#java -jar target/elixa-0.9.2.jar tag-gp -f tabNotagged -p elixa-models-0.9/fr-twt.cfg -l fr -m elixa-models-0.9/fr-twt.model -cn 3 < src/main/resources/examples/example_fr.tsv 
#java -jar target/elixa-0.9.2.jar tag-gp -f tabNotagged -p elixa-models-0.9/eu-twt.cfg -l eu -m elixa-models-0.9/eu-twt.model -cn 3 < src/main/resources/examples/example_eu.tsv 
#java -jar target/elixa-0.9.2.jar tag-gp -f tabNotagged -p elixa-models-0.9/es-twt.cfg -l es -m elixa-models-0.9/es-twt.model -cn 3 < src/main/resources/examples/example_es.tsv 

# polarity tagging with built-in models
java -jar target/elixa-0.9.2.jar tag-gp -f tabNotagged -cn 3 -l en < src/main/resources/examples/example_en.tsv 
java -jar target/elixa-0.9.2.jar tag-gp -f tabNotagged -cn 3 -l fr < src/main/resources/examples/example_fr.tsv 
java -jar target/elixa-0.9.2.jar tag-gp -f tabNotagged -cn 3 -l eu < src/main/resources/examples/example_eu.tsv 
java -jar target/elixa-0.9.2.jar tag-gp -f tabNotagged -cn 3 -l es < src/main/resources/examples/example_es.tsv 

