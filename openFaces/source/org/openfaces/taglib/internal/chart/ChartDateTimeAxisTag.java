/*
 * OpenFaces - JSF Component Library 3.0
 * Copyright (C) 2007-2010, TeamDev Ltd.
 * licensing@openfaces.org
 * Unless agreed in writing the contents of this file are subject to
 * the GNU Lesser General Public License Version 2.1 (the "LGPL" License).
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * Please visit http://openfaces.org/licensing/ for more details.
 */
package org.openfaces.taglib.internal.chart;

import org.openfaces.component.chart.ChartDateAxis;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

/**
 * @author Pavel Kaplin
 */
public class ChartDateTimeAxisTag extends ChartAxisTag {
    @Override
    public String getComponentType() {
        return "org.openfaces.ChartDateAxis";
    }

    @Override
    public String getRendererType() {
        return null;
    }

    @Override
    public void setComponentProperties(FacesContext facesContext, UIComponent uiComponent) {
        super.setComponentProperties(facesContext, uiComponent);

        ChartDateAxis axis = (ChartDateAxis) uiComponent;

        setObjectProperty(axis, "lowerBound");
        setObjectProperty(axis, "upperBound");
        setObjectProperty(axis, "dateFormat");
    }
}
