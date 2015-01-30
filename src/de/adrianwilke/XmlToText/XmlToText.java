package de.adrianwilke.XmlToText;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Parses a XML file, extracts values of XML attributes, and writes CSV
 * (comma-separated values) to a new text file.
 * 
 * @author Adrian Wilke
 * @version 0.1, 2015-01-30
 */
public class XmlToText extends DefaultHandler {

	private final static String CHARSET_NAME = "UTF8";
	private final static String FILE_CHOOSER_DESCRIPTION = "XML files";
	private final static String FILE_EXTENSION_IN = ".xml";
	private final static String FILE_EXTENSION_OUT = ".txt";
	private final static String FILE_NOT_FOUND = "File not found";
	private final static String FILE_NOT_READABLE = "Can not read file";
	private final static String NULL_VALUE = "null";
	private final static String LINE_SEPARATOR = System
			.getProperty("line.separator");

	private StringBuilder stringBuilder = new StringBuilder();

	private boolean isFilteringDEV = false;
	private List<String> searchValuesDEV = new LinkedList<>();

	/**
	 * Main entry point.
	 */
	public static void main(String[] args) {
		try {
			XmlToText xmlToText = new XmlToText();

			// Option: if false, it only reads contents.
			boolean isParsingXmlDEV = true;

			// Option: If true, only XML elements with search values are
			// extracted.
			xmlToText.isFilteringDEV = false;
			xmlToText.searchValuesDEV.add("test");

			// Choose file / handle arguments.
			File inFile = null;
			if (args.length == 0) {
				// Dialog.
				inFile = xmlToText.showFileDialog();
				if (inFile == null) {
					System.err.println(FILE_NOT_FOUND);
					System.exit(3);
				} else if (!inFile.canRead()) {
					System.err.println(FILE_NOT_READABLE + ": "
							+ inFile.getAbsolutePath());
					System.exit(2);
				}

			} else if (args[0].equals("-h") || args[0].equals("--help")) {
				// Help.
				StringBuilder help = new StringBuilder();
				help.append("Parses a XML file, extracts values of XML attributes,");
				help.append(LINE_SEPARATOR);
				help.append("and writes CSV (comma-separated values) to a new text file.");
				help.append(LINE_SEPARATOR);
				help.append("The text file is created in the directory of the source file.");
				help.append(LINE_SEPARATOR);
				help.append(LINE_SEPARATOR);
				help.append("Website: https://github.com/adibaba/XmlToText");
				help.append(LINE_SEPARATOR);
				help.append(LINE_SEPARATOR);
				help.append("Call: java -jar XmlToText.java [OPTION]");
				help.append(LINE_SEPARATOR);
				help.append(LINE_SEPARATOR);
				help.append("XmlToText.java -h/--help  Shows this information.");
				help.append(LINE_SEPARATOR);
				help.append("XmlToText.java            Shows dialog and parses selected file.");
				help.append(LINE_SEPARATOR);
				help.append("XmlToText.java [FILE]     Parses [FILE].");
				help.append(LINE_SEPARATOR);
				System.out.println(help.toString());
				System.exit(0);

			} else {
				// Argument.
				inFile = new File(args[0]);
				if (!inFile.canRead()) {
					System.err.println(FILE_NOT_READABLE + ": "
							+ inFile.getAbsolutePath());
					System.exit(2);
				}
			}

			// Extract.
			String inFilePath = inFile.getAbsolutePath();
			xmlToText.extract(inFilePath, isParsingXmlDEV);

			// Write result. Do not overwrite existing files.
			String outFilePath = inFilePath + FILE_EXTENSION_OUT;
			File outFile = new File(outFilePath);
			int i = 2;
			while (outFile.exists()) {
				outFilePath = inFilePath + "." + i + FILE_EXTENSION_OUT;
				outFile = new File(outFilePath);
				i++;
			}
			xmlToText.writeFile(outFilePath, xmlToText.getExtractedValue());

			// Finish.
			System.out.println("In:  " + inFilePath);
			System.out.println("Out: " + outFilePath);
			System.exit(0);

		} catch (Exception e) {
			System.err.println(e);
			System.exit(1);
		}
	}

