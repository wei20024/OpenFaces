/*
 * OpenFaces - JSF Component Library 2.0
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

import org.jfree.chart.labels.ItemLabelPosition;
import org.jfree.chart.labels.StandardCategoryItemLabelGenerator;
import org.jfree.chart.labels.StandardXYItemLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.AbstractRenderer;
import org.jfree.chart.renderer.category.AbstractCategoryItemRenderer;
import org.jfree.chart.renderer.xy.AbstractXYItemRenderer;
import org.jfree.data.general.Dataset;
import org.jfree.ui.TextAnchor;
import org.openfaces.component.chart.Chart;
import org.openfaces.component.chart.ChartDomain;
import org.openfaces.component.chart.ChartGridLines;
import org.openfaces.component.chart.ChartLabelPosition;
import org.openfaces.component.chart.ChartLabels;
import org.openfaces.component.chart.GridChartView;
import org.openfaces.component.chart.impl.PropertiesConverter;
import org.openfaces.renderkit.cssparser.CSSUtil;
import org.openfaces.renderkit.cssparser.StyleBorderModel;
import org.openfaces.renderkit.cssparser.StyleObjectModel;

import java.awt.*;
import java.util.List;

/**
 * @author Ekaterina Shliakhovetskaya
 */
class PlotUtil {

    private PlotUtil() {
    }

    public static void setupColorProperties(Chart chart, Plot plot) {
        StyleObjectModel cssChartViewModel = chart.getChartView().getStyleObjectModel();

        if (chart.getChartView().getBackgroundPaint() != null) {
            plot.setBackgroundPaint(null);
        } else {
            plot.setBackgroundPaint(cssChartViewModel.getBackground());
        }

        StyleBorderModel border = cssChartViewModel.getBorder();
        plot.setOutlinePaint(border == null || border.isNone()
                ? cssChartViewModel.getBackground()
                : border.getColor());
    }

    public static void setupGridLinesProperties(GridChartView view, Plot plot, Dataset dataset) {
        if (dataset == null) {
            setDomainGridlinesVisible(plot, false);
            setRangeGridlinesVisible(plot, false);
            return;
        }

        List<ChartGridLines> gridLines = view.getGridLines();
        if (gridLines == null || gridLines.size() <= 0)
            return;

        ChartGridLines viewBaseGrid = view.getBaseGrid();
        if (viewBaseGrid != null) {
            StyleObjectModel cssStyle = viewBaseGrid.getStyleObjectModel();
            if (cssStyle != null &&
                    cssStyle.getBorder() != null &&
                    cssStyle.getBorder().getColor() != null &&
                    !cssStyle.getBorder().getStyle().equalsIgnoreCase("none")) {

                setDomainGridlinesVisible(plot, true);
                setDomainGridlineStrokeAndPaint(plot, PropertiesConverter.toStroke(cssStyle.getBorder()), cssStyle.getBorder().getColor());

                setRangeGridlinesVisible(plot, true);
                setRangeGridlineStrokeAndPaint(plot, PropertiesConverter.toStroke(cssStyle.getBorder()), cssStyle.getBorder().getColor());
            }
        } else {
            setDomainGridlinesVisible(plot, false);
            setRangeGridlinesVisible(plot, false);
        }

        for (ChartGridLines lines : gridLines) {
            StyleObjectModel cssStyle = lines.getStyleObjectModel();
            if (lines.getDomain().equals(ChartDomain.KEY)) {
                if (cssStyle != null &&
                        cssStyle.getBorder() != null &&
                        cssStyle.getBorder().getColor() != null &&
                        !cssStyle.getBorder().getStyle().equalsIgnoreCase("none")) {

                    setDomainGridlinesVisible(plot, true);
                    setDomainGridlineStrokeAndPaint(plot, PropertiesConverter.toStroke(cssStyle.getBorder()), cssStyle.getBorder().getColor());
                }

            } else if (lines.getDomain().equals(ChartDomain.VALUE)) {
                if (cssStyle != null &&
                        cssStyle.getBorder() != null &&
                        cssStyle.getBorder().getColor() != null &&
                        !cssStyle.getBorder().getStyle().equalsIgnoreCase("none")) {

                    setRangeGridlinesVisible(plot, true);
                    setRangeGridlineStrokeAndPaint(plot, PropertiesConverter.toStroke(cssStyle.getBorder()), cssStyle.getBorder().getColor());
                }
            }
        }

    }

    private static void setDomainGridlinesVisible(Plot plot, boolean value) {
        if (plot instanceof CategoryPlot)
            ((CategoryPlot) plot).setDomainGridlinesVisible(value);
        else if (plot instanceof XYPlot)
            ((XYPlot) plot).setDomainGridlinesVisible(value);
        else
            throw new IllegalArgumentException("Unknown plot type: " + plot.getClass().getName());
    }

