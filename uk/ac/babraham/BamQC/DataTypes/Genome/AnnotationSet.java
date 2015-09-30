/**
 * Copyright Copyright 2014 Simon Andrews
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

package uk.ac.babraham.BamQC.DataTypes.Genome;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import uk.ac.babraham.BamQC.BamQCApplication;
import uk.ac.babraham.BamQC.Modules.ModuleConfig;
import uk.ac.babraham.BamQC.Preferences.BamQCPreferences;
import net.sf.samtools.SAMRecord;

public class AnnotationSet {
	
	
	/** The reference file for this annotation set */
	private File file = null;
	
	private ChromosomeFactory factory = new ChromosomeFactory();
	
	private HashMap<String, FeatureClass> features = new HashMap<String, FeatureClass>();
	
	private FeatureClass [] featureArray = null;
	
	private HashSet<Feature> allFeatures = new HashSet<Feature>();
	
	private final int cacheSize = ModuleConfig.getParam("AnnotationSet_annotation_cache_size", "ignore").intValue();
	private List<ShortRead> readCache = new ArrayList<ShortRead>(cacheSize);

	
	
	// TODO
	// imported from SeqMonk
//	protected Genome genome;
//	private String name;
//	private boolean finalised = false;
//	/*
//	 * We store features by chromosome.  Within each chromosome we store
//	 * by feature type for quick access and then within that we store
//	 * a vector of features.
//	 */
//	protected FeatureSet chromosomeFeatures = new FeatureSet();
//	private HashSet<String> featureTypes = new HashSet<String>();
	// end imported from SeqMonk
	
	
	
	
	
	public File getFile() {
		return file;
	}

	public void setFile(File file) {
		this.file = file;
	}

	public ChromosomeFactory chromosomeFactory () {
		return factory;
	}
	
	public void addFeature (Feature f) {
		// in the future, we need to find a way to merge: 
		// BamQC: FeatureClass -> FeatureSubclass (external classes)
		// SeqMonk: FeatureSet:FeatureTypeFeatureSet:FeatureTypeCollection (inner classes)
		
		// Original addFeature from BamQC
		if (!features.containsKey(f.type())) {
			features.put(f.type(), new FeatureClass(this));
			allFeatures.add(f);
		}	
		features.get(f.type()).addFeature(f);

		
		// TODO
//		// addFeature from SeqMonk
//		chromosomeFeatures.addFeature(f);
//		if (!featureTypes.contains(f.type())) {
//			featureTypes.add(f.type());
//		}
	}
	
	
	public Feature[] getAllFeatures() {
		return allFeatures.toArray(new Feature[0]);
	}
	
	
	
	public boolean hasFeatures () {
		return !features.isEmpty();
	}
	
	public String [] listFeatureTypes () {
		return features.keySet().toArray(new String [0]);
	}
	
	public FeatureClass getFeatureClassForType (String type) {
		return features.get(type);
	}
	
	
	public void processSequenceNoCache(SAMRecord r) {
		// implementation using ShortRead
		processCachedSequence(new ShortRead(r.getReferenceName(), r.getAlignmentStart(), r.getAlignmentEnd()));
	}
	
	
	public void processSequence (SAMRecord r) {
			
		// implementation using ShortRead
	    if(readCache.size() < cacheSize) {
	    	readCache.add(new ShortRead(r.getReferenceName(), r.getAlignmentStart(), r.getAlignmentEnd()));
	    } else {
	    	// sort the cache
	    	Collections.sort(readCache);
	    	// now parse the sorted cache
	    	for(int i=0; i < cacheSize; i++) {
	    		processCachedSequence(readCache.get(i));
	    	}
	    	// let's clear and reuse the array for now, instead of reallocating a new one every time.
	    	// Tricky to say what's the best is.. an O(n)remove vs allocation+GC ... 
	    	readCache.clear();
	    }       
	}
	
	private void processCachedSequence(ShortRead r) {	
		if (!r.getReferenceName().equals("*")) {
			Chromosome c = factory.getChromosome(r.getReferenceName());
			c.processSequence(r);
		}
		if (featureArray == null) {
			featureArray = features.values().toArray(new FeatureClass[0]);
		}
		for (int i=0;i<featureArray.length;i++) {
			featureArray[i].processSequence(r);
		}
	}	
	
	


	public AnnotationSet() { }

