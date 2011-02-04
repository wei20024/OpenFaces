/*
 * OpenFaces - JSF Component Library 3.0
 * Copyright (C) 2007-2011, TeamDev Ltd.
 * licensing@openfaces.org
 * Unless agreed in writing the contents of this file are subject to
 * the GNU Lesser General Public License Version 2.1 (the "LGPL" License).
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * Please visit http://openfaces.org/licensing/ for more details.
 */
package org.openfaces.renderkit.chart;

import org.openfaces.component.chart.ChartMenu;
import org.openfaces.component.chart.PrintChartMenuItem;
import org.openfaces.component.chart.SaveChartMenuItem;
import org.openfaces.renderkit.command.PopupMenuRenderer;
<<<<<<< HEAD
import org.openfaces.util.Resources;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ComponentSystemEvent;
import javax.faces.event.ComponentSystemEventListener;
import javax.faces.event.ListenerFor;
import javax.faces.event.PostAddToViewEvent;
import java.io.IOException;
import java.util.List;

@ListenerFor(systemEventClass = PostAddToViewEvent.class)
public class ChartMenuRenderer extends PopupMenuRenderer implements ComponentSystemEventListener {
    public void processEvent(ComponentSystemEvent event) throws AbortProcessingException {
        if (event instanceof PostAddToViewEvent)
            Resources.includeJQuery();
    }

=======

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import java.io.IOException;
import java.util.List;

public class ChartMenuRenderer extends PopupMenuRenderer {
>>>>>>> master
    @Override
    public void encodeBegin(FacesContext context, UIComponent component) throws IOException {
        ChartMenu menu = (ChartMenu) component;
        if (menu.getChildren().size() == 0) {
            List<UIComponent> menuItems = menu.getChildren();
            menuItems.add(new SaveChartMenuItem());
            menuItems.add(new PrintChartMenuItem());
        }

        super.encodeBegin(context, component);
    }

<<<<<<< HEAD
=======

>>>>>>> master
}
