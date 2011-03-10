/*
 * OpenFaces - JSF Component Library 2.0
 * Copyright (C) 2007-2011, TeamDev Ltd.
 * licensing@openfaces.org
 * Unless agreed in writing the contents of this file are subject to
 * the GNU Lesser General Public License Version 2.1 (the "LGPL" License).
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * Please visit http://openfaces.org/licensing/ for more details.
 */
package org.openfaces.renderkit.timetable;

import org.openfaces.component.LoadingMode;
import org.openfaces.component.panel.LayeredPane;
import org.openfaces.component.panel.SubPanel;
import org.openfaces.component.timetable.DayTable;
import org.openfaces.component.timetable.MonthTable;
import org.openfaces.component.timetable.Timetable;
import org.openfaces.component.timetable.TimetableView;
import org.openfaces.component.timetable.WeekTable;
import org.openfaces.util.AjaxUtil;
import org.openfaces.util.Components;
import org.openfaces.util.Faces;
import org.openfaces.util.Rendering;
import org.openfaces.util.Resources;
import org.openfaces.util.ScriptBuilder;
import org.openfaces.util.Styles;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TimetableRenderer extends TimetableRendererBase {

    @Override
    public void encodeBegin(FacesContext context, UIComponent component) throws IOException {
        ResponseWriter writer = context.getResponseWriter();
        Timetable timetable = (Timetable) component;

        if (!component.isRendered())
            return;

        Rendering.registerDateTimeFormatObject(timetable.getLocale());
        AjaxUtil.prepareComponentForAjax(context, timetable);

        timetable.setEvent(null);
        String clientId = timetable.getClientId(context);
        writer.startElement("table", timetable);
        writer.writeAttribute("id", clientId, "id");
        writer.writeAttribute("cellspacing", "0", null);
        writer.writeAttribute("cellpadding", "0", null);
        writer.writeAttribute("border", "0", null);
        writer.writeAttribute("class", Styles.getCSSClass(context,
                timetable, timetable.getStyle(), "o_timetableView", timetable.getStyleClass()), null);
        Rendering.writeStandardEvents(writer, timetable);
        writer.startElement("tbody", timetable);

        renderHeader(context, timetable);

        writer.startElement("tr", timetable);
        writer.writeAttribute("class", "o_timetableView_tableRow", null);
        writer.startElement("td", timetable);
        writer.writeAttribute("style", "height: 100%", null);

        LayeredPane layeredPane = getLayeredPane(timetable);
        layeredPane.encodeAll(context);

        List<String> viewIds = new ArrayList<String>();
        for (UIComponent c : layeredPane.getChildren()) {
            SubPanel subPanel = (SubPanel) c;
            if (subPanel.getChildCount() != 1) throw new IllegalStateException();
            TimetableView timetableView = (TimetableView) subPanel.getChildren().get(0);
            String viewId = timetableView.getClientId(context);
            viewIds.add(viewId);
        }

        Rendering.renderInitScript(context, new ScriptBuilder().initScript(context, timetable, "O$.Timetable._init",
                layeredPane,
                viewIds,
                timetable.getView()
        ), Resources.getInternalURL(context, "timetable/timetable.js"));

        writer.endElement("td");
        writer.endElement("tr");

        renderFooter(context, timetable);

        writer.endElement("tbody");
        writer.endElement("table");

        Styles.renderStyleClasses(context, timetable);


    }

    @Override
    public boolean getRendersChildren() {
        return true;
    }

    @Override
    public void encodeChildren(FacesContext context, UIComponent component) throws IOException {
    }

    private LayeredPane getLayeredPane(Timetable timetable) {
        LayeredPane layeredPane = Components.getChildWithClass(timetable, LayeredPane.class, "layeredPane");
        layeredPane.setStyle("width: 100%");
        layeredPane.setContainerStyle("padding: 0");
        layeredPane.setLoadingMode(LoadingMode.CLIENT);
        if (layeredPane.getChildCount() == 0) {
            List<UIComponent> children = layeredPane.getChildren();
            children.add(new SubPanel(null, timetable.getMonthView()));
            children.add(new SubPanel(null, timetable.getWeekView()));
            children.add(new SubPanel(null, timetable.getDayView()));
        }

        Timetable.View currentView = timetable.getView();
        List<UIComponent> children = layeredPane.getChildren();
        int viewIndex = 0;
        for (int i = 0, count = children.size(); i < count; i++) {
            SubPanel subPanel = (SubPanel) children.get(i);
            if (subPanel.getChildCount() != 1)
                throw new IllegalArgumentException("One child component expected, but was " + subPanel.getChildCount() + "; panel index: " + i);
            TimetableView viewInThisPanel = (TimetableView) subPanel.getChildren().get(0);
            Timetable.View viewType = getViewType(viewInThisPanel);
            if (viewType == currentView) {
                viewIndex = i;
                break;
            }

        }
        layeredPane.setSelectedIndex(viewIndex);
        return layeredPane;
    }

    private Timetable.View getViewType(TimetableView timetableView) {
        if (timetableView instanceof DayTable) return Timetable.View.DAY;
        if (timetableView instanceof WeekTable) return Timetable.View.WEEK;
        if (timetableView instanceof MonthTable) return Timetable.View.MONTH;
        throw new IllegalArgumentException("Unknown view type: " + timetableView.getClass().getName());
    }

    @Override
    public void decode(FacesContext context, UIComponent component) {
        super.decode(context, component);
        Timetable timetable = (Timetable) component;
        String viewStr = Faces.requestParam(timetable.getClientId(context) + Rendering.CLIENT_ID_SUFFIX_SEPARATOR + "view");
        Timetable.View view = Timetable.View.valueOf(viewStr.toUpperCase());
        timetable.setView(view);
    }
}
