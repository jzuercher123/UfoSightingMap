package com.ufomap.ufosightingmap.data.repositories;

import com.ufomap.ufosightingmap.data.correlation.dao.PopulationDataDao;
import com.ufomap.ufosightingmap.data.correlation.dao.PopulationDensityDistribution;
import com.ufomap.ufosightingmap.data.correlation.models.PopulationData;

import kotlinx.coroutines.flow.Flow;
import android.content.Context;
import kotlinx.coroutines.CoroutineScope;
import timber.log.Timber;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import android.util.Log;

/**
 * Repository for population data.
 * Manages interactions between the data sources (database) and the rest of the app.
 */
public class PopulationDataRepositoryJava {
    private String TAG = "PopulationDataRepository";
    private PopulationDataDao populationDataDao;
    private Context context;

    public PopulationDataRepositoryJava(PopulationDataDao populationDataDao, Context context) {
        this.populationDataDao = populationDataDao;
        this.context = context;
    }

    /**
     * Get all population data from the database.
     * @return Flow emitting a list of all population data.
     * @throws Exception if there's an error fetching data.
     */
    public @NotNull Flow<@NotNull List<@NotNull PopulationData>> getAllPopulationData() {
        Timber.tag(TAG).d("getAllPopulationData");
        // Fetch data from the database
        // Replace this with your actual database query
        // For example:
        // return populationDataDao.getAllPopulationData()
        return populationDataDao.getAllPopulationData();
    }

    public @NotNull Flow<@NotNull List<@NotNull PopulationDensityDistribution>> getPopulationDensityDistribution() {
        Timber.tag(TAG).d("getPopulationDensityDistribution");
        return populationDataDao.getPopulationDensityDistribution();
    }

    /**
     * Initialize the population database with data if needed.
     * Checks count and staleness, then calls loadPopulationDataFromAssets.
     * @param coroutineScope
     * @throws Exception if there's an error initializing the database.
     */
    public void initializeDatabaseIfNeeded(CoroutineScope coroutineScope) {

    }

    /**
     * Force reload population data by clearing the DB and fetching from assets.
     */
    public void forceReloadData() {

    }

    /**
     * Load population data from assets into the database.
     */
    private void loadPopulationDataFromAssets() {

    }

    /**
     * Get population data for a specific year from the database.
     * @param year The year to fetch data for.
     * @return Flow emitting a list of population data for the specified year.
     */
    public Flow<@NotNull List<@NotNull PopulationData>> getPopulationDataByYear(int year) {
        Timber.tag(TAG).d("getPopulationDataByYear: %d", year);

        // Fetch data from the database
        // Replace this with your actual database query
        // For example:
        // return populationDataDao.getPopulationDataByYear(year)
        return populationDataDao.getPopulationDataByYear(year);
    }

    /**
     * Get population data for a specific state from the database.
     * @param state The state to fetch data for.
     * @return Flow emitting a list of population data for the specified state.
     */
    public Flow<@NotNull List<@NotNull PopulationData>> getPopulationDataByState(String state) {
        Timber.tag(TAG).d("getPopulationDataByState: %s", state);
        return populationDataDao.getPopulationDataByState(state);

    }

    /**
     * Get population data for a specific city from the database.
     * @param year
     * @return
     */
    public Flow<@NotNull List<@NotNull PopulationDensityDistribution>> getSightingsByPopulationDensity(int year) {
        Timber.tag(TAG).d("getSightingsByPopulationDensity: %d", year);
        return populationDataDao.getSightingsByPopulationDensity(year);
    }
}
