/*
GanttProject is an opensource project management tool. License: GPL3
Copyright (C) 2003-2012 GanttProject Team

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 3
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
*/

package net.sourceforge.ganttproject.document.webdav;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SpringLayout;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.document.webdav.WebDavResource.WebDavException;
import net.sourceforge.ganttproject.gui.UIUtil;
import net.sourceforge.ganttproject.gui.options.OptionsPageBuilder;
import net.sourceforge.ganttproject.gui.options.SpringUtilities;
import net.sourceforge.ganttproject.gui.options.model.BooleanOption;
import net.sourceforge.ganttproject.gui.options.model.ChangeValueEvent;
import net.sourceforge.ganttproject.gui.options.model.ChangeValueListener;
import net.sourceforge.ganttproject.gui.options.model.DefaultBooleanOption;
import net.sourceforge.ganttproject.gui.options.model.DefaultStringOption;
import net.sourceforge.ganttproject.gui.options.model.IntegerOption;
import net.sourceforge.ganttproject.gui.options.model.ListOption;
import net.sourceforge.ganttproject.gui.options.model.StringOption;
import net.sourceforge.ganttproject.language.GanttLanguage;

import org.jdesktop.swingx.JXList;

/**
 * UI component for WebDAV operarions.
 *
 * @author dbarashev (Dmitry Barashev)
 */
class GanttURLChooser {
  private static final GanttLanguage language = GanttLanguage.getInstance();

  private final StringOption myUsername;

  private final StringOption myPassword;

  private final StringOption myPath;

  private final BooleanOption myLock;

  private final IntegerOption myTimeout;

  private SelectionListener mySelectionListener;

  private final ListOption myServers;

  private final GPAction myReloadAction = new GPAction("fileChooser.reload") {
    @Override
    public void actionPerformed(ActionEvent event) {
      try {
        WebDavResource resource = WebDavStorageImpl.createResource(buildUrl(), myUsername.getValue(), myPassword.getValue());
        if (resource.exists() && resource.isCollection()) {
          tableModel.setCollection(resource);
          return;
        }
        WebDavResource parent = resource.getParent();
        if (parent.exists() && parent.isCollection()) {
          tableModel.setCollection(parent);
          return;
        }
      } catch (WebDavException e) {
        showError(e);
      }
    }
  };

  private final GPAction myUpAction = new GPAction("fileChooser.up") {
    @Override
    public void actionPerformed(ActionEvent event) {
      try {
        WebDavResource collection = tableModel.getCollection();
        WebDavResource parent = collection.getParent();
        if (parent != null && parent.exists() && parent.isCollection()) {
          tableModel.setCollection(parent);
          myPath.setValue(new URL(parent.getUrl()).getPath());
        }
      } catch (WebDavException e) {
        showError(e);
      } catch (MalformedURLException e) {
        showError(e);
      }
    }
  };

  private final FilesTableModel tableModel = new FilesTableModel();

  static interface SelectionListener {
    public void setSelection(WebDavResource resource);
  }

  GanttURLChooser(ListOption servers, String urlSpec, StringOption username, String password, IntegerOption lockTimeoutOption) {
    myPath = new DefaultStringOption("path");
    myServers = servers;
    myUsername = username;
    myPassword = new DefaultStringOption("password", password);
    myPassword.setScreened(true);
    myLock = new DefaultBooleanOption("lock", true);
    myTimeout = lockTimeoutOption;

    myServers.addChangeValueListener(new ChangeValueListener() {
      @Override
      public void changeValue(ChangeValueEvent event) {
        myPath.setValue("");
        myPassword.setValue("");
        myUsername.setValue("");
        boolean empty = "".equals(event.getNewValue());
        myReloadAction.setEnabled(!empty);
      }
    });
    myPath.addChangeValueListener(new ChangeValueListener() {
      @Override
      public void changeValue(ChangeValueEvent event) {
        String path = (String) event.getNewValue();
        myUpAction.setEnabled(path.split("/").length > 1);
      }
    });
    try {
      URL url = new URL(urlSpec);
      String host = MessageFormat.format("{0}://{1}{2}",url.getProtocol(), url.getHost(), url.getPort() == -1 ? "" : ":" + url.getPort());
      myServers.addValue(host);
      myServers.setValue(host);
      myPath.setValue(url.getPath());
    } catch (MalformedURLException e) {
      GPLogger.logToLogger(e);
    }
  }

  public JComponent createOpenDocumentUi() {
    return createComponent();
  }

  public JComponent createSaveDocumentUi() {
    return createComponent();
  }

  private String buildUrl() {
    String host = myServers.getValue();
    if (host.endsWith("/")) {
      host = host.substring(0, host.length() - 1);
    }
    String path = myPath.getValue();
    if (path.startsWith("/")) {
      path = path.substring(1);
    }
    return host + "/" + path;
  }

