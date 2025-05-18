package com.example.jupitertheaterapp.database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Main database class for the Jupiter Theater application
 * Contains all entities and manages database connection
 */
@Database(entities = {Show.class, Booking.class, Review.class, Discount.class}, version = 1, exportSchema = false)
@TypeConverters({Converters.class})
public abstract class TheaterDatabase extends RoomDatabase {

    // Single DAO that handles all operations
    public abstract TheaterDao theaterDao();

    // Singleton instance
    private static volatile TheaterDatabase INSTANCE;
    
    // Thread pool for database operations
    private static final int NUMBER_OF_THREADS = 4;
    public static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    /**
     * Get the database instance (singleton pattern)
     */
    public static TheaterDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (TheaterDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            TheaterDatabase.class, "theater_database")
                            .addCallback(sRoomDatabaseCallback)
                            .fallbackToDestructiveMigration() // Force replace DB when schema changes
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * Callback for database creation
     * Initializes the database with sample data on first creation
     */
    private static RoomDatabase.Callback sRoomDatabaseCallback = new RoomDatabase.Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);

            // Populate database in the background when app first launches
            databaseWriteExecutor.execute(() -> {
                // Get database instance and DAO
                Context appContext = TheaterApplication.getAppContext();
                if (appContext != null) {
                    TheaterDatabase database = TheaterDatabase.getDatabase(appContext);
                    
                    // Load sample data
                    new DataLoader(appContext).loadSampleData(database);
                }
            });
        }
    };
}
