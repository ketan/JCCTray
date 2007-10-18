/*******************************************************************************
 *  Copyright 2007 Ketan Padegaonkar http://ketan.padegaonkar.name
 *  
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 ******************************************************************************/
package net.sourceforge.jcctray.ui;

import net.sourceforge.jcctray.model.DashBoardProject;
import net.sourceforge.jcctray.model.IJCCTraySettings;
import net.sourceforge.jcctray.model.JCCTraySettings;
import net.sourceforge.jcctray.ui.settings.SettingsDialog;
import net.sourceforge.jcctray.ui.settings.providers.EnabledProjectsFilter;
import net.sourceforge.jcctray.ui.settings.providers.ProjectContentProvider;
import net.sourceforge.jcctray.ui.settings.providers.ProjectLabelProvider;

import org.apache.log4j.Logger;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Tray;
import org.eclipse.swt.widgets.TrayItem;

/**
 * The main JCCTray window.
 * 
 * @author Ketan Padegaonkar
 */
public class JCCTray {

	private static final Logger	log	= Logger.getLogger(JCCTray.class);

	private final class AboutDialogListener implements SelectionListener {
		public void widgetDefaultSelected(SelectionEvent e) {
			new AboutDialog(shell, traySettings).open();
		}

		public void widgetSelected(SelectionEvent e) {
			widgetDefaultSelected(e);
		}
	}

	private final class BugsWebPageListener implements SelectionListener {
		public void widgetDefaultSelected(SelectionEvent e) {
			new Browser(traySettings).open("http://jcctray.sourceforge.net/wiki/Bugs");
		}

		public void widgetSelected(SelectionEvent e) {
			widgetDefaultSelected(e);
		}
	}

	private final class TableSelectionListener implements SelectionListener {
		public void widgetDefaultSelected(SelectionEvent e) {
			setButtonVisibilities();
		}

		public void widgetSelected(SelectionEvent e) {
			widgetDefaultSelected(e);
		}

		private void setButtonVisibilities() {
			boolean enabled = table.getSelectionCount() > 0;
			forceBuildButton.setEnabled(enabled);
			displayWebPageButton.setEnabled(enabled);
		}
	}

	private final class SettingsMenuListener implements SelectionListener {
		public void widgetDefaultSelected(SelectionEvent e) {
			showSettingsDialog();
		}

		public void widgetSelected(SelectionEvent e) {
			widgetDefaultSelected(e);
		}
	}

	private final class FileExitMenuListener implements SelectionListener {
		public void widgetDefaultSelected(SelectionEvent e) {
			shell.dispose();
			trayItem.dispose();
			display.dispose();
			System.exit(0);
		}

		public void widgetSelected(SelectionEvent e) {
			widgetDefaultSelected(e);
		}
	}

	private final class ShellListener extends ShellAdapter {
		public void shellClosed(ShellEvent e) {
			e.doit = false;
			minimizeToTray();
		}
	}

	private final class SystemTrayListener implements SelectionListener {
		public void widgetDefaultSelected(SelectionEvent e) {
			shell.setVisible(!shell.getVisible());
		}

		public void widgetSelected(SelectionEvent e) {
			widgetDefaultSelected(e);
		}
	}

	private final class ForceBuildBackgroundThread extends Thread {
		private final DashBoardProject	project;

		private ForceBuildBackgroundThread(DashBoardProject project) {
			this.project = project;
		}

		public void run() {
			try {
				this.project.forceBuild();
			} catch (final Exception ex) {
				display.asyncExec(new Runnable() {
					public void run() {
						MessageBox messageBox = new MessageBox(table.getShell(), SWT.ICON_ERROR);
						messageBox.setText("Could not force build");
						messageBox.setMessage("" + ex.getMessage());
						messageBox.open();
					}
				});
			}
		}
	}

	private final class ForceBuildListener implements SelectionListener {

		public void widgetDefaultSelected(SelectionEvent e) {
			if (table.getSelectionCount() > 0) {
				TableItem[] selection = table.getSelection();
				final DashBoardProject project = (DashBoardProject) selection[0].getData();

				new ForceBuildBackgroundThread(project).start();
			}
		}

