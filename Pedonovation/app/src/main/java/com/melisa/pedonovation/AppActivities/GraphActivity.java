package com.melisa.pedonovation.AppActivities;

import static android.content.ContentValues.TAG;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.melisa.pedonovation.GraphVisualiser;
import com.melisa.pedonovation.PedometricSole.PedometricHelper;
import com.melisa.pedonovation.Utilities;
import com.melisa.pedonovation.databinding.ActivityGraphsBinding;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GraphActivity extends AppCompatActivity {

    private ActivityGraphsBinding binding;

    private List<GraphVisualiser> accelerometerGraphs;
    private List<GraphVisualiser> capacitiveGraphs;

    // Names for line data legend
    private final String legendLineData1 = "Dispozitiv 1";
    private final String legendLineData2 = "Dispozitiv 2";

    // Colors for graphs
    private final int colorLine1 = Color.GREEN;
    private final int colorLine2 = Color.rgb(200, 155, 255);

    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGraphsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        accelerometerGraphs = new ArrayList<>(Arrays.asList(
                new GraphVisualiser(binding.graphAcc1),
                new GraphVisualiser(binding.graphAcc2),
                new GraphVisualiser(binding.graphAcc3)
        ));
        capacitiveGraphs = new ArrayList<>(Arrays.asList(
                new GraphVisualiser(binding.graphPresure1),
                new GraphVisualiser(binding.graphPresure2),
                new GraphVisualiser(binding.graphPresure3)
        ));


        File externalAppStoragePath = this.getExternalMediaDirs()[0];

        String fileName1 = String.format(Utilities.FILE_NAME_FORMAT, "1");
        File file1 = new File(externalAppStoragePath, fileName1);

        String fileName2 = String.format(Utilities.FILE_NAME_FORMAT, "2");
        File file2 = new File(externalAppStoragePath, fileName2);

        List<double[]> dataListGraphDev1 = extractData(file1);
        List<double[]> dataListGraphDev2 = extractData(file2);

        GenerateGraphData(dataListGraphDev1, dataListGraphDev2);
    }


    public List<double[]> extractData(File dataFile) {

        PedometricHelper pedometricHelper = new PedometricHelper();

        String data = "";
        if (dataFile.exists()) {
            data = readFromFile(dataFile);
        } else {
//            Toast.makeText(this, "no file" + dataFile.getName(), Toast.LENGTH_SHORT).show();
            return null;
        }

        List<String> lines = pedometricHelper.textToArrayList(data);

        // Contains double[] for x values, double[] for y values, double[] for z values
        //  we ll need them to draw 3 lines
        List<double[]> dataListGraphDev = new ArrayList<>(Arrays.asList(
                // acc data x,y,z
                new double[lines.size()],
                new double[lines.size()],
                new double[lines.size()],
                // cap data s1, s2, s3
                new double[lines.size()],
                new double[lines.size()],
                new double[lines.size()]
        ));

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);

            String[] strValues = line.split(" ");

            // Generate accelerometer data
            // divide with 256 to restrain the range of the values (ex.: to show values [-1,1] not bigger than 100
            double accValue1 = (double) Integer.parseInt(strValues[0]) / 256;
            double accValue2 = (double) Integer.parseInt(strValues[1]) / 256;
            double accValue3 = (double) Integer.parseInt(strValues[2]) / 256;

            // Put in list the accelerometer data
            dataListGraphDev.get(0)[i] = accValue1;
            dataListGraphDev.get(1)[i] = accValue2;
            dataListGraphDev.get(2)[i] = accValue3;

            // Generate capacitive data
            double capValue1 = Double.parseDouble(strValues[3]);
            double capValue2 = Double.parseDouble(strValues[4]);
            double capValue3 = Double.parseDouble(strValues[5]);

            // Put in list the capacitive data
            dataListGraphDev.get(3)[i] = capValue1;
            dataListGraphDev.get(4)[i] = capValue2;
            dataListGraphDev.get(5)[i] = capValue3;
        }

        return dataListGraphDev;
    }

    public String readFromFile(File file) {
        StringBuilder content = new StringBuilder();

        try (FileInputStream reader = new FileInputStream(file);
             InputStreamReader inputStreamReader = new InputStreamReader(reader);
             BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                content.append(line).append('\n');
            }
        } catch (Exception e) {
            Log.d(TAG, String.format("[LOG] %s", e));
        }

        return content.toString();
    }


    private void GenerateGraphData(List<double[]> dataListGraphDev1, List<double[]> dataListGraphDev2) {

        for (int i = 0; i < accelerometerGraphs.size(); i++) {
            GraphVisualiser accGraph = accelerometerGraphs.get(i);

            if (dataListGraphDev1 != null) {
                accGraph.putInGraphDataLine(dataListGraphDev1.get(i), colorLine1, legendLineData1);
            }
            if (dataListGraphDev2 != null) {
                accGraph.putInGraphDataLine(dataListGraphDev2.get(i), colorLine2, legendLineData2);
            }

            accGraph.graph.getViewport().setXAxisBoundsManual(true);
            accGraph.graph.getViewport().setMinX(0);
            accGraph.graph.getViewport().setMaxX(2);
            accGraph.graph.getViewport().setScalable(true);
        }

        for (int i = 0; i < capacitiveGraphs.size(); i++) {
            GraphVisualiser capGraph = capacitiveGraphs.get(i);

            if (dataListGraphDev1 != null) {
                capGraph.putInGraphDataLine(dataListGraphDev1.get(i + 3), colorLine1, legendLineData1);
            }
            if (dataListGraphDev2 != null) {
                capGraph.putInGraphDataLine(dataListGraphDev2.get(i + 3), colorLine2, legendLineData2);
            }

            capGraph.graph.getViewport().setXAxisBoundsManual(true);
            capGraph.graph.getViewport().setYAxisBoundsManual(true);
            capGraph.graph.getViewport().setMinX(0);
            capGraph.graph.getViewport().setMaxX(2);
            capGraph.graph.getViewport().setMinY(0);
            capGraph.graph.getViewport().setMaxY(1);
            capGraph.graph.getViewport().setScalable(true);
        }


    }

}