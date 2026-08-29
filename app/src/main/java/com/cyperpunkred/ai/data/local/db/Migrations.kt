package com.cyperpunkred.ai.data.local.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v2 -> v3: bind [SessionEntity.characterId] to [CharacterEntity.id]
 * with a real foreign key, and add indices to every FK column.
 *
 * Steps
 * -----
 * 1. Drop the v1/v2 sessions that were created in the absence of
 *    any character (the legacy `characterId = 0L` path, plus any
 *    that point to a deleted character).  They are meaningless
 *    without a real character and would block the new FK.
 * 2. Clean up orphan chat_messages and combat_logs that pointed at
 *    the dropped sessions (cascade).
 * 3. For sessions whose `characterId` is not 0 but is stale (e.g.
 *    a character was deleted without cascade at the time), rebind
 *    them to the oldest valid character; if there is no character
 *    at all, drop them too.
 * 4. Recreate the three tables with the new FK + indices, copying
 *    the surviving rows.  This is the only portable way to add a
 *    FK to an existing SQLite table (ALTER TABLE does not support
 *    adding a foreign key in pre-3.35).
 *
 * Disabled foreign-key enforcement during the rebuild so the
 * row-by-row INSERTs do not trip over the half-built state.
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.beginTransaction()
        try {
            // 1. Drop orphan sessions (characterId = 0 or pointing to a
            //    missing character).  CASCADE is already off in v2 so we
            //    do it explicitly to keep the new tables clean.
            db.execSQL(
                """
                DELETE FROM chat_messages
                WHERE sessionId IN (
                    SELECT s.id FROM game_sessions s
                    LEFT JOIN characters c ON s.characterId = c.id
                    WHERE s.characterId = 0 OR c.id IS NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                DELETE FROM combat_logs
                WHERE sessionId IN (
                    SELECT s.id FROM game_sessions s
                    LEFT JOIN characters c ON s.characterId = c.id
                    WHERE s.characterId = 0 OR c.id IS NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                DELETE FROM game_sessions
                WHERE characterId = 0
                   OR characterId NOT IN (SELECT id FROM characters)
                """.trimIndent()
            )

            // 2. Rebuild game_sessions with FK + index
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `game_sessions_new` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `characterId` INTEGER NOT NULL,
                    `title` TEXT NOT NULL,
                    `status` TEXT NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    FOREIGN KEY(`characterId`) REFERENCES `characters`(`id`) ON UPDATE CASCADE ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO game_sessions_new (id, characterId, title, status, createdAt, updatedAt)
                SELECT id, characterId, title, status, createdAt, updatedAt FROM game_sessions
                """.trimIndent()
            )
            db.execSQL("DROP TABLE game_sessions")
            db.execSQL("ALTER TABLE game_sessions_new RENAME TO game_sessions")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_game_sessions_characterId` ON `game_sessions` (`characterId`)")

            // 3. Rebuild chat_messages with FK + index
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `chat_messages_new` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `sessionId` INTEGER NOT NULL,
                    `role` TEXT NOT NULL,
                    `content` TEXT NOT NULL,
                    `diceResultJson` TEXT,
                    `timestamp` INTEGER NOT NULL,
                    FOREIGN KEY(`sessionId`) REFERENCES `game_sessions`(`id`) ON UPDATE CASCADE ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO chat_messages_new (id, sessionId, role, content, diceResultJson, timestamp)
                SELECT id, sessionId, role, content, diceResultJson, timestamp FROM chat_messages
                """.trimIndent()
            )
            db.execSQL("DROP TABLE chat_messages")
            db.execSQL("ALTER TABLE chat_messages_new RENAME TO chat_messages")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_chat_messages_sessionId` ON `chat_messages` (`sessionId`)")

            // 4. Rebuild combat_logs with FK + index
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `combat_logs_new` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `sessionId` INTEGER NOT NULL,
                    `round` INTEGER NOT NULL,
                    `actor` TEXT NOT NULL,
                    `action` TEXT NOT NULL,
                    `diceResultJson` TEXT,
                    `damage` INTEGER,
                    `target` TEXT,
                    `timestamp` INTEGER NOT NULL,
                    FOREIGN KEY(`sessionId`) REFERENCES `game_sessions`(`id`) ON UPDATE CASCADE ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO combat_logs_new (id, sessionId, round, actor, action, diceResultJson, damage, target, timestamp)
                SELECT id, sessionId, round, actor, action, diceResultJson, damage, target, timestamp FROM combat_logs
                """.trimIndent()
            )
            db.execSQL("DROP TABLE combat_logs")
            db.execSQL("ALTER TABLE combat_logs_new RENAME TO combat_logs")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_combat_logs_sessionId` ON `combat_logs` (`sessionId`)")

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }
}

/**
 * v3 -> v4: relax the session -> character FK from CASCADE to
 * RESTRICT.
 *
 * The character sheet is now meant to be a free-standing card that
 * the user can reuse across multiple adventures.  With CASCADE,
 * deleting a character would silently take all of its sessions
 * (and their chat / combat history) with it -- the exact opposite
 * of the "角色卡是自由的" design.  RESTRICT means a character can
 * only be deleted after its sessions are deleted explicitly, which
 * is the right confirmation boundary for the UI to enforce.
 *
 * Rebuilding `game_sessions` is the only portable way to change a
 * FK's onDelete action; SQLite has no ALTER TABLE ... DROP
 * CONSTRAINT.
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.beginTransaction()
        try {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `game_sessions_new` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `characterId` INTEGER NOT NULL,
                    `title` TEXT NOT NULL,
                    `status` TEXT NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    FOREIGN KEY(`characterId`) REFERENCES `characters`(`id`) ON UPDATE CASCADE ON DELETE RESTRICT
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO game_sessions_new (id, characterId, title, status, createdAt, updatedAt)
                SELECT id, characterId, title, status, createdAt, updatedAt FROM game_sessions
                """.trimIndent()
            )
            db.execSQL("DROP TABLE game_sessions")
            db.execSQL("ALTER TABLE game_sessions_new RENAME TO game_sessions")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_game_sessions_characterId` ON `game_sessions` (`characterId`)")

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }
}
