/**
*
*  Tarsos is developed by Joren Six at 
*  The Royal Academy of Fine Arts & Royal Conservatory,
*  University College Ghent,
*  Hoogpoort 64, 9000 Ghent - Belgium
*
**/
package be.hogent.tarsos.sampled.pitch;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.exec.ExecuteException;

import be.hogent.tarsos.util.AudioFile;
import be.hogent.tarsos.util.Command;
import be.hogent.tarsos.util.ConfKey;
import be.hogent.tarsos.util.Configuration;
import be.hogent.tarsos.util.FileUtils;

/**
 * The IPEM_SIX Pitch detector uses an auditory model for polyphonic pitch
 * tracking. More information can be found in following papers: Factors
 * affecting music retrieval in query-by-melody De Mulder, Tom; Martens, Jean;
 * PAUWS, S; VIGNOLI, F et al. IEEE TRANSACTIONS ON MULTIMEDIA (2006) Recent
 * improvements of an auditory model based front-end for the transcription of
 * vocal queries De Mulder, Tom; MARTENS, J; Lesaffre, Micheline; Leman, Marc et
 * al. 2004 IEEE INTERNATIONAL CONFERENCE ON ACOUSTICS, SPEECH, AND SIGNAL
 * PROCESSING, VOL IV, PROCEEDINGS (2004) An Auditory Model Based Transcriber of
 * Vocal Queries
 * 
 * De Mulder, Tom; Martens, Jean; Lesaffre, Micheline; Leman, Marc et al.
 * Proceedings of the Fourth International Conference on Music Information
 * Retrieval (ISMIR) 2003 (2003) The text file generated by the pitch detector
 * consists of 12 columns: 6 times a frequency in Hertz followed by a
 * probability. The frequencies are ordered by their respective probabilities.
 * 
 * @author Joren Six
 */
public final class IPEMPitchDetection implements PitchDetector {
	/**
	 * Log messages.
	 */
	private static final Logger LOG = Logger.getLogger(IPEMPitchDetection.class.getName());

	private final PitchDetectionMode mode;

	private final AudioFile file;

	private final List<Annotation> annotations;

	/**
	 * @param audioFile
	 *            The file to detect pitch for
	 * @param detectionMode
	 * 			Defines the detection mode used.
	 */
	public IPEMPitchDetection(final AudioFile audioFile, final PitchDetectionMode detectionMode) {
		this.file = audioFile;
		this.annotations = new ArrayList<Annotation>();
		this.mode = detectionMode;
		copyBinaryFiles();
	}

	/**
	 * Copies the exe files from the jar to disk.
	 */
	private void copyBinaryFiles() {
		// check files and copy them if needed
		for (final String target : ipemFiles()) {
			if (!FileUtils.exists(target)) {
				final String name = FileUtils.basename(target) + "." + FileUtils.extension(target);
				FileUtils.copyFileFromJar("/be/hogent/tarsos/sampled/pitch/resources/" + name, target);
			}
		}
	}

	/**
	 * @return A list of absolute paths
	 */
	private String[] ipemFiles() {
		final String[] names = { "ipem_pitch_detection.sh", "libsndfile.dll",
				mode.getParametername() + ".exe" };
		final String[] paths = new String[names.length];
		final String dataDirectory = FileUtils.temporaryDirectory();
		int i = 0;
		for (final String ipemFile : names) {
			paths[i++] = FileUtils.combine(dataDirectory, ipemFile);
		}
		return paths;
	}

