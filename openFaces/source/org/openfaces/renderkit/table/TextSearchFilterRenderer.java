/*
 * OpenFaces - JSF Component Library 2.0
 * Copyright (C) 2007-2009, TeamDev Ltd.
 * licensing@openfaces.org
 * Unless agreed in writing the contents of this file are subject to
 * the GNU Lesser General Public License Version 2.1 (the "LGPL" License).
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * Please visit http://openfaces.org/licensing/ for more details.
 */
package org.openfaces.renderkit.table;

import org.openfaces.component.table.AbstractTable;
import org.openfaces.component.table.Filter;
import org.openfaces.component.table.TextSearchFilter;
import org.openfaces.util.ResourceUtil;
import org.openfaces.util.StyleUtil;

import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import java.io.IOException;

/**
 * @author Dmitry Pikhulya
 */
public abstract class TextSearchFilterRenderer extends FilterRenderer {

    @Override
    public boolean getRendersChildren() {
        return true;
    }

    @Override
    public void encodeChildren(FacesContext context, UIComponent component) throws IOException {
    }

    @Override
    public void encodeBegin(FacesContext context, UIComponent component) throws IOException {
        super.encodeBegin(context, component);
        ResourceUtil.renderJSLinkIfNeeded(ResourceUtil.getUtilJsURL(context), context);
        TextSearchFilter filter = (TextSearchFilter) component;

        UIInput inputComponent = (UIInput) filter.getSearchComponent();
        inputComponent.setValue(getStringValue(filter));
        configureInputComponent(context, filter, inputComponent);

        inputComponent.encodeAll(context);

        StyleUtil.renderStyleClasses(context, component);
    }

    protected abstract void configureInputComponent(FacesContext context, Filter filter, UIInput inputComponent);

    @Override
    public void decode(FacesContext context, UIComponent component) {
        super.decode(context, component);
        TextSearchFilter filter = (TextSearchFilter) component;
        UIInput input = (UIInput) filter.getSearchComponent();
        String newSearchString = (String) input.getSubmittedValue();
        if (newSearchString == null) {
            newSearchString = "";
        }
        setDecodedString(filter, newSearchString);
    }

    protected String getFilterOnEnterScript(FacesContext context, Filter filter) {
        AbstractTable table = (AbstractTable) filter.getFilteredComponent();
        String tableId = table.getClientId(context);
        return "return O$.Table._filterFieldKeyPressHandler('" + tableId + "', this, event);";
    }

    protected String getStringValue(Filter filter) {
        return ((TextSearchFilter) filter).getStringValue();
    }
}
