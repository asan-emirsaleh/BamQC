/**
 * Copyright Copyright 2010-15 Simon Andrews
 *
 *    This file is part of BamQC.
 *
 *    BamQC is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    BamQC is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with BamQC; if not, write to the Free Software
 *    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package uk.ac.babraham.BamQC.DataParsers;

import java.io.File;
import java.util.Vector;

import javax.swing.JPanel;
import javax.swing.filechooser.FileFilter;

import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMRecord;

import uk.ac.babraham.BamQC.BamQCException;
import uk.ac.babraham.BamQC.DataTypes.DataCollection;
import uk.ac.babraham.BamQC.DataTypes.DataSet;
import uk.ac.babraham.BamQC.DataTypes.PairedDataSet;
import uk.ac.babraham.BamQC.DataTypes.Genome.Location;
import uk.ac.babraham.BamQC.DataTypes.Sequence.SequenceRead;
import uk.ac.babraham.BamQC.DataTypes.Sequence.SequenceReadWithChromosome;
import uk.ac.babraham.BamQC.Utilities.ChromosomeWithOffset;

/**
 * Parses data in the program-independent BAM file format.  Can cope with
 * simple, paired end and spliced reads.  Has mainly been tested with 
 * TopHat output but reports of success with other programs have been
 * received.
 */
public class BAMFileParser extends DataParser {

	// Extra options which can be set
	private boolean pairedEndImport = false;
	private int pairedEndDistance = 1000;
	private boolean separateSplicedReads = false;
	private boolean importIntrons = false;
	private int extendBy = 0;
	private DataParserOptionsPanel prefs = new DataParserOptionsPanel(true, true, false,true);
	private int minMappingQuality = 0;
	private boolean primaryAlignmentsOnly = true;
	

	/**
	 * Instantiates a new SAM file parser.
	 * 
	 * @param data The dataCollection to which new data will be added.
	 */
	public BAMFileParser (DataCollection data) {
		super(data);		
	}
	
