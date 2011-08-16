/*
 * Created on 01.05.2005
 */
package net.sourceforge.ganttproject.export;

import java.awt.event.ActionEvent;

import net.sourceforge.ganttproject.GanttOptions;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.gui.UIFacade;

/**
 * @author bard
 */
public class ExportFileAction extends GPAction {
    private final IGanttProject myProject;

    private final UIFacade myUIFacade;

    private final GanttOptions myOptions;

    public ExportFileAction(UIFacade uiFacade, IGanttProject project,
            GanttOptions options) {
        super(null, "16");
        myProject = project;
        myUIFacade = uiFacade;
        myOptions = options;
    }

    @Override
    protected String getIconFilePrefix() {
        return "export_";
    }

    public void actionPerformed(ActionEvent e) {
        ExportFileWizardImpl wizard = new ExportFileWizardImpl(myUIFacade,
                myProject, myOptions);
        wizard.show();
    }

    @Override
    protected String getLocalizedName() {
        return getI18n("export");
    }
}
