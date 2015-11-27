/**
 * Copyright Copyright 2014 Bart Ailey Eagle Genomics Ltd
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

package uk.ac.babraham.BamQC.Graphs;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.util.Random;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import uk.ac.babraham.BamQC.Utilities.LinearRegression;

public class ScatterGraph extends JPanel {

	private static final long serialVersionUID = -7292512222510200683L;

	private String xLabel;
	private String yLabel;
	private String[] xCategories;
	private double[] data;
	private String graphTitle;
	private double minY;
	private double maxY;
	private double yInterval;
	private int height = -1;
	private int width = -1;

	public ScatterGraph(double[] data, double minY, double maxY, String xLabel, String yLabel, int[] xCategories, String graphTitle) {
		this(data, minY, maxY, xLabel, yLabel, new String[0], graphTitle);
		this.xCategories = new String[xCategories.length];

		for (int i = 0; i < xCategories.length; i++) {
			this.xCategories[i] = "" + xCategories[i];
		}
	}

	public ScatterGraph(double[] data, double minY, double maxY, String xLabel, String yLabel, String[] xCategories, String graphTitle) {
		this.data = data;
		this.minY = minY;
		this.maxY = maxY;
		this.xLabel = xLabel;
		this.yLabel = yLabel;
		this.xCategories = xCategories;
		this.graphTitle = graphTitle;
		this.yInterval = findOptimalYInterval(maxY);
	}

	private double findOptimalYInterval(double max) {
		int base = 1;
		double[] divisions = new double[] { 1, 2, 2.5, 5 };

		while (true) {

			for (int d = 0; d < divisions.length; d++) {
				double tester = base * divisions[d];
				if (max / tester <= 10) {
					return tester;
				}
			}
			base *= 10;
		}
	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(800, 600);
	}

	@Override
	public Dimension getMinimumSize() {
		return new Dimension(100, 200);
	}

	@Override
	public int getHeight() {
		if (height < 0) {
			return super.getHeight();
		}
		return height;
	}

	@Override
	public int getWidth() {
		if (width < 0) {
			return super.getWidth();
		}
		return width;
	}

	public void paint(Graphics g, int width, int height) {
		this.height = height;
		this.width = width;
		paint(g);
		this.height = -1;
		this.width = -1;
	}

	@Override
	public void paint(Graphics g) {
		super.paint(g);

		g.setColor(Color.WHITE);
		g.fillRect(0, 0, getWidth(), getHeight());
		g.setColor(Color.BLACK);

		if (g instanceof Graphics2D) {
			((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		}
		
		double yStart;

		if (minY % yInterval == 0) {
			yStart = minY;
		}
		else {
			yStart = yInterval * (((int) minY / yInterval) + 1);
		}

		int xOffset = 0;

		// Draw the yLabel on the left of the yAxis
		int yLabelRightShift = 12;
		if(yLabel == null || yLabel.isEmpty()) {
			yLabelRightShift = 0;
		} else {
			if (g instanceof Graphics2D) {
				Graphics2D g2 = (Graphics2D)g;
				AffineTransform orig = g2.getTransform();
				g2.rotate(-Math.PI/2);
				g2.setColor(Color.BLACK);
				g2.drawString(yLabel, -getY(-yInterval)/2 - (g.getFontMetrics().stringWidth(yLabel)/2), yLabelRightShift);
				g2.setTransform(orig);
			}
		}
		
		
		// Draw the y axis labels
		for (double i = yStart; i <= maxY; i += yInterval) {
			String label = "" + i;
			label = label.replaceAll(".0$", ""); // Don't leave trailing .0s
													// where we don't need them.
			int width = g.getFontMetrics().stringWidth(label);
			if (width > xOffset) {
				xOffset = width;
			}

			g.drawString(label, yLabelRightShift+6, getY(i) + (g.getFontMetrics().getAscent() / 2));
		}
		
		
		// Give the x axis a bit of breathing space
		xOffset = xOffset + yLabelRightShift + 8;
		
		
		// Now draw horizontal lines across from the y axis
		g.setColor(new Color(180,180,180));
		for (double i=yStart; i<=maxY; i+=yInterval) {
			g.drawLine(xOffset, getY(i), getWidth()-10, getY(i));
		}
		g.setColor(Color.BLACK);
		

		
		// Draw the graph title
		int titleWidth = g.getFontMetrics().stringWidth(graphTitle);
		g.drawString(graphTitle, (xOffset + ((getWidth() - (xOffset + 10)) / 2)) - (titleWidth / 2), 30);



		// Draw the xLabel under the xAxis
		g.drawString(xLabel, (getWidth() / 2) - (g.getFontMetrics().stringWidth(xLabel) / 2), getHeight() - 5);

		
		// Now draw the data points
		double baseWidth = (getWidth() - (xOffset + 10)) / data.length;
		if (baseWidth < 1) baseWidth = 1;

		// System.out.println("Base Width is "+baseWidth);
		// Let's find the longest label, and then work out how often we can draw
		// labels
		int lastXLabelEnd = 0;
		
		// Draw the x axis labels
		for (int i = 0; i < data.length; i++) {
			g.setColor(Color.BLACK);
			String baseNumber = "" + xCategories[i];
			int baseNumberWidth = g.getFontMetrics().stringWidth(baseNumber);
			int baseNumberPosition = (int)((baseWidth / 2) + xOffset + (baseWidth * i) - (baseNumberWidth / 2));

			if (baseNumberPosition > lastXLabelEnd) {
				g.drawString(baseNumber, baseNumberPosition, getHeight() - 25);
				lastXLabelEnd = baseNumberPosition + baseNumberWidth + 5;
			}
		}
		
		
		
		// Now draw the axes
		g.drawLine(xOffset, getHeight() - 40, getWidth() - 10, getHeight() - 40);
		g.drawLine(xOffset, getHeight() - 40, xOffset, 40);
		

		g.setColor(Color.BLUE);
		// Draw the data points
		double ovalSize = 5;
		// We distinguish two inputs since the x label does not start from 0.
		// used for computing the actual line points as if they were starting from 0.
		double[] inputVar = new double[data.length];
		// used for the equation legend as this does not start from 0.
		double[] legendInputVar = new double[data.length];
		double[] responseVar = new double[data.length];
		for (int d = 0; d < data.length; d++) {
			double x = xOffset + ((baseWidth * d) + (baseWidth * (d+1)))/2;
			double y = getY(data[d])-ovalSize/2;
			g.fillOval((int)x, (int)y, (int)(ovalSize), (int)(ovalSize));
			// TODO this plots correctly but shouldn't .... 
			inputVar[d] = Double.valueOf(d);  
			//inputVar[d] = Double.valueOf(xCategories[d]); 
			//legendInputVar[d] = Double.valueOf(d);
			legendInputVar[d] = Double.valueOf(xCategories[d]);
			responseVar[d] = data[d];	
		}
		g.setColor(Color.BLACK);
		
		
		
		
		
		
		// Draw the intercept 
		
		// WARNING: Is drawing a least squares regression line asserting that "the distribution follows a power law" correct?
		// This is our case if we plot log-log..
		// It seems not in this paper (Appendix A) http://arxiv.org/pdf/0706.1062v2.pdf
		
//		if(data.length > 1) {
//			LinearRegression linReg = new LinearRegression(inputVar, responseVar);
//			double intercept = linReg.intercept();
//			double slope = linReg.slope();
//		
//			// Let's now calculate the two points (x1, y1) and (xn, yn)
//			// The point (x1, y1) is where the intercept crosses the x axis (since we are not interested 
//			// in what there is below): y=ax+b => ax+b=0 . Therefore (x1, y1) = (-b/a, 0). 
//			// The point (xn, yn) is the last point of our discrete intercept.
//			
//			double x1 = -intercept / slope;			
//			double y1=0;
//			if(x1 < 0) {
//				x1 = 0;
//				y1 = intercept;
//			}
//			
//			
//			
//			double xn = inputVar.length-1;
//			double yn = slope*inputVar[inputVar.length-1] + intercept;
//
//			
//			
//			// Note that y1 and yn are the actual points calculated from the intercept. These need to be "converted" 
//			// in real plot coordinates.
//			// x1 and xn represents the factors of baseWidth on the x-axis. 
//			
//			g.setColor(Color.RED);
//			g.drawLine((int)(xOffset + ((baseWidth * (x1) + (baseWidth * (x1+1)))/2)), 
//					   getY(y1), 
//					   (int)(xOffset + ((baseWidth * (xn)) + (baseWidth * (xn+1)))/2), 
//					   getY(yn));
//			g.setColor(Color.BLACK);
//			
//			// Draw the legend for the intercept
//			// First we need to find the widest label
//			LinearRegression legendEquationLinReg = new LinearRegression(legendInputVar, responseVar);
//			double legendIntercept = legendEquationLinReg.intercept();
//			double legendSlope = legendEquationLinReg.slope();
//			double legendRSquare = legendEquationLinReg.R2();
//			
//			// Translate line to x for inputVar[0]
//			//legendIntercept = legendIntercept - slope*inputVar[0];
//			
//			String legendInterceptString = "y = " + (float)(legendSlope) + " * x ";
//			
//			if(legendIntercept < 0) 
//				legendInterceptString += " - " + (float)(-legendIntercept);
//			else 
//				legendInterceptString += " + " + (float)legendIntercept;
//			legendInterceptString += " , R^2 = " + (float)legendRSquare;
//			int width = g.getFontMetrics().stringWidth(legendInterceptString);
//			
//			// First draw a box to put the legend in
//			g.setColor(Color.WHITE);
//			g.fillRect(xOffset+10, 40, width+8, 23);
//			g.setColor(Color.LIGHT_GRAY);
//			g.drawRect(xOffset+10, 40, width+8, 23);
//	
//			// Now draw the intercept label
//			g.setColor(Color.RED);
//			g.drawString(legendInterceptString, xOffset+13, 60);
//			g.setColor(Color.BLACK);
//		}
		
	}
	
	
	
	private int getY(double y) {
		return (getHeight() - 40) - (int) (((getHeight() - 80) / (maxY - minY)) * y);
	}
	
	

	
	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				Random r = new Random();
				int sampleSize = 1000;
				double[] data = new double[sampleSize];
				String[] xCategories = new String[sampleSize];
				double minY = Double.MAX_VALUE;
				double maxY = Double.MIN_VALUE;
				for(int i=0; i<sampleSize; i++) {
					data[i] = (r.nextGaussian()*0.9 + 10)*i;
					xCategories[i] = Integer.toString(i);
					if(data[i] > maxY) maxY = data[i];
					else if(data[i] < minY) minY = data[i];
				}
					
				String xLabel = "xLabel";
				String yLabel = "yLabel";
				//String yLabel = null;
				String graphTitle = "Graph Title";

				JFrame frame = new JFrame();
				ScatterGraph scatterGraph = new ScatterGraph(data, minY, maxY, xLabel, yLabel, xCategories, graphTitle);

				frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				frame.setSize(500, 500);
				frame.add(scatterGraph);
				frame.setLocationRelativeTo(null);
				frame.setVisible(true);
			}
		});
	}

}