	private void setOptionsFromFile (File file) {

		// This just reads the first few thousand lines from the first file and
		// tries to set the preferences options to the correct defaults.
		
		SAMFileReader.setDefaultValidationStringency(SAMFileReader.ValidationStringency.SILENT);

		SAMFileReader inputSam = new SAMFileReader(file); 

		int lineCount = 0;
		// Now process the file

		boolean pairedEnd = false;
		boolean spliced = false;
		
		for (SAMRecord samRecord : inputSam) {
			++lineCount;
			
			if (lineCount == 10000) break;
			
			if (samRecord.getCigarString().contains("N")) {
				spliced = true;
			}
			if (samRecord.getReadPairedFlag()) {
				pairedEnd = true;
			}
			
		}
		
		inputSam.close();
		
		prefs.setPairedEnd(pairedEnd);
		prefs.setSpliced(spliced);
		

		
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {

		pairedEndImport = prefs.pairedEnd();
		pairedEndDistance = prefs.pairDistanceCutoff();
		separateSplicedReads = prefs.splitSplicedReads();
		importIntrons = prefs.importIntrons();
		extendBy = prefs.extendReads();
		minMappingQuality = prefs.minMappingQuality();
		primaryAlignmentsOnly = prefs.primaryAlignmentsOnly();

		File [] samFiles = getFiles();
		DataSet [] newData = new DataSet[samFiles.length];

		try {
			for (int f=0;f<samFiles.length;f++) {

				SAMFileReader.setDefaultValidationStringency(SAMFileReader.ValidationStringency.SILENT);

				SAMFileReader inputSam = new SAMFileReader(samFiles[f]); 

				if (prefs.isHiC()) {
					newData[f] = new PairedDataSet(samFiles[f].getName(),samFiles[f].getCanonicalPath(),prefs.removeDuplicates(),prefs.hiCDistance(),prefs.hiCIgnoreTrans());
				}
				else {
					newData[f] = new DataSet(samFiles[f].getName(),samFiles[f].getCanonicalPath(),prefs.removeDuplicates());				
				}

				int lineCount = 0;
				// Now process the file

				// A flag we can set to skip the next record if we're getting
				// out of sync during single end HiC import.
				boolean skipNext = false;

				for (SAMRecord samRecord : inputSam) {

					if (skipNext) {
						skipNext = false;
						continue;
					}

					if (cancel) {
						inputSam.close();
						progressCancelled();
						return;
					}

					++lineCount;

					if (lineCount%100000 == 0) {
						progressUpdated("Read "+lineCount+" lines from "+samFiles[f].getName(),f,samFiles.length);
					}


					if (pairedEndImport && ! samRecord.getReadPairedFlag()) {
						progressWarningReceived(new BamQCException("Data was single ended during paired end import"));
						continue;
					}

					if (samRecord.getReadUnmappedFlag()) {
						// There was no match
						continue;
					}
					if (primaryAlignmentsOnly && samRecord.getNotPrimaryAlignmentFlag()) {
						// This is a secondary alignment and we're only importing primary
						// alignments
						continue;
					}
					if (pairedEndImport && ! separateSplicedReads && samRecord.getMateUnmappedFlag()) {
						// No match on the reverse strand.  Doesn't matter if we're doing spliced reads.
						continue;
					}

					if (minMappingQuality > 0  && samRecord.getMappingQuality() < minMappingQuality) {
						// The match isn't good enough
						continue;
					}
					
					if (pairedEndImport && ! separateSplicedReads && ! samRecord.getReadNegativeStrandFlag()) {
						// For paired reads we only send in reads on the negative strand since we can 
						// be sure that we can position these correctly without having to rely on the tlen
						// field.
						continue;
					}
					
					if (pairedEndImport && ! separateSplicedReads && ! samRecord.getReferenceName().equals(samRecord.getMateReferenceName())) {
						// The two ends of a pair don't map to the same chromosome
//						progressWarningReceived(new BamQCException("Paired reads mapped to different chromosomes"));
						continue;
					}


					// TODO: Check what this actually stores - might be a real name rather than 0/=
					if (pairedEndImport && ! separateSplicedReads && ! prefs.isHiC() && samRecord.getMateReferenceName() == "0") {
						if (samRecord.getMateReferenceName() != "=") {
							inputSam.close();
							throw new BamQCException("Unexpected mate referenece name "+samRecord.getMateReferenceName());
						}
						// Matches were on different chromosomes
						continue;
					}

					try {
						if (pairedEndImport && ! separateSplicedReads) {
							SequenceReadWithChromosome read = getPairedEndRead(samRecord);
							newData[f].addData(read.chromosome,read.read);
						}
						else if (separateSplicedReads) {
							SequenceReadWithChromosome [] reads = getSplitSingleEndRead(samRecord);
							for (int r=0;r<reads.length;r++) {
								newData[f].addData(reads[r].chromosome,reads[r].read);
							}
						}
						else {
							SequenceReadWithChromosome read = getSingleEndRead(samRecord);
							newData[f].addData(read.chromosome,read.read);
						}
					}
					catch (BamQCException ex) {
						progressWarningReceived(ex);

						if (prefs.isHiC() && ! pairedEndImport) {
							if (((PairedDataSet)newData[f]).importSequenceSkipped()) {
								// Skip the next line
								skipNext = true;
							}
						}
					}

				}

				// We're finished with the file.
				inputSam.close();

				// Cache the data in the new dataset
				progressUpdated("Caching data from "+samFiles[f].getName(), f, samFiles.length);
				newData[f].finalise();

			}
		}	

		catch (Exception ex) {
			progressExceptionReceived(ex);
			return;
		}

		processingFinished(newData);
	}

	/**
	 * Gets a split single end read.  The only reason for asking about whether the
	 * import is single or paired end is that if we're doing a paired end import
	 * then we reverse the strand for all second read data.  For single end import
	 * we leave the strand alone whichever read it originates from.
	 * 
	 * @param samRecord The picard record entry for this read
	 * @param flag pairedEnd Is this a paired end import
	 * @return The read which was read
	 * @throws BamQCException
	 */
	private SequenceReadWithChromosome [] getSplitSingleEndRead (SAMRecord samRecord) throws BamQCException {
		int strand;
		int start;
		int lastEnd = -1;

		start = samRecord.getAlignmentStart();

		// For paired end data we want to flip the strand of the second read
		// so that we get the correct strand for the fragment so we don't end
		// up making mixed libraries from what should be strand specific data

		if (samRecord.getReadNegativeStrandFlag()) {
			if (samRecord.getReadPairedFlag() && samRecord.getSecondOfPairFlag()) {
				strand = Location.FORWARD;
			}
			else {
				strand = Location.REVERSE;
			}
		}
		else {
			if (samRecord.getReadPairedFlag() && samRecord.getSecondOfPairFlag()) {
				strand = Location.REVERSE;
			}
			else {
				strand = Location.FORWARD;
			}
		}

		ChromosomeWithOffset c;

		try {
			c = dataCollection().genome().getChromosome(samRecord.getReferenceName());
		}
		catch (Exception e) {
			throw new BamQCException(e.getLocalizedMessage());
		}
		
		start = c.position(start);
		

		if (start < 1) {
			throw new BamQCException("Reading position "+start+" was before the start of chr"+c.chromosome().name()+" ("+c.chromosome().length()+")");			
		}


		// We now need to work our way through the cigar string breaking every time
		// there is a skip section in the cigar string
		
		// Do a quick check to see if we can avoid having to parse the cigar string.  If there
		// isn't an N in it, then we can treat it as a simple single span.
		
		if (! samRecord.getCigarString().contains("N")) {
			if (!importIntrons) {
				int end = c.position(samRecord.getAlignmentEnd());
				return new SequenceReadWithChromosome []{new SequenceReadWithChromosome(c.chromosome(),SequenceRead.packPosition(start,end,strand))};
				
			}
			return new SequenceReadWithChromosome[0];

		}
		
		
		String [] cigarOperations = samRecord.getCigarString().split("\\d+");
		String [] cigarNumbers = samRecord.getCigarString().split("[MIDNSHP]");

		if (cigarOperations.length != cigarNumbers.length+1) {
			throw new BamQCException("Couldn't parse CIGAR string "+samRecord.getCigarString()+" counts were "+cigarOperations.length+" vs "+cigarNumbers.length);
		}

		Vector<SequenceReadWithChromosome> newReads = new Vector<SequenceReadWithChromosome>();

		int currentPosition = start;
		for (int pos=0;pos<cigarNumbers.length;pos++) {

			if (cigarOperations[pos+1].equals("M")) {
				currentPosition += Integer.parseInt(cigarNumbers[pos])-1;
			}
			else if (cigarOperations[pos+1].equals("I")) {
				currentPosition += Integer.parseInt(cigarNumbers[pos])-1;
			}
			else if (cigarOperations[pos+1].equals("D")) {
				currentPosition -= Integer.parseInt(cigarNumbers[pos])-1;
			}
			else if (cigarOperations[pos+1].equals("N")) {
				// Make a new sequence as far as this point

				if (importIntrons) {
					if (lastEnd > 0) {
						if (start > c.chromosome().length()) {
							int overrun = (start-1) - c.chromosome().length();
							throw new BamQCException("Reading position "+(start-1)+" was "+overrun+"bp beyond the end of chr"+c.chromosome().name()+" ("+c.chromosome().length()+")");
						}
						newReads.add(new SequenceReadWithChromosome(c.chromosome(),SequenceRead.packPosition(lastEnd+1,start-1,strand)));
					}

					// Update the lastEnd whether we added a read or not since this
					// will be the start of the next intron
					lastEnd = currentPosition;
				}

				else {
					// We also don't allow readings which are beyond the end of the chromosome
					if (currentPosition > c.chromosome().length()) {
						int overrun = currentPosition - c.chromosome().length();
						throw new BamQCException("Reading position "+currentPosition+" was "+overrun+"bp beyond the end of chr"+c.chromosome().name()+" ("+c.chromosome().length()+")");
					}

					newReads.add(new SequenceReadWithChromosome(c.chromosome(),SequenceRead.packPosition(start,currentPosition,strand)));
				}

				currentPosition += Integer.parseInt(cigarNumbers[pos])+1;
				start=currentPosition;
			}

		}

		if (importIntrons) {
			if (lastEnd > 0) {
				if (start > c.chromosome().length()) {
					int overrun = (start-1) - c.chromosome().length();
					throw new BamQCException("Reading position "+(start-1)+" was "+overrun+"bp beyond the end of chr"+c.chromosome().name()+" ("+c.chromosome().length()+")");
				}
				newReads.add(new SequenceReadWithChromosome(c.chromosome(),SequenceRead.packPosition(lastEnd+1,start-1,strand)));
			}
		}
		else {
			// We have to process the last read in the string.

			// We also don't allow readings which are beyond the end of the chromosome
			if (currentPosition > c.chromosome().length()) {
				int overrun = currentPosition - c.chromosome().length();
				throw new BamQCException("Reading position "+currentPosition+" was "+overrun+"bp beyond the end of chr"+c.chromosome().name()+" ("+c.chromosome().length()+")");
			}

			newReads.add(new SequenceReadWithChromosome(c.chromosome(),SequenceRead.packPosition(start,currentPosition,strand)));
		}

		return newReads.toArray(new SequenceReadWithChromosome[0]);

	}

	/**
	 * Gets a single end read.
	 * 
	 * @param sections The tab split sections from the SAM file
	 * @param flag The binary flag field from the file
	 * @return The read which was read
	 * @throws BamQCException
	 */
	private SequenceReadWithChromosome getSingleEndRead (SAMRecord samRecord) throws BamQCException {
		int strand;
		int start;
		int end;

		start = samRecord.getAlignmentStart();
		end = samRecord.getAlignmentEnd();

		if (samRecord.getReadNegativeStrandFlag()) {
			strand = Location.REVERSE;
		}
		else {
			strand = Location.FORWARD;
		}

		//		// We now need to work our way through the cigar string to work out the
		//		// end of the string
		//		String [] cigarOperations = samRecord.getCigarString().split("\\d+");
		//		String [] cigarNumbers = samRecord.getCigarString().split("[MIDNSHP]");
		//			
		//		end = start;
		//		for (int pos=0;pos<cigarNumbers.length;pos++) {
		//				
		//			if (cigarOperations[pos+1].equals("M")) {
		//				end += Integer.parseInt(cigarNumbers[pos])-1;
		//			}
		//			else if (cigarOperations[pos+1].equals("I")) {
		//				end += Integer.parseInt(cigarNumbers[pos])-1;
		//			}
		//			else if (cigarOperations[pos+1].equals("D")) {
		//				end -= Integer.parseInt(cigarNumbers[pos])-1;
		//			}
		//			else if (cigarOperations[pos+1].equals("N")) {					
		//				end += Integer.parseInt(cigarNumbers[pos])-1;
		//			}
		//		}			


		if (extendBy > 0) {
			if (strand == Location.FORWARD) {
				end += extendBy;
			}
			else if (strand == Location.REVERSE) {
				start -= extendBy;
			}
		}

		ChromosomeWithOffset c;
		try {
			c = collection.genome().getChromosome(samRecord.getReferenceName());
		}
		catch (Exception iae) {
			throw new BamQCException(iae.getLocalizedMessage());
		}
		
		start = c.position(start);
		end = c.position(end);

		// We also don't allow readings which are beyond the end of the chromosome
		if (end > c.chromosome().length()) {
			int overrun = end - c.chromosome().length();
			throw new BamQCException("Reading position "+end+" was "+overrun+"bp beyond the end of chr"+c.chromosome().name()+" ("+c.chromosome().length()+")");
		}
		if (start < 1) {
			throw new BamQCException("Reading position "+start+" was before the start of chr"+c.chromosome().name()+" ("+c.chromosome().length()+")");			
		}

		// We can now make the new reading
		SequenceReadWithChromosome read = new SequenceReadWithChromosome(c.chromosome(),SequenceRead.packPosition(start,end,strand));

		return read;
	}

	
	/**
	 * Gets a paired end read.  This method assumes that it will only be passed reads which map
	 * to the reverse strand since these are the ones which contain enough information to 
	 * unambiguously locate both ends of the pair.
	 * 
	 * @param sections The tab split sections from the SAM file
	 * @param flag The binary flag field
	 * @return The read which was read
	 * @throws BamQCException
	 */
	private SequenceReadWithChromosome getPairedEndRead (SAMRecord samRecord) throws BamQCException {
		int strand;
		int start;
		int end;

		if (!samRecord.getReadNegativeStrandFlag()) {
			throw new BamQCException("Read passed to parse pair was not on the negative strand");
		}
		
		if (samRecord.getMateNegativeStrandFlag()) {
			throw new BamQCException("Ignored discordantly stranded read pair");
		}
		
		end = samRecord.getAlignmentEnd();
		start = samRecord.getMateAlignmentStart();
		
		if (start > end) {
			throw new BamQCException("Ignored discordantly stranded read pair");
		}
		
		if (samRecord.getFirstOfPairFlag()) {
			strand = Location.REVERSE;
		}
		else {
			strand = Location.FORWARD;
		}
		
		if ((end - start)+1 > pairedEndDistance) {
			throw new BamQCException("Distance between ends "+((end - start)+1)+" was larger than cutoff ("+pairedEndDistance+")");
		}

		ChromosomeWithOffset c;

		try {
			c = dataCollection().genome().getChromosome(samRecord.getReferenceName());
		}
		catch (Exception e) {
			throw new BamQCException(e.getLocalizedMessage());
		}
		
		start = c.position(start);
		end = c.position(end);

		// We also don't allow readings which are beyond the end of the chromosome
		if (end > c.chromosome().length()) {
			int overrun = end - c.chromosome().length();
			throw new BamQCException("Reading position "+end+" was "+overrun+"bp beyond the end of chr"+c.chromosome().name()+" ("+c.chromosome().length()+")");
		}
		if (start < 1) {
			throw new BamQCException("Reading position "+start+" was before the start of chr"+c.chromosome().name()+" ("+c.chromosome().length()+")");			
		}

		// We can now make the new reading
		SequenceReadWithChromosome read = new SequenceReadWithChromosome(c.chromosome(),SequenceRead.packPosition(start,end,strand));

		return read;
	}



//	/**
//	 * Gets a paired end read.  This method assumes that it will only be passed the first read of
//	 * a pair to avoid duplicating the reads which were seen.
//	 * 
//	 * @param sections The tab split sections from the SAM file
//	 * @param flag The binary flag field
//	 * @return The read which was read
//	 * @throws BamQCException
//	 */
//	private SequenceReadWithChromosome getPairedEndRead (SAMRecord samRecord) throws BamQCException {
//		int strand;
//		int start;
//		int end;
//
//		if (samRecord.getInferredInsertSize() == 0) {
//			if (samRecord.getReadNegativeStrandFlag()) {
//				end = samRecord.getAlignmentEnd();
//				start = samRecord.getMateAlignmentStart();			
//			}
//			else {
//				start = samRecord.getAlignmentStart();
//				end = samRecord.getMateAlignmentStart()+ samRecord.getCigar().getPaddedReferenceLength();
//			}
//		}
//
//		else if (samRecord.getReadNegativeStrandFlag()) {
//			end = samRecord.getAlignmentEnd();
//			start = end+(samRecord.getInferredInsertSize()+1);			
//		}
//		else {
//			start = samRecord.getAlignmentStart();
//			end = start+samRecord.getInferredInsertSize()-1;
//		}
//
//		if (end < start) {
//			int temp = start;
//			start = end;
//			end = temp;
//		}
//
//		// We assign the strand for the pair as the strand for the
//		// first read in the pair
//
//		if (samRecord.getReadNegativeStrandFlag() && !samRecord.getMateNegativeStrandFlag()) {
//			strand = Location.REVERSE;
//		}
//		else if (!samRecord.getReadNegativeStrandFlag() && samRecord.getMateNegativeStrandFlag()) {
//			strand = Location.FORWARD;
//		}
//		else {
//			strand = Location.UNKNOWN;
//		}
//
//		if ((end - start)+1 > pairedEndDistance) {
//			throw new BamQCException("Distance between ends "+((end - start)+1)+" was larger than cutoff ("+pairedEndDistance+")");
//		}
//
//
//		ChromosomeWithOffset c;
//
//		try {
//			c = dataCollection().genome().getChromosome(samRecord.getReferenceName());
//		}
//		catch (Exception e) {
//			throw new BamQCException(e.getLocalizedMessage());
//		}
//		
//		start = c.position(start);
//		end = c.position(end);
//
//		// We also don't allow readings which are beyond the end of the chromosome
//		if (end > c.chromosome().length()) {
//			int overrun = end - c.chromosome().length();
//			throw new BamQCException("Reading position "+end+" was "+overrun+"bp beyond the end of chr"+c.chromosome().name()+" ("+c.chromosome().length()+")");
//		}
//		if (start < 1) {
//			throw new BamQCException("Reading position "+start+" was before the start of chr"+c.chromosome().name()+" ("+c.chromosome().length()+")");			
//		}
//
//		// We can now make the new reading
//		SequenceReadWithChromosome read = new SequenceReadWithChromosome(c.chromosome(),SequenceRead.packPosition(start,end,strand));
//
//		return read;
//	}


	/* (non-Javadoc)
	 * @see uk.ac.babraham.BamQC.DataParsers.DataParser#description()
	 */
	@Override
	public String description() {
		return "Imports Data standard BAM Format files";
	}


	/* (non-Javadoc)
	 * @see uk.ac.babraham.BamQC.DataParsers.DataParser#getOptionsPanel()
	 */
	@Override
	public JPanel getOptionsPanel() {
		File firstFile = getFiles()[0];		
		setOptionsFromFile(firstFile);

		return prefs;
	}


	/* (non-Javadoc)
	 * @see uk.ac.babraham.BamQC.DataParsers.DataParser#hasOptionsPanel()
	 */
	@Override
	public boolean hasOptionsPanel() {
		return true;
	}


	/* (non-Javadoc)
	 * @see uk.ac.babraham.BamQC.DataParsers.DataParser#name()
	 */
	@Override
	public String name() {
		return "BAM File Importer";
	}


	/* (non-Javadoc)
	 * @see uk.ac.babraham.BamQC.DataParsers.DataParser#readyToParse()
	 */
	@Override
	public boolean readyToParse() {
		return true;
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.BamQC.DataParsers.DataParser#getFileFilter()
	 */
	@Override
	public FileFilter getFileFilter () {
		return new FileFilter() {

			@Override
			public String getDescription() {
				return "BAM/SAM Files";
			}

			@Override
			public boolean accept(File f) {
				if (f.isDirectory() || f.getName().toLowerCase().endsWith(".bam") || f.getName().toLowerCase().endsWith(".sam")) {
					return true;
				}
				return false;
			}

		};
	}

}