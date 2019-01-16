/*
 * Copyright 2014 Elhuyar Fundazioa

This file is part of EliXa.

    EliXa is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    EliXa is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with EliXa.  If not, see <http://www.gnu.org/licenses/>.
 */


package elh.eus.absa;

import ixa.kaflib.KAFDocument;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import net.sourceforge.argparse4j.inf.Subparsers;

import org.apache.commons.io.FileUtils;
import org.jdom2.JDOMException;

import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SparseInstance;
//import elh.eus.absa.TrainerSVMlight;


/**
 * Main class of elh-eus-absa-atp, the elhuyar absa ATP modules
 * tagger.
 *
 * @author isanvi
 * @version 2014-12-13
 *
 */
public class CLI {
	/**
	 * Get dynamically the version of elh-eus-absa-atp by looking at the MANIFEST
	 * file.
	 */
	private final String version = CLI.class.getPackage().getImplementationVersion();
	/**
	 * Name space of the arguments provided at the CLI.
	 */
	private Namespace parsedArguments = null;
	/**
	 * Argument parser instance.
	 */
	private ArgumentParser argParser = ArgumentParsers.newFor("elixa-" + version + ".jar")
			.build()
				.defaultHelp(true)
				.description("elixa-" + version
					+ " is a multilingual document classification and ABSA module developed by the Elhuyar Foundation R&D Unit.\n");
	/**
	 * Sub parser instance.
	 */
	private Subparsers subParsers = argParser.addSubparsers().help(
			"sub-command help");
	/**
	 * The parser that manages the tagging sub-command.
	 */
	private Subparser annotateParser;
	/**
	 * The parser that manages the document classifier training sub-command.
	 */
	private Subparser trainDocParser;
	/**
	 * The parser that manages the document classifier evaluation sub-command.
	 */
	private Subparser evalDocParser;
	/**
	 * The parser that manages the document classifier tagging sub-command.
	 */
	private Subparser tagDocParser;
	/**
	 * The parser that manages the ATC (target category classification) training sub-command.
	 */
	private Subparser trainATCParser;
	private Subparser trainATC2Parser;
	
	/**
	 * The parser that manages the slot2 (OTE) tagging sub-command.
	 */
	private Subparser slot2Parser;


	/**
	 * The parser that manages the evaluation sub-command.
	 */
	private Subparser tagSentParser;
	
	
	/**
	 * Parser that manages the polarity tagging and estimation of a text (KAF/NAF format for the moment).
	 */
	private Subparser predictParser;
	
	
	/**
	 * Default polarity lexicon names. 
	 * 2017/04/25: for the moment, this values are hard coded because lexicons are not given standard names
	 * or provided with its own property file. In the future one of the two alternatives should be provided.
	 */
	private final static Properties defaultLexicons = new Properties();
	static {
		defaultLexicons.setProperty("en", "en-union_restrictNew.lex");
		defaultLexicons.setProperty("eu", "ElhPolar_euLLR.lex");
		defaultLexicons.setProperty("es", "ElhPolarMoodmap.lex");
		defaultLexicons.setProperty("fr", "FEEL-fr.lex");
	}
	

	/**
	 * Construct a CLI object with the three sub-parsers to manage the command
	 * line parameters.
	 */
	public CLI() {
		annotateParser = subParsers.addParser("tag-ate").help("Tagging CLI");
		loadAnnotateParameters();
		trainDocParser = subParsers.addParser("train-doc").help("Document classification training CLI");
		loadDocTrainingParameters();
		evalDocParser = subParsers.addParser("eval-doc").help("Document classification evaluation CLI");
		loadDocevalParameters();
		tagDocParser = subParsers.addParser("tag-doc").help("Document tagging CLI");
		loadDocTagParameters();
		trainATCParser = subParsers.addParser("train-atc").help("ATC training CLI (single classifier)");
		loadATCTrainingParameters();
		trainATC2Parser = subParsers.addParser("train-atc2").help("ATC Training CLI (E & A classifiers");
		loadATC2TrainingParameters();
		slot2Parser = subParsers.addParser("slot2").help("Semeval 2015 slot2 (ATE) formatting CLI");
		loadslot2Parameters();
		tagSentParser = subParsers.addParser("tagSentences").help("Lemmatization and PoS tagging CLI");
		loadTagSentParameters();
		predictParser = subParsers.addParser("tag-naf").help("Predict polarity of a text");
		loadPredictionParameters();
	}
	
	
	/**
	 * Main entry point of elixa.
	 *
	 * @param args
	 * the arguments passed through the CLI
	 * @throws IOException
	 * exception if input data not available
	 * @throws JDOMException
	 * if problems with the xml formatting of NAF
	 */
	public static void main(final String[] args) throws IOException,
	JDOMException {
		CLI cmdLine = new CLI();
		cmdLine.parseCLI(args);
	}
	
	
	/**
	 * Parse the command interface parameters with the argParser.
	 *
	 * @param args
	 * the arguments passed through the CLI
	 * @throws IOException
	 * exception if problems with the incoming data
	 * @throws JDOMException
	 */
	public final void parseCLI(final String[] args) throws IOException, JDOMException {
		try {
			parsedArguments = argParser.parseArgs(args);
			System.err.println("CLI options: " + parsedArguments);
			if (args[0].equals("tagSentences")) {
				tagSents(System.in);
			} else if (args[0].equals("train-doc")) {
				trainDoc(System.in);
			} else if (args[0].equals("eval-doc")) {
				evalDoc(System.in);
			} else if (args[0].equals("train-atc")) {
				trainATC(System.in);
			} else if (args[0].equals("train-atc2")) {
				trainATC2(System.in);
			} else if (args[0].equals("tag-doc")) {
				tagDoc(System.in);
			} else if (args[0].equals("tag-ate")) {
					tagATE(System.in, System.out);					
			} else if (args[0].equals("slot2")) {
				slot2(System.in);
			}else if (args[0].equals("tag-naf")){
				predictPolarity(System.in);
			}
		} catch (ArgumentParserException e) {
			argParser.handleError(e);
			System.out.println("Run java -jar target/elixa-" + version
					+ ".jar (train-atc|slot2|tagSentences|tag-ate|train-doc|eval-doc|tag-doc|tag-naf) -help for details");
			System.exit(1);
		}
	}
	
	
	public final void predictPolarity(final InputStream inputStream) throws IOException {

		//String files = parsedArguments.getString("file");
		String lexicon = parsedArguments.getString("lexicon");
		//String estimator = parsedArguments.getString("estimator");
		String synset = parsedArguments.getString("synset");
		//String dictw = parsedArguments.getString("weights");
		float threshold = parsedArguments.getFloat("threshold");
		boolean printPol = parsedArguments.getBoolean("estimatePolarity");
		boolean polarWrds = parsedArguments.getBoolean("polarWords");
		
		//System.out.println("Polarity Predictor: ");
		//BufferedReader freader = new BufferedReader(new FileReader(files));   		
		//String line;
		//while ((line = freader.readLine()) != null) 
		//{
			try {
				KAFDocument naf = KAFDocument.createFromStream(new InputStreamReader(inputStream));
				
				File lexFile = new File(lexicon);
				Evaluator evalDoc = new Evaluator(lexFile, synset, threshold, "avg");
				Map<String, String> results = evalDoc.processKaf(naf, lexFile.getName());
				if (polarWrds)
				{
					FileUtilsElh.printPolarWordsFromNaf(naf);
				}
				else
				{
					naf.print();					
				}
				
				if (printPol)
				{
					System.out.println("<Elixa-gp>\n"
							+ "\t<sentiment-words>"+results.get("sentTermNum")+"</sentiment-words>\n"
							+ "\t<polarity-score>"+results.get("avg")+"</polarity-score>\n"
							+ "\t<polarity-threshold>"+results.get("thresh")+"</polarity-threshold>\n"
							+ "\t<polarity>"+results.get("polarity")+"</polarity>\n"
							+ "</Elixa-gp>\n");
				}
				//Map<String, Double> results = avg.processCorpus(corpus);
				//System.out.println("eval avg done"+results.toString());
				/*System.out.println("Prediction with avg done: \n"
		    				+ "\tTagged file: "+results.get("taggedFile")+"\n"
		    				+ "\tNumber of words containing sentiment found: "+results.get("sentTermNum")+"\n"
		    				+ "\tPolarity score: "+results.get("avg")
		    				+ "\tPolarity (threshold -> "+results.get("thresh")+"): "+results.get("polarity"));*/
				//FileUtilsElh.prettyPrintSentKaf(results);					
			} catch (Exception e) {
				//System.err.println("predictPolarity: error when processing "+line+" file");
				System.err.println("EliXa::tag-naf: error when processing naf");
				//e.printStackTrace();
			}
		//}
		//freader.close();
	}
	