//	// imported from SeqMonk
//	/**
//	 * Instantiates a new annotation set.
//	 * 
//	 * @param genome The base genome which this annotation applies to
//	 */
//	public AnnotationSet (Genome genome) {
//		this.genome = genome;
//	}
//	
//	/**
//	 * Instantiates a new annotation set.
//	 * 
//	 * @param genome The base genome which this annotation applies to
//	 * @param name The name for the set (file name, URL etc)
//	 */
//	public AnnotationSet (Genome genome, String name) {
//		this.genome = genome;
//		this.name = name;		
//	}
//
//
//	/**
//	 * Provides an enumeration to iterate through all chromosomes which have
//	 * data in this set.  This is because we don't want to have to return
//	 * all features in this set at the same time due to memory constraints.
//	 * Therefore using this iterator is the recommended way to get hold of
//	 * all features in the set.
//	 * 
//	 * @return An enumeration of all chromosome names which have data in this set.
//	 */
//	public Enumeration<String> chromosomes () {
//		return chromosomeFeatures.chromosomeNames();
//	}
//	
//	/**
//	 * Checks if this set holds any features of the given type
//	 * 
//	 * @param type The annotation type to check
//	 * @return true, if this set has features for that type
//	 */
//	public boolean hasDataForType (String type) {
//		return featureTypes.contains(type);
//	}
//	
//	/**
//	 * This is used to clean up the set when it is being removed.
//	 */
//	public void delete () {
//		chromosomeFeatures = null;
//		if(featureTypes != null) {
//			featureTypes.clear();
//		}
//	}
//	
//	/**
//	 * The name of this annotation set
//	 * 
//	 * @return The annotation set name
//	 */
//	public String name () {
//		return name;
//	}
//	
//	/**
//	 * Sets the annotation set name.
//	 * 
//	 * @param name The new name for this set.
//	 */
//	public void setName (String name) {
//		this.name = name;
//	}
//	
//	/**
//	 * Renames one class of features to a different name
//	 * 
//	 * @param oldName
//	 * @param newName
//	 */
//	public void renameFeatures (String oldName, String newName) {
//
//		String [] chrNames = chromosomeFeatures.chrFeatures.keySet().toArray(new String [0]);
//
//		for (int c=0;c<chrNames.length;c++) {
//
//			Feature [] existingFeatures = chromosomeFeatures.getFeatures(chrNames[c], newName);
//
//			Feature [] featuresToAdd = chromosomeFeatures.getFeatures(chrNames[c],oldName);
//
//			FeatureTypeCollection ftc = new FeatureTypeCollection(newName, chrNames[c]);
//
//			for (int f=0;f<existingFeatures.length;f++) {
//				ftc.addFeature(existingFeatures[f]);
//			}
//
//			for (int f=0;f<featuresToAdd.length;f++) {
//				featuresToAdd[f].setType(newName);
//				ftc.addFeature(featuresToAdd[f]);
//			}
//
//			chromosomeFeatures.chrFeatures.get(chrNames[c]).typeFeatures.remove(oldName);
//			chromosomeFeatures.chrFeatures.get(chrNames[c]).typeFeatures.remove(newName);
//			chromosomeFeatures.chrFeatures.get(chrNames[c]).typeFeatures.put(newName,ftc);
//
//			ftc.finalise();
//
//
//		}				
//
//		featureTypes.remove(oldName);
//		featureTypes.add(newName);
//
//	}
//	
//	/* (non-Javadoc)
//	 * @see java.lang.Object#toString()
//	 */
//	@Override
//	public String toString () {
//		return name();
//	}
//	
//	/**
//	 * Genome
//	 * 
//	 * @return The genome which underlies this annotation set
//	 */
//	public Genome genome () {
//		return genome;
//	}
//	
//	/**
//	 * Adds a feature. Note that this operation can fail if the
//	 * data in this set has been cached to disk.  This happens
//	 * when the set is first queried or when it is added to an
//	 * annotation collection.  In these cases an IllegalArgumentException
//	 * will be thrown.
//	 * 
//	 * @param f The feature to add.  
//	 * 
//	 */
//// this method from SeqMonk is temporarily merged with the original method addFeature provided in BamQC
////	public void addFeature (Feature f) {
////		chromosomeFeatures.addFeature(f);
////		if (!featureTypes.contains(f.type())) {
////			featureTypes.add(f.type());
////		}
////	}
//	
//	
//	
//		
//	/**
//	 * Gets all features for a given type on a given chromosome.
//	 * 
//	 * Features returned by this method are not guaranteed to be sorted.
//	 * 
//	 * @param chromosomeName The chromosome name
//	 * @param type The type of feature
//	 * @return A list of features of this type
//	 */
//	public Feature [] getFeaturesForType (String chromosomeName, String type) {
//		return chromosomeFeatures.getFeatures(chromosomeName, type);
//	}
//	
//	/**
//	 * Gets all features for a given type.
//	 * 
//	 * Features returned by this method are not guaranteed to be sorted.
//	 * 
//	 * @param type The type of feature
//	 * @return A list of features of this type
//	 */
//	public Feature [] getFeaturesForType (String type) {
//		return chromosomeFeatures.getFeatures(type);
//	}
//	
//	
//	/**
//	 * Gets a list of feature types for which this annotation set holds data
//	 * 
//	 * @return A list of feature types
//	 */
//	public String [] getAvailableFeatureTypes () {
//		return featureTypes.toArray(new String[0]);
//	}
//	
//	/**
//	 * Gets the all features.  WARNING: This method can have serious performance
//	 * effects since it loads all features into memory at the same time.  It is
//	 * nearly always better to iterate through the chromosomes one at a time to
//	 * load just a subset of features.  This method is retained for compatability
//	 * but may be removed in future to improve memory usage.
//	 * 
//	 * Features returned by this method are not guaranteed to be sorted.
//	 * 
//	 * @return All features in this set.
//	 */
//	public Feature [] getAllFeatures () {
//		
//		// TODO: Find a way to not load all features into memory just to do this.
//		
//		Vector<Feature>allFeatures = new Vector<Feature>();
//		
//		Enumeration<String> chrs = chromosomeFeatures.chromosomeNames();
//		
//		while (chrs.hasMoreElements()) {
//			String c = chrs.nextElement();
//
//			Iterator<String> types = featureTypes.iterator();
//			
//			while (types.hasNext()) {
//				Feature [] theseFeatures = chromosomeFeatures.getFeatures(c, types.next());
//				for (int i=0;i<theseFeatures.length;i++) {
//					allFeatures.add(theseFeatures[i]);
//				}
//			}
//			
//		}
//		
//		return allFeatures.toArray(new Feature[0]);
//	}
//	
//	/**
//	 * Optimises the data structure used to store features and caches annotation
//	 * to disk if this option is set in the preferences.  Once this method has
//	 * been called then no new features can be added to this set.  Calling this
//	 * method more than once will have no additional effect.
//	 */
//	public synchronized void finalise () {
//		
//		// This is called when we're added to a collection and lets us optimise
//		// storage and cache off unused data.  It should only be called once
//		// and we prevent the adding of more features once it's been called.
//		
//		if (finalised) return;
//		
//		finalised = true;
//		
//		chromosomeFeatures.finalise();
//		
//	}
//	
//
//	
// // TODO	
//	
//	/**
//	 * FeatureSet represents the different sets of feature types and chromosomes.
//	 */
//	protected class FeatureSet {
//		
//		private Hashtable<String, FeatureTypeFeatureSet> chrFeatures = new Hashtable<String, FeatureTypeFeatureSet>();
//	
//		/**
//		 * Adds a feature.
//		 * 
//		 * @param f The feature to add
//		 */
//		public void addFeature (Feature f) {
//			if (chrFeatures.containsKey(f.chromosome().name())) {
//				chrFeatures.get(f.chromosome().name()).addFeature(f);
//			}
//			else {
//				FeatureTypeFeatureSet t = new FeatureTypeFeatureSet();
//				t.addFeature(f);
//				chrFeatures.put(f.chromosome().name(), t);
//			}
//		}
//		
//		/**
//		 * Chromosome names.
//		 * 
//		 * @return An enumeration of all chromosome names for which we hold data
//		 */
//		public Enumeration<String>chromosomeNames () {
//			return chrFeatures.keys();
//		}
//		
//		/**
//		 * Gets a list of features.
//		 * 
//		 * @param chromsomeName The chromsome name
//		 * @param type The feature type
//		 * @return A list of features
//		 */
//		public Feature [] getFeatures (String chromsomeName, String type) {
//			if (chrFeatures.containsKey(chromsomeName)) {
//				return chrFeatures.get(chromsomeName).getFeatures(type);
//			}
//			return new Feature[0];
//		}
//		
//		/**
//		 * Gets a list of features.
//		 * 
//		 * @param type The feature type
//		 * @return A list of features
//		 */
//		public Feature [] getFeatures (String type) {
//			Vector<Feature> features = new Vector<Feature>();
//			Enumeration<String> e = chrFeatures.keys();
//			while (e.hasMoreElements()) {
//				Feature [] f = chrFeatures.get(e.nextElement()).getFeatures(type);
//				for (int i=0;i<f.length;i++) {
//					features.add(f[i]);
//				}
//			}
//			return features.toArray(new Feature[0]);
//		}
//		
//		protected FeatureTypeFeatureSet getFeatureTypeFeatureSet (String chr) {
//			if (!chrFeatures.containsKey(chr)) {
//				chrFeatures.put(chr,new FeatureTypeFeatureSet());
//			}
//			return chrFeatures.get(chr);
//		}
//		
//		/**
//		 * Optimises and caches all held data.
//		 */
//		public void finalise () {
//			// We're not optimising anything at this level, but we do need to tell
//			// the FeatureTypeFeatureSets about this.
//			
//			Iterator<FeatureTypeFeatureSet> i = chrFeatures.values().iterator();
//			while (i.hasNext()) {
//				i.next().finalise();
//			}
//		}
//		
//	}
//	
//	/**
//	 * FeatureTypeFeatureSet represents a set of features on the
//	 * same chromosome.
//	 */
//	protected class FeatureTypeFeatureSet {
//		
//		private Hashtable<String, FeatureTypeCollection> typeFeatures = new Hashtable<String, FeatureTypeCollection>();
//				
//		/**
//		 * Optimises and caches all held data
//		 */
//		public void finalise () {
//			// We need to tell the individual feature type collections to finalise
//			Iterator<FeatureTypeCollection>i = typeFeatures.values().iterator();
//			while (i.hasNext()) {
//				i.next().finalise();
//			}
//		}
//		
//		/**
//		 * Adds a feature.  No validation is done to ensure that the feature is 
//		 * on the correct chromosome at this level so if you mess this up it's your 
//		 * problem.
//		 * 
//		 * @param f The feature to add.
//		 */
//		public void addFeature (Feature f) {
//			if (finalised) {
//				throw new IllegalArgumentException("Can't add more features to a finalised annotation set");
//			}
//			if (typeFeatures.containsKey(f.type())) {
//				typeFeatures.get(f.type()).addFeature(f);
//			}
//			else {
//				FeatureTypeCollection c = new FeatureTypeCollection(f.type(),f.chromosome().name());
//				c.addFeature(f);
//				typeFeatures.put(f.type(), c);
//			}
//		}
//				
//		/**
//		 * Gets a list of features for a given type
//		 * 
//		 * @param type The feature type
//		 * @return A list of features
//		 */
//		public Feature [] getFeatures (String type) {
//			if (typeFeatures.containsKey(type)) {
//				return typeFeatures.get(type).getFeatures();
//			}
//			return new Feature[0];
//		}
//		
//		/**
//		 * For the reloading of pre-cached data we can bypass the normal
//		 * loading mechanism and input a cache file directly.  Should
//		 * only be used for the core annotation set and not to be used
//		 * lightly as this will break if you try to load features by the
//		 * conventional route after calling it this way.
//		 * 
//		 * @param type
//		 * @param col
//		 */
//		protected void addPreCachedFeatureTypeCollection (String type, FeatureTypeCollection col) {
//			if (!featureTypes.contains(type)) {
//				featureTypes.add(type);
//			}
//			typeFeatures.put(type,col);
//		}
//		
//		
//	}
//	
//	/**
//	 * FeatureTypeCollection represents a list of features of a given
//	 * type on the same chromosome.  It also provides the caching
//	 * mechanism for feature data.
//	 */
//	protected class FeatureTypeCollection implements Runnable {
//		
//		private LinkedList<Feature> buildFeatures = new LinkedList<Feature>();
//		private Feature [] featureList = null;
//		private File cacheFile = null;
//		private String featureType;
//		private String chromosome;
//		
//		public FeatureTypeCollection (String featureType, String chromosome) {
//			this.featureType = featureType;
//			this.chromosome = chromosome;
//		}
//		/**
//		 * This constructor is a shortcut where there is a pre-cached
//		 * file which we can reuse.  Since this is somewhat fragile it
//		 * should be used with caution.
//		 * 
//		 * @param cacheFile
//		 */
//		public FeatureTypeCollection (String featureType, String chromosome, File cacheFile) {
//			this.featureType = featureType;
//			this.chromosome = chromosome;
//			this.cacheFile = cacheFile;
//			buildFeatures = null;
//		}
//		
//		/**
//		 * Adds a feature.
//		 * 
//		 * @param f The feature to add
//		 */
//		public void addFeature (Feature f) {
//			if (buildFeatures == null) {
//				throw new IllegalArgumentException("Can't add data to a finalsed type collection");
//			}
//			buildFeatures.add(f);
//		}
//		
//		
//		/**
//		 * Optimises and caches feature data.
//		 */
//		public void finalise () {
//			if (buildFeatures == null || cacheFile != null) {
////				System.err.println("This already appears to be finalised");
//				return;
//			}
//			featureList = buildFeatures.toArray(new Feature[0]);
//			buildFeatures.clear();
//			buildFeatures = null;
//			
//			// If this is a core annotation set then we want to maintain a set of 
//			// cache files for future use so we write them where we can get back
//			// to them in future.  We also don't optimise for size since we need
//			// to keep them all.
//			if (AnnotationSet.this instanceof CoreAnnotationSet) {
//				try {
//					File cacheBase = new File(BamQCPreferences.getInstance().getGenomeBase()+"/"+genome.species()+"/"+genome.assembly()+"/cache");
//					if (! cacheBase.exists()) {
//						if (!cacheBase.mkdir()) {
//							throw new IOException("Can't create cache file for core annotation set");
//						}
//					}
//					
//					cacheFile = new File(cacheBase.getAbsoluteFile()+"/"+chromosome+"%"+featureType+".cache");
//					ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(cacheFile)));
//					oos.writeObject(featureList);
//					oos.close();
//					featureList = null;
//					
//					
//				}
//				catch (IOException ioe) {
//					ioe.printStackTrace();
//				}
//			}
//
//			// If this isn't core annotation then we cache this to the normal
//			// cache directory and set a shutdown hook to delete it at the end
//			// of the session.
//			else {
//				if (featureList.length > 500) {
//					try {
//						cacheFile = File.createTempFile("bamqc_annotation", ".temp", BamQCPreferences.getInstance().tempDirectory());
//						ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(cacheFile)));
//						oos.writeObject(featureList);
//						oos.close();
//						featureList = null;
//						Runtime.getRuntime().addShutdownHook(new Thread(this));
//					}
//					catch (IOException ioe) {
//						ioe.printStackTrace();
//					}
//				}
//			}
//		}
//		
//		/* (non-Javadoc)
//		 * @see java.lang.Runnable#run()
//		 */
//		@Override
//		public void run () {
//			// Clean up any temp files.
//			if (cacheFile != null) {
//				cacheFile.delete();
//			}
//		}
//		
//		
//		/**
//		 * Gets a list of features.
//		 * 
//		 * @return The list of stored features.
//		 */
//		public Feature [] getFeatures () {
//			if (buildFeatures != null) {
//				finalise();
//			}
//			if (cacheFile != null) {
//				BamQCApplication.getInstance().cacheUsed();
//				try {
//					ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(cacheFile)));
//					Feature [] returnedFeatures = (Feature [])ois.readObject();
//					ois.close();
//					return returnedFeatures;
//				}
//				catch (Exception e) {
//					e.printStackTrace();
//				}
//			}
//			return featureList;
//		}
//	}
	
		
}






























