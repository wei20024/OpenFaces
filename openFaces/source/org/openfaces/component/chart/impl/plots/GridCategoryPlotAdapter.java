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

import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.labels.CategoryToolTipGenerator;
import org.jfree.chart.labels.StandardCategoryItemLabelGenerator;
import org.jfree.chart.plot.CategoryCrosshairState;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.renderer.category.AbstractCategoryItemRenderer;
import org.jfree.chart.renderer.category.CategoryItemRenderer;
import org.jfree.chart.renderer.category.CategoryItemRendererState;
import org.jfree.chart.urls.CategoryURLGenerator;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.general.DatasetUtilities;
import org.jfree.util.SortOrder;
import org.openfaces.component.chart.Chart;
import org.openfaces.component.chart.ChartAxis;
import org.openfaces.component.chart.ChartCategoryAxis;
import org.openfaces.component.chart.ChartDomain;
import org.openfaces.component.chart.ChartNumberAxis;
import org.openfaces.component.chart.GridChartView;
import org.openfaces.component.chart.impl.PropertiesConverter;
import org.openfaces.component.chart.impl.generators.DynamicCategoryGenerator;
import org.openfaces.component.chart.impl.helpers.CategoryAxis3DAdapter;
import org.openfaces.component.chart.impl.helpers.CategoryAxisAdapter;
import org.openfaces.component.chart.impl.helpers.NumberAxis3DAdapter;
import org.openfaces.component.chart.impl.helpers.NumberAxisAdapter;
import org.openfaces.component.chart.impl.helpers.SelectionUtil;
import org.openfaces.component.chart.impl.renderers.AreaFillRenderer;

import java.awt.*;
import java.awt.geom.Rectangle2D;

/**
 * @author Ekaterina Shliakhovetskaya
 */
public class GridCategoryPlotAdapter extends CategoryPlot {

    public GridCategoryPlotAdapter(CategoryDataset ds, AbstractCategoryItemRenderer renderer,
                                   Chart chart, GridChartView chartView) {
        setDataset(ds);
        setRenderer(renderer);

        ChartAxis baseAxis = chartView.getBaseAxis();
        ChartAxis keyAxis = chartView.getKeyAxis();
        ChartAxis valueAxis = chartView.getValueAxis();
        ChartDomain showAxes = chartView.getShowAxes();
        if (showAxes == null) {
            showAxes = ChartDomain.BOTH;
            chartView.setShowAxes(showAxes);
        }
        boolean keyAxisVisible = showAxes.equals(ChartDomain.BOTH) || showAxes.equals(ChartDomain.KEY);
        boolean valueAxisVisible = showAxes.equals(ChartDomain.BOTH) || showAxes.equals(ChartDomain.VALUE);

        if (!(keyAxis instanceof ChartCategoryAxis))
            keyAxis = null;

        if (!(valueAxis instanceof ChartNumberAxis))
            valueAxis = null;

        CategoryAxis categoryAxis = chartView.isEnable3D()
                ? new CategoryAxis3DAdapter(chartView.getKeyAxisLabel(), keyAxisVisible, (ChartCategoryAxis) keyAxis, baseAxis, chartView)
                : new CategoryAxisAdapter(chartView.getKeyAxisLabel(), keyAxisVisible, (ChartCategoryAxis) keyAxis, baseAxis, chartView);
        NumberAxis numberAxis = chartView.isEnable3D()
                ? new NumberAxis3DAdapter(chartView.getValueAxisLabel(), valueAxisVisible, (ChartNumberAxis) valueAxis, baseAxis, chartView)
                : new NumberAxisAdapter(chartView.getValueAxisLabel(), valueAxisVisible, (ChartNumberAxis) valueAxis, baseAxis, chartView);

        if (ds == null) {
            categoryAxis.setVisible(false);
            numberAxis.setVisible(false);
        }

        setDomainAxis(categoryAxis);
        setRangeAxis(numberAxis);

        PlotUtil.initGridLabels(chartView, renderer);

        if (chartView.getLabels() != null && chartView.getLabels().getText() != null) {
            //   renderer.setItemLabelGenerator(new LabelGenerator(myGridChartRenderer.getLabels().getTextDynamicProperty()));
        } else
            renderer.setBaseItemLabelGenerator(new StandardCategoryItemLabelGenerator());

        PlotUtil.setupColorProperties(chart, this);
        setOrientation(PropertiesConverter.toPlotOrientation(chartView.getOrientation()));
        PlotUtil.setupGridLinesProperties(chartView, this, ds);
        setupTooltips(chartView, renderer);
        setupUrls(chartView, renderer);
        SelectionUtil.setupSelectionHighlighting(this, chart, chartView);
    }

