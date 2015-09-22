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
package uk.ac.babraham.BamQC.Dialogs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import uk.ac.babraham.BamQC.BamQCApplication;
import uk.ac.babraham.BamQC.DataTypes.ProgressListener;

/**
 * The Class ProgressDialog is a generic progress dialog showing a progress
 * bar and a changing label.  It can also display a cancel button for 
 * progress listners which allow it.
 */
public class ProgressDialog extends JDialog implements Runnable, ProgressListener, ActionListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2922407571116142442L;

	/** The label. */
	private JLabel label;
	
	/** The cancellable. */
	private Cancellable cancellable;
	
	private boolean hasCancelled = false;
	
	/** The current. */
	private int current = 0;
	
	/** The total. */
	private int total = 1;
	
	/** The progress bar. */
	private ProgressBar progressBar = new ProgressBar();
	
	/** The warning count. */
	private int warningCount = 0;
	
	private boolean ignoreExceptions = false;
	
	/** The warnings. */
	private Vector<Exception>warnings = new Vector<Exception>();
	
	/** A record of any exception we've recevied */
	private Exception reportedException = null;

	/**
	 * Instantiates a new progress dialog.
	 * 
	 * @param title the title
	 */
	public ProgressDialog (String title) {
		this(BamQCApplication.getInstance(),title,null);
	}

	/**
	 * Instantiates a new progress dialog.
	 * 
	 * @param title the title
	 * @param cancellable a cancellable object to end this process
	 */
	public ProgressDialog (String title, Cancellable cancellable){
		this(BamQCApplication.getInstance(),title,cancellable);
	}
	
	/**
	 * Instantiates a new progress dialog.
	 * 
	 * @param parent the parent
	 * @param title the title
	 */
	public ProgressDialog (JFrame parent, String title) {
		this(parent,title,null);
	}
	
	/**
	 * Instantiates a new progress dialog.
	 * 
	 * @param parent the parent
	 * @param title the title
	 * @param cancellable the cancellable
	 */
	public ProgressDialog (JFrame parent, String title, Cancellable cancellable) {
		super(parent,title);
		setup(parent,cancellable);
	}

	/**
	 * Instantiates a new progress dialog.
	 * 
	 * @param parent the parent
	 * @param title the title
	 */
	public ProgressDialog (JDialog parent, String title) {
		this(parent,title,null);
	}
	
	/**
	 * Instantiates a new progress dialog.
	 * 
	 * @param parent the parent
	 * @param title the title
	 * @param cancellable the cancellable
	 */
	public ProgressDialog (JDialog parent, String title, Cancellable cancellable) {
		super(parent,title);
		setup(parent,cancellable);
	}	
	
	
	/**
	 * Setup.
	 * 
	 * @param parent the parent
	 * @param cancellable the cancellable
	 */
	private void setup (Component parent, Cancellable cancellable) {
		setSize(400,75);
		setLocationRelativeTo(parent);
		
		this.cancellable = cancellable;
		label = new JLabel("",JLabel.CENTER);
		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(label,BorderLayout.CENTER);
		
		if (cancellable != null) {
			JButton cancelButton = new JButton("Cancel");
			cancelButton.addActionListener(this);
			cancelButton.setActionCommand("cancel");
			getContentPane().add(cancelButton,BorderLayout.EAST);
		}
		
		getContentPane().add(progressBar,BorderLayout.SOUTH);
		Thread t = new Thread(this);
		t.start();
		setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		setVisible(true);
		
	}
	
	public void setIgnoreExceptions (boolean ignore) {
		this.ignoreExceptions = ignore;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {}
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.BamQC.DataTypes.ProgressListener#progressUpdated(java.lang.String, int, int)
	 */
	@Override
	public void progressUpdated(String message, int currentPos, int totalPos) {
		label.setText(message);
		current = currentPos;
		total = totalPos;
		progressBar.repaint();
		
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.BamQC.DataTypes.ProgressListener#progressExceptionReceived(java.lang.Exception)
	 */
	@Override
	public void progressExceptionReceived(Exception e) {
		
		if (reportedException != null && reportedException == e) return;
		
		reportedException = e;
		
		setVisible(false);
		dispose();
		if (! ignoreExceptions) {
			e.printStackTrace();
		}
	}

	
	/* (non-Javadoc)
	 * @see uk.ac.babraham.BamQC.DataTypes.ProgressListener#progressCancelled()
	 */
	@Override
	public void progressCancelled() {
		setVisible(false);
		dispose();
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.BamQC.DataTypes.ProgressListener#progressComplete(java.lang.String, java.lang.Object)
	 */
	@Override
	public void progressComplete(String command, Object result) {
		setVisible(false);

		if (warningCount > 0) {
			// We need to display a list of the warnings
			new WarningDisplayDialog(this,warningCount,warnings.toArray(new Exception [0]));
		}
		dispose();
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.BamQC.DataTypes.ProgressListener#progressWarningReceived(java.lang.Exception)
	 */
	@Override
	public void progressWarningReceived(Exception e) {
		warningCount++;
		// We just store this warning so we can display all
		// of them at the end.  We only keep the first 5000
		// so that things don't get too out of hand
		if (warningCount<=5000){
			warnings.add(e);
		}		
	}

	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		// This can only come from the cancel button
		
		if (hasCancelled) {
			// They've already pressed cancel and it's been ignored so we'll let the box go
			// if they press it again.  This could leave stuff running in the background but
			// having errant boxes around is really annoying.
			
			setVisible(false);
			dispose();
			return;
			
			
		}
		cancellable.cancel();
		hasCancelled = true;
	}

	/**
	 * The Class ProgressBar.
	 */
	private class ProgressBar extends JPanel {
		
		/**
		 * 
		 */
		private static final long serialVersionUID = 7887876890327169221L;

		/* (non-Javadoc)
		 * @see javax.swing.JComponent#paint(java.awt.Graphics)
		 */
		@Override
		public void paint (Graphics g) {
			super.paint(g);
			g.setColor(Color.RED);
			g.fillRect(0,0,(int)(getWidth()*((float)current/total)),getHeight());
		}
		
	}

}
