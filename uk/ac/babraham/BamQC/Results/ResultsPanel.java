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
package uk.ac.babraham.BamQC.Results;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.util.Vector;

import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import uk.ac.babraham.BamQC.BamQCConfig;
import uk.ac.babraham.BamQC.Analysis.AnalysisListener;
import uk.ac.babraham.BamQC.Modules.QCModule;
import uk.ac.babraham.BamQC.Sequence.SequenceFile;

public class ResultsPanel extends JPanel implements ListSelectionListener, AnalysisListener{

	private static final long serialVersionUID = 2141753423963373692L;
	private static final ImageIcon ERROR_ICON = new ImageIcon(ClassLoader.getSystemResource("uk/ac/babraham/BamQC/Resources/error.png"));
	private static final ImageIcon WARNING_ICON = new ImageIcon(ClassLoader.getSystemResource("uk/ac/babraham/BamQC/Resources/warning.png"));
	private static final ImageIcon OK_ICON = new ImageIcon(ClassLoader.getSystemResource("uk/ac/babraham/BamQC/Resources/tick.png"));

	
	private QCModule [] modules;
	private JList<?> moduleList;
	private JPanel [] panels;
	private JPanel currentPanel = null;
	private JLabel progressLabel;
	private SequenceFile sequenceFile;
	
	public ResultsPanel (SequenceFile sequenceFile) {
		this.sequenceFile = sequenceFile;
		setLayout(new BorderLayout());
		progressLabel = new JLabel("Waiting to start...",JLabel.CENTER);
		add(progressLabel,BorderLayout.CENTER);
	}

	@Override
	public void valueChanged(ListSelectionEvent e) {
		int index = moduleList.getSelectedIndex();
		if (index >= 0) {
			remove(currentPanel);
			currentPanel = panels[index]; 
			add(currentPanel,BorderLayout.CENTER);
			validate();
			repaint();
		}
	}
	
	public SequenceFile sequenceFile () {
		return sequenceFile;
	}
	
	public QCModule [] modules () {
		return modules;
	}
	
	private class ModuleRenderer extends DefaultListCellRenderer {

		/**
		 * 
		 */
		private static final long serialVersionUID = 3665918496548081577L;

		@Override
		public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
			if (! (value instanceof QCModule)) {
				return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			}
			
			QCModule module = (QCModule)value;
			ImageIcon icon = OK_ICON;
			if (module.raisesError()) {
				icon = ERROR_ICON;
			}
			else if (module.raisesWarning()) {
				icon = WARNING_ICON;
			}

			JLabel returnLabel = new JLabel(module.name(),icon,JLabel.LEFT);
			returnLabel.setOpaque(true);
			if (isSelected) {
				returnLabel.setBackground(Color.LIGHT_GRAY);
			}
			else {
				returnLabel.setBackground(Color.WHITE);
			}
			
			return returnLabel;
		}
		
	}

	@Override
	public void analysisComplete(SequenceFile file, QCModule[] rawModules) {
		remove(progressLabel);

		Vector<QCModule> modulesToDisplay = new Vector<QCModule>();
		
		for (int m=0;m<rawModules.length;m++) {
			if (!rawModules[m].ignoreInReport()) {
				modulesToDisplay.add(rawModules[m]);
			}
		}
		
		modules = modulesToDisplay.toArray(new QCModule[0]);
		
		panels = new JPanel[modules.length];
		
		for (int m=0;m<modules.length;m++) {
			panels[m] = modules[m].getResultsPanel();
		}
		
		moduleList = new JList<Object>(modules);
		moduleList.setCellRenderer(new ModuleRenderer());
		moduleList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		moduleList.setSelectedIndex(0);
		moduleList.addListSelectionListener(this);
		
		add(new JScrollPane(moduleList),BorderLayout.WEST);
		
		currentPanel = panels[0];
		add(currentPanel,BorderLayout.CENTER);
		validate();
		
	}

	@Override
	public void analysisUpdated(SequenceFile file, int sequencesProcessed, int percentComplete) {
		if (percentComplete > 99) {
			progressLabel.setText("Read "+sequencesProcessed+" sequences");			
		}
		else {
			progressLabel.setText("Read "+sequencesProcessed+" sequences ("+percentComplete+"%)");
		}
	}

	@Override
	public void analysisExceptionReceived(SequenceFile file, Exception e) {
		progressLabel.setText("Failed to process file: "+e.getLocalizedMessage());
	}

	@Override
	public void analysisStarted(SequenceFile file) {
		if (BamQCConfig.getInstance().gff_file == null) {
			progressLabel.setText("Starting analysis...");
		}
		else {
			progressLabel.setText("Reading "+BamQCConfig.getInstance().gff_file.getName());			
		}
	}
	
	
}
