package com.melisa.innovamotionapp.data;

import com.melisa.innovamotionapp.data.posture.Posture;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PostureDataLoader {
    public static List<Posture> loadPostures(File file) throws IOException {
        List<Posture> postures = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Posture posture = PostureFactory.createPosture(line); // Use the factory to create the posture
                postures.add(posture);
            }
        }

        return postures;
    }
}

