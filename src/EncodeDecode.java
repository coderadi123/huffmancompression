import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;

import myfileio.MyFileIO;

/**
 * The Class EncodeDecode. 
 */
public class EncodeDecode {
	
	/** The encodeMap maps each ascii value to its huffman code */
	private String[] encodeMap;
	private final int NUM_ASCII = 128;
	/** Instance of the huffman compression utilites for building the tree and encode man */
	private HuffmanCompressionUtilities huffUtil;
	
	/** Instance of GenWeights used to generate the frequency weights if no weights file is specified */
	private GenWeights gw;
	
	/** Instance of HuffCompAlerts for relaying information to the GUI or console */
	private HuffCompAlerts hca;
	
	/**  Provides facilities to robustly handle external file IO. */
	private MyFileIO fio;
	
	/** The bin util. */
	private BinaryIO binUtil;
	
	/**  The array for storing the frequency weights. */
	private int[] weights;	

	/**
	 * Instantiates a new EncodeDecode instance
	 *
	 * @param gw - instance of GenWeights
	 * @param hca - instance of HuffCompAlerts
	 */
	public EncodeDecode (GenWeights gw, HuffCompAlerts hca) {
		fio = new MyFileIO();
		this.gw = gw;
		this.hca = hca;
		huffUtil = new HuffmanCompressionUtilities();
		binUtil = new BinaryIO();
		int[] weights = new int[NUM_ASCII];
		encodeMap = new String[NUM_ASCII];
		for (int i=0; i<NUM_ASCII; i++) {
			weights[i] =0;
		}
		this.weights = weights;
	}
	
	/**
	 * Encode. This function will do the following actions:
	 *         1) Error check the inputs
	 * 	       - Perform error checking on the file to encode, using MyFileIO fio.
	 *         - Generate the array of frequency weights - either read from a file in the output/ directory
	 *           or regenerate from the file to encode in the data/ directory
	 *         - Error check the output file...
	 *         Any errors will abort the conversion...
	 *         
	 *         2) set the weights in huffUtils
	 *         3) build the Huffman tree using huffUtils;
	 *         4) create the Huffman codes by traversing the trees.
	 *         5) call executeEncode to perform the conversion.
	 *
	 * @param fName 	the name of the input file to be encoded
	 * @param bfName 	the name of the binary (compressed) file to be created
	 * @param freqWts 	the name of the file to read for the frequency weights. If blank, or other error,
	 *                  generate the frequency weights from fName.
	 * @param optimize 	if true, ONLY add leaf nodes with non-zero weights to the priority queue
	 */
	void encode(String fName,String bfName, String freqWts, boolean optimize) {
		File fd = fio.getFileHandle(fName);
		checkInputFileStatus(fd);
		
		File fdw = fio.getFileHandle(freqWts);
		initializeWeights(fdw, fName);
		
		File fdo = fio.getFileHandle(bfName);
		checkOutputFileStatus(fdo);
		
		huffUtil.setWeights(weights);
		huffUtil.buildHuffmanTree(optimize);
		huffUtil.createHuffmanCodes(huffUtil.getTreeRoot(), "", 0);
		executeEncode(fd, fdo);
	}
	
	private void checkInputFileStatus (File fd) {
		int fileOK = fio.getFileStatus(fd, true);
		if(fileOK == MyFileIO.EMPTY_NAME) {
			hca.issueAlert(HuffAlerts.OUTPUT, "WARNING", "Empty File Name");
		}
		else if(fileOK == MyFileIO.FILE_DOES_NOT_EXIST || fileOK == MyFileIO.READ_ZERO_LENGTH) {
			hca.issueAlert(HuffAlerts.INPUT, "WARNING", "Empty File or File Doesn't Exist");
		}
		else if(fileOK == MyFileIO.NO_READ_ACCESS) {
			hca.issueAlert(HuffAlerts.INPUT, "WARNING", "File Isn't Readable");
		}
	}
	
	private void checkOutputFileStatus(File fd0) {
		int fileoOK = fio.getFileStatus(fd0, false);
		if(fileoOK == MyFileIO.EMPTY_NAME) {
			hca.issueAlert(HuffAlerts.OUTPUT, "WARNING", "Empty File Name");
		}else if(fileoOK == MyFileIO.NO_WRITE_ACCESS) {
			hca.issueAlert(HuffAlerts.OUTPUT, "WARNING", "File Isn't Writable");
		}
	}
	
