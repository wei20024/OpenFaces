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
package org.openfaces.component.chart.impl.plots;

import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.renderer.xy.AbstractXYItemRenderer;
import org.jfree.data.xy.XYDataset;
import org.openfaces.component.chart.Chart;
import org.openfaces.component.chart.ChartAxis;
import org.openfaces.component.chart.ChartNumberAxis;
import org.openfaces.component.chart.GridChartView;
import org.openfaces.component.chart.impl.helpers.NumberAxis3DAdapter;
import org.openfaces.component.chart.impl.helpers.NumberAxisAdapter;

/**
 * @author Ekaterina Shliakhovetskaya
 */
public class GridXYPlotAdapter extends XYPlotAdapter {

    public GridXYPlotAdapter(XYDataset ds, AbstractXYItemRenderer renderer,
                             Chart chart, GridChartView chartView) {
        super(ds, renderer, chart, chartView);
    }

    public ValueAxis getDomainAxisAdapter() {
        final GridChartView view = getChartView();
        ChartAxis baseAxis = view.getBaseAxis();
        ChartNumberAxis keyAxis = (view.getKeyAxis() instanceof ChartNumberAxis)
                ? (ChartNumberAxis) view.getKeyAxis()
                : null;

        return !view.isEnable3D()
                ? new NumberAxisAdapter(view.getKeyAxisLabel(), isKeyAxisVisible(), keyAxis, baseAxis, view)
                : new NumberAxis3DAdapter(getChartView().getKeyAxisLabel(), isKeyAxisVisible(), keyAxis, baseAxis, view);
    }

    public ValueAxis getRangeAxisAdapter() {
        final GridChartView view = getChartView();
        ChartAxis baseAxis = view.getBaseAxis();
        ChartNumberAxis valueAxis = (view.getValueAxis() instanceof ChartNumberAxis)
                ? (ChartNumberAxis) view.getValueAxis()
                : null;

        return !view.isEnable3D()
                ? new NumberAxisAdapter(view.getValueAxisLabel(), isValueAxisVisible(), valueAxis, baseAxis, view)
                : new NumberAxis3DAdapter(getChartView().getValueAxisLabel(), isValueAxisVisible(), valueAxis, baseAxis, view);
    }
}

