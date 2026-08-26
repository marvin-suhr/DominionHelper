package dev.msuhr.dominionkingdoms.data

import android.util.Log
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {
    // Migration from version 2 to 3: Rename and copy data to preserve preferences
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {

            Log.d("DatabaseMigrations", "Migrating from version 2 to 3")

            // 1. Rename old table to preserve data
            db.execSQL("ALTER TABLE cards RENAME TO cards_old")

            // 2. Recreate the cards table with the new schema
            // id is now a primary key from JSON instead of auto-generated
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS cards (
                    id INTEGER PRIMARY KEY NOT NULL,
                    name TEXT NOT NULL,
                    imageName TEXT NOT NULL,
                    sets TEXT NOT NULL,
                    types TEXT NOT NULL,
                    categories TEXT NOT NULL,
                    cost INTEGER,
                    overpay INTEGER NOT NULL,
                    specialCost INTEGER NOT NULL,
                    potion INTEGER NOT NULL,
                    debt INTEGER,
                    supply INTEGER NOT NULL,
                    landscape INTEGER NOT NULL,
                    basic INTEGER NOT NULL,
                    isEnabled INTEGER NOT NULL,
                    isFavorite INTEGER NOT NULL
                )
                """.trimIndent()
            )

            // 3. Copy data from old to new.
            // Note: We copy the existing ID. CardDataUpdater will later reconcile
            // any ID shifts by matching cards by name.
            db.execSQL(
                """
                INSERT INTO cards (
                    id, name, imageName, sets, types, categories, cost,
                    overpay, specialCost, potion, debt, supply, landscape,
                    basic, isEnabled, isFavorite
                )
                SELECT
                    id, name, imageName, sets, types, categories, cost,
                    overpay, specialCost, potion, debt, supply, landscape,
                    basic, isEnabled, isFavorite
                FROM cards_old
                """.trimIndent()
            )

            // 4. Drop old table
            db.execSQL("DROP TABLE IF EXISTS cards_old")
        }
    }
}