	private void initializeWeights(File fdw, String fName) {
		int filewOK = fio.getFileStatus(fdw, true);
		if(filewOK == MyFileIO.EMPTY_NAME || filewOK == MyFileIO.FILE_DOES_NOT_EXIST || filewOK == MyFileIO.READ_ZERO_LENGTH || filewOK == MyFileIO.NO_READ_ACCESS) {
			weights = gw.readInputFileAndReturnWeights(fName); 
		} else {
			weights = huffUtil.readFreqWeights(fdw);
		}
	}
	/**
	 * Execute encode. This function will write compressed binary file as part of part 3
	 * 
	 * This functions should:
	 * 1) get the encodeMap from HuffUtils 
	 * 2) initialize binStr to ""
	 * 3) open a BufferedReader for the text file and a BufferedOutputStream for the binary file
	 * 4) for each character in the textfile:
	 * 	  - append the huffman code to binStr;
	 *    - if binStr length >= 8, write the binStr to the binary file using binUtils.writeBinString();
	 *      binStr should be set to any returned string value.
	 * 5) when the input file is exhausted, write the EOF character, padding with 0's if needed 
	 * 6) close the the input and output files...
	 *
	 * @param inFile the File object that represents the file to be compressed
	 * @param binFile the File object that represents the compressed output file
	 */
	private void executeEncode(File inFile, File binFile) {
		String binStr = "";
		BufferedReader br = fio.openBufferedReader(inFile);
		BufferedOutputStream bro = fio.openBufferedOutputStream(binFile);
		encodeMap = huffUtil.getEncodeMap();
		
		int c = 0;
		try {
			while((c = br.read()) != -1) {
				c = c&127;
				String huffCode = encodeMap[c];
				binStr += huffCode;
				while (binStr.length() >= BinaryIO.BYTE_LENGTH) {
					binStr = binUtil.writeBinString(bro, binStr);			
				}
			}
			binStr += encodeMap[0];
			while (binStr.length() >= BinaryIO.BYTE_LENGTH) {
				binStr = binUtil.writeBinString(bro, binStr);			
			}
			if (binStr.length() != 0) {
				int padLength = BinaryIO.BYTE_LENGTH-binStr.length() +1;
				for (int i=0; i<padLength; i++) {
					binStr += "0";
				}
			}
			binStr = binUtil.writeBinString(bro, binStr);
			br.close();
			bro.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}
	
	// DO NOT CODE THIS METHOD UNTIL EXPLICITLY INSTRUCTED TO DO SO!!!
	/**
	 * Decode. This function will only be addressed in part 5. It will 
	 *         1) Error check the inputs
	 * 	       - Perform error checking on the file to decode
	 *         - Generate the array of frequency weights - this MUST be provided as a file
	 *         - Error check the output file...
	 *         Any errors will abort the conversion...
	 *         
	 *         2) set the weights in huffUtils
	 *         3) build the Huffman tree using huffUtils;
	 *         4) create the Huffman codes by traversing the trees.
	 *         5) executeDecode
	 *
	 * @param bfName 	the name of the binary file to read
	 * @param ofName 	the name of the text file to write...
	 * @param freqWts the freq wts
	 * @param optimize - exclude 0-weight nodes from the tree
	 */
	void decode(String bfName, String ofName, String freqWts,boolean optimize) {
		File fd = fio.getFileHandle(bfName);
		checkInputFileStatus(fd);

		File fdw = fio.getFileHandle(freqWts);
		checkInputFileStatus(fdw);
		weights = huffUtil.readFreqWeights(fdw);

		File fdo = fio.getFileHandle(ofName);
		checkOutputFileStatus(fdo);

		huffUtil.setWeights(weights);
		huffUtil.buildHuffmanTree(optimize);
		huffUtil.createHuffmanCodes(huffUtil.getTreeRoot(), "", 0);

		try {
			executeDecode(fd, fdo);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		

	}

	// DO NOT CODE THIS METHOD UNTIL EXPLICITLY INSTRUCTED TO DO SO!!!
	/**
	 * Execute decode.  - This is part of PART 5...
	 * This function performs the decode of the binary(compressed) file.
	 * It will read each byte from the binary file and convert it to a string of 1's and 0's
	 * This will be appended to any leftover bits from prior conversions.
	 * Starting from the head of the string, decode occurs by traversing the Huffman Tree from the root
	 * until a Leaf node is reached. If a leaf node is reached, the character is written to the output
	 * file, and the corresponding # of bits is removed from the string. If the end of the bit string is reached
	 * without reaching a leaf node, the next byte is processed, and so on until the encoded EOF
	 * character is encountered. 
	 * After completely decoding the file, close the input file and
	 * flushed and close the output file.
	 *
	 * @param binFile the file object for the binary input file
	 * @param outFile the file object for the binary output file
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	private void executeDecode(File binFile, File outFile) throws IOException {
		String binStr = "";

		BufferedInputStream br = fio.openBufferedInputStream(binFile);
		BufferedOutputStream bro = fio.openBufferedOutputStream(outFile);
		encodeMap = huffUtil.getEncodeMap();

		int c = 0;
		try {
			while(br.available() > 0) {
				c = br.read();
				binStr += binUtil.convBinToStr(c);
				while (binStr.length() > 0) {
					int currChar = huffUtil.decodeString(binStr);
					if (currChar == 0) {
						br.close();
						bro.close();
						return;
					} else if (currChar == -1) {
						break;
					} else {
						bro.write(currChar);
						String huffCode = encodeMap[currChar];
						binStr = binStr.substring(huffCode.length());
					}
				}
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		

	}

}
