package com.ufomap.ufosightingmap.data.repositories;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.ufomap.ufosightingmap.data.correlation.dao.PopulationDataDao;
import com.ufomap.ufosightingmap.data.correlation.models.PopulationData;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import kotlinx.coroutines.flow.Flow;

/**
 * Repository for population data.
 * Manages data operations between the data sources (local database, JSON assets)
 * and the rest of the app.
 */
public class PopulationDataRepositoryJava {
    private static final String TAG = "PopulationDataRepo";
    private final PopulationDataDao populationDataDao;
    private final Context context;

    public PopulationDataRepositoryJava(PopulationDataDao populationDataDao, Context context) {
        this.populationDataDao = populationDataDao;
        this.context = context;
    }

    /**
     * Get all population data
     */
    public Flow<List<PopulationData>> getAllPopulationData() {
        return populationDataDao.getAllPopulationData();
    }

    /**
     * Get population data for a specific state
     */
    public Flow<List<PopulationData>> getPopulationDataByState(String state) {
        return populationDataDao.getPopulationDataByState(state);
    }

    /**
     * Get sightings grouped by population density categories
     */
    public Flow<List<PopulationDensityDistribution>> getSightingsByPopulationDensity(int year) {
        // Use the correct method from the DAO - this should exist in PopulationDataDao
        return populationDataDao.getSightingsByPopulationDensity(year);
    }

    /**
     * Load population data from JSON asset
     */
    public void loadPopulationDataFromAsset(String fileName) {
        try {
            String jsonString = readAssetFile(fileName);
            Log.d(TAG, "Successfully read population data JSON file");

            Type listType = new TypeToken<ArrayList<PopulationData>>() {}.getType();
            List<PopulationData> populationData = new Gson().fromJson(jsonString, listType);

            if (populationData != null && !populationData.isEmpty()) {
                // Insert data into database - would need to be done in a coroutine or async task
                Log.d(TAG, "Parsed " + populationData.size() + " population data records");
            } else {
                Log.e(TAG, "Failed to parse population data - null or empty");
            }
        } catch (IOException e) {
            Log.e(TAG, "Error reading population data JSON file: " + e.getMessage(), e);
        } catch (Exception e) {
            Log.e(TAG, "Error processing population data: " + e.getMessage(), e);
        }
    }

    /**
     * Helper method to read asset file content
     */
    private String readAssetFile(String fileName) throws IOException {
        return context.getAssets().open(fileName).bufferedReader().use(reader -> {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line);
            }
            return content.toString();
        });
    }

    /**
     * Get the average population density for areas with UFO sightings
     */
    public float getAveragePopulationDensityForSightings(int year) {
        // This would typically call a DAO method, but for now we'll return a placeholder
        return 150.0f; // Placeholder value
    }
}