  private JComponent createComponent() {
    OptionsPageBuilder builder = new OptionsPageBuilder();
    final JComponent timeoutComponent = (JComponent) builder.createOptionComponent(null, myTimeout);

    JPanel panel = new JPanel(new SpringLayout());
    panel.add(new JLabel(language.getCorrectedLabel("webServer")));
    panel.add(builder.createOptionComponent(null, myServers));
    panel.add(new JLabel(language.getText("userName")));
    panel.add(builder.createOptionComponent(null, myUsername));
    panel.add(new JLabel(language.getText("password")));
    panel.add(builder.createOptionComponent(null, myPassword));

    addEmptyRow(panel);

    {
      JPanel filesTablePanel = new JPanel(new BorderLayout());
      JButton refreshButton = new JButton(myReloadAction);
      JButton upButton = new JButton(myUpAction);
      panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(myUpAction.getKeyStroke(), myUpAction);
      panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(myReloadAction.getKeyStroke(), myReloadAction);
      panel.getActionMap().put(myUpAction, myUpAction);
      panel.getActionMap().put(myReloadAction, myReloadAction);
      upButton.setText("");
      refreshButton.setText("");
      Box filesHeaderBox = Box.createHorizontalBox();
      filesHeaderBox.add(builder.createOptionComponent(null, myPath));
      filesHeaderBox.add(Box.createHorizontalStrut(5));
      filesHeaderBox.add(upButton);
      filesHeaderBox.add(Box.createHorizontalStrut(5));
      filesHeaderBox.add(refreshButton);
      filesHeaderBox.add(Box.createHorizontalGlue());
      filesTablePanel.add(filesHeaderBox, BorderLayout.NORTH);
      //panel.add(filesActionsPanel);
      final JXList table = new JXList(tableModel);
      table.setHighlighters(UIUtil.ZEBRA_HIGHLIGHTER);
      table.setCellRenderer(new FilesCellRenderer());
      table.addListSelectionListener(new ListSelectionListener() {
        @Override
        public void valueChanged(ListSelectionEvent e) {
          WebDavResource resource = (WebDavResource) table.getSelectedValue();
          if (resource == null) {
            return;
          }
          mySelectionListener.setSelection(resource);
          try {
            URL url = new URL(resource.getUrl());
            myPath.setValue(url.getPath());
          } catch (MalformedURLException ex) {
            GPLogger.logToLogger(ex);
          }
        }
      });
      table.addMouseListener(new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
          if (e.getClickCount() == 2) {
            tryEnterCollection(table, tableModel);
          }
        }
      });
      table.addKeyListener(new KeyAdapter() {
        @Override
        public void keyReleased(KeyEvent e) {
          if (e.getKeyCode() == KeyEvent.VK_ENTER) {
            tryEnterCollection(table, tableModel);
          }
        }
      });
      filesTablePanel.add(new JScrollPane(table), BorderLayout.CENTER);
      panel.add(new JLabel(language.getText("fileChooser.fileList")));
      panel.add(filesTablePanel);
    }

    addEmptyRow(panel);
//    panel.add(new JLabel(language.getText("webdav.lockResource.label")));
//    panel.add(lockComponent);
    panel.add(new JLabel(language.getText("webdav.lockTimeout.label")));

    myLock.addChangeValueListener(new ChangeValueListener() {
      @Override
      public void changeValue(ChangeValueEvent event) {
        timeoutComponent.setEnabled(myLock.isChecked());
      }
    });
    panel.add(timeoutComponent);
    SpringUtilities.makeCompactGrid(panel, 7, 2, 0, 0, 10, 5);

    JPanel properties = new JPanel(new BorderLayout());
    properties.add(panel, BorderLayout.NORTH);
    properties.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 0));

    return properties;
  }

  protected void tryEnterCollection(JXList table, FilesTableModel tableModel) {
    WebDavResource resource = (WebDavResource) table.getSelectedValue();
    try {
      if (resource.isCollection()) {
        tableModel.setCollection(resource);
      }
    } catch (WebDavException e) {
      showError(e);
    }
  }

  private static void addEmptyRow(JPanel form) {
    form.add(Box.createRigidArea(new Dimension(1, 10)));
    form.add(Box.createRigidArea(new Dimension(1, 10)));
  }

  String getUrl() {
    return buildUrl();
  }

  String getUsername() {
    return myUsername.getValue();
  }

  String getPassword() {
    return myPassword.getValue();
  }

  int getLockTimeout() {
    return myTimeout.getValue();
  }

  void setSelectionListener(SelectionListener selectionListener) {
    mySelectionListener = selectionListener;
  }

  StringOption getPathOption() {
    return myPath;
  }
  void showError(Exception e) {
    GPLogger.log(e);
  }
}
