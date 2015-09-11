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

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Vector;
import java.util.Hashtable;

import javax.swing.table.AbstractTableModel;

public class ManualGenome extends AbstractTableModel {

	private Vector<ManualGenomeChromosome> chrs = new Vector<ManualGenomeChromosome>();
	private Hashtable<String, ManualGenomeChromosome> chrNames = new Hashtable<String, ManualGenomeChromosome>();
	private ManualPseudoGenome pseudoGenome = null;

	public ManualGenomeChromosome getChromosome (String name) {
		if (!chrNames.containsKey(name)) {
			ManualGenomeChromosome chr = new ManualGenomeChromosome(name,this);
			chrs.add(chr);
			chrNames.put(name,chr);
			fireTableRowsInserted(chrs.size()-1, chrs.size()-1);
			updatePseudoGenome();
		}
		
		return chrNames.get(name);
	}
	
	public ManualGenomeChromosome getChromosome (int index) {
		return chrs.elementAt(index);
	}
	
	public void removeChromosome (ManualGenomeChromosome chr) {
		int index = chrs.indexOf(chr);
		chrs.remove(chr);
		chrNames.remove(chr.name());
		fireTableRowsDeleted(index, index);
		updatePseudoGenome();
	}

	public void removeChromosome (ManualGenomeChromosome [] chrsToDelete) {
		int [] indices = new int [chrsToDelete.length];
		for (int c=0;c<chrsToDelete.length;c++) {
			indices[c] = chrs.indexOf(chrsToDelete[c]);
			chrNames.remove(chrsToDelete[c].name());
		}
		Arrays.sort(indices);
		for (int i=indices.length-1;i>=0;i--) {
			chrs.remove(indices[i]);
		}

		fireTableDataChanged();
		updatePseudoGenome();
	}

	
	public void writeGenomeFiles (File baseFolder) throws IOException {
		// We need to write out a list of the full length chromosomes.  If we're making
		// pseudo genomes this will be the pseudo chromosomes or if we're making real
		// chromosomes it will be the real ones.
		
		PrintWriter pr = new PrintWriter(new File(baseFolder.getAbsolutePath()+"/chr_list"));
		
		if (pseudoGenome != null) {
			
			pseudoGenome.writeChrList(pr);
			
			// For the pseudo genome we also need to write out an aliases file to allow
			// use to import data mapped against the original scaffolds but visualise this
			// on the pseudo chromosomes.  We also need to write out a gff file of the 
			// scaffold positions.
			
			pseudoGenome.writeScaffoldAlises(baseFolder);
			
		}
		else {
			// We're just writing out the real chromosomes
			for (int i=0;i<chrs.size();i++) {
				pr.println(chrs.elementAt(i).name()+"\t"+chrs.elementAt(i).length());
			}
		}
		
		pr.close();
		
		
	}
	
	public ManualGenomeChromosome [] getChromosomes () {
		return chrs.toArray(new ManualGenomeChromosome[0]);
	}
	
	protected void addedFeatures(ManualGenomeChromosome chr) {
		fireTableCellUpdated(chrs.indexOf(chr), 4);
	}
	
	protected void lengthUpdated (ManualGenomeChromosome chr) {
		fireTableCellUpdated(chrs.indexOf(chr), 1);
		updatePseudoGenome();
	}

	protected void pseudoChromosomeUpdated (ManualGenomeChromosome chr) {
		fireTableCellUpdated(chrs.indexOf(chr), 2);		
		fireTableCellUpdated(chrs.indexOf(chr), 3);		
	}

	public void removePseudoGenome () {
		if (pseudoGenome != null) {
			pseudoGenome = null;
			for (int i=0;i<chrs.size();i++) {
				ManualGenomeChromosome chr = chrs.elementAt(i);
				chr.setPseudoChromosome(null, 0);
			}
		}
	}
	
	public void addPseudoGenome (int numberOfChromosomes) {
		pseudoGenome = new ManualPseudoGenome(numberOfChromosomes, this);
	}
	
	public void updatePseudoGenome () {
		if (pseudoGenome != null) {
			pseudoGenome = new ManualPseudoGenome(pseudoGenome.numberOfChromosomes(), this);
		}
	}
	
	
	@Override
	public int getColumnCount() {
		return 5;
	}

	@Override
	public String getColumnName (int c) {
		switch(c) {

		case 0: return "Region name";
		case 1: return "Length";
		case 2: return "Pseudo Chr Name";
		case 3: return "Pseudo Chr Offset";
		case 4: return "Features";

		}

		return null;
	}

	@Override
	public int getRowCount() {
		return chrs.size();
	}
	
	@Override
	public boolean isCellEditable (int r, int c) {
		if (c==1) return true;
		return false;
	}
	
	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Class getColumnClass (int c) {
		switch(c) {

		case 0: return String.class;
		case 1: return Integer.class;
		case 2: return String.class;
		case 3: return Integer.class;
		case 4: return String.class;
		}
		
		return null;
	}
	
	@Override
	public void setValueAt (Object value, int r, int c) {
		System.err.println("Called set value for "+r+","+c+" with value "+value);
		if (c==1) {
			int intValue = (Integer)value;
			chrs.elementAt(r).setLength(intValue);
		}
	}
	
	@Override
	public Object getValueAt(int r, int c) {
		switch(c) {
		case 0: return chrs.elementAt(r).name();
		case 1: return chrs.elementAt(r).length();
		case 2: return chrs.elementAt(r).pseudoChromosomeName();
		case 3: return chrs.elementAt(r).pseudoChromosomeOffset();
		case 4: if (chrs.elementAt(r).hasFeatures()) return "Yes";
			return "No";
		}
		return null;
	}
	
	
	

}
