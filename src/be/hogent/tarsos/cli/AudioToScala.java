/**
*
*  Tarsos is developed by Joren Six at 
*  The Royal Academy of Fine Arts & Royal Conservatory,
*  University College Ghent,
*  Hoogpoort 64, 9000 Ghent - Belgium
*
**/
package be.hogent.tarsos.cli;

import java.io.File;
import java.util.List;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import be.hogent.tarsos.sampled.pitch.Annotation;
import be.hogent.tarsos.sampled.pitch.PitchDetectionMode;
import be.hogent.tarsos.sampled.pitch.PitchDetector;
import be.hogent.tarsos.transcoder.ffmpeg.EncoderException;
import be.hogent.tarsos.util.AudioFile;
import be.hogent.tarsos.util.FileUtils;
import be.hogent.tarsos.util.histogram.HistogramFactory;
import be.hogent.tarsos.util.histogram.PitchHistogram;
import be.hogent.tarsos.util.histogram.PitchClassHistogram;
import be.hogent.tarsos.util.histogram.peaks.Peak;
import be.hogent.tarsos.util.histogram.peaks.PeakDetector;

/**
 * @author Joren Six
 */
public final class AudioToScala extends AbstractTarsosApp {

	@Override
	public String description() {
		return "Detects a tone scale from an audio file and exports it as a scala file.";
	}


	@Override
	public void run(final String... args) {
		final OptionParser parser = new OptionParser();
		final OptionSpec<File> fileSpec = parser.accepts("in", "The file to annotate.").withRequiredArg()
				.ofType(File.class).withValuesSeparatedBy(' ');
		final OptionSpec<File> scalaSpec = parser
				.accepts("scala", "The output scala file. (default: 'name of the input file'.scl)")
				.withRequiredArg().ofType(File.class).withValuesSeparatedBy(' ');

		final OptionSpec<PitchDetectionMode> detectionModeSpec = createDetectionModeSpec(parser);

		final OptionSet options = parse(args, parser, this);

		if (isHelpOptionSet(options)) {
			printHelp(parser);
		} else {
			final File inputFile = options.valueOf(fileSpec);
			final PitchDetectionMode detectionMode = options.valueOf(detectionModeSpec);
			File scalaFile;
			if (options.valueOf(scalaSpec) == null) {
				scalaFile = new File(FileUtils.basename(inputFile.getAbsolutePath()) + ".scl");
			} else {
				scalaFile = options.valueOf(scalaSpec);
			}
			exportScalaFile(scalaFile, inputFile, detectionMode);
		}
	}

	/**
	 * Export a Scala file using a detector and an input file.
	 * 
	 * @param scalaFile
	 *            The location to export to.
	 * @param inputFile
	 *            The audio input file.
	 * @param detectionMode
	 *            The detection mode.
	 */
	private void exportScalaFile(final File scalaFile, final File inputFile,
			final PitchDetectionMode detectionMode) {
		AudioFile audioFile;
		try {
			audioFile = new AudioFile(inputFile.getAbsolutePath());
			final PitchDetector pitchDetector = detectionMode.getPitchDetector(audioFile);
			pitchDetector.executePitchDetection();
			final List<Annotation> samples = pitchDetector.getAnnotations();
			final PitchHistogram pitchHistogram = HistogramFactory.createPitchHistogram(samples);
			final PitchClassHistogram scaleHistogram = pitchHistogram.pitchClassHistogram();
			scaleHistogram.plot(FileUtils.basename(scalaFile.getAbsolutePath()) + "png",
					FileUtils.basename(scalaFile.getAbsolutePath()));
			scaleHistogram.gaussianSmooth(1.0);
			final List<Peak> peaks = PeakDetector.detect(scaleHistogram, 15,15);
			PitchClassHistogram.exportPeaksToScalaFileFormat(scalaFile.getAbsolutePath(),
					FileUtils.basename(inputFile.getAbsolutePath()), peaks);
		} catch (EncoderException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
