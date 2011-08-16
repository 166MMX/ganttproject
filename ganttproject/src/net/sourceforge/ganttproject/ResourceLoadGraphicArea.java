/*
GanttProject is an opensource project management tool.
Copyright (C) 2002-2011 Thomas Alexandre, GanttProject team

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package net.sourceforge.ganttproject;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;

import javax.swing.Action;
import javax.swing.Icon;

import net.sourceforge.ganttproject.chart.ChartModelBase;
import net.sourceforge.ganttproject.chart.ChartModelResource;
import net.sourceforge.ganttproject.chart.ChartSelection;
import net.sourceforge.ganttproject.chart.ChartViewState;
import net.sourceforge.ganttproject.chart.ResourceChart;
import net.sourceforge.ganttproject.chart.export.RenderedChartImage;
import net.sourceforge.ganttproject.font.Fonts;
import net.sourceforge.ganttproject.gui.zoom.ZoomManager;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.resource.HumanResourceManager;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.time.gregorian.GregorianCalendar;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

/**
 * Class for the graphic part of the soft
 */
public class ResourceLoadGraphicArea extends ChartComponentBase implements
        ResourceChart {


    /** Render the ganttproject version */
    private boolean drawVersion = false;

    /** The main application */
    private final GanttProject appli;

    private final ChartModelResource myChartModel;

    private final ChartViewState myViewState;

    public ResourceLoadGraphicArea(GanttProject app, ZoomManager zoomManager) {
        super(app.getProject(), app.getUIFacade(), zoomManager);
        this.setBackground(Color.WHITE);
        myChartModel = new ChartModelResource(getTaskManager(),
                (HumanResourceManager) app.getHumanResourceManager(),
                getTimeUnitStack(), getUIConfiguration(), (ResourceChart) this);
        myChartImplementation = new ResourcechartImplementation(app.getProject(), myChartModel, this);
        myViewState = new ChartViewState(this, app.getUIFacade());
        super.setStartDate(GregorianCalendar.getInstance().getTime());
        appli = app;
        //myTableHeader = app.getResourcePanel().table.getTableHeader();
    }

    /** @return the preferred size of the panel. */
    @Override
    public Dimension getPreferredSize() {
        return new Dimension(465, 600);
    }

    protected int getRowHeight() {
        return appli.getResourcePanel().table.getRowHeight();
    }

    public void drawGPVersion(Graphics g) {
        g.setColor(Color.black);
        g.setFont(Fonts.GP_VERSION_FONT);
        g.drawString("GanttProject (" + GanttProject.version + ")", 3,
                getHeight() - 8);
    }

    public BufferedImage getChart(GanttExportSettings settings) {
        RenderedChartImage renderedImage = (RenderedChartImage) getRenderedImage(settings);
        BufferedImage result = renderedImage.getWholeImage();
        repaint();
        return result;
    }

    public RenderedImage getRenderedImage(GanttExportSettings settings) {
        settings.setRowCount(getResourceManager().getResources().size());
        return getRenderedImage(settings, appli.getResourcePanel().getResourceTreeTable());
    }

    @Override
    public String getName() {
        return GanttLanguage.getInstance().getText("resourcesChart");
    }
//
//    public Date getStartDate() {
//        // return this.beg.getTime();
//        return getTaskManager().getProjectStart();
//    }
//
//    public Date getEndDate() {
//        return getTaskManager().getProjectEnd();
//    }

    @Override
    protected ChartModelBase getChartModel() {
        return myChartModel;
    }

    @Override
    protected MouseListener getMouseListener() {
        if (myMouseListener == null) {
            myMouseListener = new MouseListenerBase() {
                @Override
                protected Action[] getPopupMenuActions() {
                    return new Action[] { getOptionsDialogAction()};
                }

            };
        }
        return myMouseListener;
    }

    @Override
    protected MouseMotionListener getMouseMotionListener() {
        if (myMouseMotionListener == null) {
            myMouseMotionListener = new MouseMotionListenerBase();
        }
        return myMouseMotionListener;
    }

    @Override
    protected AbstractChartImplementation getImplementation() {
        return myChartImplementation;
    }

    public boolean isExpanded(HumanResource resource) {
        return true;
    }

    private MouseMotionListener myMouseMotionListener;

    private MouseListener myMouseListener;

    private final AbstractChartImplementation myChartImplementation;

    public void setTaskManager(TaskManager taskManager) {
        // TODO Auto-generated method stub
    }

    public void reset() {
        repaint();
    }

    public Icon getIcon() {
        // TODO Auto-generated method stub
        return null;
    }

    private class ResourcechartImplementation extends AbstractChartImplementation {

        public ResourcechartImplementation(
                IGanttProject project, ChartModelBase chartModel, ChartComponentBase chartComponent) {
            super(project, chartModel, chartComponent);
            // TODO Auto-generated constructor stub
        }
        @Override
        public void paintChart(Graphics g) {
            synchronized (ChartModelBase.STATIC_MUTEX) {
                // LaboPM
                // ResourceLoadGraphicArea.super.paintComponent(g);
                if (isShowing()) {
                    myChartModel.setHeaderHeight(getImplementation().getHeaderHeight(
                        appli.getResourcePanel(), appli.getResourcePanel().table.getTable()));
                }
                myChartModel.setBottomUnitWidth(getViewState().getBottomUnitWidth());
                myChartModel.setRowHeight(getRowHeight());// myChartModel.setRowHeight(tree.getJTree().getRowHeight());
                myChartModel.setTopTimeUnit(getViewState().getTopTimeUnit());
                myChartModel.setBottomTimeUnit(getViewState().getBottomTimeUnit());
                //myChartModel.paint(g);
                super.paintChart(g);

                if (drawVersion) {
                    drawGPVersion(g);
                }
            }
        }
        @Override
        public ChartSelection getSelection() {
            ChartSelectionImpl result = new ChartSelectionImpl() {
                @Override
                public boolean isEmpty() {
                    return false;
                }
                @Override
                public void startCopyClipboardTransaction() {
                    super.startCopyClipboardTransaction();
                    appli.getResourcePanel().copySelection();
                }

                @Override
                public void startMoveClipboardTransaction() {
                    super.startMoveClipboardTransaction();
                    appli.getResourcePanel().cutSelection();
                }
            };
            return result;
        }
        @Override
        public IStatus canPaste(ChartSelection selection) {
            return Status.OK_STATUS;
        }
        @Override
        public void paste(ChartSelection selection) {
            appli.getResourcePanel().pasteSelection();
        }
    }

    @Override
    public ChartViewState getViewState() {
        return myViewState;
    }

    private HumanResourceManager getResourceManager() {
        return appli.getHumanResourceManager();
    }
}