	String getExtractedValue() {
		return stringBuilder.toString();
	}

	/**
	 * Parses/reads XML file and writes result to {@link #stringBuilder}.
	 * 
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 * @throws IOException
	 */
	public void extract(String file, boolean parse)
			throws ParserConfigurationException, SAXException, IOException {
		if (parse) {
			SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
			SAXParser saxParser = saxParserFactory.newSAXParser();
			saxParser.parse(file, this);
		} else {
			stringBuilder.append(readFile(file));
		}
	}

	/**
	 * XML element start. Used for {@link #extract(String, boolean)}.
	 */
	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {

		boolean searchValueFoundDEV = false;

		StringBuilder localStringBuilder = new StringBuilder();
		for (int i = 0; i < attributes.getLength(); i++) {
			if (i != 0) {
				localStringBuilder.append(", ");
			}
			String value = attributes.getValue(i);

			if (isFilteringDEV) {
				for (String searchValue : searchValuesDEV) {
					if (value.contains(searchValue)) {
						searchValueFoundDEV = true;
					}
				}
			}

			if (!value.equals(NULL_VALUE)) {
				localStringBuilder.append(value.replace("\r\n", " ").replace(
						"\n", " "));
			}
		}

		localStringBuilder.append(LINE_SEPARATOR);

		if (isFilteringDEV && searchValueFoundDEV || !isFilteringDEV) {
			stringBuilder.append(localStringBuilder);
		}
	}

	/**
	 * Reads file.
	 * 
	 * @return File contents
	 * @throws IOException
	 */
	private String readFile(String file) throws IOException {
		FileInputStream fileInputStream = new FileInputStream(file);
		InputStreamReader inputStreamReader = new InputStreamReader(
				fileInputStream, CHARSET_NAME);
		BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

		StringBuilder stringBuilder = new StringBuilder();
		String line;
		while ((line = bufferedReader.readLine()) != null)
			stringBuilder.append(line).append(LINE_SEPARATOR);
		bufferedReader.close();
		return stringBuilder.toString();
	}

	/**
	 * Writes file.
	 * 
	 * @throws IOException
	 */
	private void writeFile(String file, String content) throws IOException {
		StringReader stringReader = new StringReader(content);
		BufferedReader bufferedReader = new BufferedReader(stringReader);
		FileOutputStream fileOutputStream = new FileOutputStream(file);
		OutputStreamWriter outputStreamWriter = new OutputStreamWriter(
				fileOutputStream, CHARSET_NAME);
		BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter);
		String line;
		while ((line = bufferedReader.readLine()) != null) {
			bufferedWriter.write(line);
			bufferedWriter.write(LINE_SEPARATOR);
		}
		bufferedWriter.close();
		bufferedReader.close();
	}

	/**
	 * Shows file selection dialog.
	 * 
	 * @return File or <code>null</code> on errors/cancel
	 */
	private File showFileDialog() {
		final JFileChooser jFileChooser = new JFileChooser();
		jFileChooser.setFileFilter(new FileFilter() {

			@Override
			public String getDescription() {
				return FILE_CHOOSER_DESCRIPTION;
			}

			@Override
			public boolean accept(File f) {
				if (f.isDirectory()) {
					return true;
				} else if (f.getPath().toLowerCase()
						.endsWith(FILE_EXTENSION_IN)) {
					return true;
				} else {
					return false;
				}
			}
		});

		int returnVal = jFileChooser.showOpenDialog(null);
		if (returnVal == JFileChooser.CANCEL_OPTION) {
			return null;
		} else if (returnVal == JFileChooser.ERROR_OPTION) {
			return null;
		} else {
			return jFileChooser.getSelectedFile();
		}
	}
}
