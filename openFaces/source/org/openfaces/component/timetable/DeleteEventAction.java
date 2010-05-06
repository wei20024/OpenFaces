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
package org.openfaces.component.timetable;

import org.openfaces.util.ValueBindings;

import javax.faces.context.FacesContext;

/**
 * @author Dmitry Pikhulya
 */
public class DeleteEventAction extends EventAction {
    public static final String COMPONENT_TYPE = "org.openfaces.DeleteEventAction";
    public static final String COMPONENT_FAMILY = "org.openfaces.DeleteEventAction";

    private Boolean showConfirmation;

    @Override
    public String getFamily() {
        return COMPONENT_FAMILY;
    }

    @Override
    public Object saveState(FacesContext context) {
        return new Object[]{
                super.saveState(context),
                showConfirmation
        };
    }

    @Override
    public void restoreState(FacesContext context, Object stateObj) {
        Object[] state = (Object[]) stateObj;
        int i = 0;
        super.restoreState(context, state[i++]);
        showConfirmation = (Boolean) state[i++];
    }

    public boolean getShowConfirmation() {
        return ValueBindings.get(this, "showConfirmation", showConfirmation, true);
    }

    public void setShowConfirmation(boolean showConfirmation) {
        this.showConfirmation = showConfirmation;
    }

    @Override
    protected String getDefaultHint() {
        return "Delete";
    }

}