		public void widgetSelected(SelectionEvent e) {
			widgetDefaultSelected(e);
		}
	}

	private final class DisplayWebPageListener implements SelectionListener, IDoubleClickListener {
		public void widgetDefaultSelected(SelectionEvent e) {
			openWebPage();
		}

		public void widgetSelected(SelectionEvent e) {
			openWebPage();
		}

		public void doubleClick(DoubleClickEvent event) {
			openWebPage();
		}

		private void openWebPage() {
			if (table.getSelectionCount() > 0) {
				TableItem[] selection = table.getSelection();
				DashBoardProject project = (DashBoardProject) selection[0].getData();
				new Browser(traySettings).open(project.getWebUrl());
			}
		}

	}

	private Shell					shell;
	private Display					display;
	private Menu					menuBar;
	private MenuItem				fileMenuHeader;
	private Menu					fileMenu;
	private Table					table;
	private TrayItem				trayItem;
	private Button					displayWebPageButton;
	private Button					forceBuildButton;
	private MenuItem				fileSettingsItem;
	private MenuItem				fileExitItem;
	private TableViewer				tableViewer;
	private final IJCCTraySettings	traySettings;
	private MenuItem				helpMenuHeader;
	private Menu					helpMenu;
	private MenuItem				helpAboutItem;
	private MenuItem				helpBugsItem;

	public JCCTray() {
		this(JCCTraySettings.getInstance());
	}

	public JCCTray(IJCCTraySettings traySettings) {
		this.traySettings = traySettings;
		initialize();
	}

	private void initialize() {
		display = new Display();
		createShell();
		createMenus();
		createTable();
		createContextMenus();
		createButtons();
		createSystemTray();
		hookEvents();

	}

	private void createButtons() {
		Composite composite = new Composite(shell, SWT.NONE);
		GridLayout gridLayout = new GridLayout(2, true);
		composite.setLayout(gridLayout);
		displayWebPageButton = new Button(composite, SWT.NONE);
		displayWebPageButton.setText("Display &Web Page");
		displayWebPageButton.setEnabled(false);

		forceBuildButton = new Button(composite, SWT.NONE);
		forceBuildButton.setText("Force &Build");
		forceBuildButton.setEnabled(false);
	}

	private void createSystemTray() {
		Tray systemTray = display.getSystemTray();
		trayItem = new TrayItem(systemTray, SWT.NONE);
	}

	private void hookEvents() {
		shell.addShellListener(new ShellListener());

		trayItem.addSelectionListener(new SystemTrayListener());

		displayWebPageButton.addSelectionListener(new DisplayWebPageListener());
		forceBuildButton.addSelectionListener(new ForceBuildListener());

		fileSettingsItem.addSelectionListener(new SettingsMenuListener());
		fileExitItem.addSelectionListener(new FileExitMenuListener());

		helpBugsItem.addSelectionListener(new BugsWebPageListener());
		helpAboutItem.addSelectionListener(new AboutDialogListener());

		table.addSelectionListener(new TableSelectionListener());
		tableViewer.addDoubleClickListener(new DisplayWebPageListener());
	}

	protected void minimizeToTray() {
		shell.setVisible(false);
	}

	private void createTable() {
		table = new Table(shell, SWT.FULL_SELECTION);
		table.setLayoutData(new GridData(GridData.FILL_VERTICAL | GridData.FILL_HORIZONTAL));
		table.setHeaderVisible(true);
		table.setLinesVisible(false);

		TableColumn tableColumn;

		tableColumn = new TableColumn(table, SWT.NONE);
		tableColumn.setWidth(100);
		tableColumn.setMoveable(true);
		tableColumn.setText("Project");

		tableColumn = new TableColumn(table, SWT.NONE);
		tableColumn.setWidth(100);
		tableColumn.setMoveable(true);
		tableColumn.setText("Host");

		tableColumn = new TableColumn(table, SWT.NONE);
		tableColumn.setWidth(100);
		tableColumn.setMoveable(true);
		tableColumn.setText("Activity");

		tableColumn = new TableColumn(table, SWT.NONE);
		tableColumn.setWidth(300);
		tableColumn.setMoveable(true);
		tableColumn.setText("Detail");

		tableColumn = new TableColumn(table, SWT.NONE);
		tableColumn.setWidth(150);
		tableColumn.setMoveable(true);
		tableColumn.setText("Last Build Label");

		tableColumn = new TableColumn(table, SWT.NONE);
		tableColumn.setWidth(150);
		tableColumn.setMoveable(true);
		tableColumn.setText("Last Build Time");

		tableViewer = new TableViewer(table);
		tableViewer.setLabelProvider(new ProjectLabelProvider());
		tableViewer.setContentProvider(new ProjectContentProvider());
		tableViewer.setFilters(new ViewerFilter[] { new EnabledProjectsFilter(traySettings) });
	}

