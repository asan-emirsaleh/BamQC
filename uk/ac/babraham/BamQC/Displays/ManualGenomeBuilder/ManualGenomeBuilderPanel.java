/**
 * Copyright 2013-15 Simon Andrews
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
package uk.ac.babraham.BamQC.Displays.ManualGenomeBuilder;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Vector;
import java.util.zip.GZIPInputStream;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;

import uk.ac.babraham.BamQC.BamQCException;
import uk.ac.babraham.BamQC.Dialogs.CrashReporter;
import uk.ac.babraham.BamQC.Dialogs.ProgressDialog;
import uk.ac.babraham.BamQC.Preferences.BamQCPreferences;
import uk.ac.babraham.BamQC.Utilities.NumberKeyListener;
import uk.ac.babraham.BamQC.Utilities.FileFilters.GFFFileFilter;

public class ManualGenomeBuilderPanel extends JPanel implements ActionListener {

	private JTextField speciesField;
	private JTextField assemblyField;

	private ManualGenome manualGenome = new ManualGenome();

	private JCheckBox pseudoBox;
	private JTextField pseudoNumberField;
	
	private JTable manualChrTable;

	private Vector<File> gffFiles = new Vector<File>();
	
	private JDialog parent;


	public ManualGenomeBuilderPanel () {
		this(null);
	}
	public ManualGenomeBuilderPanel (JDialog parent) {

		this.parent = parent;
		
		setLayout(new GridBagLayout());

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx=1;
		gbc.gridy=1;
		gbc.weightx=0.5;
		gbc.weighty=0.001;
		gbc.fill = GridBagConstraints.BOTH;

		JPanel namePanel = new JPanel();

		namePanel.add(new JLabel("Species "));
		speciesField = new JTextField(15);
		namePanel.add(speciesField);

		namePanel.add(new JLabel("   Assembly "));
		assemblyField = new JTextField(15);
		namePanel.add(assemblyField);

		add(namePanel,gbc);

		gbc.gridy++;

		JPanel pseudoPanel = new JPanel();

		pseudoBox = new JCheckBox();
		pseudoBox.setActionCommand("pseudo");
		pseudoBox.addActionListener(this);

		pseudoPanel.add(pseudoBox);
		pseudoPanel.add(new JLabel(" Create "));
		pseudoNumberField = new JTextField("25",4);
		pseudoNumberField.addKeyListener(new NumberKeyListener(false, false, 50));
		pseudoPanel.add(pseudoNumberField);
		pseudoPanel.add(new JLabel(" pseudo-chromosomes"));


		add(pseudoPanel,gbc);

		gbc.gridy++;
		gbc.weighty = 0.999;

		manualChrTable = new JTable(manualGenome);
		manualChrTable.setAutoCreateRowSorter(true);

		add(new JScrollPane(manualChrTable),gbc);


		gbc.gridy++;
		gbc.weighty = 0.001;

		JPanel buttonPanel = new JPanel();

		JButton gffButton = new JButton("Read GFF");
		gffButton.setActionCommand("gff");
		gffButton.addActionListener(this);
		buttonPanel.add(gffButton);

		JButton fastaButton = new JButton("Read Fasta");
		fastaButton.setActionCommand("fasta");
		fastaButton.addActionListener(this);
		buttonPanel.add(fastaButton);

		JButton addButton = new JButton("Add Region");
		addButton.setActionCommand("add");
		addButton.addActionListener(this);
		buttonPanel.add(addButton);

		JButton removeButton = new JButton("Remove Region");
		removeButton.setActionCommand("remove");
		removeButton.addActionListener(this);
		buttonPanel.add(removeButton);
		
		buttonPanel.add(new JLabel("         "));
		
		JButton createButton = new JButton("Create Genome");
		createButton.setActionCommand("create");
		createButton.addActionListener(this);
		buttonPanel.add(createButton);

		add(buttonPanel,gbc);

	}



	@Override
	public void actionPerformed(ActionEvent ae) {

		if (ae.getActionCommand().equals("add")) {
			String newName = (String)JOptionPane.showInputDialog(this,"Region name","Add region",JOptionPane.QUESTION_MESSAGE,null,null,"");
			if (newName != null) {
				manualGenome.getChromosome(newName);
			}
		}

		else if (ae.getActionCommand().equals("gff")) {
			JFileChooser chooser = new JFileChooser(BamQCPreferences.getInstance().getDataLocation());
			chooser.setMultiSelectionEnabled(false);
			chooser.setFileFilter(new GFFFileFilter());
			int result = chooser.showOpenDialog(this);

			/*
			 * There seems to be a bug in the file chooser which allows the user to
			 * select no files, but not cancel if the control+double click on a file
			 */
			if (result == JFileChooser.CANCEL_OPTION || chooser.getSelectedFile() == null) {
				return;
			}

			BamQCPreferences.getInstance().setLastUsedDataLocation(chooser.getSelectedFile());

			new SimpleGFFReader(chooser.getSelectedFile());

			gffFiles.add(chooser.getSelectedFile());

		}

		else if (ae.getActionCommand().equals("pseudo")) {

			if (pseudoBox.isSelected()) {
				int pseudoNumber = 25;
				if (pseudoNumberField.getText().length() > 0) {
					pseudoNumber = Integer.parseInt(pseudoNumberField.getText());
				}
				manualGenome.addPseudoGenome(pseudoNumber);
			}
			else {
				manualGenome.removePseudoGenome();
			}

		}
		
		else if (ae.getActionCommand().equals("remove")) {
		
			int [] rows = manualChrTable.getSelectedRows();

			ManualGenomeChromosome [] chrsToRemove = new ManualGenomeChromosome[rows.length];
			
			for (int i=0;i<rows.length;i++) {
				chrsToRemove[i] = manualGenome.getChromosome(manualChrTable.convertRowIndexToModel(rows[i]));
			}
			
			manualGenome.removeChromosome(chrsToRemove);
			
		}
		
		else if (ae.getActionCommand().equals("create")) {
			
			// Check if the species name is set and make the folder if needs be
			String speciesName = speciesField.getText().trim();
			if (speciesName.length() == 0) {
				reportError("Species name isn't filled in");
				return;
			}
			
			// Check that the assembly name is set
			String assemblyName = assemblyField.getText().trim();
			if (assemblyName.length() == 0) {
				reportError("Assembly name isn't filled in");
				return;
			}
			
			// Check that we have some chromosomes
			if (manualGenome.getRowCount() == 0) {
				reportError("You need to define some chromosomes before creating your genome");
				return;
			}
			
			// Check to see if they're making a silly number of chromosomes
			if (manualGenome.getRowCount() > 100 && !pseudoBox.isSelected()) {
				
				if (JOptionPane.showConfirmDialog(this, "<html>You're making a very large number of chromosomes which won't work well.<br>You should probably choose to make pseudo chromosomes for this genome.<br><br>Really continue?</html>", "That's a lot of chromosomes", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
				
				JOptionPane.showMessageDialog(this, "OK, but don't say we didn't warn you", "Going ahead", JOptionPane.WARNING_MESSAGE);
			}
			
			
			// See if the species folder exists already and make it if it doesn't
			File speciesFolder = null;
			try {
			speciesFolder = new File(BamQCPreferences.getInstance().getGenomeBase().getAbsolutePath()+"/"+speciesName);
			if (!speciesFolder.exists()) {
				if (! speciesFolder.mkdir()) {
					reportError("Failed to make "+speciesFolder+" folder");
					return;
				}
			}
			}
			catch (IOException ioe) {
				new CrashReporter(ioe);
				return;
			}
			
			// Make the assembly folder and bail out if it exists
			File assemblyFolder = new File(speciesFolder.getAbsolutePath()+"/"+assemblyName);
			if (assemblyFolder.exists()) {
				reportError("There is already an assembly called "+speciesName+"/"+assemblyName+" you need to delete this manually before creating a new genome of the same name");
				return;
			}
			if (!assemblyFolder.mkdir()) {
				reportError("Failed to make "+assemblyFolder+" folder");
				return;
			}
			
			// Copy over the gff files
			try {
				for (int f=0;f<gffFiles.size();f++) {
					PrintWriter pr = new PrintWriter(new File(assemblyFolder.getAbsolutePath()+"/"+gffFiles.elementAt(f).getName()));
				
					BufferedReader br = new BufferedReader(new FileReader(gffFiles.elementAt(f)));
				
					String line;
					while ((line = br.readLine()) != null) {
						pr.println(line);
					}
				
					br.close();
					pr.close();
				}

				// Write out chromosome lengths along with aliases for pseudo chromosomes			
				manualGenome.writeGenomeFiles(assemblyFolder);

			}
			catch (IOException ioe) {
				new CrashReporter(ioe);
				return;
			}
			
			JOptionPane.showMessageDialog(this, "Successfully created new custom genome","Success",JOptionPane.INFORMATION_MESSAGE);
			
			if (parent != null) {
				parent.setVisible(false);
				parent.dispose();
			}
			
		}

	}
	
	private void reportError (String message) {
		JOptionPane.showMessageDialog(this, message,"Error",JOptionPane.ERROR_MESSAGE);
	}


	private class SimpleGFFReader implements Runnable {

		private File gffFile;
		private ProgressDialog pd;

		public SimpleGFFReader (File gffFile) {
			this.gffFile = gffFile;

			pd = new ProgressDialog("Reading GFF");

			Thread t = new Thread(this);
			t.start();
		}

		@Override
		public void run () {

			try {
				
				BufferedReader br;
				
				if (gffFile.getName().toLowerCase().endsWith(".gz")) {
					br = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(gffFile))));
				}
				else {
					br = new BufferedReader(new FileReader(gffFile));
				}

				String line;
				while ((line = br.readLine()) != null) {

					if (line.startsWith("#")) continue;

					String [] fields = line.split("\t");

					if (fields.length < 5) {
						pd.progressWarningReceived(new BamQCException("No enough fields on line '"+line+"'"));
						continue;
					}

					ManualGenomeChromosome chr = manualGenome.getChromosome(fields[0]);
					chr.setHasFeatures();
					chr.setMinLength(Integer.parseInt(fields[4]));
				}

				br.close();

				pd.progressComplete("simple_gff", null);


			}

			catch (IOException ioe) {
				pd.progressExceptionReceived(ioe);
			}


		}

	}


}