// ORIGINAL CODE
// THE ABOVE CODE WAS MODIFIED FOR INTEGRATING the GENOME CLASS
//
//
//
///**
// * Copyright Copyright 2014 Simon Andrews
// *
// *    This file is part of BamQC.
// *
// *    BamQC is free software; you can redistribute it and/or modify
// *    it under the terms of the GNU General Public License as published by
// *    the Free Software Foundation; either version 3 of the License, or
// *    (at your option) any later version.
// *
// *    BamQC is distributed in the hope that it will be useful,
// *    but WITHOUT ANY WARRANTY; without even the implied warranty of
// *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// *    GNU General Public License for more details.
// *
// *    You should have received a copy of the GNU General Public License
// *    along with BamQC; if not, write to the Free Software
// *    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
// */
//
//package uk.ac.babraham.BamQC.DataTypes.Genome;
//
//import java.io.BufferedInputStream;
//import java.io.BufferedOutputStream;
//import java.io.File;
//import java.io.FileInputStream;
//import java.io.FileOutputStream;
//import java.io.IOException;
//import java.io.ObjectInputStream;
//import java.io.ObjectOutputStream;
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.Enumeration;
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.Hashtable;
//import java.util.Iterator;
//import java.util.LinkedList;
//import java.util.List;
//import java.util.Vector;
//
//import uk.ac.babraham.BamQC.BamQCApplication;
//import uk.ac.babraham.BamQC.Modules.ModuleConfig;
//import uk.ac.babraham.BamQC.Preferences.BamQCPreferences;
//import net.sf.samtools.SAMRecord;
//
//public class AnnotationSet {
//	
//	
//	/** The reference file for this annotation set */
//	private File file = null;
//	
//	private ChromosomeFactory factory = new ChromosomeFactory();
//	
//	private HashMap<String, FeatureClass> features = new HashMap<String, FeatureClass>();
//	
//	private FeatureClass [] featureArray = null;
//	
//	private final int cacheSize = ModuleConfig.getParam("AnnotationSet_annotation_cache_size", "ignore").intValue();
//	private List<ShortRead> readCache = new ArrayList<ShortRead>(cacheSize);
//
//	// imported from SeqMonk
//	protected Genome genome;
//	private String name;
//	private boolean finalised = false;
//	/*
//	 * We store features by chromosome.  Within each chromosome we store
//	 * by feature type for quick access and then within that we store
//	 * a vector of features.
//	 */
//	protected FeatureSet chromosomeFeatures = new FeatureSet();
//	private HashSet<String> featureTypes = new HashSet<String>();
//	// end imported from SeqMonk
//	
//	
//	
//	
//	
//	public File getFile() {
//		return file;
//	}
//
//	public void setFile(File file) {
//		this.file = file;
//	}
//
//	public ChromosomeFactory chromosomeFactory () {
//		return factory;
//	}
//	
//	public void addFeature (Feature f) {
//		// in the future, we need to find a way to merge: 
//		// BamQC: FeatureClass -> FeatureSubclass (external classes)
//		// SeqMonk: FeatureSet:FeatureTypeFeatureSet:FeatureTypeCollection (inner classes)
//		
//		// Original addFeature from BamQC
//		if (!features.containsKey(f.type())) {
//			features.put(f.type(), new FeatureClass(this));
//		}	
//		features.get(f.type()).addFeature(f);
//
//		
//		// addFeature from SeqMonk
//		chromosomeFeatures.addFeature(f);
//		if (!featureTypes.contains(f.type())) {
//			featureTypes.add(f.type());
//		}
//	}
//	
//	public boolean hasFeatures () {
//		return !features.isEmpty();
//	}
//	
//	public String [] listFeatureTypes () {
//		return features.keySet().toArray(new String [0]);
//	}
//	
//	public FeatureClass getFeatureClassForType (String type) {
//		return features.get(type);
//	}
//	
//	
//	public void processSequenceNoCache(SAMRecord r) {
//		// implementation using ShortRead
//		processCachedSequence(new ShortRead(r.getReferenceName(), r.getAlignmentStart(), r.getAlignmentEnd()));
//	}
//	
//	
//	public void processSequence (SAMRecord r) {
//			
//		// implementation using ShortRead
//	    if(readCache.size() < cacheSize) {
//	    	readCache.add(new ShortRead(r.getReferenceName(), r.getAlignmentStart(), r.getAlignmentEnd()));
//	    } else {
//	    	// sort the cache
//	    	Collections.sort(readCache);
//	    	// now parse the sorted cache
//	    	for(int i=0; i < cacheSize; i++) {
//	    		processCachedSequence(readCache.get(i));
//	    	}
//	    	// let's clear and reuse the array for now, instead of reallocating a new one every time.
//	    	// Tricky to say what's the best is.. an O(n)remove vs allocation+GC ... 
//	    	readCache.clear();
//	    }       
//	}
//	
//	private void processCachedSequence(ShortRead r) {	
//		if (!r.getReferenceName().equals("*")) {
//			Chromosome c = factory.getChromosome(r.getReferenceName());
//			c.processSequence(r);
//		}
//		if (featureArray == null) {
//			featureArray = features.values().toArray(new FeatureClass[0]);
//		}
//		for (int i=0;i<featureArray.length;i++) {
//			featureArray[i].processSequence(r);
//		}
//	}	
//	
//
//	
//	
//	
//	// imported from SeqMonk
//
//	public AnnotationSet() { }
//
//	
//	/**
//	 * Instantiates a new annotation set.
//	 * 
//	 * @param genome The base genome which this annotation applies to
//	 */
//	public AnnotationSet (Genome genome) {
//		this.genome = genome;
//	}
//	
//	/**
//	 * Instantiates a new annotation set.
//	 * 
//	 * @param genome The base genome which this annotation applies to
//	 * @param name The name for the set (file name, URL etc)
//	 */
//	public AnnotationSet (Genome genome, String name) {
//		this.genome = genome;
//		this.name = name;		
//	}
//
//	/**
//	 * Provides an enumeration to iterate through all chromosomes which have
//	 * data in this set.  This is because we don't want to have to return
//	 * all features in this set at the same time due to memory constraints.
//	 * Therefore using this iterator is the recommended way to get hold of
//	 * all features in the set.
//	 * 
//	 * @return An enumeration of all chromosome names which have data in this set.
//	 */
//	public Enumeration<String> chromosomes () {
//		return chromosomeFeatures.chromosomeNames();
//	}
//	
//	/**
//	 * Checks if this set holds any features of the given type
//	 * 
//	 * @param type The annotation type to check
//	 * @return true, if this set has features for that type
//	 */
//	public boolean hasDataForType (String type) {
//		return featureTypes.contains(type);
//	}
//	
//	/**
//	 * This is used to clean up the set when it is being removed.
//	 */
//	public void delete () {
//		chromosomeFeatures = null;
//		if(featureTypes != null) {
//			featureTypes.clear();
//		}
//	}
//	
//	/**
//	 * The name of this annotation set
//	 * 
//	 * @return The annotation set name
//	 */
//	public String name () {
//		return name;
//	}
//	
//	/**
//	 * Sets the annotation set name.
//	 * 
//	 * @param name The new name for this set.
//	 */
//	public void setName (String name) {
//		this.name = name;
//	}
//	
//	/**
//	 * Renames one class of features to a different name
//	 * 
//	 * @param oldName
//	 * @param newName
//	 */
//	public void renameFeatures (String oldName, String newName) {
//
//		String [] chrNames = chromosomeFeatures.chrFeatures.keySet().toArray(new String [0]);
//
//		for (int c=0;c<chrNames.length;c++) {
//
//			Feature [] existingFeatures = chromosomeFeatures.getFeatures(chrNames[c], newName);
//
//			Feature [] featuresToAdd = chromosomeFeatures.getFeatures(chrNames[c],oldName);
//
//			FeatureTypeCollection ftc = new FeatureTypeCollection(newName, chrNames[c]);
//
//			for (int f=0;f<existingFeatures.length;f++) {
//				ftc.addFeature(existingFeatures[f]);
//			}
//
//			for (int f=0;f<featuresToAdd.length;f++) {
//				featuresToAdd[f].setType(newName);
//				ftc.addFeature(featuresToAdd[f]);
//			}
//
//			chromosomeFeatures.chrFeatures.get(chrNames[c]).typeFeatures.remove(oldName);
//			chromosomeFeatures.chrFeatures.get(chrNames[c]).typeFeatures.remove(newName);
//			chromosomeFeatures.chrFeatures.get(chrNames[c]).typeFeatures.put(newName,ftc);
//
//			ftc.finalise();
//
//
//		}				
//
//		featureTypes.remove(oldName);
//		featureTypes.add(newName);
//
//	}
//	
//	/* (non-Javadoc)
//	 * @see java.lang.Object#toString()
//	 */
//	@Override
//	public String toString () {
//		return name();
//	}
//	
//	/**
//	 * Genome
//	 * 
//	 * @return The genome which underlies this annotation set
//	 */
//	public Genome genome () {
//		return genome;
//	}
//	
//	/**
//	 * Adds a feature. Note that this operation can fail if the
//	 * data in this set has been cached to disk.  This happens
//	 * when the set is first queried or when it is added to an
//	 * annotation collection.  In these cases an IllegalArgumentException
//	 * will be thrown.
//	 * 
//	 * @param f The feature to add.  
//	 * 
//	 */
//// this method from SeqMonk is temporarily merged with the original method addFeature provided in BamQC
////	public void addFeature (Feature f) {
////		chromosomeFeatures.addFeature(f);
////		if (!featureTypes.contains(f.type())) {
////			featureTypes.add(f.type());
////		}
////	}
//	
//	
//	
//		
//	/**
//	 * Gets all features for a given type on a given chromosome.
//	 * 
//	 * Features returned by this method are not guaranteed to be sorted.
//	 * 
//	 * @param chromosomeName The chromosome name
//	 * @param type The type of feature
//	 * @return A list of features of this type
//	 */
//	public Feature [] getFeaturesForType (String chromosomeName, String type) {
//		return chromosomeFeatures.getFeatures(chromosomeName, type);
//	}
//	
//	/**
//	 * Gets all features for a given type.
//	 * 
//	 * Features returned by this method are not guaranteed to be sorted.
//	 * 
//	 * @param type The type of feature
//	 * @return A list of features of this type
//	 */
//	public Feature [] getFeaturesForType (String type) {
//		return chromosomeFeatures.getFeatures(type);
//	}
//	
//	
//	/**
//	 * Gets a list of feature types for which this annotation set holds data
//	 * 
//	 * @return A list of feature types
//	 */
//	public String [] getAvailableFeatureTypes () {
//		return featureTypes.toArray(new String[0]);
//	}
//	
//	/**
//	 * Gets the all features.  WARNING: This method can have serious performance
//	 * effects since it loads all features into memory at the same time.  It is
//	 * nearly always better to iterate through the chromosomes one at a time to
//	 * load just a subset of features.  This method is retained for compatability
//	 * but may be removed in future to improve memory usage.
//	 * 
//	 * Features returned by this method are not guaranteed to be sorted.
//	 * 
//	 * @return All features in this set.
//	 */
//	public Feature [] getAllFeatures () {
//		
//		// TODO: Find a way to not load all features into memory just to do this.
//		
//		Vector<Feature>allFeatures = new Vector<Feature>();
//		
//		Enumeration<String> chrs = chromosomeFeatures.chromosomeNames();
//		
//		while (chrs.hasMoreElements()) {
//			String c = chrs.nextElement();
//
//			Iterator<String> types = featureTypes.iterator();
//			
//			while (types.hasNext()) {
//				Feature [] theseFeatures = chromosomeFeatures.getFeatures(c, types.next());
//				for (int i=0;i<theseFeatures.length;i++) {
//					allFeatures.add(theseFeatures[i]);
//				}
//			}
//			
//		}
//		
//		return allFeatures.toArray(new Feature[0]);
//	}
//	
//	/**
//	 * Optimises the data structure used to store features and caches annotation
//	 * to disk if this option is set in the preferences.  Once this method has
//	 * been called then no new features can be added to this set.  Calling this
//	 * method more than once will have no additional effect.
//	 */
//	public synchronized void finalise () {
//		
//		// This is called when we're added to a collection and lets us optimise
//		// storage and cache off unused data.  It should only be called once
//		// and we prevent the adding of more features once it's been called.
//		
//		if (finalised) return;
//		
//		finalised = true;
//		
//		chromosomeFeatures.finalise();
//		
//	}
//	
//	
//	/**
//	 * FeatureSet represents the different sets of feature types and chromosomes.
//	 */
//	protected class FeatureSet {
//		
//		private Hashtable<String, FeatureTypeFeatureSet> chrFeatures = new Hashtable<String, FeatureTypeFeatureSet>();
//	
//		/**
//		 * Adds a feature.
//		 * 
//		 * @param f The feature to add
//		 */
//		public void addFeature (Feature f) {
//			if (chrFeatures.containsKey(f.chromosome().name())) {
//				chrFeatures.get(f.chromosome().name()).addFeature(f);
//			}
//			else {
//				FeatureTypeFeatureSet t = new FeatureTypeFeatureSet();
//				t.addFeature(f);
//				chrFeatures.put(f.chromosome().name(), t);
//			}
//		}
//		
//		/**
//		 * Chromosome names.
//		 * 
//		 * @return An enumeration of all chromosome names for which we hold data
//		 */
//		public Enumeration<String>chromosomeNames () {
//			return chrFeatures.keys();
//		}
//		
//		/**
//		 * Gets a list of features.
//		 * 
//		 * @param chromsomeName The chromsome name
//		 * @param type The feature type
//		 * @return A list of features
//		 */
//		public Feature [] getFeatures (String chromsomeName, String type) {
//			if (chrFeatures.containsKey(chromsomeName)) {
//				return chrFeatures.get(chromsomeName).getFeatures(type);
//			}
//			return new Feature[0];
//		}
//		
//		/**
//		 * Gets a list of features.
//		 * 
//		 * @param type The feature type
//		 * @return A list of features
//		 */
//		public Feature [] getFeatures (String type) {
//			Vector<Feature> features = new Vector<Feature>();
//			Enumeration<String> e = chrFeatures.keys();
//			while (e.hasMoreElements()) {
//				Feature [] f = chrFeatures.get(e.nextElement()).getFeatures(type);
//				for (int i=0;i<f.length;i++) {
//					features.add(f[i]);
//				}
//			}
//			return features.toArray(new Feature[0]);
//		}
//		
//		protected FeatureTypeFeatureSet getFeatureTypeFeatureSet (String chr) {
//			if (!chrFeatures.containsKey(chr)) {
//				chrFeatures.put(chr,new FeatureTypeFeatureSet());
//			}
//			return chrFeatures.get(chr);
//		}
//		
//		/**
//		 * Optimises and caches all held data.
//		 */
//		public void finalise () {
//			// We're not optimising anything at this level, but we do need to tell
//			// the FeatureTypeFeatureSets about this.
//			
//			Iterator<FeatureTypeFeatureSet> i = chrFeatures.values().iterator();
//			while (i.hasNext()) {
//				i.next().finalise();
//			}
//		}
//		
//	}
//	
//	/**
//	 * FeatureTypeFeatureSet represents a set of features on the
//	 * same chromosome.
//	 */
//	protected class FeatureTypeFeatureSet {
//		
//		private Hashtable<String, FeatureTypeCollection> typeFeatures = new Hashtable<String, FeatureTypeCollection>();
//				
//		/**
//		 * Optimises and caches all held data
//		 */
//		public void finalise () {
//			// We need to tell the individual feature type collections to finalise
//			Iterator<FeatureTypeCollection>i = typeFeatures.values().iterator();
//			while (i.hasNext()) {
//				i.next().finalise();
//			}
//		}
//		
//		/**
//		 * Adds a feature.  No validation is done to ensure that the feature is 
//		 * on the correct chromosome at this level so if you mess this up it's your 
//		 * problem.
//		 * 
//		 * @param f The feature to add.
//		 */
//		public void addFeature (Feature f) {
//			if (finalised) {
//				throw new IllegalArgumentException("Can't add more features to a finalised annotation set");
//			}
//			if (typeFeatures.containsKey(f.type())) {
//				typeFeatures.get(f.type()).addFeature(f);
//			}
//			else {
//				FeatureTypeCollection c = new FeatureTypeCollection(f.type(),f.chromosome().name());
//				c.addFeature(f);
//				typeFeatures.put(f.type(), c);
//			}
//		}
//				
//		/**
//		 * Gets a list of features for a given type
//		 * 
//		 * @param type The feature type
//		 * @return A list of features
//		 */
//		public Feature [] getFeatures (String type) {
//			if (typeFeatures.containsKey(type)) {
//				return typeFeatures.get(type).getFeatures();
//			}
//			return new Feature[0];
//		}
//		
//		/**
//		 * For the reloading of pre-cached data we can bypass the normal
//		 * loading mechanism and input a cache file directly.  Should
//		 * only be used for the core annotation set and not to be used
//		 * lightly as this will break if you try to load features by the
//		 * conventional route after calling it this way.
//		 * 
//		 * @param type
//		 * @param col
//		 */
//		protected void addPreCachedFeatureTypeCollection (String type, FeatureTypeCollection col) {
//			if (!featureTypes.contains(type)) {
//				featureTypes.add(type);
//			}
//			typeFeatures.put(type,col);
//		}
//		
//		
//	}
//	
//	/**
//	 * FeatureTypeCollection represents a list of features of a given
//	 * type on the same chromosome.  It also provides the caching
//	 * mechanism for feature data.
//	 */
//	protected class FeatureTypeCollection implements Runnable {
//		
//		private LinkedList<Feature> buildFeatures = new LinkedList<Feature>();
//		private Feature [] featureList = null;
//		private File cacheFile = null;
//		private String featureType;
//		private String chromosome;
//		
//		public FeatureTypeCollection (String featureType, String chromosome) {
//			this.featureType = featureType;
//			this.chromosome = chromosome;
//		}
//		/**
//		 * This constructor is a shortcut where there is a pre-cached
//		 * file which we can reuse.  Since this is somewhat fragile it
//		 * should be used with caution.
//		 * 
//		 * @param cacheFile
//		 */
//		public FeatureTypeCollection (String featureType, String chromosome, File cacheFile) {
//			this.featureType = featureType;
//			this.chromosome = chromosome;
//			this.cacheFile = cacheFile;
//			buildFeatures = null;
//		}
//		
//		/**
//		 * Adds a feature.
//		 * 
//		 * @param f The feature to add
//		 */
//		public void addFeature (Feature f) {
//			if (buildFeatures == null) {
//				throw new IllegalArgumentException("Can't add data to a finalsed type collection");
//			}
//			buildFeatures.add(f);
//		}
//		
//		
//		/**
//		 * Optimises and caches feature data.
//		 */
//		public void finalise () {
//			if (buildFeatures == null || cacheFile != null) {
////				System.err.println("This already appears to be finalised");
//				return;
//			}
//			featureList = buildFeatures.toArray(new Feature[0]);
//			buildFeatures.clear();
//			buildFeatures = null;
//			
//			// If this is a core annotation set then we want to maintain a set of 
//			// cache files for future use so we write them where we can get back
//			// to them in future.  We also don't optimise for size since we need
//			// to keep them all.
//			if (AnnotationSet.this instanceof CoreAnnotationSet) {
//				try {
//					File cacheBase = new File(BamQCPreferences.getInstance().getGenomeBase()+"/"+genome.species()+"/"+genome.assembly()+"/cache");
//					if (! cacheBase.exists()) {
//						if (!cacheBase.mkdir()) {
//							throw new IOException("Can't create cache file for core annotation set");
//						}
//					}
//					
//					cacheFile = new File(cacheBase.getAbsoluteFile()+"/"+chromosome+"%"+featureType+".cache");
//					ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(cacheFile)));
//					oos.writeObject(featureList);
//					oos.close();
//					featureList = null;
//					
//					
//				}
//				catch (IOException ioe) {
//					ioe.printStackTrace();
//				}
//			}
//
//			// If this isn't core annotation then we cache this to the normal
//			// cache directory and set a shutdown hook to delete it at the end
//			// of the session.
//			else {
//				if (featureList.length > 500) {
//					try {
//						cacheFile = File.createTempFile("bamqc_annotation", ".temp", BamQCPreferences.getInstance().tempDirectory());
//						ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(cacheFile)));
//						oos.writeObject(featureList);
//						oos.close();
//						featureList = null;
//						Runtime.getRuntime().addShutdownHook(new Thread(this));
//					}
//					catch (IOException ioe) {
//						ioe.printStackTrace();
//					}
//				}
//			}
//		}
//		
//		/* (non-Javadoc)
//		 * @see java.lang.Runnable#run()
//		 */
//		@Override
//		public void run () {
//			// Clean up any temp files.
//			if (cacheFile != null) {
//				cacheFile.delete();
//			}
//		}
//		
//		
//		/**
//		 * Gets a list of features.
//		 * 
//		 * @return The list of stored features.
//		 */
//		public Feature [] getFeatures () {
//			if (buildFeatures != null) {
//				finalise();
//			}
//			if (cacheFile != null) {
//				BamQCApplication.getInstance().cacheUsed();
//				try {
//					ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(cacheFile)));
//					Feature [] returnedFeatures = (Feature [])ois.readObject();
//					ois.close();
//					return returnedFeatures;
//				}
//				catch (Exception e) {
//					e.printStackTrace();
//				}
//			}
//			return featureList;
//		}
//	}
//	
//		
//}
//
