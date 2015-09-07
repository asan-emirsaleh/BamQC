/**
 * Copyright 2010-15 Simon Andrews
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
package uk.ac.babraham.BamQC.Network.DownloadableGenomes;

import java.util.Vector;

public class GenomeSpecies {

	private Vector<GenomeAssembly> assemblies = new Vector<GenomeAssembly>();
	private String name;
	
	public GenomeSpecies (String name) {
		this.name = name;
	}
	
	public String name () {
		return name;
	}
	
	@Override
	public String toString () {
		return name();
	}
	
	public void addAssembly (GenomeAssembly assembly) {
		if (assembly != null) {
			assemblies.add(assembly);
		}
	}
	
	public GenomeAssembly [] assemblies () {
		return assemblies.toArray(new GenomeAssembly[0]);
	}
	
}