    @Override
    public boolean render(Graphics2D g2, Rectangle2D dataArea, int index,
                          PlotRenderingInfo info, CategoryCrosshairState crossHairState) {
        final boolean customRenderingModeEnabled = getRenderer() != null && getRenderer() instanceof AreaFillRenderer;

        if (!customRenderingModeEnabled) {
            return super.render(g2, dataArea, index, info, crossHairState);
        } else {
            CategoryItemRenderer categoryItemRenderer = getRenderer(index);
            CategoryDataset categoryDataset = getDataset(index);

            boolean isDataSetNotEmpty = !DatasetUtilities.isEmptyOrNull(categoryDataset);
            boolean isAscendingRowOrdering = getRowRenderingOrder() == SortOrder.ASCENDING;
            boolean isRendered = false;

            if (isDataSetNotEmpty && categoryItemRenderer != null) {
                CategoryItemRendererState rendererState = categoryItemRenderer.initialise(g2, dataArea, this, index, info);
                rendererState.setCrosshairState(crossHairState);
                int totalRows = categoryDataset.getRowCount();

                if (isAscendingRowOrdering) {
                    for (int currentRowIndex = 0; currentRowIndex < totalRows; currentRowIndex++) {
                        renderColumns(g2, rendererState, dataArea, categoryItemRenderer, categoryDataset,
                                index, currentRowIndex);
                    }
                } else {
                    for (int currentRowIndex = totalRows - 1; currentRowIndex >= 0; currentRowIndex--) {
                        renderColumns(g2, rendererState, dataArea, categoryItemRenderer, categoryDataset,
                                index, currentRowIndex);
                    }
                }
                isRendered = true;
            }

            return isRendered;
        }
    }

    private void renderColumns(Graphics2D g2, CategoryItemRendererState state, Rectangle2D dataArea,
                               CategoryItemRenderer renderer, CategoryDataset currentDataSet, int index, int row) {
        boolean isAscendingColumnOrder = getColumnRenderingOrder() == SortOrder.ASCENDING;
        CategoryAxis categoryAxis = getDomainAxisForDataset(index);
        ValueAxis valueAxis = getRangeAxisForDataset(index);
        int totalRendererPasses = renderer.getPassCount();
        int totalColumns = currentDataSet.getColumnCount();

        for (int currentPassIndex = 0; currentPassIndex < totalRendererPasses; currentPassIndex++) {
            if (isAscendingColumnOrder) {
                for (int columnIndex = 0; columnIndex < totalColumns; columnIndex++) {
                    final boolean isLastColumn = columnIndex == totalColumns - 1;
                    renderColumn(g2, state, dataArea, renderer, currentDataSet, categoryAxis, valueAxis,
                            row, currentPassIndex, columnIndex, isLastColumn);
                }
            } else {
                for (int columnIndex = totalColumns - 1; columnIndex >= 0; columnIndex--) {
                    final boolean isLastColumn = columnIndex == 0;
                    renderColumn(g2, state, dataArea, renderer, currentDataSet, categoryAxis, valueAxis,
                            row, currentPassIndex, columnIndex, isLastColumn);
                }
            }
        }
    }

    private void renderColumn(Graphics2D g2, CategoryItemRendererState state, Rectangle2D dataArea,
                              CategoryItemRenderer renderer, CategoryDataset currentDataSet,
                              CategoryAxis categoryAxis, ValueAxis valueAxis,
                              int row, int currentPassIndex, int columnIndex, boolean lastColumn) {
        renderer.drawItem(g2, state, dataArea, this, categoryAxis, valueAxis, currentDataSet,
                row, columnIndex, currentPassIndex);

        if (lastColumn) {
            ((AreaFillRenderer) renderer).completePass(g2, state, dataArea, this,
                    categoryAxis, valueAxis, currentDataSet, row, currentPassIndex);
        }
    }

    private void setupTooltips(final GridChartView chartView, AbstractCategoryItemRenderer renderer) {
        if (chartView.getTooltip() != null) {
            renderer.setBaseToolTipGenerator(new CategoryToolTipGenerator() {
                public String generateToolTip(CategoryDataset categoryDataset, int i, int i1) {
                    return chartView.getTooltip();
                }
            });
        } else if (chartView.getDynamicTooltip() != null) {
            renderer.setBaseToolTipGenerator(new DynamicCategoryGenerator(chartView, chartView.getDynamicTooltip()));
        }
    }

    private void setupUrls(final GridChartView chartView, AbstractCategoryItemRenderer renderer) {
        if (chartView.getUrl() != null) {
            renderer.setItemURLGenerator(new CategoryURLGenerator() {
                public String generateURL(CategoryDataset categoryDataset, int i, int i1) {
                    return chartView.getUrl();
                }
            });
        } else if (chartView.getDynamicUrl() != null) {
            renderer.setItemURLGenerator(new DynamicCategoryGenerator(chartView, chartView.getDynamicUrl()));
        }
    }
}