	private void createMenus() {
		menuBar = new Menu(shell, SWT.BAR);
		createFileMenu();
		createHelpMenu();
		shell.setMenuBar(menuBar);
	}

	private void createContextMenus() {
		Menu menu = new Menu(table);
		MenuItem menuItem;

		menuItem = new MenuItem(menu, SWT.CASCADE);
		menuItem.setText("Display &Web Page");
		menuItem.addSelectionListener(new DisplayWebPageListener());

		menuItem = new MenuItem(menu, SWT.CASCADE);
		menuItem.setText("Force &Build");
		menuItem.addSelectionListener(new ForceBuildListener());

		table.setMenu(menu);
	}

	private void createHelpMenu() {
		helpMenuHeader = new MenuItem(menuBar, SWT.CASCADE);
		helpMenuHeader.setText("&Help");
		helpMenu = new Menu(shell, SWT.DROP_DOWN);
		helpMenuHeader.setMenu(helpMenu);
		createBugsItem();
		new MenuItem(helpMenu, SWT.SEPARATOR);
		createAboutMenuItem();
	}

	private void createAboutMenuItem() {
		helpAboutItem = new MenuItem(helpMenu, SWT.PUSH);
		helpAboutItem.setText("&About JCCTray	CTRL+J");
		helpAboutItem.setAccelerator(SWT.MOD1 | 'J');
	}

	private void createBugsItem() {
		helpBugsItem = new MenuItem(helpMenu, SWT.PUSH);
		helpBugsItem.setText("&Bugs	CTRL+B");
		helpBugsItem.setAccelerator(SWT.MOD1 | 'B');
	}

	private void createFileMenu() {
		fileMenuHeader = new MenuItem(menuBar, SWT.CASCADE);
		fileMenuHeader.setText("&File");
		fileMenu = new Menu(shell, SWT.DROP_DOWN);
		fileMenuHeader.setMenu(fileMenu);
		createSettingsMenuItem();
		new MenuItem(fileMenu, SWT.SEPARATOR);
		createFileExitMenuItem();
	}

	private void createSettingsMenuItem() {
		fileSettingsItem = new MenuItem(fileMenu, SWT.PUSH);
		fileSettingsItem.setText("&Settings	CTRL+S");
		fileSettingsItem.setAccelerator(SWT.MOD1 | 'S');
	}

	protected void showSettingsDialog() {
		new SettingsDialog(shell, traySettings).open();
	}

	private void createFileExitMenuItem() {
		fileExitItem = new MenuItem(fileMenu, SWT.PUSH);
		fileExitItem.setText("E&xit	CTRL+X");
		fileExitItem.setAccelerator(SWT.MOD1 | 'X');
	}

	private void createShell() {
		shell = new Shell(display);
		shell.setText("JCCTray");
		GridLayout gridLayout = new GridLayout();
		shell.setLayout(gridLayout);
		shell.setSize(900, 300);
	}

	public void open() {
		shell.open();

		JCCTrayRunnable runnable = new JCCTrayRunnable(tableViewer, trayItem, traySettings);
		Thread thread = new Thread(runnable, "XmlStatusReportThread");
		thread.start();
		while (!shell.isDisposed())
			if (!display.readAndDispatch())
				display.sleep();
		try {
			runnable.shouldRun = false;
			thread.join();
		} catch (InterruptedException e) {
			log.error("Interrupted when waiting on thread.", e);
		}
	}
}
