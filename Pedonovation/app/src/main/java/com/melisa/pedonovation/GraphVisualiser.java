package com.melisa.pedonovation;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.LegendRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

public class GraphVisualiser {

    public GraphView graph;
    private final double graphTimeSpeed = 0.050;


    public GraphVisualiser(GraphView graph) {
        this.graph = graph;
    }

    public void putInGraphDataLine(double[] dataList, int colorLine, String legendName) {

        DataPoint[] dataPoints = new DataPoint[dataList.length];

        // Create DataPoints for GraphView
        double time = 0;

        for (int i = 0; i < dataList.length; i++) {
            dataPoints[i] = new DataPoint(time, dataList[i]);
            time += graphTimeSpeed;
        }

        LineGraphSeries<DataPoint> dataLine = new LineGraphSeries<>(dataPoints);
        dataLine.setColor(colorLine);

        graph.addSeries(dataLine);

// legend
        dataLine.setTitle(legendName);
        // Make legend label background invisible
        graph.getLegendRenderer().setBackgroundColor(24);
        graph.getLegendRenderer().setVisible(true);
        graph.getLegendRenderer().setAlign(LegendRenderer.LegendAlign.TOP);
    }

}