	public List<Annotation> executePitchDetection() {

		final String transcodedBaseName = FileUtils.basename(file.transcodedPath());
		FileUtils.writeFile(transcodedBaseName + "\n", "lijst.txt");
		final String name = mode.getParametername();

		String outputDirectory = FileUtils.combine(file.transcodedDirectory()) + "/";

		final String csvFileName = FileUtils.combine(outputDirectory, transcodedBaseName + ".txt");
		
		String audioDirectory = file.transcodedDirectory() + "/";
		String executableDirectory = FileUtils.temporaryDirectory();

		if (System.getProperty("os.name").contains("indows")) {
			Command command = null;
			audioDirectory = audioDirectory.replace("/", "\\").replace(":\\", "://");
			outputDirectory = outputDirectory.replace("/", "\\").replace(":\\", "://");
			if (mode == PitchDetectionMode.IPEM_ONE) {
				String cmd = FileUtils.combine(executableDirectory, name + ".exe  ");
				command = new Command(cmd);
				command.addArgument(audioDirectory + transcodedBaseName + ".wav ");
				command.addArgument(outputDirectory + transcodedBaseName + ".txt");
			} else {
				String cmd = FileUtils.combine(executableDirectory, name + ".exe  ");
				command = new Command(cmd);
				command.addArgument("lijst.txt");
				command.addArgument(audioDirectory);
				command.addArgument(outputDirectory);
			}
			try {
				command.execute();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else { // on linux use wine's Z-directory
			String executable = makeWinePath(FileUtils.combine(executableDirectory, name + ".exe"));	
			String command;
			if(mode==PitchDetectionMode.IPEM_ONE){
				String audioFile = makeWinePath(audioDirectory + transcodedBaseName + ".wav");
				String outputFile = makeWinePath(csvFileName);
				command = "wine " + executable  + " " + audioFile + " " + outputFile + " ";
			}else{
				String lijstFile = makeWinePath(FileUtils.combine(FileUtils.runtimeDirectory(),"lijst.txt"));
				audioDirectory = makeWinePath(audioDirectory);
				outputDirectory = makeWinePath(outputDirectory);
				command = "wine " + executable  + " " + lijstFile + " " + audioDirectory + " " + outputDirectory + " ";	
			}
			String scriptFile = FileUtils.combine(FileUtils.runtimeDirectory(),"ipem.sh");
			FileUtils.writeFile("#!/bin/bash\n"+command, scriptFile);
			executeBashScript(new File(scriptFile));
		}		

		if (mode == PitchDetectionMode.IPEM_ONE) {
			parseIpemOne(csvFileName);
		} else {
			parseIpemSix(csvFileName);
		}

		// Is keeping the intermediate CSV file required?
		// I don't think so:
		if (new File(csvFileName).delete()) {
			LOG.fine(String.format("Deleted intermediate CSV file %s", csvFileName));
		} else {
			// mark for deletion when JVM closes
			LOG.fine(String.format("Failed to deleted intermediate CSV file %s", csvFileName));
		}

		LOG.fine(String.format("%s pitch detection finished for %s.", mode.name(), file.originalBasename()));

		return annotations;
	}
	
	private void executeBashScript(File bashScript){
		Command cmd = new Command("bash");
		cmd.addFileArgument(bashScript.getPath());
		try {
			String output = cmd.execute();
			LOG.fine(output.toString());
		} catch (ExecuteException e) {
			LOG.warning("Failed to execute " + bashScript.getAbsolutePath() + ": " + e.getMessage());
		} catch (IOException e) {
			LOG.warning("Failed to execute " + bashScript.getAbsolutePath() + ": " + e.getMessage());
		}
	}
	
	private String makeWinePath(String path){
		path = ("z://" + path.replace("/", "\\\\")).replace("//\\\\", "//");
		path = "\"" + path + "\"";
		return path;
	}

	/**
	 * Parse a CSV file containing this information: ' 0.070 84.064 ' the first
	 * column is a time stamp (in seconds), the second the pitch in Hz.
	 * 
	 * @param csvFileName
	 */
	private void parseIpemOne(final String csvFileName) {
		// split on a one or more whitespace characters
		final List<String[]> csvData = FileUtils.readCSVFile(csvFileName, "\\s+", -1);
		long start = 0;
		for (final String[] row : csvData) {
			final int pitchIndex;
			if (row.length == 2) {
				pitchIndex = 1;
			} else {
				pitchIndex = 2;
			}
			try {
				double pitch = Double.parseDouble(row[pitchIndex]);
				if (pitch != 0) {
					final Annotation sample = new Annotation(start / 1000.0, pitch, mode);
					annotations.add(sample);
				}
			} catch (final NumberFormatException e) {
				LOG.info("Ignored incorrectly formatted pitch: " + row[pitchIndex]);
			}
			start += 10;
		}
	}

	private void parseIpemSix(final String csvFileName) {
		long start = 0;

		final double minimumAcceptableProbability = Configuration.getDouble(ConfKey.ipem_pitch_threshold);

		final List<String[]> csvData = FileUtils.readCSVFile(csvFileName, " ", 12);

		for (final String[] row : csvData) {
			double timeStamp = start / 1000.0;
			for (int index = 0; index < 6; index++) {
				Double probability = 0.0;
				try {
					probability = Double.parseDouble(row[index * 2 + 1]);
				} catch (final NumberFormatException e) {
					LOG.info("Ignored incorrectly formatted number: " + row[index * 2 + 1]);
				}

				Double pitch = row[index * 2].equals("-1.#IND00") || row[index * 2].equals("-1.#QNAN0") ? 0.0
						: Double.parseDouble(row[index * 2]);
				// only accept values smaller than 25000Hz
				// bigger values are annotated incorrectly
				// With the ipem pitchdetector this happens sometimes, on wine
				// a big value is
				if (pitch > 25000) {
					pitch = 0.0;
				}

				// Do not store 0 Hz values
				if (pitch != 0.0 && probability > minimumAcceptableProbability) {
					final Annotation annotation = new Annotation(timeStamp, pitch, mode, probability);
					annotations.add(annotation);
				}
			}
			start += 10;
		}
	}

	public String getName() {
		return this.mode.getParametername();
	}

	public List<Annotation> getAnnotations() {
		return this.annotations;
	}

	
	public double progress() {
		return -1;
	}
}