	private void loadPredictionParameters() {
	
		//predictParser.addArgument("-f", "--file")
		//.required(true)
		//.help("Input file to predict the polarity lexicon in KAF/NAF format.\n");

		predictParser.addArgument("-l", "--lexicon")
		.required(true)
		.help("Path to the polarity lexicon file.\n");

		predictParser.addArgument("-s", "--synset")
		.choices("lemma", "first","rank")
		.required(false)
		.setDefault("lemma")
		.help(
				"Default polarities are calculated over lemmas. With this option polarity of synsets is taken into account instead of words. Possible values: (lemma|first|rank). 'first' uses the sense with the highest confidence value for the lemma. 'rank' uses complete ranking of synsets.\n");

//		predictParser.addArgument("-w", "--weights")
//		.action(Arguments.storeTrue())
//		.help(
//				"Use polarity weights instead of binary polarities (pos/neg). If the dictionary does not provide polarity scores the program defaults to binary polarities.\n");

		predictParser.addArgument("-t", "--threshold")
		.required(false)
		.setDefault((float)0)
		.help(
				"Threshold which limits positive and negative reviews. Float in the [-1,1] range. Default value is 0."
				+ " It is used in combination with the --estimatPolarity\n");

		predictParser.addArgument("-e", "--estimatePolarity")
		.action(Arguments.storeTrue())
		.help(
				"print a polarity estimation based on a simple average word polarity count (from words in the lexicon given).\n"
				+ "WARNING: this polarity estimation is for test purposes. If you activate it an additional element will be "
				+ "printed with the estimation statistics <Elixa-gp>, but the resulting naf won't be valid if that line is not deleted.\n");
		
		predictParser.addArgument("-p", "--polarWords")
		.action(Arguments.storeTrue())
		.help("Print list of polar words in the NAF file instead of returning the NAF file with tagged sentiment words\n.");
	}


	
	public final void tagSents(final InputStream inputStream)
	{
		String posModel = parsedArguments.getString("model");
		String lemmaModel = parsedArguments.getString("lemmaModel");
		String dir = parsedArguments.getString("dir");
		String lang = parsedArguments.getString("language");
		String format = parsedArguments.getString("format");
		boolean print = parsedArguments.getBoolean("print");
		
		System.err.println("Sentence tagging CLI, going to read the corpus.");
		CorpusReader reader = new CorpusReader(inputStream, format, lang);
		try {
			String tagDir= dir+File.separator+lang;
			Files.createDirectories(Paths.get(tagDir));
			reader.tagSentences(tagDir, posModel, lemmaModel, print);
		} catch (Exception e) {			
			e.printStackTrace();
		} 
		
	}
	
	public final void loadTagSentParameters()
	{
		tagSentParser.addArgument("-m", "--model")
		.setDefault("default")
		.help("Pass the model to do the tagging as a parameter.\n");
		tagSentParser.addArgument("-lm", "--lemmaModel")
		.setDefault("default")
		.help("Pass the model to do the lemmatization as a parameter.\n");
		tagSentParser.addArgument("-d", "--dir")
		.required(true)
		.help("directory to store tagged files.\n");
		tagSentParser.addArgument("-f", "--format")
		.setDefault("tabNotagged")
		.choices("tabNotagged", "semeval2015")	
		.help("format of the input corpus.\n"
				+ "\ttabNotagged = \"id<tab>polarity<tab>text[<tab>addittionalfields]\" (polarity may be '?' if unknown)\n"
				+ "\tsemeval2015 = Semeval 2015 ABSA shared task xml format.\n");
		tagSentParser.addArgument("-p", "--print")
		.action(Arguments.storeTrue())
		.help("Whether the tagged files should be printed as a corpus.\n");
		tagSentParser.addArgument("-l","--language")
		.setDefault("en")
		.choices("de", "en", "es", "eu", "it", "nl", "fr")
		.help("Choose language; if not provided it defaults to: \n"
				+ "\t- If the input format is NAF, the language value in incoming NAF file."
				+ "\t- en otherwise.\n");
	}
	
	
	/**
	 * Main method to do Aspect Term Extraction tagging.
	 *
	 * @param inputStream
	 * the input stream containing the content to tag, it must be NAF format
	 * @param outputStream
	 * the output stream providing the named entities
	 * @throws IOException
	 * exception if problems in input or output streams
	 */
	public final void tagATE(final InputStream inputStream,
			final OutputStream outputStream) throws IOException, JDOMException {
		BufferedReader breader = new BufferedReader(new InputStreamReader(
				inputStream, "UTF-8"));
		BufferedWriter bwriter = new BufferedWriter(new OutputStreamWriter(
				outputStream, "UTF-8"));
		// read KAF document from inputstream
		KAFDocument naf = KAFDocument.createFromStream(breader);
		// load parameters into a properties
		String model = parsedArguments.getString("model");
		//String outputFormat = parsedArguments.getString("outputFormat");
		String lexer = parsedArguments.getString("lexer");
		String dictTag = parsedArguments.getString("dictTag");
		String dictPath = parsedArguments.getString("dictPath");
		// language parameter
		String lang = null;
		if (parsedArguments.getString("language") != null) {
			lang = parsedArguments.getString("language");
			if (!naf.getLang().equalsIgnoreCase(lang)) {
				System.err
				.println("Language parameter in NAF and CLI do not match!!");
				System.exit(1);
			}
		} else {
			lang = naf.getLang();
		}
		
		naf = NLPpipelineWrapper.ixaPipesNERC(naf, model, lexer, dictTag, dictPath);	
		naf.save("entity-annotated.kaf");
		
		bwriter.close();
		breader.close();
	}
	
	
	/**
	 * Main access to the document classification training functionalities. Mainly designed for polarity
	 *
	 * @throws IOException
	 * input output exception if problems with corpora
	 */
	public final void trainDoc(final InputStream inputStream) throws IOException {
		// load training parameters file
		String paramFile = parsedArguments.getString("params");
		String corpusFormat = parsedArguments.getString("corpusFormat");
		String validation = parsedArguments.getString("validation");
		String lang = parsedArguments.getString("language");
		String classes = parsedArguments.getString("classnum");
		String classifier = parsedArguments.getString("classifier");
		String cparam = parsedArguments.getString("cparameter");

		int foldNum = Integer.parseInt(parsedArguments.getString("foldNum"));
		//boolean printPreds = parsedArguments.getBoolean("printPreds");
		
		Properties params = loadParameters(paramFile, lang);
		String kafDir = setPoStaggingFolder(params,"train");
		System.err.println("trainDoc : pos tagging folder set to: "+kafDir);
		
		
		CorpusReader reader = new CorpusReader(inputStream, corpusFormat, lang);
		System.err.println("trainDoc : Corpus read, creating features");
		Features atpTrain = new Features (reader, params, classes);			
		Instances traindata;
		if (corpusFormat.startsWith("tab") && !corpusFormat.equalsIgnoreCase("tabNotagged"))
		{
			traindata = atpTrain.loadInstancesConll(true, "atp",false);
		}
		else if (lang.equalsIgnoreCase("eu") && params.getProperty("pos-model","default").contains("eustagger"))
		{
			traindata = atpTrain.loadInstancesConll(true, "atp",true);			
		}
		else
		{
			//traindata = atpTrain.loadInstancesMod(true, "atp");
			traindata = atpTrain.loadInstances(true, "atp");
		}
		
		//setting class attribute (entCat|attCat|entAttCat|polarityCat)
		traindata.setClass(traindata.attribute("polarityCat"));		
		WekaWrapper classify;
		try {			
			String modelPath = params.getProperty("fVectorDir");
			classify = new WekaWrapper(traindata, true,classifier,cparam);
			classify.saveModel(modelPath+File.separator+"elixa-atp_"+lang+".model");			
			switch (validation)
			{
			case "cross":
				classify.crossValidate(foldNum); break;				
			case "trainTest":
				classify.trainTest(); break;
			case "both":
				classify.crossValidate(foldNum); classify.trainTest(); break;
			default:
				System.out.println("train-atp: wrong validation option. Model saved but not tested");
			}
		} catch (Exception e) {
			e.printStackTrace();			
		}
	}
	