    private static void setRangeGridlinesVisible(Plot plot, boolean value) {
        if (plot instanceof CategoryPlot)
            ((CategoryPlot) plot).setRangeGridlinesVisible(value);
        else if (plot instanceof XYPlot)
            ((XYPlot) plot).setRangeGridlinesVisible(value);
        else
            throw new IllegalArgumentException("Unknown plot type: " + plot.getClass().getName());
    }

    private static void setDomainGridlineStrokeAndPaint(Plot plot, Stroke stroke, Paint paint) {
        if (plot instanceof CategoryPlot) {
            ((CategoryPlot) plot).setDomainGridlinePaint(paint);
            ((CategoryPlot) plot).setDomainGridlineStroke(stroke);
        } else if (plot instanceof XYPlot) {
            ((XYPlot) plot).setDomainGridlinePaint(paint);
            ((XYPlot) plot).setDomainGridlineStroke(stroke);
        } else
            throw new IllegalArgumentException("Unknown plot type: " + plot.getClass().getName());
    }

    private static void setRangeGridlineStrokeAndPaint(Plot plot, Stroke stroke, Paint paint) {
        if (plot instanceof CategoryPlot) {
            ((CategoryPlot) plot).setRangeGridlinePaint(paint);
            ((CategoryPlot) plot).setRangeGridlineStroke(stroke);
        } else if (plot instanceof XYPlot) {
            ((XYPlot) plot).setRangeGridlinePaint(paint);
            ((XYPlot) plot).setRangeGridlineStroke(stroke);
        } else
            throw new IllegalArgumentException("Unknown plot type: " + plot.getClass().getName());
    }


    public static void initGridLabels(GridChartView chartView, AbstractRenderer renderer) {
        boolean isLabelsVisible = chartView.isLabelsVisible();
        if (isLabelsVisible) {
            renderer.setBaseItemLabelsVisible(true);

            setGenerator(renderer);
            defaultInit(chartView, renderer);
            colorInit(chartView, renderer);

        } else {
            renderer.setBaseItemLabelsVisible(false);
        }
    }

    private static ItemLabelPosition createItemLabelPosition(ChartLabelPosition labelPosition) {
        return new ItemLabelPosition(labelPosition.getLabelAnchor(), TextAnchor.CENTER);
    }

    private static void setGenerator(AbstractRenderer renderer) {
        if (renderer instanceof AbstractCategoryItemRenderer) {
            ((AbstractCategoryItemRenderer) renderer).setBaseItemLabelGenerator(new StandardCategoryItemLabelGenerator());
        } else if (renderer instanceof AbstractXYItemRenderer) {
            ((AbstractXYItemRenderer) renderer).setBaseItemLabelGenerator(new StandardXYItemLabelGenerator());
        }

    }

    private static void colorInit(GridChartView chartView, AbstractRenderer renderer) {
        StyleObjectModel cssViewModel = chartView.getStyleObjectModel();

        ChartLabels labels = chartView.getLabels();

        if (labels != null) {
            StyleObjectModel cssLabelsModel = labels.getStyleObjectModel();
            renderer.setBaseItemLabelPaint(cssLabelsModel.getColor());
            renderer.setBaseItemLabelFont(CSSUtil.getFont(cssLabelsModel));

        } else {
            renderer.setBaseItemLabelPaint(cssViewModel.getColor());
            renderer.setBaseItemLabelFont(CSSUtil.getFont(cssViewModel));
        }

    }

    private static void defaultInit(GridChartView chartView, AbstractRenderer renderer) {
        final ChartLabelPosition positiveLabelsPosition = chartView.getPositiveLabelsPosition();
        final ChartLabelPosition negativeLabelsPosition = chartView.getNegativeLabelsPosition();
        final ChartLabelPosition defaultLabelPosition = chartView.getDefaultLabelsPosition();

        if (defaultLabelPosition != null && positiveLabelsPosition == null) {
            renderer.setBasePositiveItemLabelPosition(createItemLabelPosition(defaultLabelPosition));
        } else if (positiveLabelsPosition != null) {
            renderer.setBasePositiveItemLabelPosition(createItemLabelPosition(positiveLabelsPosition));
        }

        if (defaultLabelPosition != null && negativeLabelsPosition == null) {
            renderer.setBaseNegativeItemLabelPosition(createItemLabelPosition(defaultLabelPosition));
        } else if (negativeLabelsPosition != null) {
            renderer.setBaseNegativeItemLabelPosition(createItemLabelPosition(negativeLabelsPosition));
        }

        renderer.setItemLabelAnchorOffset(chartView.getLabelsOffset());
    }

}
