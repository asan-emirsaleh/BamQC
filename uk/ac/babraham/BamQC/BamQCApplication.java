/**
 * Copyright Copyright 2010-14 Simon Andrews
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
package uk.ac.babraham.BamQC;

import java.awt.BorderLayout;
import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;
import javax.swing.filechooser.FileFilter;

import sun.net.ProgressListener;
import uk.ac.babraham.BamQC.Analysis.AnalysisRunner;
import uk.ac.babraham.BamQC.Analysis.OfflineRunner;
import uk.ac.babraham.BamQC.Dialogs.WelcomePanel;
import uk.ac.babraham.BamQC.FileFilters.BAMFileFilter;
import uk.ac.babraham.BamQC.FileFilters.GFFFileFilter;
import uk.ac.babraham.BamQC.Modules.ModuleFactory;
import uk.ac.babraham.BamQC.Modules.QCModule;
import uk.ac.babraham.BamQC.Report.HTMLReportArchive;
import uk.ac.babraham.BamQC.Results.ResultsPanel;
import uk.ac.babraham.BamQC.Sequence.SequenceFactory;
import uk.ac.babraham.BamQC.Sequence.SequenceFile;
import uk.ac.babraham.BamQC.Sequence.SequenceFormatException;
import uk.ac.babraham.BamQC.AnnotationParsers.GenomeParser;
import uk.ac.babraham.BamQC.DataTypes.Genome.AnnotationCollectionListener;
import uk.ac.babraham.BamQC.DataTypes.Genome.Genome;
import uk.ac.babraham.BamQC.Dialogs.ProgressDialog;
import uk.ac.babraham.BamQC.Network.GenomeDownloader;
import uk.ac.babraham.BamQC.DataTypes.CacheListener;



public class BamQCApplication extends JFrame { //implements ProgressListener, AnnotationCollectionListener {	
	
	private static BamQCApplication application;
	
	private static final long serialVersionUID = -1761781589885333860L;

	public static final String VERSION = "0.1.0_devel";
	
	private JTabbedPane fileTabs;
	private WelcomePanel welcomePanel;
	private File lastUsedDir = null;
	
	
	/** The cache listeners */
	private Vector<CacheListener> cacheListeners = new Vector<CacheListener>();
	
	
	public BamQCApplication () {
			setTitle("BamQC");
			setIconImage(new ImageIcon(ClassLoader.getSystemResource("uk/ac/babraham/BamQC/Resources/bamqc_icon.png")).getImage());
			setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	//		setSize(1280, 720);
			setSize(800,600);
			setLocationRelativeTo(null);
			
			welcomePanel = new WelcomePanel();
			
			fileTabs = new JTabbedPane(JTabbedPane.TOP);
			setContentPane(welcomePanel);
			
			setJMenuBar(new BamQCMenuBar(this));
			
		}

	public void close () {
		if (fileTabs.getSelectedIndex() >=0) {
			fileTabs.remove(fileTabs.getSelectedIndex());
		}
		if (fileTabs.getTabCount() == 0) {
			setContentPane(welcomePanel);
			validate();
			repaint();
		}
	}
	
	public void closeAll () {
		fileTabs.removeAll();
		setContentPane(welcomePanel);
		validate();
		repaint();
	}
	
	public void openFile () {
		JFileChooser chooser;
		
		if (lastUsedDir == null) {
			chooser = new JFileChooser();
		}
		else {
			chooser = new JFileChooser(lastUsedDir);
		}
		chooser.setMultiSelectionEnabled(true);
		BAMFileFilter bff = new BAMFileFilter();
		chooser.addChoosableFileFilter(bff);
		chooser.setFileFilter(bff);
		int result = chooser.showOpenDialog(this);
		if (result == JFileChooser.CANCEL_OPTION) return;
	
		// See if they forced a file format
		FileFilter chosenFilter = chooser.getFileFilter();
		if (chosenFilter instanceof BAMFileFilter) {
			System.setProperty("bamqc.sequence_format", "bam");
		}
		
		// If we're still showing the welcome panel switch this out for
		// the file tabs panel
		if (fileTabs.getTabCount() == 0) {
			setContentPane(fileTabs);
			validate();
			repaint();
		}
		
		File [] files = chooser.getSelectedFiles();		
			
		for (int i=0;i<files.length;i++) {
			lastUsedDir = files[i].getParentFile();
			SequenceFile sequenceFile;
			
			
			try {
				sequenceFile = SequenceFactory.getSequenceFile(files[i]);
			}
			catch (SequenceFormatException e) {
				JPanel errorPanel = new JPanel();
				errorPanel.setLayout(new BorderLayout());
				errorPanel.add(new JLabel("File format error: "+e.getLocalizedMessage(), JLabel.CENTER),BorderLayout.CENTER);
				fileTabs.addTab(files[i].getName(), errorPanel);
				e.printStackTrace();
				continue;
			}
			catch (IOException e) {
				System.err.println("File broken");
				e.printStackTrace();
				JOptionPane.showMessageDialog(this, "Couldn't read file:"+e.getLocalizedMessage(), "Error reading file", JOptionPane.ERROR_MESSAGE);
				continue;
			}
					
			AnalysisRunner runner = new AnalysisRunner(sequenceFile);
			ResultsPanel rp = new ResultsPanel(sequenceFile);
			runner.addAnalysisListener(rp);
			fileTabs.addTab(sequenceFile.name(), rp);
			

			QCModule [] module_list = ModuleFactory.getStandardModuleList();
	
			runner.startAnalysis(module_list);
		}
	}
	
	public void openGFF () {
		JFileChooser chooser;
		
		if (lastUsedDir == null) {
			chooser = new JFileChooser();
		}
		else {
			chooser = new JFileChooser(lastUsedDir);
		}
		chooser.setMultiSelectionEnabled(false);
		GFFFileFilter gff = new GFFFileFilter();
		chooser.addChoosableFileFilter(gff);
		chooser.setFileFilter(gff);
		int result = chooser.showOpenDialog(this);
		if (result == JFileChooser.CANCEL_OPTION) return;
	
		
		File gff_file = chooser.getSelectedFile();

		if (!(gff_file.exists() && gff_file.canRead())) {
			JOptionPane.showMessageDialog(this, "GFF file "+gff_file+" doesn't exist or can't be read", "Invalid GFF file", JOptionPane.ERROR_MESSAGE);
		}
		else {
			BamQCConfig.getInstance().gff_file = gff_file;
		}
	}


	public void saveReport () {
		JFileChooser chooser;
		
		if (lastUsedDir == null) {
			chooser = new JFileChooser();
		}
		else {
			chooser = new JFileChooser(lastUsedDir);
		}
		
		if (fileTabs.getSelectedComponent() == null) {
			JOptionPane.showMessageDialog(this, "No SAM/BAM files are open yet", "Can't save report", JOptionPane.ERROR_MESSAGE);
			return;
		}
		chooser.setSelectedFile(new File(((ResultsPanel)fileTabs.getSelectedComponent()).sequenceFile().getFile().getName().replaceAll(".gz$","").replaceAll(".bz2$","").replaceAll(".txt$","").replaceAll(".fastq$", "").replaceAll(".fq$", "").replaceAll(".sam$", "").replaceAll(".bam$", "")+"_bamqc.html"));
		chooser.setMultiSelectionEnabled(false);
		chooser.setFileFilter(new FileFilter() {
		
			@Override
			public String getDescription() {
				return "HTML files";
			}
		
			@Override
			public boolean accept(File f) {
				if (f.isDirectory() || f.getName().toLowerCase().endsWith(".html")) {
					return true;
				}
				return false;
			}
		
		});
	
		File reportFile;
		while (true) {
			int result = chooser.showSaveDialog(this);
			if (result == JFileChooser.CANCEL_OPTION) return;
			
			reportFile = chooser.getSelectedFile();
			if (! reportFile.getName().toLowerCase().endsWith(".html")) {
				reportFile = new File(reportFile.getAbsoluteFile()+".html");
			}
			
			// Check if we're overwriting something
			if (reportFile.exists()) {
				int reply = JOptionPane.showConfirmDialog(this, reportFile.getName()+" already exists.  Overwrite?", "Overwrite existing file?", JOptionPane.YES_NO_OPTION);
				if (reply == JOptionPane.NO_OPTION) {
					continue;
				}
				break;
			}
			break;
		}
		
		
		ResultsPanel selectedPanel = (ResultsPanel)fileTabs.getSelectedComponent();
		
		try {
			new HTMLReportArchive(selectedPanel.sequenceFile(), selectedPanel.modules(), reportFile);
		} 
		catch (Exception e) {
			JOptionPane.showMessageDialog(this, "Failed to create archive: "+e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}
	}
	
	
	
	
	
	
	
	
	
	
	// TODO code extracted from BamQC and then edited.

	/**
	 * Notifies all listeners that the disk cache was used.
	 */
	public void cacheUsed () {
		Enumeration<CacheListener>en = cacheListeners.elements();
		while (en.hasMoreElements()) {
			en.nextElement().cacheUsed();
		}
	}
	
	/**
	 * This method is usually called from data gathered by the genome selector
	 * which will provide the required values for the assembly name.  This does
	 * not actually load the specified genome, but just downloads it from the
	 * online genome repository.
	 * 
	 * @param species Species name
	 * @param assembly Assembly name
	 * @param size The size of the compressed genome file in bytes
	 */
	public void downloadGenome (String species, String assembly, int size) {
		GenomeDownloader d = new GenomeDownloader();
		//d.addProgressListener(this);
		ProgressDialog pd = new ProgressDialog(this,"Downloading genome...");
		d.addProgressListener(pd);
		d.downloadGenome(species,assembly,size,true);
		pd.requestFocus();
	}
	
	/**
	 * Loads a genome assembly.  This will fail if the genome isn't currently
	 * in the local cache and downloadGenome should be set first in this case.
	 * 
	 * @param baseLocation The folder containing the requested genome.
	 */
	public void loadGenome (File baseLocation) {
		GenomeParser parser = new GenomeParser();
		ProgressDialog pd = new ProgressDialog(this,"Loading genome...");
		parser.addProgressListener(pd);
		//parser.addProgressListener(this);
		parser.parseGenome(baseLocation);
		pd.requestFocus();
	}
	
	// End of the GenomeProgressListener methods. (code from SeqMonk)

	
	
	
	
	
	
	
	
	
	
	
	/**
	 * Provides a static way to access the main instance of the SeqMonk
	 * Application so we don't need to keep passing references around
	 * through obscure paths.
	 * 
	 * @return The currently running application instance.
	 */
	
	public static BamQCApplication getInstance () {
		return application;
	}
	
	

	public static void main(String[] args) {
		
		// See if we just have to print out the version
		if (System.getProperty("bamqc.show_version") != null && System.getProperty("bamqc.show_version").equals("true")) {
			System.out.println("BamQC v"+VERSION);
			System.exit(0);
		}
		
		if (args.length > 0) {
			// Set headless to true so we don't get problems
			// with people working without an X display.
			System.setProperty("java.awt.headless", "true");
			
			// We used to default to unzipping the zip file in 
			// non-interactive runs.  As we now save an HTML
			// report at the top level we no longer do this
			// so unzip is false unless explicitly set to be true.
						
			if (BamQCConfig.getInstance().do_unzip == null) {
				BamQCConfig.getInstance().do_unzip = false;
			}
			
			new OfflineRunner(args);
			System.exit(0);
		}
		
		else {
			try {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			} catch (Exception e) {}
			
	
			// The interactive default is to not uncompress the
			// reports after they have been generated
			if (BamQCConfig.getInstance().do_unzip == null) {
				BamQCConfig.getInstance().do_unzip = false;
			}
	
			application = new BamQCApplication();
	
			application.setVisible(true);
		}
	}	

}