	/**
	 * Create the main parameters available for training ATP models.
	 */
	private void loadDocTrainingParameters() {
		trainDocParser.addArgument("-p", "--params").required(true)
		.help("Load the training parameters file\n");		
		trainDocParser.addArgument("-cl","--classifier")
		.required(false)
		.choices("smo", "libsvm","linearsvm")
		.setDefault("smo")
		.help("Choose svm classifier. It defaults to weka smo implementation.\n");
		trainDocParser.addArgument("-cp","--cparameter")
		.required(false)		
		.setDefault("1")
		.help("Choose svm classifier parameter c. It defaults to 1.\n");
		trainDocParser.addArgument("-cvf", "--foldNum")
		.required(false)
		.setDefault(10)
		.help("Number of folds to run the cross validation on (default is 10).\n");
		trainDocParser.addArgument("-v","--validation")
		.required(false)
		.choices("cross", "trainTest", "both")
		.setDefault("cross")
		.help("Choose the way the trained model will be validated\n"
				+ "\t - cross : 10 fold cross validation.\n"
				+ "\t - trainTest : 90% train / 10% test division.\n"
				+ "\t - both (default): both cross validation and train/test division will be tested.");
		trainDocParser.addArgument("-f","--corpusFormat")
		.required(false)
		.choices("semeval2015", "semeval2014", "tab", "tabglobal", "tabNotagged", "globalNotagged")
		.setDefault("tabNotagged")
		.help("Choose format of reference corpus; it defaults to semeval2015 format.\n"
				+ "	- tabNotagged = \"id<tab>polarity<tab>text[<tab>addittionalfields]\" (polarity may be '?' or 'null' if unknown). "
				+ "Text is raw text. If this format is used, Elixa takes care of linguistically tagging the texts through ixa-pipes.\n"
				+ "	- semeval2014 = Semeval 2014 ABSA shared task xml format.\n"
				+ "	- semeval2015 = Semeval 2015 ABSA shared task xml format.\n"
				+ "	- globalNotagged|ireom = \"id<tab>text\" per line, Same as tabNotagged, but without polarity annotations.\n"				
				+ "	- tabGlobal = Already linguistically tagged corpus in conll format (if you have a corpus tagged with a tagger other than ixa-pipes for example)."
				+ "A pseudo xml format is used to pass document boundaries and polarity annotations. The format of the corpus must be as follows:\n\n" + 
				"	<doc id=\"([^\"]+)\" (pol|polarity)=\"([^\"]+)\"( score=\"([^\"]+)\")?>\n" + 
				"	form<tab>lemma<tab>PoS\n" + 
				"	form<tab>lemma<tab>PoS\n" + 
				"	...	   \n" + 
				"	</doc>\n" + 
				"	...\n\n" + 
				"	where\n" + 
				"		- id is any character string ([^\"]+)\n" + 
				"		- \"pol|polarity\" = pos,neg,neu \n" + 
				"		- score is the same as polarity but in a numeric scale (e.g. [1..5])\n"
				+ "	- tab = \"id<tab>polarity<tab>text[<tab>addittionalfields]\" (polarity may be '?' or 'null' if unknown)\n"
				);

		trainDocParser.addArgument("-cn","--classnum")
		.required(false)
		.choices("3", "3+", "5+", "5", "binary")
		.setDefault("3+none")
		.help("Choose the number of classes the classifier should work on "
				+ "(binary=p|n ; 3=p|n|neu ; 3+=p|n|neu|none ; 5=p|n|neu|p+|n+ ; 5+=p|n|neu|p+|n+|none )"
				+ " it defaults to 3 (p|n|neu).\n");
		trainDocParser.addArgument("-o","--outputpredictions")		
		.action(Arguments.storeTrue())
		.setDefault("false")
		.help("Output predictions or not; output is the corpus annotated with semeval2015 format.\n");
		trainDocParser.addArgument("-l","--language")
		.setDefault("en")
		.choices("de", "en", "es", "eu", "it", "nl", "fr")
		.help("Choose language; if not provided it defaults to: \n"
				+ "\t- If the input format is NAF, the language value in incoming NAF file."
				+ "\t- en otherwise.\n");		
	}
	
	

	/**
	 * Main access to the document classification evaluation functionality. Mainly polarity classification. 
	 *
	 * @throws IOException
	 * input output exception if problems with corpora
	 */
	public final void evalDoc(final InputStream inputStream) throws IOException, JDOMException {
		
		String paramFile = parsedArguments.getString("params");
		String corpusFormat = parsedArguments.getString("corpusFormat");
		String model = parsedArguments.getString("model");
		String lang = parsedArguments.getString("language");	
		String classnum = parsedArguments.getString("classnum");
		boolean ruleBased = parsedArguments.getBoolean("ruleBasedClassifier");
		boolean printPreds = parsedArguments.getBoolean("outputPredictions");
		
		//Read corpus sentences
		CorpusReader reader = new CorpusReader(inputStream, corpusFormat, lang);
		
		//parameter file
		Properties params = loadParameters(paramFile, lang);

		String posModelPath = params.getProperty("pos-model", "default");
		String lemmaModelPath = params.getProperty("lemma-model","default");
		String kafDir = setPoStaggingFolder(params, "eval");
		
		//polarity lexicon
		String lexiconDom = params.getProperty("polarLexiconDomain","none");
		String lexiconGen = params.getProperty("polarLexiconGeneral","default");
		if (lexiconGen.equalsIgnoreCase("default"))
		{
			InputStream lexRsrc =  this.getClass().getClassLoader().getResourceAsStream(lang+File.separator+defaultLexicons.getProperty(lang));
			lexiconGen = FileUtilsElh.getElixaResource(lexRsrc,"elixa-lexicon");
			params.setProperty("polarLexiconGeneral",lexiconGen);
			System.err.println(params.getProperty("polarityLexiconGeneral", "WARN: No general polarity lexicon"));
		}
		
		//Rule-based Classifier.
		if (ruleBased) 
		{		
			
			/* polarity lexicon. Domain specific polarity lexicon is given priority.
			 * If no domain lexicon is found it reverts to general polarity lexicon.
			 * If no general polarity lexicon is found program exits with error message.
			*/
			String lex = lexiconDom;
			if (lex.equalsIgnoreCase("none"))
			{
				lex = lexiconGen;
				if (lex.equalsIgnoreCase("none"))
				{
					System.err.println("Elixa Error :: Rule-based classifier is selected but no polarity"
							+ " lexicon has been specified. No default lexicon could be loaded neither."
							+ " Either specify one or choose ML classifier");
					System.exit(1);					
				}
			}			
			File lexFile = new File(lex);			
			Evaluator evalDoc = new Evaluator(lexFile, "lemma");
			
			for (String oId : reader.getOpinions().keySet())
			{
				// sentence posTagging
				String taggedKaf = reader.tagSentenceTab(reader.getOpinion(oId).getsId(), kafDir, posModelPath, lemmaModelPath);
				//process the postagged sentence with the word count based polarity tagger
				Map<String, String> results = evalDoc.polarityScoreTab(taggedKaf, lexFile.getName());				 
				String lblStr = results.get("polarity");
				String actual = "?";
				if (reader.getOpinion(oId).getPolarity() != null)
				{
					actual = reader.getOpinion(oId).getPolarity();
				}
				String rId = reader.getOpinion(oId).getsId().replaceFirst("_g$", "");
				System.out.println(rId+"\t"+actual+"\t"+lblStr+"\t"+reader.getOpinionSentence(oId));
				reader.getOpinion(oId).setPolarity(lblStr);
			}
		}
		//ML Classifier (default)
		else
		{		
			model = WekaWrapper.getModelResource(model, lang, "twt");
			Features atpTest = new Features (reader, params, classnum, model);
			Instances testdata;
			if (corpusFormat.startsWith("tab") && !corpusFormat.equalsIgnoreCase("tabNotagged"))
			{	
				testdata = atpTest.loadInstancesConll(true, "atp",false);
			}
			else if (lang.equalsIgnoreCase("eu") && posModelPath.contains("eustagger"))
			{
				testdata = atpTest.loadInstancesConll(true, "atp",true);
			}
			
			else
			{	
				testdata = atpTest.loadInstances(true, "atp");
			}
			//	setting class attribute (entCat|attCat|entAttCat|polarityCat)
			testdata.setClass(testdata.attribute("polarityCat"));
		
			try {
				WekaWrapper classify = new WekaWrapper(model,lang);	

				System.err.println("evalDoc : going to test the model");
				//sort according to the instanceId
				//traindata.sort(atpTrain.getAttIndexes().get("instanceId"));
				//Instances testdata = new Instances(traindata);
				//testdata.deleteAttributeAt(0);
				//classify.setTestdata(testdata);
				classify.setTestdata(testdata);
				classify.testModel(model);

				if (printPreds)
				{
					for (String oId : reader.getOpinions().keySet())
					{
						int iId = atpTest.getOpinInst().get(oId);
						Instance i = testdata.get(iId-1);
						double label = classify.getMLclass().classifyInstance(i);
						String lblStr = i.classAttribute().value((int) label);
						String actual = "?";
						if (reader.getOpinion(oId).getPolarity() != null)
						{
							actual = reader.getOpinion(oId).getPolarity();
						}
						String rId = reader.getOpinion(oId).getsId().replaceFirst("_g$", "");
						String oSent = reader.getOpinionSentence(oId);
						if (corpusFormat.startsWith("tab"))
						{
							StringBuilder sb = new StringBuilder();
							for (String kk : oSent.split("\n"))
							{
								sb.append(kk.split("\\t")[0]);
								sb.append(" ");
							}
							oSent=sb.toString(); 
						}
						
						System.out.println(rId+"\t"+actual+"\t"+lblStr+"\t"+oSent+"\t"+reader.getOpinionSentence(oId).replaceAll("\n", " ").replaceAll("\\t",":::"));
						reader.getOpinion(oId).setPolarity(lblStr);
					}
				}
				//reader.print2Semeval2015format(model+"tagATP.xml");
				//reader.print2conll(model+"tagAtp.conll");
			} catch (Exception e) {
				e.printStackTrace();			
			}
		}
	}

	/**
	 * Create the main parameters available for training ATP models.
	 */
	private void loadDocevalParameters() {
		evalDocParser.addArgument("-p", "--params")
		.setDefault("default")
		.help("Load the training parameters file\n");
		evalDocParser.addArgument("-cl","--classifier")
		.required(false)
		.choices("smo", "libsvm","linearsvm")
		.setDefault("smo")
		.help("Choose svm classifier. It defaults to weka smo implementation.\n");
		evalDocParser.addArgument("-m", "--model")
		.setDefault("default")
		.help("The pretrained model we want to test.\n");
		//evalDocParser.addArgument("-t", "--testset")
		//.required(false)
		//.help("The test corpus to evaluate our model.\n");
		evalDocParser.addArgument("-f","--corpusFormat")
		.required(false)
		.choices("semeval2015", "semeval2014", "tab", "tabglobal", "tabNotagged","globalNotagged")
		.setDefault("semeval2015")
		.help("Choose format of the test corpus; it defaults to semeval2015 format.\n");
		evalDocParser.addArgument("-cn","--classnum")
		.required(false)
		.choices("3", "3+", "5+", "5", "binary")
		.setDefault("3+none")
		.help("Choose the number of classes the classifier should work on "
				+ "(binary=p|n ; 3=p|n|neu ; 3+=p|n|neu|none ; 5=p|n|neu|p+|n+ ; 5+=p|n|neu|p+|n+|none )"
				+ " it defaults to 3 (p|n|neu).\n");
		evalDocParser.addArgument("-r","--ruleBasedClassifier")		
		.action(Arguments.storeTrue())
		.setDefault(false)
		.help("Whether rule based classifier should be used instead of the default ML classifier."
				+ " A polarity lexicon is mandatory if the rule based classifier is used.\n");
		evalDocParser.addArgument("-o","--outputPredictions")		
		.action(Arguments.storeTrue())
		.setDefault(false)
		.help("Output predictions or not; output is the corpus annotated with semeval2015 format.\n");
		evalDocParser.addArgument("-l","--language")
		.setDefault("en")
		.choices("de", "en", "es", "eu", "it", "nl", "fr")
		.help("Choose language; if not provided it defaults to: \n"
				+ "\t- If the input format is NAF, the language value in incoming NAF file."
				+ "\t- en otherwise.\n");		
	}
	
	
	/**
	 * Main access to the document tagging functionalities. Target based polarity. 
	 *
	 * @throws IOException
	 * input output exception if problems with corpora
	 * @throws JDOMException 
	 */
	public final void tagDoc(final InputStream inputStream) throws IOException, JDOMException {
		// load training parameters file
		String paramFile = parsedArguments.getString("params");
		String corpusFormat = parsedArguments.getString("corpusFormat");
		String model = parsedArguments.getString("model");
		String lang = parsedArguments.getString("language");	
		String classnum = parsedArguments.getString("classnum");
		boolean ruleBased = parsedArguments.getBoolean("ruleBasedClassifier");
		
		//Read corpus sentences
		CorpusReader reader = new CorpusReader(inputStream, corpusFormat, lang);
		
		Properties params = loadParameters(paramFile, lang);

		String posModelPath = params.getProperty("pos-model", "default");
		String lemmaModelPath = params.getProperty("lemma-model", "default");
		String kafDir = setPoStaggingFolder(params, "tag");
		
		// polarity lexicons	
		String lexiconDom = params.getProperty("polarLexiconDomain","none");
		String lexiconGen = params.getProperty("polarLexiconGeneral","default");		
		if (lexiconGen.equalsIgnoreCase("default"))
		{
			InputStream lexRsrc =  this.getClass().getClassLoader().getResourceAsStream(lang+File.separator+defaultLexicons.getProperty(lang));
			lexiconGen = FileUtilsElh.getElixaResource(lexRsrc,"elixa-lexicon");
			params.setProperty("polarLexiconGeneral",lexiconGen);
			System.err.println(params.getProperty("polarityLexiconGeneral", "WARN: No general polarity lexicon"));
		}
		//Rule-based Classifier.
		if (ruleBased) 
		{		
			
			/* polarity lexicon. Domain specific polarity lexicon is given priority.
			 * If no domain lexicon is found it reverts to general polarity lexicon.
			 * If no general polarity lexicon is found program exits with error message.
			*/			
			String lex = lexiconDom;
			if (lex.equalsIgnoreCase("none"))
			{
				lex = lexiconGen;
				if (lex.equalsIgnoreCase("none"))
				{
					System.err.println("Elixa Error :: Rule-based classifier is selected but no polarity"
							+ " lexicon has been specified. No default lexicon could be loaded neither."
							+ " Either specify one or choose ML classifier");
					System.exit(1);					
				}
			}			
			File lexFile = new File(lex);			
			Evaluator evalDoc = new Evaluator(lexFile, "lemma");
			
			for (String oId : reader.getOpinions().keySet())
			{
				// sentence posTagging
				String taggedKaf = reader.tagSentenceTab(reader.getOpinion(oId).getsId(), kafDir, posModelPath, lemmaModelPath);
				//process the postagged sentence with the word count based polarity tagger
				Map<String, String> results = evalDoc.polarityScoreTab(taggedKaf, lexFile.getName());				 
				String lblStr = results.get("polarity");
				String actual = "?";
				if (reader.getOpinion(oId).getPolarity() != null)
				{
					actual = reader.getOpinion(oId).getPolarity();
				}
				String rId = reader.getOpinion(oId).getsId().replaceFirst("_g$", "");
				System.out.println(rId+"\t"+actual+"\t"+lblStr+"\t"+reader.getOpinionSentence(oId));
				reader.getOpinion(oId).setPolarity(lblStr);
			}
		}
		else
		{	
			model = WekaWrapper.getModelResource(model, lang, "twt");
			Features atpTrain = new Features (reader, params, classnum, model);
			Instances traindata;
			if (corpusFormat.startsWith("tab") && !corpusFormat.equalsIgnoreCase("tabNotagged"))
			{
				traindata = atpTrain.loadInstancesConll(true, "atp",false);
			}	
			else if (lang.equalsIgnoreCase("eu") && posModelPath.contains("eustagger"))
			{
				traindata = atpTrain.loadInstancesConll(true, "atp",true);
			}
			else
			{
				traindata = atpTrain.loadInstances(true, "atp");
			}
					
			//	setting class attribute (entCat|attCat|entAttCat|polarityCat)
			traindata.setClass(traindata.attribute("polarityCat"));
	
			try {
				WekaWrapper classify = new WekaWrapper(model,lang);	

				System.err.println();
				//sort according to the instanceId
				//traindata.sort(atpTrain.getAttIndexes().get("instanceId"));
				//Instances testdata = new Instances(traindata);
				//testdata.deleteAttributeAt(0);
				//classify.setTestdata(testdata);
				classify.setTestdata(traindata);
				classify.loadModel(model);

				for (String oId : reader.getOpinions().keySet())
				{
					int iId = atpTrain.getOpinInst().get(oId);
					Instance i = traindata.get(iId-1);
					double label = classify.getMLclass().classifyInstance(i);
					String lblStr = i.classAttribute().value((int) label);
					String actual = "?";
					if (reader.getOpinion(oId).getPolarity() != null)
					{
						actual = reader.getOpinion(oId).getPolarity();
					}
					String rId = reader.getOpinion(oId).getsId().replaceFirst("_g$", "");
					String oSent = reader.getOpinionSentence(oId);
					if (corpusFormat.startsWith("tab"))
					{
						StringBuilder sb = new StringBuilder();
						for (String kk : oSent.split("\n"))
						{
							sb.append(kk.split("\\t")[0]);
							sb.append(" ");
						}
						oSent=sb.toString(); 
					}
					
					System.out.println(rId+"\t"+actual+"\t"+lblStr+"\t"+oSent+"\t"+reader.getOpinionSentence(oId).replaceAll("\n", " ").replaceAll("\\t",":::"));
					reader.getOpinion(oId).setPolarity(lblStr);
				}

				//reader.print2Semeval2015format(model+"tagATP.xml");
				//reader.print2conll(model+"tagAtp.conll");
			} catch (Exception e) {
				e.printStackTrace();			
			}
		}
	}
	
	
	/**
	 * Create the main parameters available for training ATP models.
	 */
	private void loadDocTagParameters() {
		tagDocParser.addArgument("-p", "--params")
		.setDefault("default")
		.help("Load the training parameters file\n");
		tagDocParser.addArgument("-f","--corpusFormat")
		.required(false)
		.choices("semeval2015", "semeval2014", "tab", "tabglobal", "tabNotagged", "ireom", "globalNotagged")
		.setDefault("semeval2015")
		.help("Choose format of reference corpus; it defaults to semeval2015 format.\n"
				+ "");
		tagDocParser.addArgument("-m","--model")		
		.setDefault("default")
		.help("Pre trained model to classify corpus opinions with. Features are extracted from the model\n");
		tagDocParser.addArgument("-r","--ruleBasedClassifier")		
		.action(Arguments.storeTrue())
		.setDefault(false)
		.help("Whether rule based classifier should be used instead of the default ML classifier."
				+ " A polarity lexicon is mandatory if the rule based classifier is used.\n");
		tagDocParser.addArgument("-cn","--classnum")
		.required(false)
		.choices("3", "3+", "5+", "5", "binary")
		.setDefault("3+none")
		.help("Choose the number of classes the classifier should work on "
				+ "(binary=p|n ; 3=p|n|neu ; 3+=p|n|neu|none ; 5=p|n|neu|p+|n+ ; 5+=p|n|neu|p+|n+|none )"
				+ " it defaults to 3 (p|n|neu).\n");		
		tagDocParser.addArgument("-l","--language")
		.setDefault("en")
		.choices("de", "en", "es", "eu", "it", "nl", "fr")
		.help("Choose language; if not provided it defaults to: \n"
				+ "\t- If the input format is NAF, the language value in incoming NAF file."
				+ "\t- en otherwise.\n");
	}
	
	
	/**
	 * Format ixa-pipes based ATE results to Semeval 2015 format.  
	 * 
	 * @param inputStream
	 * @throws IOException
	 */
	public final void slot2(final InputStream inputStream) throws IOException {
		// load training parameters file
		//String paramFile = parsedArguments.getString("params");
		String corpusFormat = parsedArguments.getString("corpusFormat");
		String naf = parsedArguments.getString("naf");
		String lang = parsedArguments.getString("lang");
		
		CorpusReader reader = new CorpusReader(inputStream, corpusFormat, lang);
		if(! FileUtilsElh.checkFile(naf))			
		{
			System.err.println("Error when trying to read from directory containing de annotations.");
			System.exit(2);
		}
		
		try {
			reader.slot2opinionsFromAnnotations(naf);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	
	
	/**
	 * Main access to the train-atc functionalities.
	 * Train ATC using a single classifier (one vs. all) for E#A aspect categories.
	 * 
	 * @throws Exception 
	 */
	public final void trainATC(final InputStream inputStream) throws IOException {
		// load training parameters file
		String paramFile = parsedArguments.getString("params");
		String corpusFormat = parsedArguments.getString("corpusFormat");
		//String validation = parsedArguments.getString("validation");
		int foldNum = Integer.parseInt(parsedArguments.getString("foldNum"));
		String lang = parsedArguments.getString("language");
		String classifier = parsedArguments.getString("classifier");
		String cparam = parsedArguments.getString("cparameter");
		
		
		//boolean printPreds = parsedArguments.getBoolean("printPreds");
		boolean nullSentenceOpinions = parsedArguments.getBoolean("nullSentences");
		//double threshold = 0.2;
		//String modelsPath = "/home/inaki/Proiektuak/BOM/SEMEVAL2015/ovsaModels";
		
		Properties params = loadParameters(paramFile, lang);
		
		CorpusReader reader = new CorpusReader(inputStream, corpusFormat, nullSentenceOpinions, lang);
		Features atcTrain = new Features (reader, params,"3");	
		Instances traindata = atcTrain.loadInstances(true, "atc");
		
		//setting class attribute (entCat|attCat|entAttCat|polarityCat)
		
		//HashMap<String, Integer> opInst = atcTrain.getOpinInst();
		WekaWrapper classifyEnts;
		WekaWrapper classifyAtts;
		//WekaWrapper onevsall;
		try {
			//train first classifier (entities)
			Instances traindataEnt = new Instances(traindata);
			// IMPORTANT: filter indexes are added 1 because weka remove function counts attributes from 1, 
			traindataEnt.setClassIndex(traindataEnt.attribute("entCat").index());
			classifyEnts = new WekaWrapper(traindataEnt, true,classifier,cparam);
			String filtRange = String.valueOf(traindata.attribute("attCat").index()+1)+","
					+ String.valueOf(traindata.attribute("entAttCat").index()+1);			
			classifyEnts.filterAttribute(filtRange);
				
			System.out.println("trainATC: entity classifier results -> ");
			classifyEnts.crossValidate(foldNum);
			classifyEnts.saveModel("elixa-atc_ent-"+lang+".model");
			
			//Classifier entityCl = classify.getMLclass();
			
			//train second classifier (attributes)
			Instances traindataAtt = new Instances(traindata);
			traindataAtt.setClassIndex(traindataAtt.attribute("attCat").index());
			classifyAtts = new WekaWrapper(traindataAtt, true,classifier,cparam);
			filtRange = String.valueOf(traindataAtt.attribute("entAttCat").index()+1);			
			classifyAtts.filterAttribute(filtRange);		
			
			System.out.println("trainATC: attribute classifier results -> ");
			classifyAtts.crossValidate(foldNum);
			classifyAtts.saveModel("elixa-atc_att-"+lang+".model");
			/*
			Instances traindataEntadded = classifyEnts.addClassification(classifyEnts.getMLclass(), traindataEnt);
			//train second classifier (entCat attributes will have the values of the entities always)
			traindataEntadded.setClassIndex(traindataEntadded.attribute("attCat").index());
			WekaWrapper classify2 = new WekaWrapper(traindataEntadded, true);
			System.out.println("trainATC: enhanced attribute classifier results -> ");
			classify2.saveModel("elixa-atc_att_enhanced.model");
			classify2.crossValidate(foldNum);		
			*/
			//classify.printMultilabelPredictions(classify.multiLabelPrediction());		*/	
						
			//reader.print2Semeval2015format(paramFile+"entAttCat.xml");
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		//traindata.setClass(traindata.attribute("entAttCat"));
		System.err.println("DONE CLI train-atc");				
	}
		
	
	
	/**
	 * Main access to the train-atc functionalities. Train ATC using a double one vs. all classifier
	 * (E and A) for E#A aspect categories
	 * @throws Exception 
	 */
	public final void trainATC2(final InputStream inputStream) throws IOException {
		// load training parameters file
		String paramFile = parsedArguments.getString("params");
		String testFile = parsedArguments.getString("testset");
		String paramFile2 = parsedArguments.getString("params2");
		String corpusFormat = parsedArguments.getString("corpusFormat");
		//String validation = parsedArguments.getString("validation");
		String lang = parsedArguments.getString("language");
		String classifier = parsedArguments.getString("classifier");
		String cparam = parsedArguments.getString("cparameter");
		//int foldNum = Integer.parseInt(parsedArguments.getString("foldNum"));
		//boolean printPreds = parsedArguments.getBoolean("printPreds");
		boolean nullSentenceOpinions = parsedArguments.getBoolean("nullSentences");
		boolean onlyTest = parsedArguments.getBoolean("testOnly");
		double threshold = 0.5;
		double threshold2 = 0.5;
		String modelsPath = "/home/inaki/elixa-atp/ovsaModels";
		
		Properties params = loadParameters(paramFile, lang);

		CorpusReader reader = new CorpusReader(inputStream, corpusFormat, nullSentenceOpinions, lang);
		Features atcTrain = new Features (reader, params,"3");		
		Instances traindata = atcTrain.loadInstances(true, "atc");
		
		if (onlyTest)
		{
			if (FileUtilsElh.checkFile(testFile))
			{
				System.err.println("read from test file");
				reader = new CorpusReader(new FileInputStream(new File(testFile)), corpusFormat, nullSentenceOpinions, lang);
				atcTrain.setCorpus(reader);
				traindata = atcTrain.loadInstances(true, "atc");
			}
		}
		
		
		//setting class attribute (entCat|attCat|entAttCat|polarityCat)
		
		//HashMap<String, Integer> opInst = atcTrain.getOpinInst();		
		//WekaWrapper classifyAtts;
		WekaWrapper onevsall;
		try {
			
			//classify.printMultilabelPredictions(classify.multiLabelPrediction());		*/	
			
			//onevsall
			Instances entdata = new Instances(traindata);
			entdata.deleteAttributeAt(entdata.attribute("attCat").index());
			entdata.deleteAttributeAt(entdata.attribute("entAttCat").index());
			entdata.setClassIndex(entdata.attribute("entCat").index());
			onevsall = new WekaWrapper(entdata,true,classifier,cparam);								
			
			
			if (! onlyTest)
			{
				onevsall.trainOneVsAll(modelsPath, paramFile+"entCat");			
				System.out.println("trainATC: one vs all models ready");
			}
			onevsall.setTestdata(entdata);
			HashMap<Integer, HashMap<String, Double>> ovsaRes = onevsall.predictOneVsAll(modelsPath, paramFile+"entCat");
			System.out.println("trainATC: one vs all predictions ready");
			HashMap<Integer, String> instOps = new HashMap<Integer,String>();
			for (String oId : atcTrain.getOpinInst().keySet())
			{
				instOps.put(atcTrain.getOpinInst().get(oId), oId);
			}
			
			Properties params2 = loadParameters(paramFile2, lang);

			atcTrain = new Features (reader, params2,"3");
			entdata = atcTrain.loadInstances(true, "attTrain2_data");
			entdata.deleteAttributeAt(entdata.attribute("entAttCat").index());
			//entdata.setClassIndex(entdata.attribute("entCat").index());

			Attribute insAtt = entdata.attribute("instanceId");
			double maxInstId = entdata.kthSmallestValue(insAtt, entdata.numDistinctValues(insAtt)-1);
			System.err.println("last instance has index: "+maxInstId);
			for (int ins=0; ins<entdata.numInstances(); ins++)
			{
			    System.err.println("ins"+ins);
				int i = (int)entdata.instance(ins).value(insAtt);
				Instance currentInst = entdata.instance(ins);
				//System.err.println("instance "+i+" oid "+kk.get(i+1)+"kk contains key i?"+kk.containsKey(i));
				String sId = reader.getOpinion(instOps.get(i)).getsId();
				String oId = instOps.get(i);
				reader.removeSentenceOpinions(sId);
				int oSubId =0;				
				for (String cl : ovsaRes.get(i).keySet())
				{				
					//System.err.println("instance: "+i+" class "+cl+" value: "+ovsaRes.get(i).get(cl));
					if  (ovsaRes.get(i).get(cl) > threshold)
					{
						//System.err.println("one got through ! instance "+i+" class "+cl+" value: "+ovsaRes.get(i).get(cl));						
						// for the first one update the instances
						if (oSubId >= 1)
						{
							Instance newIns = new SparseInstance(currentInst);
							newIns.setDataset(entdata);
							entdata.add(newIns);
							newIns.setValue(insAtt, maxInstId+oSubId);
							newIns.setClassValue(cl);
							instOps.put((int)maxInstId+oSubId, oId);
							
						}						
						// if the are more create new instances
						else
						{
							currentInst.setClassValue(cl);
							//create and add opinion to the structure
							//	trgt, offsetFrom, offsetTo, polarity, cat, sId);
							//Opinion op = new Opinion(instOps.get(i)+"_"+oSubId, "", 0, 0, "", cl, sId);
							//reader.addOpinion(op);
						}
						oSubId++;
					}					
				} //finished updating instances data												
			}
			
			entdata.setClass(entdata.attribute("attCat"));
			onevsall = new WekaWrapper(entdata, true,classifier,cparam);
			
			/**
			 *  Bigarren sailkatzailea
			 * 
			 * */
			if (! onlyTest)
			{				
				onevsall.trainOneVsAll(modelsPath, paramFile+"attCat");			
				System.out.println("trainATC: one vs all attcat models ready");
			}
			
			ovsaRes = onevsall.predictOneVsAll(modelsPath, paramFile+"entAttCat");
			
			
			insAtt = entdata.attribute("instanceId");
			maxInstId = entdata.kthSmallestValue(insAtt, insAtt.numValues());
			System.err.println("last instance has index: "+maxInstId);
			for (int ins=0; ins<entdata.numInstances(); ins++)
			{
				System.err.println("ins: "+ins);
				int i = (int)entdata.instance(ins).value(insAtt);
				Instance currentInst = entdata.instance(ins);
				//System.err.println("instance "+i+" oid "+kk.get(i+1)+"kk contains key i?"+kk.containsKey(i));
				String sId = reader.getOpinion(instOps.get(i)).getsId();
				String oId = instOps.get(i);
				reader.removeSentenceOpinions(sId);
				int oSubId =0;
				for (String cl : ovsaRes.get(i).keySet())
				{				
					//System.err.println("instance: "+i+" class "+cl+" value: "+ovsaRes.get(i).get(cl));
					if  (ovsaRes.get(i).get(cl) > threshold2)
					{
						///System.err.println("instance: "+i+" class "+cl+" value: "+ovsaRes.get(i).get(cl));
						if  (ovsaRes.get(i).get(cl) > threshold)
						{
							//System.err.println("one got through ! instance "+i+" class "+cl+" value: "+ovsaRes.get(i).get(cl));						
							// for the first one update the instances
							if (oSubId >= 1)
							{
								String label = currentInst.stringValue(entdata.attribute("entAtt"))+"#"+cl;							
								//create and add opinion to the structure
								//	trgt, offsetFrom, offsetTo, polarity, cat, sId);							
								Opinion op = new Opinion(oId+"_"+oSubId, "", 0, 0, "", label, sId);
								reader.addOpinion(op);							
							}						
							// if the are more create new instances
							else
							{
								String label = currentInst.stringValue(entdata.attribute("entAtt"))+"#"+cl;							
								//create and add opinion to the structure
								//	trgt, offsetFrom, offsetTo, polarity, cat, sId);
								reader.removeOpinion(oId);
								Opinion op = new Opinion(oId+"_"+oSubId, "", 0, 0, "", label, sId);
								reader.addOpinion(op);
							}
							oSubId++;
						}					
					} //finished updating instances data												
				}
			}			
			reader.print2Semeval2015format(paramFile+"entAttCat.xml");
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		//traindata.setClass(traindata.attribute("entAttCat"));
		System.err.println("DONE CLI train-atc2 (oneVsAll)");				
	}		
	
	
	
	/**
	 * train ATC using a single classifier (one vs. all) for E#A aspect categories.
	 * 
	 * @param inputStream
	 * @throws IOException
	 */
	public final void trainATCsingleCategory(final InputStream inputStream) throws IOException {
		// load training parameters file
		String paramFile = parsedArguments.getString("params");
		String testFile = parsedArguments.getString("testset");
		String corpusFormat = parsedArguments.getString("corpusFormat");
		//String validation = parsedArguments.getString("validation");
		String lang = parsedArguments.getString("language");
		String classifier = parsedArguments.getString("classifier");
		String cparam = parsedArguments.getString("cparameter");
		//int foldNum = Integer.parseInt(parsedArguments.getString("foldNum"));
		//boolean printPreds = parsedArguments.getBoolean("printPreds");
		boolean nullSentenceOpinions = parsedArguments.getBoolean("nullSentences");
		boolean onlyTest = parsedArguments.getBoolean("testOnly");
		double threshold = 0.5;
		
		String modelsPath = "/home/inaki/Proiektuak/BOM/SEMEVAL2015/ovsaModels";
		
		Properties params = loadParameters(paramFile, lang);

		
		CorpusReader reader = new CorpusReader(inputStream, corpusFormat, nullSentenceOpinions, lang);
		Features atcTrain = new Features (reader, params,"3");
		Instances traindata = atcTrain.loadInstances(true, "atc");
		
		if (onlyTest)
		{
			if (FileUtilsElh.checkFile(testFile))
			{
				System.err.println("read from test file");
				reader = new CorpusReader(new FileInputStream(new File(testFile)), corpusFormat, nullSentenceOpinions, lang);
				atcTrain.setCorpus(reader);
				traindata = atcTrain.loadInstances(true, "atc");
			}
		}
		
			
		//setting class attribute (entCat|attCat|entAttCat|polarityCat)
		
		//HashMap<String, Integer> opInst = atcTrain.getOpinInst();
		//WekaWrapper classifyEnts;
		//WekaWrapper classifyAtts;
		WekaWrapper onevsall;
		try {
			
			//classify.printMultilabelPredictions(classify.multiLabelPrediction());		*/	
			
			//onevsall
			//Instances entdata = new Instances(traindata);
			traindata.deleteAttributeAt(traindata.attribute("attCat").index());
			traindata.deleteAttributeAt(traindata.attribute("entCat").index());
			traindata.setClassIndex(traindata.attribute("entAttCat").index());
			onevsall = new WekaWrapper(traindata,true,classifier,cparam);
			
			if (! onlyTest)
			{
				onevsall.trainOneVsAll(modelsPath, paramFile+"entAttCat");			
				System.out.println("trainATC: one vs all models ready");
			}
			onevsall.setTestdata(traindata);
			HashMap<Integer, HashMap<String, Double>> ovsaRes = onevsall.predictOneVsAll(modelsPath, paramFile+"entAttCat");
			System.out.println("trainATC: one vs all predictions ready");
			HashMap<Integer, String> kk = new HashMap<Integer,String>();
			for (String oId : atcTrain.getOpinInst().keySet())
			{
				kk.put(atcTrain.getOpinInst().get(oId), oId);
			}
			
			Object[] ll = ovsaRes.get(1).keySet().toArray();
			for (Object l : ll)
			{
				System.err.print((String)l+" - ");
			}
			System.err.print("\n");
			
			for (int i : ovsaRes.keySet())
			{
				//System.err.println("instance "+i+" oid "+kk.get(i+1)+"kk contains key i?"+kk.containsKey(i));
				String sId = reader.getOpinion(kk.get(i)).getsId();
				reader.removeSentenceOpinions(sId);
				int oSubId =0;				
				for (String cl : ovsaRes.get(i).keySet())
				{				
					//System.err.println("instance: "+i+" class "+cl+" value: "+ovsaRes.get(i).get(cl));
					if  (ovsaRes.get(i).get(cl) > threshold)
					{
						//System.err.println("one got through ! instance "+i+" class "+cl+" value: "+ovsaRes.get(i).get(cl));
						oSubId++;
						//create and add opinion to the structure
						//trgt, offsetFrom, offsetTo, polarity, cat, sId);
						Opinion op = new Opinion(kk.get(i)+"_"+oSubId, "", 0, 0, "", cl, sId);
						reader.addOpinion(op);
					}					
				}
			}			
			reader.print2Semeval2015format(paramFile+"entAttCat.xml");
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		//traindata.setClass(traindata.attribute("entAttCat"));
		System.err.println("DONE CLI train-atc2 (oneVsAll)");				
	}
	
	
	
	/**
	 * Main access to the train functionalities.
	 * @throws Exception 
	 */
	public final void tagATC(final InputStream inputStream) throws IOException {
		// load training parameters file
		String paramFile = parsedArguments.getString("params");
		String corpusFormat = parsedArguments.getString("corpusFormat");
		//String validation = parsedArguments.getString("validation");
		String lang = parsedArguments.getString("language");
		int foldNum = Integer.parseInt(parsedArguments.getString("foldNum"));
		String classifier = parsedArguments.getString("classifier");
		String cparam = parsedArguments.getString("cparameter");
		//boolean printPreds = parsedArguments.getBoolean("printPreds");
		
		Properties params = loadParameters(paramFile, lang);

		CorpusReader reader = new CorpusReader(inputStream, corpusFormat, lang);
		Features atcTrain = new Features (reader, params,"3");	
		Instances traindata = atcTrain.loadInstances(true, "atc");
		
		//setting class attribute (entCat|attCat|entAttCat|polarityCat)
		
		//HashMap<String, Integer> opInst = atcTrain.getOpinInst();
		WekaWrapper classify;
		try {
			//train first classifier (entities)
			traindata.setClass(traindata.attribute("entCat"));				
			classify = new WekaWrapper(traindata, true,classifier,cparam);
			classify.crossValidate(foldNum);
			//Classifier entityCl = classify.getMLclass().;
			
			//train second classifier (attributtes)
			traindata.setClass(traindata.attribute("attCat"));				
			classify.setTraindata(traindata);
			classify.crossValidate(foldNum);			
			//Classifier attCl = classify.getMLclass();
			
			classify.printMultilabelPredictions(classify.multiLabelPrediction());			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		traindata.setClass(traindata.attribute("entAttCat"));
		System.err.println("DONE CLI train-atc");				
	}
	
	
	
	/**
	 * Create the available parameters for ATP tagging.
	 */
	private void loadAnnotateParameters() {
		annotateParser.addArgument("-m", "--model")
		.required(true)
		.help("Pass the model to do the tagging as a parameter.\n");
		annotateParser.addArgument("-l","--language")
		.required(false)
		.choices("de", "en", "es", "eu", "it", "nl","fr")
		.help("Choose language; it defaults to the language value in incoming NAF file.\n");
		annotateParser.addArgument("-o","--outputFormat")
		.required(false)
		.choices("semeval2015", "naf")
		.setDefault("semeval2015")
		.help("Choose output format; it defaults to semeval2015.\n");
		annotateParser.addArgument("--dictTag")
		.required(false)
		.choices("tag", "post")
		.setDefault("post")
		.help("Choose to directly tag entities by dictionary look-up; if the 'tag' option is chosen, " +
				"only tags entities found in the dictionary; if 'post' option is chosen, it will " +
				"post-process the results of the statistical model.\n");
		annotateParser.addArgument("--dictPath")
		.required(false)
		.setDefault("")
		.help("Provide the path to the dictionaries for direct dictionary tagging; it ONLY WORKS if --dictTag " +
				"option is activated.\n");
	}
	

	
	/**
	 * Create the main parameters available for tagging slot2 semeval2015.
	 */
	private void loadslot2Parameters() {
		slot2Parser.addArgument("-p", "--params").required(false)
		.help("Load the training parameters file\n");
		slot2Parser.addArgument("-f","--corpusFormat")
		.required(false)
		.choices("semeval2015", "semeval2014", "tab")
		.setDefault("semeval2015")
		.help("Choose format of reference corpus; it defaults to semeval2015 format.\n");
		slot2Parser.addArgument("-n","--naf")
		.required(true)
		.help("tagged naf file path.\n");
		slot2Parser.addArgument("-l","--language")
		.setDefault("en")
		.choices("de", "en", "es", "eu", "it", "nl","fr")
		.help("Choose language; if not provided it defaults to: \n"
				+ "\t- If the input format is NAF, the language value in incoming NAF file."
				+ "\t- en otherwise.\n");
	}
	
	/**
	 * Create the main parameters available for training ATP models.
	 */
	private void loadATCTrainingParameters() {
		trainATCParser.addArgument("-p", "--params").required(true)
		.help("Load the training parameters file\n");
		trainATCParser.addArgument("-cl","--classifier")
		.required(false)
		.choices("smo", "libsvm","linearsvm")
		.setDefault("smo")
		.help("Choose svm classifier. It defaults to weka smo implementation.\n");
		trainATCParser.addArgument("-cp","--cparameter")
		.required(false)
		.setDefault("1")
		.help("Choose svm classifier parameter c. It defaults to 1.\n");
		trainATCParser.addArgument("-t", "--testset")
		.required(false)
		.help("The test or reference corpus.\n");
		trainATCParser.addArgument("-cvf", "--foldNum")
		.required(false)
		.setDefault(10)
		.help("Number of folds to run the cross validation on.\n");
		trainATCParser.addArgument("-v","--validation")
		.required(false)
		.choices("cross", "trainTest", "both")
		.setDefault("both")
		.help("Choose the way the trained model will be validated\n"
				+ "\t - cross : 10 fold cross validation.\n"
				+ "\t - trainTest : 90% train / 10% test division.\n"
				+ "\t - both (default): both cross validation and train/test division will be tested.");
		trainATCParser.addArgument("-f","--corpusFormat")
		.required(false)
		.choices("semeval2015", "semeval2014", "tab")
		.setDefault("semeval2015")
		.help("Choose format of reference corpus; it defaults to semeval2015 format.\n");
		trainATCParser.addArgument("-n","--nullSentences")		
		.action(Arguments.storeTrue())
		.required(false)
		.setDefault(false)
		.help("Whether null examples should be generated from sentences without categories or not.\n");
		trainATCParser.addArgument("-o","--outputpredictions")		
		.action(Arguments.storeTrue())
		.setDefault(false)
		.help("Output predictions or not; output is the corpus annotated with semeval2015 format.\n");
		trainATCParser.addArgument("-l","--language")
		.setDefault("en")
		.choices("de", "en", "es", "eu", "it", "nl","fr")
		.help("Choose language; if not provided it defaults to: \n"
				+ "\t- If the input format is NAF, the language value in incoming NAF file."
				+ "\t- en otherwise.\n");
	}
	
	
	/**
	 * Create the main parameters available for training ATP models.
	 */
	private void loadATC2TrainingParameters() {
		trainATC2Parser.addArgument("-p", "--params").required(true)
		.help("Load the training parameters file\n");
		trainATC2Parser.addArgument("-p2", "--params2").required(false)
		.help("Load the training parameters file\n");
		trainATC2Parser.addArgument("-cl","--classifier")
		.required(false)
		.choices("smo", "libsvm","linearsvm")
		.setDefault("smo")
		.help("Choose svm classifier. It defaults to weka smo implementation.\n");
		trainATC2Parser.addArgument("-cp","--cparameter")
		.required(false)
		.setDefault("1")
		.help("Choose svm classifier parameter c. It defaults to 1.\n");
		trainATC2Parser.addArgument("-t", "--testset")
		.required(false)
		.help("The test or reference corpus.\n");
		trainATC2Parser.addArgument("-cvf", "--foldNum")
		.required(false)
		.setDefault(10)
		.help("Number of folds to run the cross validation on.\n");
		trainATC2Parser.addArgument("-v","--validation")
		.required(false)
		.choices("cross", "trainTest", "both")
		.setDefault("both")
		.help("Choose the way the trained model will be validated\n"
				+ "\t - cross : 10 fold cross validation.\n"
				+ "\t - trainTest : 90% train / 10% test division.\n"
				+ "\t - both (default): both cross validation and train/test division will be tested.");
		trainATC2Parser.addArgument("-f","--corpusFormat")
		.required(false)
		.choices("semeval2015", "semeval2014", "tab")
		.setDefault("semeval2015")
		.help("Choose format of reference corpus; it defaults to semeval2015 format.\n");
		trainATC2Parser.addArgument("-n","--nullSentences")		
		.action(Arguments.storeTrue())
		.required(false)
		.setDefault(false)
		.help("Whether null examples should be generated from sentences without categories or not.\n");
		trainATC2Parser.addArgument("-to","--testOnly")		
		.action(Arguments.storeTrue())
		.required(false)
		.setDefault(false)
		.help("Whether only test should be done (assumes models were previously generated).\n");
		trainATC2Parser.addArgument("-o","--outputpredictions")		
		.action(Arguments.storeTrue())
		.setDefault(false)
		.help("Output predictions or not; output is the corpus annotated with semeval2015 format.\n");
		trainATC2Parser.addArgument("-l","--language")
		.setDefault("en")
		.choices("de", "en", "es", "eu", "it", "nl", "fr")
		.help("Choose language; if not provided it defaults to: \n"
				+ "\t- If the input format is NAF, the language value in incoming NAF file."
				+ "\t- en otherwise.\n");
	}
	
	
	
	
/*
 *  FROM NOW ON HELPING FUNCTIONS 
 * */	
	
	/**
	 *  Function load the parameter file into a Properties object
	 * 
	 * @param String paramFile : Path to the file containing the feature configuration file 
	 *                            (which features should be used)
	 */
	private Properties loadParameters (String paramFile,String lang)
	{
		Properties params = new Properties();
		if (paramFile.equalsIgnoreCase("default"))
		{
			Properties defaultParams = new Properties();
			try {
			    //System.err.println(modelDir+File.separator+"morph-models-1.5.0.txt"); 
				defaultParams.load(this.getClass().getClassLoader().getResourceAsStream("elixa-models"+File.separator+"elixa-models.txt"));
			} catch (Exception e) {
				System.err.println("ERROR: No parameter file was especified, and no default polarity models found. "
						+ "\nPlease especify a parameter file (-p option). EliXa will only be able to tag polarity with user especified models");
				//e.printStackTrace();
				System.exit(1);
				return params;
			}
			paramFile="elixa-models"+File.separator+defaultParams.getProperty(lang+"-twt", "")+".cfg";
			
			try {
				params.load(this.getClass().getClassLoader().getResourceAsStream(paramFile));
			} catch (IOException e) {
				System.err.println("ERROR: Default configuration file for the selected language polarity model could not be loaded."
						+ "\n EliXa will only be able to tag polarity with user especified models");
				System.exit(1);

			}
		}
		else
		{
			File pfile = new File(paramFile);
			if (FileUtilsElh.checkFile(pfile))
			{
				try {
					params.load(new FileInputStream(pfile));				
				}
				catch (IOException ioe){
					System.err.println("Features: given parameter file ("+paramFile+") is not a valid file.");
					System.exit(1);				
				}
			}
		}
		
		return params;
		
	}

	private String setPoStaggingFolder(Properties params, String functionality) throws IOException {
		String kafDir = params.getProperty("kafDir", "none");
		if (kafDir.equalsIgnoreCase("none")){
			final File tempDir = FileUtilsElh.createTempDirectory();
			Runtime.getRuntime().addShutdownHook(new Thread() {
			      @Override
			      public void run() {
			        /* Delete your file here. */
			    	FileUtils.deleteQuietly(tempDir);	
			      }
			});			
			kafDir = tempDir.getAbsolutePath();
			params.setProperty("kafDir", kafDir);
			switch (functionality)
			{
			case "train":
				System.err.println("EliXa CLI: tagged files will be created in a temporal folder and deleted after execution."
					+ "\n This may slow down your trainings if you are training over the same data-set several times."
					+ " You may want to declare a static tagged files folder (kafDir=\"/path/to/tag/files/\") in the config file"
					+ "if you are not changing tagging related features (e.g., normalization, wf vs. lemma ngrams).");				
				break;
			case "eval":
				System.err.println("EliXa CLI: tagged files will be created in a temporal folder and deleted after execution."
						+ "\n This may slow down your tests if you are evaluating over the same test set several times."
						+ " You may want to declare a static tagged files folder (kafDir=\"/path/to/tag/files/\") in the config file"
						+ "if you are not changed tagging related features (e.g., normalization, wf vs. lemma ngrams).");								
				break;
			case "default":
				System.err.println("EliXa CLI: tagged files will be created in a temporal folder and deleted after execution.");								
			}
		}
		return kafDir;
	}

	
}
